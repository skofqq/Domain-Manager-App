package com.skofqq.domainmanager.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.skofqq.domainmanager.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * One saved router connection. [name] is the user-facing label ("Дом", "Дача") —
 * profiles are recognized by it, not by the IP. Exactly one profile is ACTIVE at
 * a time; every screen and API call works against the active one.
 */
data class RouterProfile(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    /** Optional backup address (e.g. a Tailscale IP); empty = no automatic failover. */
    val fallbackHost: String,
    val fallbackPort: Int,
    val token: String,
    val defaultTarget: String,
)

class PrefsStore(context: Context) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val uiPrefs: SharedPreferences =
        context.getSharedPreferences(UI_PREFS_NAME, Context.MODE_PRIVATE)

    // ---------------------------------------------------------------------------
    // Router profiles (multi-router). Stored as a JSON array in the SAME
    // EncryptedSharedPreferences that held the old single-router fields.
    // ---------------------------------------------------------------------------

    /** All saved profiles; mirrored into a flow so switcher UI updates live. */
    val profilesFlow: MutableStateFlow<List<RouterProfile>>
    /** Id of the active profile; "" only when the profile list is empty. */
    val activeProfileIdFlow: MutableStateFlow<String>

    init {
        migrateLegacySingleRouterIfNeeded()
        profilesFlow = MutableStateFlow(readProfiles())
        activeProfileIdFlow = MutableStateFlow(prefs.getString(KEY_ACTIVE_PROFILE, "") ?: "")
    }

    /**
     * First launch after the multi-router update: the previously configured
     * single router (or the untouched defaults) becomes the one and only
     * profile, named "My router" — nothing to re-enter.
     */
    private fun migrateLegacySingleRouterIfNeeded() {
        if (prefs.contains(KEY_PROFILES)) return
        val id = UUID.randomUUID().toString()
        val migrated = RouterProfile(
            id = id,
            name = appContext.getString(R.string.default_profile_name),
            host = prefs.getString(KEY_LEGACY_HOST, "192.168.1.1") ?: "192.168.1.1",
            port = prefs.getInt(KEY_LEGACY_PORT, 80),
            fallbackHost = prefs.getString(KEY_LEGACY_FALLBACK_HOST, "") ?: "",
            fallbackPort = prefs.getInt(KEY_LEGACY_FALLBACK_PORT, 80),
            token = prefs.getString(KEY_LEGACY_TOKEN, "") ?: "",
            defaultTarget = prefs.getString(KEY_LEGACY_TARGET, "both") ?: "both",
        )
        prefs.edit()
            .putString(KEY_PROFILES, JSONArray().put(profileToJson(migrated)).toString())
            .putString(KEY_ACTIVE_PROFILE, id)
            .apply()
        // The pre-profile device-icon picks belong to the migrated router.
        uiPrefs.getString(KEY_DEVICE_ICONS_LEGACY, null)?.let { legacyIcons ->
            uiPrefs.edit()
                .putString(deviceIconsKey(id), legacyIcons)
                .remove(KEY_DEVICE_ICONS_LEGACY)
                .apply()
        }
    }

    private fun profileToJson(p: RouterProfile) = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("host", p.host)
        put("port", p.port)
        put("fallback_host", p.fallbackHost)
        put("fallback_port", p.fallbackPort)
        put("token", p.token)
        put("target", p.defaultTarget)
    }

    private fun readProfiles(): List<RouterProfile> {
        val raw = prefs.getString(KEY_PROFILES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        RouterProfile(
                            id = o.getString("id"),
                            name = o.optString("name", ""),
                            host = o.optString("host", ""),
                            port = o.optInt("port", 80),
                            fallbackHost = o.optString("fallback_host", ""),
                            fallbackPort = o.optInt("fallback_port", 80),
                            token = o.optString("token", ""),
                            defaultTarget = o.optString("target", "both"),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeProfiles(profiles: List<RouterProfile>) {
        val array = JSONArray()
        profiles.forEach { array.put(profileToJson(it)) }
        prefs.edit().putString(KEY_PROFILES, array.toString()).apply()
        profilesFlow.value = profiles
    }

    fun profiles(): List<RouterProfile> = profilesFlow.value

    val activeProfileId: String get() = activeProfileIdFlow.value

    /** null only when every profile has been deleted. */
    fun activeProfile(): RouterProfile? {
        val profiles = profilesFlow.value
        return profiles.firstOrNull { it.id == activeProfileIdFlow.value } ?: profiles.firstOrNull()
    }

    fun setActiveProfile(id: String) {
        if (profilesFlow.value.none { it.id == id }) return
        prefs.edit().putString(KEY_ACTIVE_PROFILE, id).apply()
        activeProfileIdFlow.value = id
    }

    /** Insert or update by id. */
    fun upsertProfile(profile: RouterProfile) {
        val current = profilesFlow.value
        val updated = if (current.any { it.id == profile.id }) {
            current.map { if (it.id == profile.id) profile else it }
        } else {
            current + profile
        }
        writeProfiles(updated)
        // First profile ever (or after a full wipe) becomes active automatically.
        if (activeProfileIdFlow.value.isEmpty() || current.isEmpty()) setActiveProfile(profile.id)
    }

    /** Deleting the active profile promotes the first remaining one. */
    fun deleteProfile(id: String) {
        val remaining = profilesFlow.value.filterNot { it.id == id }
        writeProfiles(remaining)
        uiPrefs.edit().remove(deviceIconsKey(id)).apply()
        if (activeProfileIdFlow.value == id) {
            val next = remaining.firstOrNull()?.id ?: ""
            prefs.edit().putString(KEY_ACTIVE_PROFILE, next).apply()
            activeProfileIdFlow.value = next
        }
    }

    // ---------------------------------------------------------------------------
    // Active-profile accessors. RouterApi (and everything else that talks to the
    // router) reads these per call — switching the active profile redirects all
    // API traffic without touching the call sites. Fall back to the historical
    // defaults when no profile exists at all.
    // ---------------------------------------------------------------------------

    val routerHost: String get() = activeProfile()?.host ?: "192.168.1.1"
    val routerPort: Int get() = activeProfile()?.port ?: 80
    val fallbackHost: String get() = activeProfile()?.fallbackHost ?: ""
    val fallbackPort: Int get() = activeProfile()?.fallbackPort ?: 80
    val token: String get() = activeProfile()?.token ?: ""
    val defaultTarget: String get() = activeProfile()?.defaultTarget ?: "both"

    // ---------------------------------------------------------------------------
    // UI prefs (unchanged, not router-specific)
    // ---------------------------------------------------------------------------

    var useDynamicColor: Boolean
        get() = uiPrefs.getBoolean(KEY_DYNAMIC_COLOR, true)
        set(value) { uiPrefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply() }

    /** "system" | "light" | "dark" */
    var themeMode: String
        get() = uiPrefs.getString(KEY_THEME_MODE, "system") ?: "system"
        set(value) { uiPrefs.edit().putString(KEY_THEME_MODE, value).apply() }

    /** "off" | "token" (auth before revealing the token) | "app" (auth on every launch) */
    var appLockMode: String
        get() = uiPrefs.getString(KEY_APP_LOCK_MODE, "off") ?: "off"
        set(value) { uiPrefs.edit().putString(KEY_APP_LOCK_MODE, value).apply() }

    /** Connect/read timeout for router API calls, seconds. */
    var httpTimeoutSeconds: Int
        get() = uiPrefs.getInt(KEY_HTTP_TIMEOUT, DEFAULT_HTTP_TIMEOUT)
        set(value) { uiPrefs.edit().putInt(KEY_HTTP_TIMEOUT, value).apply() }

    /** mihomo screen: show proxy groups as a 2-column grid instead of a list. */
    var mihomoGroupsGrid: Boolean
        get() = uiPrefs.getBoolean(KEY_MIHOMO_GROUPS_GRID, false)
        set(value) { uiPrefs.edit().putBoolean(KEY_MIHOMO_GROUPS_GRID, value).apply() }

    /** mihomo group node-picker sheet: show nodes as a 2-column grid instead of a list. */
    var mihomoNodesGrid: Boolean
        get() = uiPrefs.getBoolean(KEY_MIHOMO_NODES_GRID, false)
        set(value) { uiPrefs.edit().putBoolean(KEY_MIHOMO_NODES_GRID, value).apply() }

    // ---------------------------------------------------------------------------
    // Device icon overrides — PER ROUTER (different routers, different devices).
    // ---------------------------------------------------------------------------

    private fun deviceIconsKey(profileId: String) = "${KEY_DEVICE_ICONS_LEGACY}_$profileId"

    /**
     * Manual device-icon picks for the ACTIVE router, MAC → icon key. Purely
     * client-side; takes priority over the keyword guess from the type field.
     */
    fun deviceIconOverrides(): Map<String, String> {
        val id = activeProfile()?.id ?: return emptyMap()
        val raw = uiPrefs.getString(deviceIconsKey(id), null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            buildMap { for (key in json.keys()) put(key, json.getString(key)) }
        }.getOrDefault(emptyMap())
    }

    fun setDeviceIconOverride(mac: String, icon: String?) {
        val id = activeProfile()?.id ?: return
        val map = deviceIconOverrides().toMutableMap()
        if (icon == null) map.remove(mac) else map[mac] = icon
        uiPrefs.edit().putString(deviceIconsKey(id), JSONObject(map as Map<*, *>).toString()).apply()
    }

    // ---------------------------------------------------------------------------
    // Favorite/pinned devices — PER ROUTER, by MAC. Purely client-side.
    // ---------------------------------------------------------------------------

    private fun favoritesKey(profileId: String) = "$KEY_FAVORITE_DEVICES_PREFIX$profileId"

    fun favoriteDeviceMacs(): Set<String> {
        val id = activeProfile()?.id ?: return emptySet()
        return uiPrefs.getStringSet(favoritesKey(id), emptySet()) ?: emptySet()
    }

    fun toggleFavoriteDevice(mac: String) {
        val id = activeProfile()?.id ?: return
        val current = favoriteDeviceMacs().toMutableSet()
        if (!current.remove(mac)) current.add(mac)
        // getStringSet's contract forbids mutating the returned set in place —
        // always write a fresh copy.
        uiPrefs.edit().putStringSet(favoritesKey(id), HashSet(current)).apply()
    }

    // ---------------------------------------------------------------------------
    // Background monitoring (opt-in): WAN IP change / disk space / latency
    // degradation notifications. App-wide switches; the per-router progress state
    // they compare against lives in [RouterMonitorState] below.
    // ---------------------------------------------------------------------------

    var monitoringEnabled: Boolean
        get() = uiPrefs.getBoolean(KEY_MONITORING_ENABLED, false)
        set(value) { uiPrefs.edit().putBoolean(KEY_MONITORING_ENABLED, value).apply() }

    /** Minutes between checks. WorkManager's periodic floor is 15 — enforced here too. */
    var monitoringIntervalMinutes: Int
        get() = uiPrefs.getInt(KEY_MONITORING_INTERVAL, DEFAULT_MONITORING_INTERVAL_MINUTES)
        set(value) { uiPrefs.edit().putInt(KEY_MONITORING_INTERVAL, value.coerceIn(15, 180)).apply() }

    var monitorWanIp: Boolean
        get() = uiPrefs.getBoolean(KEY_MONITOR_WAN_IP, true)
        set(value) { uiPrefs.edit().putBoolean(KEY_MONITOR_WAN_IP, value).apply() }

    var monitorDiskSpace: Boolean
        get() = uiPrefs.getBoolean(KEY_MONITOR_DISK, true)
        set(value) { uiPrefs.edit().putBoolean(KEY_MONITOR_DISK, value).apply() }

    /**
     * One snapshot of "what we last saw" for the background monitor, PER ROUTER
     * (a different router has a different WAN, disk, and set of mihomo groups).
     */
    data class RouterMonitorState(
        val lastWanIp: String = "",
        val lastWanIpv6: String = "",
        /** Epoch day (System.currentTimeMillis()/86400000) of the last disk-space alert; -1 = never. */
        val diskAlertEpochDay: Long = -1,
        /** Group to watch for latency degradation; null = the check is off for this router. */
        val latencyGroup: String? = null,
        /** Node the baseline below belongs to; a node change invalidates the baseline silently. */
        val latencyNode: String? = null,
        val latencyBaselineMs: Int? = null,
        /** True while an already-notified degradation/outage is ongoing — suppresses repeat pings. */
        val latencyAlertActive: Boolean = false,
    )

    private fun monitorStateKey(profileId: String) = "$KEY_MONITOR_STATE_PREFIX$profileId"

    fun monitorState(profileId: String): RouterMonitorState {
        val raw = uiPrefs.getString(monitorStateKey(profileId), null)
            ?: return RouterMonitorState()
        return runCatching {
            val json = JSONObject(raw)
            RouterMonitorState(
                lastWanIp = json.optString("last_wan_ip", ""),
                lastWanIpv6 = json.optString("last_wan_ipv6", ""),
                diskAlertEpochDay = json.optLong("disk_alert_epoch_day", -1),
                latencyGroup = json.optString("latency_group", "").ifEmpty { null },
                latencyNode = json.optString("latency_node", "").ifEmpty { null },
                latencyBaselineMs = json.optInt("latency_baseline_ms", -1).takeIf { it >= 0 },
                latencyAlertActive = json.optBoolean("latency_alert_active", false),
            )
        }.getOrDefault(RouterMonitorState())
    }

    fun setMonitorState(profileId: String, state: RouterMonitorState) {
        val json = JSONObject().apply {
            put("last_wan_ip", state.lastWanIp)
            put("last_wan_ipv6", state.lastWanIpv6)
            put("disk_alert_epoch_day", state.diskAlertEpochDay)
            put("latency_group", state.latencyGroup ?: "")
            put("latency_node", state.latencyNode ?: "")
            put("latency_baseline_ms", state.latencyBaselineMs ?: -1)
            put("latency_alert_active", state.latencyAlertActive)
        }
        uiPrefs.edit().putString(monitorStateKey(profileId), json.toString()).apply()
    }

    // ---------------------------------------------------------------------------
    // App Shortcuts: which 2-4 quick actions the user wants published. App-wide
    // (a shortcut acts on whichever router is active when it's tapped).
    // ---------------------------------------------------------------------------

    var enabledShortcutIds: List<String>
        get() {
            val raw = uiPrefs.getString(KEY_SHORTCUT_IDS, null) ?: return DEFAULT_SHORTCUT_IDS
            return runCatching {
                val array = JSONArray(raw)
                buildList { for (i in 0 until array.length()) add(array.getString(i)) }
            }.getOrDefault(DEFAULT_SHORTCUT_IDS)
        }
        set(value) {
            uiPrefs.edit().putString(KEY_SHORTCUT_IDS, JSONArray(value).toString()).apply()
        }

    companion object {
        const val DEFAULT_HTTP_TIMEOUT = 10
        private const val PREFS_NAME = "domain_manager_secure_prefs"
        private const val UI_PREFS_NAME = "domain_manager_ui_prefs"
        private const val KEY_PROFILES = "router_profiles"
        private const val KEY_ACTIVE_PROFILE = "active_router_profile"
        // Pre-multi-router keys, read once by the migration and then left alone.
        private const val KEY_LEGACY_HOST = "router_host"
        private const val KEY_LEGACY_PORT = "router_port"
        private const val KEY_LEGACY_FALLBACK_HOST = "fallback_host"
        private const val KEY_LEGACY_FALLBACK_PORT = "fallback_port"
        private const val KEY_LEGACY_TOKEN = "token"
        private const val KEY_LEGACY_TARGET = "default_target"
        private const val KEY_DYNAMIC_COLOR = "use_dynamic_color"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_APP_LOCK_MODE = "app_lock_mode"
        private const val KEY_HTTP_TIMEOUT = "http_timeout_seconds"
        private const val KEY_MIHOMO_GROUPS_GRID = "mihomo_groups_grid"
        private const val KEY_MIHOMO_NODES_GRID = "mihomo_nodes_grid"
        private const val KEY_DEVICE_ICONS_LEGACY = "device_icon_overrides"
        private const val KEY_FAVORITE_DEVICES_PREFIX = "favorite_devices_"
        private const val KEY_MONITORING_ENABLED = "monitoring_enabled"
        private const val KEY_MONITORING_INTERVAL = "monitoring_interval_minutes"
        private const val KEY_MONITOR_WAN_IP = "monitor_wan_ip"
        private const val KEY_MONITOR_DISK = "monitor_disk"
        private const val KEY_MONITOR_STATE_PREFIX = "router_monitor_state_"
        private const val KEY_SHORTCUT_IDS = "shortcut_ids"
        const val DEFAULT_MONITORING_INTERVAL_MINUTES = 30
        /** "Проверить соединение" + "Список устройств" — the two safest, no-confirmation defaults. */
        val DEFAULT_SHORTCUT_IDS = listOf("status", "devices")
    }
}
