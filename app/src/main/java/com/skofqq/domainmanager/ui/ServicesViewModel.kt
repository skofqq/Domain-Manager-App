package com.skofqq.domainmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.DevicesResult
import com.skofqq.domainmanager.data.DiskInfo
import com.skofqq.domainmanager.data.DiskInfoResult
import com.skofqq.domainmanager.data.LanDevice
import com.skofqq.domainmanager.data.OkResult
import com.skofqq.domainmanager.data.PingResult
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.data.ServiceListResult
import com.skofqq.domainmanager.data.ServiceLogResult
import com.skofqq.domainmanager.data.ServiceResult
import com.skofqq.domainmanager.data.ServiceStatus
import com.skofqq.domainmanager.data.SysInfo
import com.skofqq.domainmanager.data.SysInfoResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/** zapret and zapret2 are mutually exclusive: starting one stops+disables the other on the router. */
private val ZAPRET_SIBLING = mapOf("zapret" to "zapret2", "zapret2" to "zapret")

/** One refresh() round-trip's four parallel results, kept as typed fields (not a tuple/list) so the exhaustive `when` below still type-checks per sealed class. */
private data class RefreshResults(
    val svc: ServiceListResult,
    val sys: SysInfoResult,
    val disk: DiskInfoResult,
    val dev: DevicesResult,
)

/**
 * Router-side ping sweep over the device list. [online] is keyed by MAC (IPs can
 * change between DHCP leases); a device absent from the map was not checked yet.
 */
data class DeviceScanUiState(
    val online: Map<String, Boolean> = emptyMap(),
    val scanning: Boolean = false,
    val done: Int = 0,
    val total: Int = 0,
)

data class ServicesUiState(
    /** null until the first svc_list completes (or fails). */
    val services: List<ServiceStatus>? = null,
    /** Router health header; null until the first sys_info succeeds (card hidden). */
    val sysInfo: SysInfo? = null,
    /** /overlay usage; null until the first disk_info succeeds (row hidden, not an error). */
    val diskInfo: DiskInfo? = null,
    /** Connected LAN clients; null until the first devices call succeeds. */
    val devices: List<LanDevice>? = null,
    val isRefreshing: Boolean = false,
    /** Service with an in-flight svc_* call; its card shows a spinner. */
    val busyService: String? = null,
    val error: UiMessage? = null,
)

/** One service's log-viewer sheet state. */
data class ServiceLogUiState(
    val service: String,
    val loading: Boolean = true,
    /** null lines with loading=false and no error = request in flight finished with nothing to show yet. */
    val lines: List<String>? = null,
    val error: UiMessage? = null,
)

class ServicesViewModel(private val api: RouterApi) : ViewModel() {

    private val _state = MutableStateFlow(ServicesUiState())
    val state: StateFlow<ServicesUiState> = _state

    // Manual device-icon picks (MAC → icon key), client-side only. Mirrored into a
    // flow so the Devices list recomposes right after a pick.
    private val _deviceIcons = MutableStateFlow(api.prefs.deviceIconOverrides())
    val deviceIcons: StateFlow<Map<String, String>> = _deviceIcons

    fun setDeviceIcon(mac: String, icon: String?) {
        api.prefs.setDeviceIconOverride(mac, icon)
        _deviceIcons.value = api.prefs.deviceIconOverrides()
    }

    // --- "Online now" sweep (action=ping per device, via the router) --------------

    private val _deviceScan = MutableStateFlow(DeviceScanUiState())
    val deviceScan: StateFlow<DeviceScanUiState> = _deviceScan

    private var scanJob: Job? = null

    /**
     * Pings every listed device through the router, [SCAN_PARALLELISM] at a time —
     * never all 15+ at once. Restarting cancels the previous sweep; earlier
     * results stay on screen while fresh ones overwrite them.
     */
    fun scanDevices() {
        val devices = _state.value.devices ?: return
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _deviceScan.update { it.copy(scanning = true, done = 0, total = devices.size) }
            val semaphore = Semaphore(SCAN_PARALLELISM)
            coroutineScope {
                devices.map { device ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            val result = api.ping(device.ip)
                            // Transport/API failures leave the previous verdict in
                            // place — "unknown" is more honest than "offline" when
                            // the router itself didn't answer.
                            if (result is PingResult.Success) {
                                _deviceScan.update {
                                    it.copy(
                                        online = it.online + (device.mac to result.info.reachable),
                                        done = it.done + 1,
                                    )
                                }
                            } else {
                                _deviceScan.update { it.copy(done = it.done + 1) }
                            }
                        }
                    }
                }.awaitAll()
            }
            _deviceScan.update { it.copy(scanning = false) }
        }
    }

    // --- Favorite/pinned devices, PER ROUTER (MAC-keyed) ---------------------------

    private val _favoriteDeviceMacs = MutableStateFlow(api.prefs.favoriteDeviceMacs())
    val favoriteDeviceMacs: StateFlow<Set<String>> = _favoriteDeviceMacs

    fun toggleFavoriteDevice(mac: String) {
        api.prefs.toggleFavoriteDevice(mac)
        _favoriteDeviceMacs.value = api.prefs.favoriteDeviceMacs()
    }

    // --- Per-service log viewer -----------------------------------------------------

    private val _serviceLog = MutableStateFlow<ServiceLogUiState?>(null)
    val serviceLog: StateFlow<ServiceLogUiState?> = _serviceLog

    fun openServiceLog(service: String) {
        _serviceLog.value = ServiceLogUiState(service = service, loading = true)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { api.serviceLog(service) }
            _serviceLog.update { s ->
                // The sheet may have been closed (or reopened for a DIFFERENT
                // service) while this call was in flight.
                if (s?.service != service) return@update s
                when (result) {
                    is ServiceLogResult.Success -> s.copy(loading = false, lines = result.lines)
                    is ServiceLogResult.ApiError ->
                        s.copy(loading = false, error = apiErrorMessage(result.code, result.error))
                    is ServiceLogResult.NetworkError ->
                        s.copy(loading = false, error = networkErrorMessage(result.kind, result.detail))
                }
            }
        }
    }

    /** Re-fetches the currently open service's log (the sheet's "Refresh" button). */
    fun refreshServiceLog() {
        val service = _serviceLog.value?.service ?: return
        openServiceLog(service)
    }

    fun closeServiceLog() {
        _serviceLog.value = null
    }

    // --- Wake-on-LAN --------------------------------------------------------------

    /** One-shot toast payload: ok = "packet sent" (never "device woke up" — WoL can't know). */
    private val _wakeMessage = MutableStateFlow<UiMessage?>(null)
    val wakeMessage: StateFlow<UiMessage?> = _wakeMessage

    fun clearWakeMessage() {
        _wakeMessage.value = null
    }

    fun wake(mac: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { api.wake(mac) }
            _wakeMessage.value = when (result) {
                is OkResult.Success -> UiMessage(R.string.wake_sent)
                is OkResult.ApiError -> apiErrorMessage(result.code, result.error)
                is OkResult.NetworkError -> networkErrorMessage(result.kind, result.detail)
            }
        }
    }

    /**
     * Active router switched: drop everything from the old router (services,
     * health, devices, scan verdicts, icon picks) and re-fetch for the new one.
     */
    fun reset() {
        scanJob?.cancel()
        scanJob = null
        _state.value = ServicesUiState()
        _deviceScan.value = DeviceScanUiState()
        _deviceIcons.value = api.prefs.deviceIconOverrides()
        _favoriteDeviceMacs.value = api.prefs.favoriteDeviceMacs()
        _serviceLog.value = null
        refresh()
    }

    fun refresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            // One pull refreshes everything on the tab: services, the health
            // header, disk usage and the device list, fetched in parallel. Only
            // the service list drives the error card; the rest keep their last
            // data on a partial failure (they retry on the next refresh anyway).
            val (svc, sys, disk, dev) = withContext(Dispatchers.IO) {
                coroutineScope {
                    val svcD = async { api.listServices() }
                    val sysD = async { api.sysInfo() }
                    val diskD = async { api.diskInfo() }
                    val devD = async { api.listDevices() }
                    RefreshResults(svcD.await(), sysD.await(), diskD.await(), devD.await())
                }
            }
            _state.update { s ->
                val base = s.copy(
                    isRefreshing = false,
                    sysInfo = (sys as? SysInfoResult.Success)?.info ?: s.sysInfo,
                    diskInfo = (disk as? DiskInfoResult.Success)?.info ?: s.diskInfo,
                    devices = (dev as? DevicesResult.Success)?.devices ?: s.devices,
                )
                when (svc) {
                    is ServiceListResult.Success -> base.copy(services = svc.services)
                    is ServiceListResult.ApiError -> base.copy(
                        error = apiErrorMessage(svc.code, svc.error),
                    )
                    is ServiceListResult.NetworkError -> base.copy(
                        error = networkErrorMessage(svc.kind, svc.detail),
                    )
                }
            }
        }
    }

    fun start(service: String) = control(service, "svc_start")
    fun stop(service: String) = control(service, "svc_stop")
    fun restart(service: String) = control(service, "svc_restart")

    private fun control(service: String, action: String) {
        if (_state.value.busyService != null) return
        viewModelScope.launch {
            _state.update { it.copy(busyService = service, error = null) }
            val result = withContext(Dispatchers.IO) { api.serviceAction(service, action) }
            when (result) {
                is ServiceResult.Success -> {
                    _state.update { it.copy(services = it.services.patched(result.status)) }
                    // Starting (or restarting) one zapret engine silently stops and
                    // disables the other on the router — pull the sibling's fresh
                    // status right away so the screen doesn't show stale state.
                    val sibling = ZAPRET_SIBLING[service]
                    if (sibling != null && action != "svc_stop") {
                        val sib = withContext(Dispatchers.IO) { api.serviceAction(sibling, "svc_status") }
                        if (sib is ServiceResult.Success) {
                            _state.update { it.copy(services = it.services.patched(sib.status)) }
                        }
                    }
                    _state.update { it.copy(busyService = null) }
                }
                is ServiceResult.ApiError -> _state.update {
                    it.copy(busyService = null, error = apiErrorMessage(result.code, result.error))
                }
                is ServiceResult.NetworkError -> _state.update {
                    it.copy(busyService = null, error = networkErrorMessage(result.kind, result.detail))
                }
            }
        }
    }

    private fun List<ServiceStatus>?.patched(status: ServiceStatus): List<ServiceStatus>? =
        this?.map { if (it.service == status.service) status else it }

    class Factory(private val api: RouterApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ServicesViewModel(api) as T
    }

    companion object {
        /** Concurrent router-side pings during a device sweep. */
        private const val SCAN_PARALLELISM = 4
    }
}
