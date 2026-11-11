package com.skofqq.domainmanager.data

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * The app's own settings as one JSON file (Settings → Backup → App settings),
 * separate from the router-list snapshot: theme, language, appearance, security,
 * monitoring, shortcuts, timeouts, update checks, and the router profiles with
 * their per-router device icons, favourites and watched latency group.
 *
 * Tokens are optional ([export]'s includeTokens). Without them, restoring onto a
 * profile with the same address keeps that profile's own token, and a brand-new
 * profile comes back with an empty one.
 */
object SettingsBackup {
    const val TYPE = "helm-settings"
    private const val VERSION = 1

    /** Values that live outside [PrefsStore] (AppCompat locale, update checker). */
    data class Extras(val languageTag: String, val updateAutoCheck: Boolean)

    data class RestoreResult(
        val routersAdded: Int,
        val routersUpdated: Int,
        val languageTag: String?,
        val updateAutoCheck: Boolean?,
    )

    class InvalidBackupException : Exception("Not a Helm settings backup")

    /** Cheap sniff so the shared import field can route pasted text here. */
    fun looksLikeSettingsBackup(text: String): Boolean =
        text.contains("\"type\"") && text.contains("\"$TYPE\"")

    fun export(prefs: PrefsStore, extras: Extras, appVersion: String, includeTokens: Boolean): String {
        val settings = JSONObject().apply {
            put("theme_mode", prefs.themeMode)
            put("use_dynamic_color", prefs.useDynamicColor)
            put("liquid_glass", prefs.liquidGlass)
            put("true_black", prefs.trueBlack)
            put("haptic_feedback", prefs.hapticFeedback)
            put("app_lock_mode", prefs.appLockMode)
            put("http_timeout_seconds", prefs.httpTimeoutSeconds)
            put("mihomo_groups_grid", prefs.mihomoGroupsGrid)
            put("mihomo_nodes_grid", prefs.mihomoNodesGrid)
            put("monitoring_enabled", prefs.monitoringEnabled)
            put("monitoring_interval_minutes", prefs.monitoringIntervalMinutes)
            put("monitor_wan_ip", prefs.monitorWanIp)
            put("monitor_disk", prefs.monitorDiskSpace)
            put("shortcut_ids", JSONArray(prefs.enabledShortcutIds))
            put("language", extras.languageTag)
            put("update_auto_check", extras.updateAutoCheck)
        }
        val activeId = prefs.activeProfileId
        val routers = JSONArray()
        prefs.profiles().forEach { p ->
            routers.put(JSONObject().apply {
                put("name", p.name)
                put("host", p.host)
                put("port", p.port)
                put("fallback_host", p.fallbackHost)
                put("fallback_port", p.fallbackPort)
                if (includeTokens) put("token", p.token)
                put("default_target", p.defaultTarget)
                put("active", p.id == activeId)
                put("device_icons", JSONObject(prefs.deviceIconOverrides(p.id) as Map<*, *>))
                put("favorite_devices", JSONArray(prefs.favoriteDeviceMacs(p.id).sorted()))
                prefs.monitorState(p.id).latencyGroup?.let { put("latency_group", it) }
            })
        }
        return JSONObject().apply {
            put("type", TYPE)
            put("version", VERSION)
            put("app_version", appVersion)
            put("exported_at", isoNow())
            put("includes_tokens", includeTokens)
            put("settings", settings)
            put("routers", routers)
        }.toString(2)
    }

    /**
     * Writes everything into [prefs]. Routers are merged, never wiped: a backed-up
     * router replaces the local profile with the same host and port (keeping its
     * id, so history and monitor state stay attached), otherwise it is added.
     * Language and the update toggle are returned for the caller to apply.
     */
    fun restore(prefs: PrefsStore, text: String): RestoreResult {
        val root = runCatching { JSONObject(text.trim()) }.getOrNull()
        if (root == null || root.optString("type") != TYPE) throw InvalidBackupException()

        val s = root.optJSONObject("settings") ?: JSONObject()
        if (s.has("theme_mode")) prefs.themeMode = s.getString("theme_mode")
        if (s.has("use_dynamic_color")) prefs.useDynamicColor = s.getBoolean("use_dynamic_color")
        if (s.has("liquid_glass")) prefs.liquidGlass = s.getBoolean("liquid_glass")
        if (s.has("true_black")) prefs.trueBlack = s.getBoolean("true_black")
        if (s.has("haptic_feedback")) prefs.hapticFeedback = s.getBoolean("haptic_feedback")
        if (s.has("app_lock_mode")) prefs.appLockMode = s.getString("app_lock_mode")
        if (s.has("http_timeout_seconds")) {
            prefs.httpTimeoutSeconds = s.getInt("http_timeout_seconds")
                .coerceIn(RouterApi.MIN_TIMEOUT_SECONDS, RouterApi.MAX_TIMEOUT_SECONDS)
        }
        if (s.has("mihomo_groups_grid")) prefs.mihomoGroupsGrid = s.getBoolean("mihomo_groups_grid")
        if (s.has("mihomo_nodes_grid")) prefs.mihomoNodesGrid = s.getBoolean("mihomo_nodes_grid")
        if (s.has("monitoring_enabled")) prefs.monitoringEnabled = s.getBoolean("monitoring_enabled")
        if (s.has("monitoring_interval_minutes")) prefs.monitoringIntervalMinutes = s.getInt("monitoring_interval_minutes")
        if (s.has("monitor_wan_ip")) prefs.monitorWanIp = s.getBoolean("monitor_wan_ip")
        if (s.has("monitor_disk")) prefs.monitorDiskSpace = s.getBoolean("monitor_disk")
        s.optJSONArray("shortcut_ids")?.let { arr ->
            val ids = buildList { for (i in 0 until arr.length()) add(arr.getString(i)) }
            if (ids.size in 2..4) prefs.enabledShortcutIds = ids
        }

        var added = 0
        var updated = 0
        var activeId: String? = null
        val routers = root.optJSONArray("routers") ?: JSONArray()
        for (i in 0 until routers.length()) {
            val r = routers.getJSONObject(i)
            val host = r.optString("host").trim()
            if (host.isEmpty()) continue
            val port = r.optInt("port", 80)
            val existing = prefs.profiles().firstOrNull { it.host.equals(host, ignoreCase = true) && it.port == port }
            val id = existing?.id ?: UUID.randomUUID().toString()
            prefs.upsertProfile(
                RouterProfile(
                    id = id,
                    name = r.optString("name").ifBlank { existing?.name ?: host },
                    host = host,
                    port = port,
                    fallbackHost = r.optString("fallback_host"),
                    fallbackPort = r.optInt("fallback_port", 80),
                    token = if (r.has("token")) r.getString("token") else existing?.token.orEmpty(),
                    defaultTarget = r.optString("default_target", "both"),
                )
            )
            if (existing == null) added++ else updated++

            r.optJSONObject("device_icons")?.let { icons ->
                prefs.setDeviceIconOverrides(id, buildMap { for (k in icons.keys()) put(k, icons.getString(k)) })
            }
            r.optJSONArray("favorite_devices")?.let { favs ->
                prefs.setFavoriteDeviceMacs(id, buildSet { for (k in 0 until favs.length()) add(favs.getString(k)) })
            }
            if (r.has("latency_group")) {
                val group = r.optString("latency_group").ifEmpty { null }
                // A restored group starts without a baseline, same as picking it by hand.
                prefs.setMonitorState(
                    id,
                    prefs.monitorState(id).copy(
                        latencyGroup = group,
                        latencyNode = null,
                        latencyBaselineMs = null,
                        latencyAlertActive = false,
                    ),
                )
            }
            if (r.optBoolean("active")) activeId = id
        }
        activeId?.let { prefs.setActiveProfile(it) }

        return RestoreResult(
            routersAdded = added,
            routersUpdated = updated,
            languageTag = if (s.has("language")) s.getString("language") else null,
            updateAutoCheck = if (s.has("update_auto_check")) s.getBoolean("update_auto_check") else null,
        )
    }

    /** "helm-settings-2026-11-11.json" */
    fun fileName(): String =
        "helm-settings-" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".json"

    private fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
}
