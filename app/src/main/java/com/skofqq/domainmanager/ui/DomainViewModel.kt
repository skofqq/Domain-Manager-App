package com.skofqq.domainmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skofqq.domainmanager.data.ApiResult
import com.skofqq.domainmanager.data.HistoryStore
import com.skofqq.domainmanager.data.RouterApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class StatusUiState {
    data object Idle : StatusUiState()
    data object Loading : StatusUiState()
    data class Loaded(val mihomo: Boolean, val magitrickle: Boolean) : StatusUiState()
    data class Error(val message: UiMessage) : StatusUiState()
}

class DomainViewModel(
    private val api: RouterApi,
    private val history: HistoryStore? = null,
) : ViewModel() {

    private val _domain = MutableStateFlow("")
    val domain: StateFlow<String> = _domain

    private val _status = MutableStateFlow<StatusUiState>(StatusUiState.Idle)
    val statusState: StateFlow<StatusUiState> = _status

    val defaultTarget: String get() = api.prefs.defaultTarget

    fun setDomain(value: String) {
        _domain.value = value
    }

    fun resetStatus() {
        _status.value = StatusUiState.Idle
    }

    fun loadStatus() = dispatch("status")
    fun addDomain() = dispatch("add")
    fun removeDomain() = dispatch("remove")

    private fun dispatch(action: String) {
        val d = _domain.value.trim()
        if (d.isBlank()) return
        viewModelScope.launch {
            _status.value = StatusUiState.Loading
            val result = withContext(Dispatchers.IO) { api.callApi(d, action) }
            if (action == "add" && result is ApiResult.Success) {
                withContext(Dispatchers.IO) {
                    history?.logRouting(api.prefs.activeProfileId, d, defaultTarget)
                }
            }
            _status.value = when (result) {
                is ApiResult.Success -> StatusUiState.Loaded(result.status.mihomo, result.status.magitrickle)
                is ApiResult.ApiError -> StatusUiState.Error(apiErrorMessage(result.code, result.error))
                is ApiResult.NetworkError -> StatusUiState.Error(networkErrorMessage(result.kind, result.detail))
            }
        }
    }

    class Factory(
        private val api: RouterApi,
        private val history: HistoryStore? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DomainViewModel(api, history) as T
    }
}
