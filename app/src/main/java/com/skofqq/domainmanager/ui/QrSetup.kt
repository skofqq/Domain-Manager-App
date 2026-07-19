package com.skofqq.domainmanager.ui

import android.net.Uri

/**
 * Connection settings carried by a setup QR code, as a routerdomains://setup URI.
 * The QR contains the TOKEN in clear text — the dialog showing it must say so.
 */
data class SetupPayload(
    val host: String,
    val port: Int?,
    val token: String,
    val fallbackHost: String,
    val fallbackPort: Int?,
)

fun buildSetupUri(
    host: String,
    port: String,
    token: String,
    fallbackHost: String,
    fallbackPort: String,
): String = Uri.Builder()
    .scheme("routerdomains")
    .authority("setup")
    .appendQueryParameter("host", host)
    .appendQueryParameter("port", port)
    .appendQueryParameter("token", token)
    .apply {
        if (fallbackHost.isNotBlank()) {
            appendQueryParameter("fbhost", fallbackHost)
            appendQueryParameter("fbport", fallbackPort)
        }
    }
    .build()
    .toString()

/** null when [text] is not a routerdomains://setup URI (e.g. a foreign QR was scanned). */
fun parseSetupUri(text: String): SetupPayload? {
    val uri = runCatching { Uri.parse(text) }.getOrNull() ?: return null
    if (uri.scheme != "routerdomains" || uri.authority != "setup") return null
    val host = uri.getQueryParameter("host")?.trim().orEmpty()
    if (host.isEmpty()) return null
    return SetupPayload(
        host = host,
        port = uri.getQueryParameter("port")?.toIntOrNull(),
        token = uri.getQueryParameter("token").orEmpty(),
        fallbackHost = uri.getQueryParameter("fbhost")?.trim().orEmpty(),
        fallbackPort = uri.getQueryParameter("fbport")?.toIntOrNull(),
    )
}

