package com.skofqq.domainmanager.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.HttpUrl

/**
 * Rolling in-memory buffer of the last router API requests for the Diagnostics
 * screen. The token query parameter is masked at record time, so the secret never
 * reaches this buffer — entries are safe to display, copy and share as-is.
 */
object ApiLog {

    private const val MAX_ENTRIES = 20
    private const val MAX_BODY_CHARS = 2000
    private const val TOKEN_MASK = "•••"

    data class Entry(
        val timeMillis: Long,
        /** Full request URL with the token already masked. */
        val url: String,
        /** HTTP status, or null when the request failed before a response. */
        val code: Int?,
        /** Response body (truncated), or null on transport failure. */
        val body: String?,
        val durationMs: Long,
        /** Exception summary for transport failures. */
        val error: String?,
    )

    private val entries = ArrayDeque<Entry>()
    private val _flow = MutableStateFlow<List<Entry>>(emptyList())

    /** Newest first. */
    val flow: StateFlow<List<Entry>> = _flow

    @Synchronized
    fun record(url: HttpUrl, code: Int?, body: String?, durationMs: Long, error: String?) {
        entries.addFirst(
            Entry(
                timeMillis = System.currentTimeMillis(),
                url = mask(url),
                code = code,
                body = body?.take(MAX_BODY_CHARS),
                durationMs = durationMs,
                error = error,
            )
        )
        while (entries.size > MAX_ENTRIES) entries.removeLast()
        _flow.value = entries.toList()
    }

    private fun mask(url: HttpUrl): String =
        if (url.queryParameter("token") != null) {
            url.newBuilder().setQueryParameter("token", TOKEN_MASK).build().toString()
        } else url.toString()
}
