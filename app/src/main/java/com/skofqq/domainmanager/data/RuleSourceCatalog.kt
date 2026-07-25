package com.skofqq.domainmanager.data

/**
 * Behaviors and formats the API accepts, in the order the pickers show them.
 *
 * The list of SOURCES is deliberately not here — it comes live from
 * [RuleCatalogStore] so new upstream categories appear without an app update.
 * Anything outside that catalog goes through the "custom URL" path, which
 * reaches the same provider_add with hand-entered values.
 */
val PROVIDER_BEHAVIORS = listOf("domain", "ipcidr", "classical")
val PROVIDER_FORMATS = listOf("mrs", "yaml", "text")

/**
 * Best-effort defaults for a hand-typed URL, from its file extension and path.
 * Only a starting point — the add flow always shows both pickers so the user can
 * override, because a URL alone can't prove what's inside.
 */
fun guessSourceFormat(url: String): String {
    val path = url.substringBefore('?').substringBefore('#').lowercase()
    return when {
        path.endsWith(".mrs") -> "mrs"
        path.endsWith(".yaml") || path.endsWith(".yml") -> "yaml"
        path.endsWith(".list") || path.endsWith(".lst") || path.endsWith(".txt") -> "text"
        else -> "mrs"
    }
}

/** Companion of [guessSourceFormat]: "ipcidr"/"geoip" anywhere in the path means an address list. */
fun guessSourceBehavior(url: String): String {
    val path = url.substringBefore('?').substringBefore('#').lowercase()
    return when {
        path.contains("ipcidr") || path.contains("geoip") || path.contains("/ip/") -> "ipcidr"
        path.contains("classical") -> "classical"
        else -> "domain"
    }
}

/**
 * Provider-name suggestion from a URL's file name: lowercased, everything the
 * router's `[a-z][a-z0-9-]*` rule disallows collapsed to "-". Returns "" when
 * nothing usable is left, so the caller can leave the field empty rather than
 * pre-filling garbage.
 */
fun suggestProviderName(url: String): String =
    sanitizeProviderName(
        url.substringBefore('?').substringBefore('#')
            .substringAfterLast('/')
            .substringBeforeLast('.')
    )

/**
 * Provider-name suggestion for a catalog pick. The noisy "category-" prefix that
 * most geosite files carry is dropped, and geoip entries get a "geoip-" prefix
 * instead — without it, geosite/telegram.mrs and geoip/telegram.mrs would both
 * suggest "telegram" and the second add would collide. Always editable after.
 */
fun providerNameFor(entry: CatalogEntry): String {
    val base = entry.name.substringBeforeLast('.').removePrefix("category-")
    val cleaned = sanitizeProviderName(base)
    return when {
        cleaned.isEmpty() -> ""
        entry.source == SOURCE_GEOIP -> "geoip-$cleaned"
        else -> cleaned
    }
}

/** Collapses anything outside `[a-z0-9-]` to "-" and makes the result start with a letter. */
private fun sanitizeProviderName(raw: String): String {
    val cleaned = raw.lowercase()
        .map { if (it in 'a'..'z' || it in '0'..'9') it else '-' }
        .joinToString("")
        .replace(Regex("-+"), "-")
        .trim('-')
    // Must start with a letter; a leading digit is dropped rather than prefixed
    // with something arbitrary the user didn't ask for.
    return cleaned.dropWhile { it !in 'a'..'z' }
}
