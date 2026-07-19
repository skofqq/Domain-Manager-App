package com.skofqq.domainmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skofqq.domainmanager.data.GroupDelayResult
import com.skofqq.domainmanager.data.MihomoGroup
import com.skofqq.domainmanager.data.PingInfo
import com.skofqq.domainmanager.data.PingResult
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.data.SpeedtestInfo
import com.skofqq.domainmanager.data.SpeedtestResult
import com.skofqq.domainmanager.data.TraceHop
import com.skofqq.domainmanager.data.TracerouteResult
import com.skofqq.domainmanager.data.MihomoProxiesResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Network diagnostics run BY THE ROUTER (ping/traceroute/speedtest/tunnel test).
 * The four tools are independent — each has its own busy flag and result slot, so
 * a slow traceroute doesn't block a ping.
 */
data class DiagnosticsUiState(
    val pingRunning: Boolean = false,
    val ping: PingInfo? = null,
    val pingError: UiMessage? = null,

    val tracerouteRunning: Boolean = false,
    val tracerouteHost: String? = null,
    val traceroute: List<TraceHop>? = null,
    val tracerouteError: UiMessage? = null,

    val speedtestRunning: Boolean = false,
    val speedtest: SpeedtestInfo? = null,
    val speedtestError: UiMessage? = null,

    /** Selector groups for the tunnel test; null until loaded. */
    val groups: List<MihomoGroup>? = null,
    val selectedGroup: String? = null,
    val delayRunning: Boolean = false,
    /** node → ms; a node absent here but present in [delayNodes] did not answer. */
    val delays: Map<String, Int>? = null,
    /** The tested group's full node list, captured when the test started. */
    val delayNodes: List<String>? = null,
    val delayError: UiMessage? = null,
)

class DiagnosticsViewModel(private val api: RouterApi) : ViewModel() {

    private val _state = MutableStateFlow(DiagnosticsUiState())
    val state: StateFlow<DiagnosticsUiState> = _state

    /** Active router switched: old results/groups are about a different router. */
    fun reset() {
        _state.value = DiagnosticsUiState()
    }

    /**
     * Screen freshly (re-)entered: a ping/traceroute result from an earlier visit
     * shouldn't still be showing when the user hasn't asked for one this time.
     * Deliberately scoped to just those two — the host field/preset live in the
     * screen's own state, and speedtest/tunnel-test results are left alone.
     */
    fun resetPingTraceroute() {
        _state.update {
            it.copy(
                pingRunning = false,
                ping = null,
                pingError = null,
                tracerouteRunning = false,
                tracerouteHost = null,
                traceroute = null,
                tracerouteError = null,
            )
        }
    }

    fun runPing(host: String) {
        val h = host.trim()
        if (h.isEmpty() || _state.value.pingRunning) return
        viewModelScope.launch {
            _state.update { it.copy(pingRunning = true, pingError = null) }
            val result = withContext(Dispatchers.IO) { api.ping(h) }
            _state.update { s ->
                when (result) {
                    is PingResult.Success -> s.copy(pingRunning = false, ping = result.info)
                    is PingResult.ApiError -> s.copy(
                        pingRunning = false, pingError = apiErrorMessage(result.code, result.error),
                    )
                    is PingResult.NetworkError -> s.copy(
                        pingRunning = false, pingError = networkErrorMessage(result.kind, result.detail),
                    )
                }
            }
        }
    }

    fun runTraceroute(host: String) {
        val h = host.trim()
        if (h.isEmpty() || _state.value.tracerouteRunning) return
        viewModelScope.launch {
            _state.update {
                it.copy(tracerouteRunning = true, tracerouteError = null, tracerouteHost = h)
            }
            val result = withContext(Dispatchers.IO) { api.traceroute(h) }
            _state.update { s ->
                when (result) {
                    is TracerouteResult.Success -> s.copy(
                        tracerouteRunning = false,
                        tracerouteHost = result.host,
                        traceroute = result.hops,
                    )
                    is TracerouteResult.ApiError -> s.copy(
                        tracerouteRunning = false,
                        tracerouteError = apiErrorMessage(result.code, result.error),
                    )
                    is TracerouteResult.NetworkError -> s.copy(
                        tracerouteRunning = false,
                        tracerouteError = networkErrorMessage(result.kind, result.detail),
                    )
                }
            }
        }
    }

    fun runSpeedtest() {
        if (_state.value.speedtestRunning) return
        viewModelScope.launch {
            _state.update { it.copy(speedtestRunning = true, speedtestError = null) }
            val result = withContext(Dispatchers.IO) { api.speedtest() }
            _state.update { s ->
                when (result) {
                    is SpeedtestResult.Success -> s.copy(speedtestRunning = false, speedtest = result.info)
                    is SpeedtestResult.ApiError -> s.copy(
                        speedtestRunning = false,
                        speedtestError = apiErrorMessage(result.code, result.error),
                    )
                    is SpeedtestResult.NetworkError -> s.copy(
                        speedtestRunning = false,
                        speedtestError = networkErrorMessage(result.kind, result.detail),
                    )
                }
            }
        }
    }

    /** Loads the Selector groups for the tunnel test; keeps a previous selection if still valid. */
    fun loadGroups() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { api.mihomoProxies() }
            if (result is MihomoProxiesResult.Success) {
                _state.update { s ->
                    val names = result.groups.map { it.name }
                    val selected = s.selectedGroup?.takeIf { it in names }
                        ?: names.firstOrNull { it.equals("Custom", ignoreCase = true) }
                        ?: names.firstOrNull()
                    s.copy(groups = result.groups, selectedGroup = selected)
                }
            }
            // Load failures stay silent here — the tunnel section simply shows no
            // group picker; the delay test itself reports errors when run.
        }
    }

    fun selectGroup(name: String) {
        _state.update { it.copy(selectedGroup = name) }
    }

    fun runGroupDelay() {
        val s = _state.value
        val group = s.selectedGroup ?: return
        if (s.delayRunning) return
        val nodes = s.groups?.firstOrNull { it.name == group }?.all
        viewModelScope.launch {
            _state.update {
                it.copy(delayRunning = true, delayError = null, delayNodes = nodes, delays = null)
            }
            val result = withContext(Dispatchers.IO) { api.mihomoGroupDelay(group) }
            _state.update { st ->
                when (result) {
                    is GroupDelayResult.Success -> st.copy(
                        delayRunning = false,
                        delays = result.delays,
                        // No group list loaded (mihomo_proxies failed) → at least
                        // show the answering nodes themselves.
                        delayNodes = st.delayNodes ?: result.delays.keys.toList(),
                    )
                    is GroupDelayResult.ApiError -> st.copy(
                        delayRunning = false,
                        delayError = apiErrorMessage(result.code, result.error),
                    )
                    is GroupDelayResult.NetworkError -> st.copy(
                        delayRunning = false,
                        delayError = networkErrorMessage(result.kind, result.detail),
                    )
                }
            }
        }
    }

    class Factory(private val api: RouterApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DiagnosticsViewModel(api) as T
    }
}
