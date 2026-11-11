package com.skofqq.domainmanager.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.skofqq.domainmanager.update.UpdateInstallReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/** One GitHub release that carries an installable APK. */
data class AppRelease(
    /** Normalized version, without the leading "v" ("1.7.0"). */
    val version: String,
    val notes: String,
    val apkUrl: String,
    val apkName: String,
    val apkSize: Long,
    val pageUrl: String,
)

/** Result of installing a session, posted by [UpdateInstallReceiver]. */
sealed interface InstallEvent {
    /** The system confirmation dialog was shown. */
    data object AwaitingUser : InstallEvent
    data object Success : InstallEvent
    /** The user dismissed the system dialog. */
    data object Aborted : InstallEvent
    /** Signature mismatch with the installed app (e.g. a debug build). */
    data object Conflict : InstallEvent
    data class Failed(val message: String?) : InstallEvent
}

/**
 * In-app updates from the project's GitHub releases: checks `releases/latest`,
 * downloads the attached APK and hands it to [PackageInstaller]. Talks to GitHub
 * directly (not through the router), so it works on any network.
 *
 * Once Helm has installed an update itself it becomes the installer of record and
 * later updates go through without the system confirmation dialog (API 31+).
 */
class AppUpdater(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    var autoCheck: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CHECK, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CHECK, value).apply()

    private var lastCheckAt: Long
        get() = prefs.getLong(KEY_LAST_CHECK, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CHECK, value).apply()

    val isCheckDue: Boolean
        get() = autoCheck && System.currentTimeMillis() - lastCheckAt > CHECK_INTERVAL_MS

    val installedVersion: String by lazy {
        runCatching { app.packageManager.getPackageInfo(app.packageName, 0).versionName }
            .getOrNull().orEmpty()
    }

    private val installedVersionCode: Long by lazy {
        runCatching {
            val info = app.packageManager.getPackageInfo(app.packageName, 0)
            if (Build.VERSION.SDK_INT >= 28) info.longVersionCode
            else @Suppress("DEPRECATION") info.versionCode.toLong()
        }.getOrDefault(0L)
    }

    /** Latest release if it is newer than the installed version, null when up to date. Throws on network errors. */
    suspend fun fetchNewerRelease(): AppRelease? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(LATEST_URL)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", USER_AGENT)
            .build()
        val body = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("GitHub HTTP ${response.code}")
            response.body?.string() ?: throw IOException("Empty response")
        }
        lastCheckAt = System.currentTimeMillis()
        val json = JSONObject(body)
        val version = json.optString("tag_name").removePrefix("v").removePrefix("V")
        val assets = json.optJSONArray("assets")
        var apk: JSONObject? = null
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name").endsWith(".apk", ignoreCase = true)) { apk = a; break }
            }
        }
        if (version.isEmpty() || apk == null) return@withContext null
        if (compareVersions(version, installedVersion) <= 0) return@withContext null
        AppRelease(
            version = version,
            notes = json.optString("body").trim(),
            apkUrl = apk.getString("browser_download_url"),
            apkName = apk.optString("name"),
            apkSize = apk.optLong("size"),
            pageUrl = json.optString("html_url"),
        )
    }

    /** The APK already downloaded for [release], if it is complete. */
    fun downloadedApk(release: AppRelease): File? =
        apkFile(release).takeIf { it.exists() && (release.apkSize <= 0 || it.length() == release.apkSize) }

    /** Downloads the release APK, reporting progress 0..1. Returns the verified file. */
    suspend fun download(release: AppRelease, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(app.cacheDir, DIR).apply { mkdirs() }
        // Only one update is ever kept around.
        dir.listFiles()?.forEach { it.delete() }
        val target = apkFile(release)
        val partial = File(dir, target.name + ".part")
        val request = Request.Builder().url(release.apkUrl).header("User-Agent", USER_AGENT).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty response")
            val total = body.contentLength().takeIf { it > 0 } ?: release.apkSize
            body.byteStream().use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    var lastReported = -1
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        done += n
                        if (total > 0) {
                            val percent = (done * 100 / total).toInt()
                            if (percent != lastReported) {
                                lastReported = percent
                                onProgress(done.toFloat() / total)
                            }
                        }
                    }
                }
            }
        }
        if (!partial.renameTo(target)) throw IOException("Could not save the download")
        if (!isValidUpdate(target)) {
            target.delete()
            throw InvalidApkException()
        }
        target
    }

    /** The file must be an APK of this very app with a higher versionCode. */
    private fun isValidUpdate(file: File): Boolean {
        val info = runCatching {
            app.packageManager.getPackageArchiveInfo(file.path, 0)
        }.getOrNull() ?: return false
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode
        else @Suppress("DEPRECATION") info.versionCode.toLong()
        return info.packageName == app.packageName && code > installedVersionCode
    }

    /** Whether Android lets Helm install packages ("Install unknown apps" for this app). */
    fun canInstall(): Boolean = app.packageManager.canRequestPackageInstalls()

    /** Streams [apk] into a PackageInstaller session; the outcome arrives via [installEvents]. */
    suspend fun install(apk: File) = withContext(Dispatchers.IO) {
        val installer = app.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(app.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= 31) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
            if (Build.VERSION.SDK_INT >= 33) {
                setPackageSource(PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE)
            }
        }
        val sessionId = installer.createSession(params)
        try {
            installer.openSession(sessionId).use { session ->
                session.openWrite("helm.apk", 0, apk.length()).use { out ->
                    apk.inputStream().use { it.copyTo(out) }
                    session.fsync(out)
                }
                val callback = PendingIntent.getBroadcast(
                    app,
                    sessionId,
                    Intent(app, UpdateInstallReceiver::class.java).setPackage(app.packageName),
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        // The installer fills in the status extras, so the intent must be mutable.
                        (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0),
                )
                session.commit(callback.intentSender)
            }
        } catch (e: Exception) {
            runCatching { installer.abandonSession(sessionId) }
            throw e
        }
    }

    private fun apkFile(release: AppRelease) =
        File(File(app.cacheDir, DIR), "Helm-${release.version}.apk")

    class InvalidApkException : IOException("Not a valid update APK")

    companion object {
        const val RELEASES_URL = "https://github.com/skofqq/Domain-Manager-App/releases"
        private const val LATEST_URL = "https://api.github.com/repos/skofqq/Domain-Manager-App/releases/latest"
        private const val USER_AGENT = "Helm-Android"
        private const val PREFS = "app_update"
        private const val KEY_AUTO_CHECK = "auto_check"
        private const val KEY_LAST_CHECK = "last_check_at"
        private const val DIR = "updates"
        private const val CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000

        private val _installEvents = MutableStateFlow<InstallEvent?>(null)

        /** Process-wide, since the receiver has no reference to the ViewModel. */
        val installEvents: StateFlow<InstallEvent?> = _installEvents

        internal fun post(event: InstallEvent) {
            _installEvents.value = event
        }

        fun consumeInstallEvent() {
            _installEvents.value = null
        }

        /** Numeric dot-separated comparison; anything after "-" or "+" is ignored. */
        fun compareVersions(a: String, b: String): Int {
            fun parts(v: String) = v.substringBefore('-').substringBefore('+')
                .split('.').map { it.toIntOrNull() ?: 0 }
            val pa = parts(a)
            val pb = parts(b)
            for (i in 0 until maxOf(pa.size, pb.size)) {
                val c = pa.getOrElse(i) { 0 }.compareTo(pb.getOrElse(i) { 0 })
                if (c != 0) return c
            }
            return 0
        }
    }
}
