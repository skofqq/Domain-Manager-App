package com.skofqq.domainmanager

import com.skofqq.domainmanager.data.CatalogEntry
import com.skofqq.domainmanager.data.SOURCE_GEOIP
import com.skofqq.domainmanager.data.SOURCE_GEOSITE
import com.skofqq.domainmanager.data.providerNameFor
import com.skofqq.domainmanager.data.rawUrlFor
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The trees API carries no download_url, so the raw URL is built client-side.
 * These cases are taken from the real repo listing and checked against what the
 * contents API returns as download_url for the same files — the two must agree
 * character for character, or the router would be handed a URL that 404s.
 */
class RuleCatalogUrlTest {

    @Test
    fun `plain name is untouched`() {
        assertEquals(
            "https://raw.githubusercontent.com/MetaCubeX/meta-rules-dat/meta/geo/geosite/youtube.mrs",
            rawUrlFor(SOURCE_GEOSITE, "youtube.mrs"),
        )
    }

    /** GitHub percent-encodes "@" in download_url — 250 files in the repo have one. */
    @Test
    fun `at sign is percent encoded like github does`() {
        assertEquals(
            "https://raw.githubusercontent.com/MetaCubeX/meta-rules-dat/meta/geo/geosite/acer%40cn.mrs",
            rawUrlFor(SOURCE_GEOSITE, "acer@cn.mrs"),
        )
    }

    /** "!" is a sub-delim and GitHub leaves it literal — 76 files have one. */
    @Test
    fun `exclamation mark stays literal`() {
        assertEquals(
            "https://raw.githubusercontent.com/MetaCubeX/meta-rules-dat/meta/geo/geosite/category-ai-!cn.mrs",
            rawUrlFor(SOURCE_GEOSITE, "category-ai-!cn.mrs"),
        )
    }

    @Test
    fun `both special characters in one name`() {
        assertEquals(
            "https://raw.githubusercontent.com/MetaCubeX/meta-rules-dat/meta/geo/geosite/alibaba%40!cn.list",
            rawUrlFor(SOURCE_GEOSITE, "alibaba@!cn.list"),
        )
    }

    @Test
    fun `geoip files live under their own directory`() {
        assertEquals(
            "https://raw.githubusercontent.com/MetaCubeX/meta-rules-dat/meta/geo/geoip/ru.mrs",
            rawUrlFor(SOURCE_GEOIP, "ru.mrs"),
        )
    }

    /** Anything unexpected must be encoded rather than passed through raw. */
    @Test
    fun `non ascii is percent encoded as utf8`() {
        assertEquals(
            "https://raw.githubusercontent.com/MetaCubeX/meta-rules-dat/meta/geo/geosite/%D1%8F.mrs",
            rawUrlFor(SOURCE_GEOSITE, "я.mrs"),
        )
    }
}

/** The router only accepts `[a-z][a-z0-9-]*`, so every suggestion must already fit. */
class ProviderNameTest {

    private val nameRegex = Regex("^[a-z][a-z0-9-]*$")

    @Test
    fun `category prefix is dropped`() {
        assertEquals("ads-all", providerNameFor(CatalogEntry("category-ads-all.mrs", SOURCE_GEOSITE)))
    }

    @Test
    fun `illegal characters collapse to single dashes`() {
        assertEquals("ai-cn", providerNameFor(CatalogEntry("category-ai-!cn.mrs", SOURCE_GEOSITE)))
        assertEquals("acer-cn", providerNameFor(CatalogEntry("acer@cn.mrs", SOURCE_GEOSITE)))
    }

    /** Without the prefix geosite/telegram and geoip/telegram would both suggest "telegram". */
    @Test
    fun `geoip entries are prefixed so they cannot collide with geosite`() {
        assertEquals("telegram", providerNameFor(CatalogEntry("telegram.mrs", SOURCE_GEOSITE)))
        assertEquals("geoip-telegram", providerNameFor(CatalogEntry("telegram.mrs", SOURCE_GEOIP)))
    }

    @Test
    fun `a leading digit is dropped because names must start with a letter`() {
        assertEquals("x0", providerNameFor(CatalogEntry("0x0.list", SOURCE_GEOSITE)))
    }

    @Test
    fun `every suggestion satisfies the router's rule`() {
        val samples = listOf(
            "youtube.mrs", "category-ads-all.mrs", "category-ai-!cn.mrs", "acer@cn.list",
            "alibaba@!cn.yaml", "0x0.list", "steam.mrs", "category-games@cn.mrs",
        )
        for (source in listOf(SOURCE_GEOSITE, SOURCE_GEOIP)) {
            for (file in samples) {
                val suggested = providerNameFor(CatalogEntry(file, source))
                assertEquals("$file ($source) -> '$suggested'", true, nameRegex.matches(suggested))
            }
        }
    }
}

/** Directory decides behavior, real extension decides format — neither is hardcoded per category. */
class CatalogEntryDefaultsTest {

    @Test
    fun `behavior comes from the directory`() {
        assertEquals("domain", CatalogEntry("youtube.mrs", SOURCE_GEOSITE).behavior)
        assertEquals("ipcidr", CatalogEntry("ru.mrs", SOURCE_GEOIP).behavior)
    }

    @Test
    fun `format comes from the real extension`() {
        assertEquals("mrs", CatalogEntry("youtube.mrs", SOURCE_GEOSITE).format)
        assertEquals("yaml", CatalogEntry("youtube.yaml", SOURCE_GEOSITE).format)
        assertEquals("text", CatalogEntry("youtube.list", SOURCE_GEOSITE).format)
    }
}
