package com.skofqq.domainmanager.util

import java.net.URI

private val TWO_WORD_TLDS = setOf(
    "co.uk", "org.uk", "gov.uk", "ac.uk",
    "com.ru", "net.ru", "org.ru", "gov.ru", "edu.ru", "pp.ru", "msk.ru", "spb.ru",
    "com.ua", "net.ua", "org.ua", "kiev.ua",
    "com.br", "com.au", "co.jp", "co.kr", "com.tr", "com.cn", "co.in",
    "github.io",
)

private val URL_REGEX = Regex("""https?://[^\s"'<>]+""")

/**
 * Extracts the registrable domain from a URL, page title, or any shared text.
 * Returns null when no domain can be found.
 */
fun extractDomain(input: String): String? {
    val trimmed = input.trim()
    val host = URL_REGEX.find(trimmed)?.let { parseHost(it.value) }
        ?: run {
            // Treat bare token (no spaces, has a dot) as a domain/URL
            if (trimmed.contains('.') && !trimmed.contains(' '))
                trimmed.substringBefore('/').substringBefore('?').substringBefore(':')
                    .removePrefix("www.").trimEnd('.').lowercase()
            else null
        }
    return host?.let { toRegistrableDomain(it) }?.takeIf { it.isNotBlank() }
}

private fun parseHost(url: String): String? = try {
    var host = URI(url).host ?: return null
    if (host.startsWith("www.")) host = host.removePrefix("www.")
    host.lowercase().takeIf { it.isNotBlank() }
} catch (_: Exception) {
    null
}

private fun toRegistrableDomain(host: String): String {
    val labels = host.split('.')
    if (labels.size < 2) return host
    val twoLabel = "${labels[labels.size - 2]}.${labels[labels.size - 1]}"
    return if (twoLabel in TWO_WORD_TLDS && labels.size >= 3) {
        "${labels[labels.size - 3]}.$twoLabel"
    } else {
        twoLabel
    }
}
