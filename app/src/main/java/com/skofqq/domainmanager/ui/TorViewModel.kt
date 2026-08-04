package com.skofqq.domainmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.data.ServiceResult
import com.skofqq.domainmanager.data.TorBridgesListResult
import com.skofqq.domainmanager.data.TorBridgesSetResult
import com.skofqq.domainmanager.data.TorNewnymResult
import com.skofqq.domainmanager.data.TorStatus
import com.skofqq.domainmanager.data.TorStatusResult
import com.skofqq.domainmanager.data.TorTestInfo
import com.skofqq.domainmanager.data.TorTestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TorUiState(
    /** null until the first tor_status completes (or fails). */
    val status: TorStatus? = null,
    /** null until the first tor_bridges_list completes; empty = direct connection. */
    val bridges: List<String>? = null,
    val isRefreshing: Boolean = false,
    val error: UiMessage? = null,
    /** True while an svc_start/stop/restart is in flight — the button row is disabled. */
    val busyService: Boolean = false,
    /** True while tor_newnym is in flight. */
    val newnymRunning: Boolean = false,
    /** True while tor_test is in flight (several seconds — a real network check). */
    val testRunning: Boolean = false,
    /**
     * Last connectivity check. ok=false is a legitimate "not connected" verdict,
     * shown as a failure card, not as an error.
     */
    val testResult: TorTestInfo? = null,
    val testError: UiMessage? = null,
    /**
     * True while a bridge change is being saved — one combined state covering both
     * tor_bridges_set AND the restart that makes it take effect.
     */
    val savingBridges: Boolean = false,
    val bridgesError: UiMessage? = null,
    /** One-shot toast for a completed bridge removal / "new identity" request. */
    val message: UiMessage? = null,
)

/**
 * Tor drill-down. Start/stop/restart go through the same generic svc_* actions
 * every other service uses — "tor" is just another entry in the router's service
 * whitelist, there is no Tor-specific toggle endpoint.
 */
class TorViewModel(private val api: RouterApi) : ViewModel() {

    private val _state = MutableStateFlow(TorUiState())
    val state: StateFlow<TorUiState> = _state

    /** Active router switched: the old router's Tor state and bridges mean nothing here. */
    fun reset() {
        _state.value = TorUiState()
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    /** tor_status + tor_bridges_list in parallel — only the status drives the error card. */
    fun refresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            val statusD = async(Dispatchers.IO) { api.torStatus() }
            val bridgesD = async(Dispatchers.IO) { api.torBridgesList() }
            val statusResult = statusD.await()
            val bridgesResult = bridgesD.await()
            _state.update { s ->
                var next = when (statusResult) {
                    is TorStatusResult.Success -> s.copy(status = statusResult.status, error = null)
                    is TorStatusResult.ApiError ->
                        s.copy(error = apiErrorMessage(statusResult.code, statusResult.error))
                    is TorStatusResult.NetworkError ->
                        s.copy(error = networkErrorMessage(statusResult.kind, statusResult.detail))
                }
                next = when (bridgesResult) {
                    is TorBridgesListResult.Success ->
                        next.copy(bridges = bridgesResult.bridges, bridgesError = null)
                    is TorBridgesListResult.ApiError -> next.copy(
                        bridgesError = apiErrorMessage(bridgesResult.code, bridgesResult.error),
                    )
                    is TorBridgesListResult.NetworkError -> next.copy(
                        bridgesError = networkErrorMessage(bridgesResult.kind, bridgesResult.detail),
                    )
                }
                next.copy(isRefreshing = false)
            }
        }
    }

    fun start() = control("svc_start")
    fun stop() = control("svc_stop")
    fun restart() = control("svc_restart")

    private fun control(action: String) {
        if (_state.value.busyService) return
        viewModelScope.launch {
            _state.update { it.copy(busyService = true, error = null) }
            val result = withContext(Dispatchers.IO) { api.serviceAction(SERVICE, action) }
            when (result) {
                is ServiceResult.Success -> {
                    // svc_* answers with running/enabled only; bootstrap, uptime and
                    // the bridge count come from tor_status, so pull a fresh one.
                    _state.update { s ->
                        s.copy(
                            busyService = false,
                            status = s.status?.copy(
                                running = result.status.running,
                                enabled = result.status.enabled,
                            ),
                        )
                    }
                    refresh()
                }
                is ServiceResult.ApiError -> _state.update {
                    it.copy(busyService = false, error = apiErrorMessage(result.code, result.error))
                }
                is ServiceResult.NetworkError -> _state.update {
                    it.copy(busyService = false, error = networkErrorMessage(result.kind, result.detail))
                }
            }
        }
    }

    /**
     * action=tor_newnym — fresh circuits for NEW connections only. Not a restart:
     * nothing already connected is dropped. ok=false carries the router's own
     * reason (usually the control port not being ready yet).
     */
    fun newIdentity() {
        if (_state.value.newnymRunning) return
        viewModelScope.launch {
            _state.update { it.copy(newnymRunning = true) }
            val result = withContext(Dispatchers.IO) { api.torNewnym() }
            _state.update {
                it.copy(
                    newnymRunning = false,
                    message = when (result) {
                        is TorNewnymResult.Success ->
                            if (result.ok) UiMessage(R.string.tor_new_identity_sent)
                            else UiMessage(R.string.tor_new_identity_failed)
                        is TorNewnymResult.ApiError -> apiErrorMessage(result.code, result.error)
                        is TorNewnymResult.NetworkError ->
                            networkErrorMessage(result.kind, result.detail)
                    },
                )
            }
        }
    }

    /**
     * action=tor_test — a real request through the SOCKS port, several seconds.
     * An ok=false answer is kept as a result (shown as "not connected"), never
     * converted into an error card.
     */
    fun testConnection() {
        if (_state.value.testRunning) return
        viewModelScope.launch {
            _state.update { it.copy(testRunning = true, testResult = null, testError = null) }
            val result = withContext(Dispatchers.IO) { api.torTest() }
            _state.update {
                when (result) {
                    is TorTestResult.Success -> it.copy(testRunning = false, testResult = result.info)
                    is TorTestResult.ApiError -> it.copy(
                        testRunning = false,
                        testError = apiErrorMessage(result.code, result.error),
                    )
                    is TorTestResult.NetworkError -> it.copy(
                        testRunning = false,
                        testError = networkErrorMessage(result.kind, result.detail),
                    )
                }
            }
        }
    }

    /**
     * Appends one bridge line and saves the whole list — there is no incremental
     * add on the router, [saveBridges] always sends the complete list. Returns
     * false when the line is blank or already there, so the caller can keep the
     * sheet open.
     */
    fun addBridge(line: String): Boolean {
        val bridge = line.trim()
        if (bridge.isEmpty()) return false
        val current = _state.value.bridges ?: return false
        if (bridge in current || _state.value.savingBridges) return false
        saveBridges(current + bridge, R.string.tor_bridge_added)
        return true
    }

    fun removeBridge(line: String) {
        val current = _state.value.bridges ?: return
        if (line !in current) return
        saveBridges(current.filterNot { it == line }, R.string.tor_bridge_removed)
    }

    /**
     * tor_bridges_set followed by a restart, as one user-visible operation:
     * tor_bridges_set only rewrites torrc, so without the restart the new bridges
     * would silently not be in use.
     */
    private fun saveBridges(bridges: List<String>, doneMessage: Int) {
        if (_state.value.savingBridges) return
        viewModelScope.launch {
            _state.update { it.copy(savingBridges = true, bridgesError = null) }
            val result = withContext(Dispatchers.IO) { api.torBridgesSet(bridges) }
            when (result) {
                is TorBridgesSetResult.Success -> {
                    _state.update { it.copy(bridges = bridges, status = result.status) }
                    val restart = withContext(Dispatchers.IO) { api.serviceAction(SERVICE, "svc_restart") }
                    _state.update { s ->
                        when (restart) {
                            is ServiceResult.Success -> s.copy(
                                savingBridges = false,
                                message = UiMessage(doneMessage),
                            )
                            is ServiceResult.ApiError -> s.copy(
                                savingBridges = false,
                                bridgesError = apiErrorMessage(restart.code, restart.error),
                            )
                            is ServiceResult.NetworkError -> s.copy(
                                savingBridges = false,
                                bridgesError = networkErrorMessage(restart.kind, restart.detail),
                            )
                        }
                    }
                    refresh()
                }
                is TorBridgesSetResult.ApiError -> _state.update {
                    it.copy(
                        savingBridges = false,
                        bridgesError = apiErrorMessage(result.code, result.error),
                    )
                }
                is TorBridgesSetResult.NetworkError -> _state.update {
                    it.copy(
                        savingBridges = false,
                        bridgesError = networkErrorMessage(result.kind, result.detail),
                    )
                }
            }
        }
    }

    class Factory(private val api: RouterApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TorViewModel(api) as T
    }

    private companion object {
        /** Router service name — already on the generic svc_* whitelist. */
        const val SERVICE = "tor"
    }
}
