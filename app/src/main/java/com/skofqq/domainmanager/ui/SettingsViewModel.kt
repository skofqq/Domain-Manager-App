package com.skofqq.domainmanager.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.MihomoProxiesResult
import com.skofqq.domainmanager.data.OkResult
import com.skofqq.domainmanager.data.PrefsStore
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.data.RouterProfile
import com.skofqq.domainmanager.data.TestResult
import com.skofqq.domainmanager.worker.RouterMonitorWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class TestUiState {
    data object Testing : TestUiState()
    data class Done(val ok: Boolean, val message: UiMessage) : TestUiState()
}

/** Full-screen "router is rebooting" overlay states; null = overlay hidden. */
sealed class RebootUiState {
    /** action=reboot request in flight (the router answers before rebooting). */
    data object Requesting : RebootUiState()
    /** Router going down / coming back — countdown before offering a check. */
    data class Waiting(val secondsLeft: Int) : RebootUiState()
    /** Countdown finished — the "check connection" button is available. */
    data object CheckReady : RebootUiState()
    data object Checking : RebootUiState()
    data class CheckDone(val ok: Boolean, val message: UiMessage) : RebootUiState()
}

class SettingsViewModel(
    private val prefs: PrefsStore,
    private val api: RouterApi,
    private val appContext: Context,
) : ViewModel() {

    // --- Router profiles (multi-router) --------------------------------------------
    // One source of truth for both the Domains title-bar switcher and the
    // Authorization list radio: PrefsStore's flows.

    val profiles: StateFlow<List<RouterProfile>> = prefs.profilesFlow
    val activeProfileId: StateFlow<String> = prefs.activeProfileIdFlow

    fun setActiveProfile(id: String) = prefs.setActiveProfile(id)

    fun saveProfile(profile: RouterProfile) = prefs.upsertProfile(profile)

    fun deleteProfile(id: String) = prefs.deleteProfile(id)

    // --- Background monitoring (Settings → Мониторинг) ------------------------------
    // Opt-in: WAN IP change / disk-space / mihomo-latency-degradation notifications,
    // via RouterMonitorWorker. Every setter here keeps the WorkManager schedule in
    // sync so the UI never needs to remember to call reschedule itself.

    var monitoringEnabled by mutableStateOf(prefs.monitoringEnabled)
        private set

    // @JvmName avoids a platform declaration clash: the property's own private
    // synthetic setter and this public function would otherwise both compile
    // to the JVM signature setMonitoringEnabled(Z).
    @JvmName("updateMonitoringEnabled")
    fun setMonitoringEnabled(value: Boolean) {
        monitoringEnabled = value
        prefs.monitoringEnabled = value
        applyMonitorSchedule()
    }

    var monitoringIntervalMinutes by mutableStateOf(prefs.monitoringIntervalMinutes)
        private set

    fun setMonitoringInterval(minutes: Int) {
        prefs.monitoringIntervalMinutes = minutes
        monitoringIntervalMinutes = prefs.monitoringIntervalMinutes // read back the clamped value
        applyMonitorSchedule()
    }

    var monitorWanIp by mutableStateOf(prefs.monitorWanIp)
        private set

    @JvmName("updateMonitorWanIp")
    fun setMonitorWanIp(value: Boolean) {
        monitorWanIp = value
        prefs.monitorWanIp = value
    }

    var monitorDiskSpace by mutableStateOf(prefs.monitorDiskSpace)
        private set

    @JvmName("updateMonitorDiskSpace")
    fun setMonitorDiskSpace(value: Boolean) {
        monitorDiskSpace = value
        prefs.monitorDiskSpace = value
    }

    /** Group watched for latency degradation, for the CURRENTLY ACTIVE router; null = off. */
    fun latencyMonitorGroup(): String? {
        val id = prefs.activeProfileId.takeIf { it.isNotEmpty() } ?: return null
        return prefs.monitorState(id).latencyGroup
    }

    /** Switching groups (or turning the check off with null) resets the baseline — a new group has no history yet. */
    fun setLatencyMonitorGroup(group: String?) {
        val id = prefs.activeProfileId.takeIf { it.isNotEmpty() } ?: return
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

    /** mihomo Selector group names, for the latency-monitor group picker. null until loaded. */
    private val _mihomoGroupNames = MutableStateFlow<List<String>?>(null)
    val mihomoGroupNames: StateFlow<List<String>?> = _mihomoGroupNames

    fun loadMihomoGroupNames() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { api.mihomoProxies() }
            if (result is MihomoProxiesResult.Success) {
                _mihomoGroupNames.value = result.groups.map { it.name }
            }
        }
    }

    private fun applyMonitorSchedule() {
        if (prefs.monitoringEnabled) {
            RouterMonitorWorker.reschedule(appContext, prefs.monitoringIntervalMinutes)
        } else {
            RouterMonitorWorker.cancel(appContext)
        }
    }

    // --- App Shortcuts (Settings → Ярлыки) -------------------------------------------

    var enabledShortcutIds by mutableStateOf(prefs.enabledShortcutIds)
        private set

    @JvmName("updateEnabledShortcutIds")
    fun setEnabledShortcutIds(ids: List<String>) {
        enabledShortcutIds = ids
        prefs.enabledShortcutIds = ids
        AppShortcuts.publish(appContext, ids)
    }

    var useDynamicColor by mutableStateOf(prefs.useDynamicColor)
        private set

    fun setDynamicColor(value: Boolean) {
        useDynamicColor = value
        prefs.useDynamicColor = value
    }

    var themeMode by mutableStateOf(prefs.themeMode)
        private set

    fun setTheme(value: String) {
        themeMode = value
        prefs.themeMode = value
    }

    /** "off" | "token" | "app" — see PrefsStore.appLockMode. Persisted immediately. */
    var appLockMode by mutableStateOf(prefs.appLockMode)
        private set

    fun setAppLock(value: String) {
        appLockMode = value
        prefs.appLockMode = value
    }

    /** Router API timeout, seconds. Persisted immediately; RouterApi picks it up per call. */
    var httpTimeoutSeconds by mutableStateOf(prefs.httpTimeoutSeconds)
        private set

    fun setHttpTimeout(value: Int) {
        val clamped = value.coerceIn(RouterApi.MIN_TIMEOUT_SECONDS, RouterApi.MAX_TIMEOUT_SECONDS)
        httpTimeoutSeconds = clamped
        prefs.httpTimeoutSeconds = clamped
    }

    private val _testState = MutableStateFlow<TestUiState?>(null)
    val testState: StateFlow<TestUiState?> = _testState

    /** Called by the UI after the result toast is shown, so it stays one-shot. */
    fun clearTestResult() {
        _testState.value = null
    }

    /** One-shot test with the EDIT FORM's (possibly unsaved) values. */
    fun testConnection(host: String, port: String, token: String) {
        val h = host.trim()
        val p = port.toIntOrNull()?.coerceIn(1, 65535) ?: 80
        val t = token.trim()
        viewModelScope.launch {
            _testState.value = TestUiState.Testing
            val result = withContext(Dispatchers.IO) { api.testConnection(h, p, t) }
            val (ok, message) = testMessage(result)
            _testState.value = TestUiState.Done(ok, message)
        }
    }

    private fun testMessage(result: TestResult): Pair<Boolean, UiMessage> = when (result) {
        is TestResult.Connected -> true to UiMessage(R.string.test_connected)
        is TestResult.ConnectedBadToken -> true to UiMessage(R.string.test_connected_bad_token)
        is TestResult.ConnectedNoToken -> true to UiMessage(R.string.test_connected_no_token)
        is TestResult.HttpError -> false to UiMessage(R.string.test_http_error, listOf(result.code))
        is TestResult.Failed -> false to networkErrorMessage(result.kind, result.detail)
    }

    // --- Router reboot ------------------------------------------------------------

    private val _rebootState = MutableStateFlow<RebootUiState?>(null)
    val rebootState: StateFlow<RebootUiState?> = _rebootState

    /** Error of the action=reboot call itself (before the overlay's wait phase). */
    var rebootError by mutableStateOf<UiMessage?>(null)
        private set

    private var rebootJob: Job? = null

    /**
     * Fires action=reboot. Callers MUST have collected an explicit user
     * confirmation first — the server gives no soft warning of its own.
     */
    fun requestReboot() {
        if (_rebootState.value != null) return
        rebootError = null
        rebootJob = viewModelScope.launch {
            _rebootState.value = RebootUiState.Requesting
            val result = withContext(Dispatchers.IO) { api.reboot() }
            when (result) {
                is OkResult.Success -> {
                    for (s in REBOOT_WAIT_SECONDS downTo 1) {
                        _rebootState.value = RebootUiState.Waiting(s)
                        delay(1000)
                    }
                    _rebootState.value = RebootUiState.CheckReady
                }
                is OkResult.ApiError -> {
                    _rebootState.value = null
                    rebootError = apiErrorMessage(result.code, result.error)
                }
                is OkResult.NetworkError -> {
                    _rebootState.value = null
                    rebootError = networkErrorMessage(result.kind, result.detail)
                }
            }
        }
    }

    /** The overlay's "check connection" — same logic as Test Connection, saved prefs. */
    fun rebootCheckConnection() {
        viewModelScope.launch {
            _rebootState.value = RebootUiState.Checking
            val result = withContext(Dispatchers.IO) {
                api.testConnection(prefs.routerHost, prefs.routerPort, prefs.token)
            }
            val (ok, message) = testMessage(result)
            _rebootState.value = RebootUiState.CheckDone(ok, message)
        }
    }

    fun dismissReboot() {
        rebootJob?.cancel()
        rebootJob = null
        _rebootState.value = null
    }

    companion object {
        /** Countdown before offering the post-reboot connection check. */
        const val REBOOT_WAIT_SECONDS = 90
    }

    class Factory(
        private val prefs: PrefsStore,
        private val api: RouterApi,
        private val appContext: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(prefs, api, appContext) as T
    }
}
