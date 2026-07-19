package com.skofqq.domainmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skofqq.domainmanager.data.MihomoCheckUpdateResult
import com.skofqq.domainmanager.data.MihomoUpdateInfo
import com.skofqq.domainmanager.data.OkResult
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.data.ServiceVersions
import com.skofqq.domainmanager.data.VersionsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RouterInfoUiState(
    /** null until the first action=versions answer (or failure). */
    val versions: ServiceVersions? = null,
    val versionsLoading: Boolean = false,
    val versionsError: UiMessage? = null,
    val checkingUpdate: Boolean = false,
    val updateInfo: MihomoUpdateInfo? = null,
    val checkUpdateError: UiMessage? = null,
    /** action=mihomo_update in flight — a real binary swap + restart, can take a while. */
    val updating: Boolean = false,
    val updateError: UiMessage? = null,
    /** One-shot success flag for a toast/snackbar; cleared by the screen after showing it. */
    val updateSucceeded: Boolean = false,
)

/**
 * Backs the router-info sheet opened from the full-width router bar on the
 * Domains tab: the four services' installed versions, plus the mihomo
 * check-update/update-with-confirm flow (the only service with such an action).
 */
class RouterInfoViewModel(private val api: RouterApi) : ViewModel() {

    private val _state = MutableStateFlow(RouterInfoUiState())
    val state: StateFlow<RouterInfoUiState> = _state

    /** Active router switched: old versions/update state is about a different router. */
    fun reset() {
        _state.value = RouterInfoUiState()
    }

    fun loadVersions() {
        if (_state.value.versionsLoading) return
        viewModelScope.launch {
            _state.update { it.copy(versionsLoading = true, versionsError = null) }
            val result = withContext(Dispatchers.IO) { api.versions() }
            _state.update { s ->
                when (result) {
                    is VersionsResult.Success -> s.copy(versionsLoading = false, versions = result.versions)
                    is VersionsResult.ApiError -> s.copy(
                        versionsLoading = false,
                        versionsError = apiErrorMessage(result.code, result.error),
                    )
                    is VersionsResult.NetworkError -> s.copy(
                        versionsLoading = false,
                        versionsError = networkErrorMessage(result.kind, result.detail),
                    )
                }
            }
        }
    }

    fun checkMihomoUpdate() {
        if (_state.value.checkingUpdate) return
        viewModelScope.launch {
            _state.update { it.copy(checkingUpdate = true, checkUpdateError = null) }
            val result = withContext(Dispatchers.IO) { api.mihomoCheckUpdate() }
            _state.update { s ->
                when (result) {
                    is MihomoCheckUpdateResult.Success -> s.copy(checkingUpdate = false, updateInfo = result.info)
                    is MihomoCheckUpdateResult.ApiError -> s.copy(
                        checkingUpdate = false,
                        checkUpdateError = apiErrorMessage(result.code, result.error),
                    )
                    is MihomoCheckUpdateResult.NetworkError -> s.copy(
                        checkingUpdate = false,
                        checkUpdateError = networkErrorMessage(result.kind, result.detail),
                    )
                }
            }
        }
    }

    /** Callers MUST have collected an explicit confirmation first — replaces the binary and restarts mihomo. */
    fun confirmMihomoUpdate() {
        if (_state.value.updating) return
        viewModelScope.launch {
            _state.update { it.copy(updating = true, updateError = null) }
            val result = withContext(Dispatchers.IO) { api.mihomoUpdate() }
            when (result) {
                is OkResult.Success -> {
                    _state.update { it.copy(updating = false, updateInfo = null, updateSucceeded = true) }
                    loadVersions()
                }
                is OkResult.ApiError -> _state.update {
                    it.copy(updating = false, updateError = apiErrorMessage(result.code, result.error))
                }
                is OkResult.NetworkError -> _state.update {
                    it.copy(updating = false, updateError = networkErrorMessage(result.kind, result.detail))
                }
            }
        }
    }

    fun clearUpdateSucceeded() {
        _state.update { it.copy(updateSucceeded = false) }
    }

    class Factory(private val api: RouterApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RouterInfoViewModel(api) as T
    }
}
