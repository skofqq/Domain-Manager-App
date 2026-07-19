package com.skofqq.domainmanager.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skofqq.domainmanager.data.DevicesResult
import com.skofqq.domainmanager.data.GroupDelayResult
import com.skofqq.domainmanager.data.MihomoConnection
import com.skofqq.domainmanager.data.MihomoConnectionsResult
import com.skofqq.domainmanager.data.MihomoGroup
import com.skofqq.domainmanager.data.MihomoNodeInfo
import com.skofqq.domainmanager.data.MihomoProxiesResult
import com.skofqq.domainmanager.data.NodeIpv6Result
import com.skofqq.domainmanager.data.OkResult
import com.skofqq.domainmanager.data.RouterApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** How often mihomo_connections is polled while the screen is visible. */
private const val POLL_INTERVAL_MS = 2500L

/** ~5 minutes of speed samples at the 2.5 s poll interval. */
private const val MAX_TRAFFIC_SAMPLES = 120

/** One client-side speed sample (bytes/s), appended once per successful poll delta. */
data class TrafficSample(val downBps: Long, val upBps: Long)

data class MihomoUiState(
    /** Selector groups; null until the first mihomo_proxies completes. */
    val groups: List<MihomoGroup>? = null,
    val groupsLoading: Boolean = false,
    val groupsError: UiMessage? = null,
    /** Group with an in-flight mihomo_select. */
    val selectingGroup: String? = null,
    val selectError: UiMessage? = null,
    /** Client-derived speed (bytes/s) from the totals delta; null until two polls. */
    val downSpeedBps: Long? = null,
    val upSpeedBps: Long? = null,
    val downloadTotal: Long? = null,
    val uploadTotal: Long? = null,
    /** null until the first mihomo_connections completes. */
    val connections: List<MihomoConnection>? = null,
    val connectionsError: UiMessage? = null,
    /** Connection with an in-flight mihomo_connection_close. */
    val closingId: String? = null,
    /** Rolling client-side speed buffer for the traffic chart (oldest first). */
    val trafficSamples: List<TrafficSample> = emptyList(),
    /** name → protocol/UDP/TFO info for every proxy, from the same mihomo_proxies call. */
    val nodes: Map<String, MihomoNodeInfo> = emptyMap(),
    /** LAN IP → display name (from action=devices) for grouping connections by device. */
    val deviceNames: Map<String, String> = emptyMap(),
    /** Which group [nodeDelays] belongs to — a stale sheet reopen doesn't show the wrong group's numbers. */
    val delayGroup: String? = null,
    val delaysLoading: Boolean = false,
    /** node → ms for [delayGroup]; a node absent here did not answer within mihomo's 5 s test window. */
    val nodeDelays: Map<String, Int>? = null,
    val delaysError: UiMessage? = null,
    /**
     * node → has-IPv6, from the separate LIVE action=mihomo_node_ipv6 call (item 10).
     * Never persisted between sheet visits and never merged with [nodes] — deliberately
     * kept apart so a stale value can't leak in from a previous open. Null = not loaded
     * yet for this sheet visit (the badge simply omits the "/IPv6" segment until then).
     */
    val nodeIpv6: Map<String, Boolean>? = null,
    val ipv6Loading: Boolean = false,
)

class MihomoViewModel(private val api: RouterApi) : ViewModel() {

    private val _state = MutableStateFlow(MihomoUiState())
    val state: StateFlow<MihomoUiState> = _state

    /** Proxy-groups layout: list (default) or 2-column grid. Persisted immediately. */
    var groupsGrid by mutableStateOf(api.prefs.mihomoGroupsGrid)
        private set

    fun setGroupsGridMode(value: Boolean) {
        groupsGrid = value
        api.prefs.mihomoGroupsGrid = value
    }

    /** Node-picker sheet layout: list (default) or 2-column grid. Persisted immediately. */
    var nodesGrid by mutableStateOf(api.prefs.mihomoNodesGrid)
        private set

    fun setNodesGridMode(value: Boolean) {
        nodesGrid = value
        api.prefs.mihomoNodesGrid = value
    }

    private var pollJob: Job? = null
    /** timestamp + totals of the previous poll, for the speed delta. */
    private var lastSample: Triple<Long, Long, Long>? = null

    /**
     * Active router switched: forget the old router's groups/connections/chart.
     * Polling and group loading restart from the screen's lifecycle as usual.
     */
    fun reset() {
        stopPolling()
        lastSample = null
        _state.value = MihomoUiState()
    }

    fun refreshGroups() {
        if (_state.value.groupsLoading) return
        viewModelScope.launch {
            _state.update { it.copy(groupsLoading = true, groupsError = null) }
            fetchGroups()
        }
    }

    /**
     * Device list for naming connection sources ("Pavel-PC" instead of an IP).
     * Failure is silent — rows just fall back to the bare IP.
     */
    fun refreshDeviceNames() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { api.listDevices() }
            if (result is DevicesResult.Success) {
                _state.update { s ->
                    s.copy(
                        deviceNames = result.devices
                            .filter { it.name.isNotEmpty() }
                            .associate { it.ip to it.name.replace('-', ' ') },
                    )
                }
            }
        }
    }

    private suspend fun fetchGroups() {
        val result = withContext(Dispatchers.IO) { api.mihomoProxies() }
        _state.update { s ->
            when (result) {
                is MihomoProxiesResult.Success -> s.copy(
                    groupsLoading = false,
                    groups = result.groups,
                    nodes = result.nodes,
                )
                is MihomoProxiesResult.ApiError -> s.copy(
                    groupsLoading = false,
                    groupsError = apiErrorMessage(result.code, result.error),
                )
                is MihomoProxiesResult.NetworkError -> s.copy(
                    groupsLoading = false,
                    groupsError = networkErrorMessage(result.kind, result.detail),
                )
            }
        }
    }

    /**
     * Switches the group's active node and re-fetches the group list on success
     * so the UI shows the router's post-switch truth. [onSuccess] lets the screen
     * close its bottom sheet.
     */
    fun select(group: String, proxy: String, onSuccess: () -> Unit) {
        if (_state.value.selectingGroup != null) return
        viewModelScope.launch {
            _state.update { it.copy(selectingGroup = group, selectError = null) }
            val result = withContext(Dispatchers.IO) { api.mihomoSelect(group, proxy) }
            when (result) {
                is OkResult.Success -> {
                    fetchGroups()
                    _state.update { it.copy(selectingGroup = null) }
                    onSuccess()
                }
                is OkResult.ApiError -> _state.update {
                    it.copy(selectingGroup = null, selectError = apiErrorMessage(result.code, result.error))
                }
                is OkResult.NetworkError -> _state.update {
                    it.copy(selectingGroup = null, selectError = networkErrorMessage(result.kind, result.detail))
                }
            }
        }
    }

    fun clearSelectError() {
        _state.update { it.copy(selectError = null) }
    }

    /**
     * Starts the connections/traffic poll. Must be paired with [stopPolling] when
     * the screen leaves the foreground — no background polling.
     */
    fun startPolling() {
        if (pollJob?.isActive == true) return
        lastSample = null
        _state.update { it.copy(downSpeedBps = null, upSpeedBps = null) }
        pollJob = viewModelScope.launch {
            while (isActive) {
                pollConnections()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private suspend fun pollConnections() {
        val result = withContext(Dispatchers.IO) { api.mihomoConnections() }
        when (result) {
            is MihomoConnectionsResult.Success -> {
                val snap = result.snapshot
                val now = System.currentTimeMillis()
                val prev = lastSample
                var down: Long? = null
                var up: Long? = null
                if (prev != null && now > prev.first) {
                    val dt = (now - prev.first) / 1000.0
                    val dDown = snap.downloadTotal - prev.second
                    val dUp = snap.uploadTotal - prev.third
                    // Negative delta = mihomo restarted and reset its counters.
                    if (dDown >= 0 && dUp >= 0) {
                        down = (dDown / dt).toLong()
                        up = (dUp / dt).toLong()
                    }
                }
                lastSample = Triple(now, snap.downloadTotal, snap.uploadTotal)
                _state.update {
                    it.copy(
                        connections = snap.connections,
                        connectionsError = null,
                        downloadTotal = snap.downloadTotal,
                        uploadTotal = snap.uploadTotal,
                        downSpeedBps = down,
                        upSpeedBps = up,
                        // The chart buffer only grows on a valid delta (counter
                        // resets are skipped, not drawn as a negative spike).
                        trafficSamples = if (down != null && up != null) {
                            (it.trafficSamples + TrafficSample(down, up)).takeLast(MAX_TRAFFIC_SAMPLES)
                        } else it.trafficSamples,
                    )
                }
            }
            is MihomoConnectionsResult.ApiError -> _state.update {
                it.copy(connectionsError = apiErrorMessage(result.code, result.error))
            }
            is MihomoConnectionsResult.NetworkError -> _state.update {
                it.copy(connectionsError = networkErrorMessage(result.kind, result.detail))
            }
        }
    }

    /**
     * mihomo's built-in latency test for every node of [group] at once
     * (action=mihomo_group_delay). Called when the node-picker sheet opens;
     * re-running for the SAME group re-tests it (no caching — latency changes).
     */
    fun loadDelays(group: String) {
        if (_state.value.delaysLoading) return
        viewModelScope.launch {
            _state.update {
                it.copy(delayGroup = group, delaysLoading = true, delaysError = null, nodeDelays = null)
            }
            val result = withContext(Dispatchers.IO) { api.mihomoGroupDelay(group) }
            _state.update { s ->
                // A sheet close-then-reopen-on-a-different-group before this
                // returns would otherwise paint the wrong group's numbers.
                if (s.delayGroup != group) return@update s
                when (result) {
                    is GroupDelayResult.Success -> s.copy(delaysLoading = false, nodeDelays = result.delays)
                    is GroupDelayResult.ApiError -> s.copy(
                        delaysLoading = false,
                        delaysError = apiErrorMessage(result.code, result.error),
                    )
                    is GroupDelayResult.NetworkError -> s.copy(
                        delaysLoading = false,
                        delaysError = networkErrorMessage(result.kind, result.detail),
                    )
                }
            }
        }
    }

    /** Sheet dismissed: stale delay numbers shouldn't flash before the next group's load. */
    fun clearDelays() {
        _state.update {
            it.copy(delayGroup = null, delaysLoading = false, nodeDelays = null, delaysError = null)
        }
    }

    /**
     * Live per-node IPv6 check (action=mihomo_node_ipv6, item 10). Deliberately
     * separate from [fetchGroups]/[loadDelays]: it is slow (real AAAA lookups on
     * the router) and must never block the node list's first paint, so the sheet
     * kicks this off independently and the badge just fades the "/IPv6" segment
     * in once it lands. Failures are silent — the badge simply omits the segment,
     * same treatment as any other best-effort enhancement.
     */
    fun loadNodeIpv6() {
        if (_state.value.ipv6Loading) return
        viewModelScope.launch {
            _state.update { it.copy(ipv6Loading = true) }
            val result = withContext(Dispatchers.IO) { api.mihomoNodeIpv6() }
            _state.update { s ->
                when (result) {
                    is NodeIpv6Result.Success -> s.copy(ipv6Loading = false, nodeIpv6 = result.ipv6ByNode)
                    is NodeIpv6Result.ApiError, is NodeIpv6Result.NetworkError ->
                        s.copy(ipv6Loading = false)
                }
            }
        }
    }

    /** Sheet dismissed: never carry a stale IPv6 snapshot into the next open. */
    fun clearNodeIpv6() {
        _state.update { it.copy(nodeIpv6 = null, ipv6Loading = false) }
    }

    fun closeConnection(id: String) {
        if (_state.value.closingId != null) return
        viewModelScope.launch {
            _state.update { it.copy(closingId = id) }
            val result = withContext(Dispatchers.IO) { api.mihomoConnectionClose(id) }
            _state.update { s ->
                when (result) {
                    // ok:true even for an already-gone id — just drop the row; the
                    // next poll is the source of truth either way.
                    is OkResult.Success -> s.copy(
                        closingId = null,
                        connections = s.connections?.filterNot { it.id == id },
                    )
                    is OkResult.ApiError -> s.copy(
                        closingId = null,
                        connectionsError = apiErrorMessage(result.code, result.error),
                    )
                    is OkResult.NetworkError -> s.copy(
                        closingId = null,
                        connectionsError = networkErrorMessage(result.kind, result.detail),
                    )
                }
            }
        }
    }

    class Factory(private val api: RouterApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MihomoViewModel(api) as T
    }
}
