package com.skofqq.domainmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.data.ServiceListResult
import com.skofqq.domainmanager.data.ServiceResult
import com.skofqq.domainmanager.data.ServiceStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** zapret and zapret2 are mutually exclusive: starting one stops+disables the other on the router. */
private val ZAPRET_SIBLING = mapOf("zapret" to "zapret2", "zapret2" to "zapret")

data class ServicesUiState(
    /** null until the first svc_list completes (or fails). */
    val services: List<ServiceStatus>? = null,
    val isRefreshing: Boolean = false,
    /** Service with an in-flight svc_* call; its card shows a spinner. */
    val busyService: String? = null,
    val error: UiMessage? = null,
)

class ServicesViewModel(private val api: RouterApi) : ViewModel() {

    private val _state = MutableStateFlow(ServicesUiState())
    val state: StateFlow<ServicesUiState> = _state

    fun refresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            val result = withContext(Dispatchers.IO) { api.listServices() }
            _state.update { s ->
                when (result) {
                    is ServiceListResult.Success -> s.copy(
                        isRefreshing = false,
                        services = result.services,
                    )
                    is ServiceListResult.ApiError -> s.copy(
                        isRefreshing = false,
                        error = apiErrorMessage(result.code, result.error),
                    )
                    is ServiceListResult.NetworkError -> s.copy(
                        isRefreshing = false,
                        error = networkErrorMessage(result.kind, result.detail),
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
}
