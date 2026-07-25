package com.skofqq.domainmanager.data

import android.content.Context
import android.util.JsonReader
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * One file of the live MetaCubeX/meta-rules-dat catalog. Nothing about the entry
 * is hardcoded per category — [behavior] and [format] are derived from where the
 * file sits and what its real extension is, and both stay editable in the UI in
 * case the guess is wrong for some unusual file.
 */
data class CatalogEntry(
    /** File name exactly as it is in the repo, e.g. "category-ai-!cn.mrs". */
    val name: String,
    /** [SOURCE_GEOSITE] (domain lists) or [SOURCE_GEOIP] (address lists). */
    val source: String,
) {
    /** Ready-to-send `url=` value for provider_add. */
    val url: String get() = rawUrlFor(source, name)

    /** Directory decides the rule type: geosite holds domains, geoip holds CIDRs. */
    val behavior: String get() = if (source == SOURCE_GEOIP) "ipcidr" else "domain"

    /** Real extension, not an assumption — the repo carries .mrs, .yaml and .list side by side. */
    val format: String
        get() = when (name.substringAfterLast('.', "").lowercase()) {
            "mrs" -> "mrs"
            "yaml", "yml" -> "yaml"
            else -> "text"
        }
}

const val SOURCE_GEOSITE = "geosite"
const val SOURCE_GEOIP = "geoip"

/** A catalog snapshot plus when it was fetched, so the UI can say how old it is. */
data class RuleCatalog(val entries: List<CatalogEntry>, val fetchedAt: Long)

sealed class CatalogFailure {
    /** HTTP 403/429 — GitHub's unauthenticated limit is 60 requests per hour per IP. */
    data object RateLimited : CatalogFailure()
    data class HttpError(val code: Int) : CatalogFailure()
    data class Network(val kind: NetFailure, val detail: String?) : CatalogFailure()
}

sealed class RuleCatalogResult {
    /** Straight from GitHub; the cache has just been rewritten. */
    data class Fresh(val catalog: RuleCatalog) : RuleCatalogResult()

    /**
     * Served from the local cache. [failure] is null when the cache was simply
     * still fresh enough to use, and non-null when a refresh was attempted and
     * failed — the caller shows the list anyway, flagged as not updated.
     */
    data class Cached(val catalog: RuleCatalog, val failure: CatalogFailure?) : RuleCatalogResult()

    /** Nothing cached and the fetch failed — the only case with no list to show. */
    data class Failed(val failure: CatalogFailure) : RuleCatalogResult()
}

/**
 * The rule-set catalog, read live from the repository rather than hardcoded, so
 * categories added upstream show up without an app update.
 *
 * Uses the Git **Trees** API, not the contents API the docs point at first:
 * `/contents/{dir}` silently caps a directory listing at 1000 entries, and both
 * geo directories are far past that (geosite alone has ~9400 files), so the
 * contents API would return a truncated catalog while looking successful. The
 * trees endpoint returns the whole `geo` subtree — both directories — in ONE
 * request, which also halves the cost against the 60/hour unauthenticated limit.
 *
 * Because the trees API carries no `download_url`, raw URLs are built here with
 * [rawUrlFor], which reproduces GitHub's own encoding (verified against the
 * contents API's download_url for every file in the repo).
 */
class RuleCatalogStore(context: Context) {

    private val cacheFile = File(context.filesDir, CACHE_FILE)

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Returns the catalog, hitting the network only when the cache is missing or
     * older than [CACHE_TTL_MS] — or when [force] is set by the explicit refresh
     * button. Blocking; call from an IO dispatcher.
     */
    fun load(force: Boolean = false): RuleCatalogResult {
        val cached = readCache()
        if (!force && cached != null && System.currentTimeMillis() - cached.fetchedAt < CACHE_TTL_MS) {
            return RuleCatalogResult.Cached(cached, failure = null)
        }
        return when (val fetched = fetch()) {
            is FetchOutcome.Ok -> {
                val catalog = RuleCatalog(fetched.entries, System.currentTimeMillis())
                writeCache(catalog)
                RuleCatalogResult.Fresh(catalog)
            }
            // A failed refresh must never throw away a usable cache.
            is FetchOutcome.Err ->
                if (cached != null) RuleCatalogResult.Cached(cached, fetched.failure)
                else RuleCatalogResult.Failed(fetched.failure)
        }
    }

    private sealed class FetchOutcome {
        data class Ok(val entries: List<CatalogEntry>) : FetchOutcome()
        data class Err(val failure: CatalogFailure) : FetchOutcome()
    }

    private fun fetch(): FetchOutcome {
        return try {
            val request = Request.Builder()
                .url(TREE_URL)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                // GitHub rejects requests without a User-Agent.
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(request).execute().use { response ->
                if (response.code == 403 || response.code == 429) {
                    return FetchOutcome.Err(CatalogFailure.RateLimited)
                }
                if (!response.isSuccessful) {
                    return FetchOutcome.Err(CatalogFailure.HttpError(response.code))
                }
                val body = response.body
                    ?: return FetchOutcome.Err(
                        CatalogFailure.Network(NetFailure.EMPTY_RESPONSE, null)
                    )
                // Streamed: the answer is ~3 MB of JSON and only two fields per
                // node matter — never materialize it as a String or a tree.
                FetchOutcome.Ok(parseTree(body.charStream()))
            }
        } catch (e: Exception) {
            FetchOutcome.Err(CatalogFailure.Network(classifyCatalog(e), e.message))
        }
    }

    private fun parseTree(source: java.io.Reader): List<CatalogEntry> {
        val entries = mutableListOf<CatalogEntry>()
        JsonReader(source).use { reader ->
            reader.beginObject()
            while (reader.hasNext()) {
                if (reader.nextName() != "tree") {
                    reader.skipValue()
                    continue
                }
                reader.beginArray()
                while (reader.hasNext()) {
                    reader.beginObject()
                    var path: String? = null
                    var type: String? = null
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "path" -> path = reader.nextString()
                            "type" -> type = reader.nextString()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                    // Paths are relative to geo/, i.e. "geosite/youtube.mrs".
                    if (type == "blob" && path != null) {
                        val dir = path.substringBefore('/')
                        val name = path.substringAfter('/')
                        if (name.isNotEmpty() && !name.contains('/') &&
                            (dir == SOURCE_GEOSITE || dir == SOURCE_GEOIP)
                        ) {
                            entries += CatalogEntry(name = name, source = dir)
                        }
                    }
                }
                reader.endArray()
            }
            reader.endObject()
        }
        return entries.sortedWith(compareBy({ it.source }, { it.name }))
    }

    // --- Cache ------------------------------------------------------------------

    /**
     * Plain text, one `dir/name` per line after a timestamp header — a JSON array
     * of 10k+ objects would cost far more to write and re-parse than this, and
     * the URL is derivable from the path anyway.
     */
    private fun writeCache(catalog: RuleCatalog) {
        try {
            cacheFile.bufferedWriter().use { out ->
                out.write(catalog.fetchedAt.toString())
                out.newLine()
                catalog.entries.forEach { entry ->
                    out.write(entry.source)
                    out.write("/")
                    out.write(entry.name)
                    out.newLine()
                }
            }
        } catch (_: Exception) {
            // A cache we couldn't write is not worth failing the load over.
        }
    }

    private fun readCache(): RuleCatalog? {
        return try {
            if (!cacheFile.exists()) return null
            cacheFile.bufferedReader().use { reader ->
                val fetchedAt = reader.readLine()?.toLongOrNull() ?: return null
                val entries = mutableListOf<CatalogEntry>()
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) continue
                    val dir = line.substringBefore('/')
                    val name = line.substringAfter('/')
                    if (name.isNotEmpty()) entries += CatalogEntry(name = name, source = dir)
                }
                if (entries.isEmpty()) null else RuleCatalog(entries, fetchedAt)
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        /**
         * `meta:geo` is the tree of the geo directory on the meta branch;
         * recursive=1 brings geosite and geoip back together.
         */
        private const val TREE_URL =
            "https://api.github.com/repos/MetaCubeX/meta-rules-dat/git/trees/meta:geo?recursive=1"
        private const val USER_AGENT = "Helm-Android"
        private const val CACHE_FILE = "rule_catalog.txt"

        /** Upstream adds categories rarely; once a day is plenty and keeps well clear of the rate limit. */
        private const val CACHE_TTL_MS = 24L * 60 * 60 * 1000
    }
}

private const val RAW_BASE = "https://raw.githubusercontent.com/MetaCubeX/meta-rules-dat/meta/geo"

/** Ready-to-use raw URL for one catalog file. */
fun rawUrlFor(source: String, name: String): String = "$RAW_BASE/$source/${encodePathSegment(name)}"

/**
 * Percent-encodes one path segment exactly the way the GitHub API does in its own
 * `download_url`: RFC 3986 unreserved characters and sub-delims stay literal,
 * everything else — `@` and `:` included — is encoded. The repo really does
 * contain both forms ("acer@cn.mrs" → "acer%40cn.mrs", "category-ai-!cn.mrs"
 * unchanged), and encoding anything unexpected is the safe default.
 */
internal fun encodePathSegment(segment: String): String {
    val out = StringBuilder(segment.length)
    for (byte in segment.toByteArray(Charsets.UTF_8)) {
        val ch = byte.toInt().toChar()
        if (ch.isLetterOrDigit() && ch.code < 128 || ch in UNRESERVED_EXTRA) {
            out.append(ch)
        } else {
            out.append('%').append("%02X".format(byte.toInt() and 0xFF))
        }
    }
    return out.toString()
}

/** unreserved ("-._~") plus sub-delims — the characters GitHub leaves alone. */
private const val UNRESERVED_EXTRA = "-._~!$&'()*+,;="

private fun classifyCatalog(e: Exception): NetFailure = when (e) {
    is java.net.SocketTimeoutException -> NetFailure.TIMEOUT
    is java.net.UnknownHostException -> NetFailure.UNKNOWN_HOST
    is java.net.ConnectException, is java.net.NoRouteToHostException -> NetFailure.UNREACHABLE
    else -> NetFailure.OTHER
}
