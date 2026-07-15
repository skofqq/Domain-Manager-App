package com.skofqq.domainmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skofqq.domainmanager.data.ApiResult
import com.skofqq.domainmanager.data.RouterApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class StatusUiState {
    object Idle : StatusUiState()
    object Loading : StatusUiState()
    data class Loaded(val mihomo: Boolean, val magitrickle: Boolean) : StatusUiState()
    data class Error(val message: String) : StatusUiState()
}

class DomainViewModel(private val api: RouterApi) : ViewModel() {

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
            _status.value = when (result) {
                is ApiResult.Success -> StatusUiState.Loaded(result.status.mihomo, result.status.magitrickle)
                is ApiResult.ApiError -> StatusUiState.Error(friendlyError(result.code, result.error))
                is ApiResult.NetworkError -> StatusUiState.Error(result.message)
            }
        }
    }

    private fun friendlyError(code: Int, error: String) = when (error) {
        "bad_token" -> "Bad token — check Settings"
        "token_not_configured" -> "Token not configured on router"
        "bad_domain" -> "Invalid domain format"
        "empty_domain" -> "Domain is empty"
        else -> "Error $code: $error"
    }

    class Factory(private val api: RouterApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DomainViewModel(api) as T
    }
}
