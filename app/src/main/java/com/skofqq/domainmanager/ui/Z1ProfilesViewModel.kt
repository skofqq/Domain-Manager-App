package com.skofqq.domainmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skofqq.domainmanager.data.CheckEntry
import com.skofqq.domainmanager.data.CheckResult
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.data.Z1Profile
import com.skofqq.domainmanager.data.Z1ProfileListResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Z1ProfilesUiState(
    /** Always exactly 4 entries once loaded — the router's fixed key set. */
    val profiles: List<Z1Profile>? = null,
    /** key → value the user picked but hasn't applied yet; empty = nothing staged. */
    val pending: Map<String, Int> = emptyMap(),
    val isRefreshing: Boolean = false,
    val applying: Boolean = false,
    val checking: Boolean = false,
    val checkResults: List<CheckEntry>? = null,
    val error: UiMessage? = null,
) {
    val hasChanges: Boolean get() = pending.isNotEmpty()

    /** Value shown on a row: the staged edit if any, else the router's last-known value. */
    fun shownValue(profile: Z1Profile): Int = pending[profile.key] ?: profile.strategy
}

/**
 * zapret v1 category switcher — same idea as [Z2ProfilesViewModel] but for the
 * older engine, with real differences: 4 fixed categories with STRING keys and
 * no proto tag; the valid range is 0..max (0 = a legitimate "default strategy"
 * state, unlike zapret2's profile screen which forbids 0); z1profile_apply takes
 * effect immediately per new connection rather than restarting the whole daemon;
 * and there is NO rollback — z4r/IndeecFOX gives no undo for this endpoint.
 */
class Z1ProfilesViewModel(private val api: RouterApi) : ViewModel() {

    private val _state = MutableStateFlow(Z1ProfilesUiState())
    val state: StateFlow<Z1ProfilesUiState> = _state

    private val voiceController = VoiceModeController(api, ENGINE_ZAPRET)
    val voice: StateFlow<VoiceModeUiState> = voiceController.state

    /** Active router switched: forget everything and let the screen's own entry re-fetch. */
    fun reset() {
        _state.value = Z1ProfilesUiState()
        voiceController.reset()
    }

    fun refresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            val result = withContext(Dispatchers.IO) { api.z1ProfileList() }
            _state.update { s ->
                when (result) {
                    is Z1ProfileListResult.Success -> s.copy(
                        isRefreshing = false,
                        profiles = result.profiles,
                        // A fresh list is the router's truth — abandon stale local edits.
                        pending = emptyMap(),
                    )
                    is Z1ProfileListResult.ApiError -> s.copy(
                        isRefreshing = false,
                        error = apiErrorMessage(result.code, result.error),
                    )
                    is Z1ProfileListResult.NetworkError -> s.copy(
                        isRefreshing = false,
                        error = networkErrorMessage(result.kind, result.detail),
                    )
                }
            }
        }
    }

    /** Stages a local edit for one category row — never talks to the network. 0 is a valid value here. */
    fun setPending(key: String, value: Int) {
        val profile = _state.value.profiles?.firstOrNull { it.key == key } ?: return
        val clamped = value.coerceIn(0, profile.max)
        _state.update { s ->
            val next = if (clamped == profile.strategy) s.pending - key else s.pending + (key to clamped)
            s.copy(pending = next)
        }
    }

    /** Sends only the changed key:strategy pairs — z1profile_apply doesn't require all 4. */
    fun apply() {
        val s = _state.value
        if (s.applying || s.pending.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(applying = true, error = null) }
            val result = withContext(Dispatchers.IO) { api.z1ProfileApply(s.pending) }
            _state.update { cur ->
                when (result) {
                    is Z1ProfileListResult.Success -> cur.copy(
                        applying = false,
                        profiles = result.profiles,
                        pending = emptyMap(),
                    )
                    is Z1ProfileListResult.ApiError -> cur.copy(
                        applying = false,
                        error = apiErrorMessage(result.code, result.error),
                    )
                    is Z1ProfileListResult.NetworkError -> cur.copy(
                        applying = false,
                        error = networkErrorMessage(result.kind, result.detail),
                    )
                }
            }
        }
    }

    fun check() {
        if (_state.value.checking) return
        viewModelScope.launch {
            _state.update { it.copy(checking = true, error = null) }
            val result = withContext(Dispatchers.IO) { api.checkConnectivity() }
            _state.update { s ->
                when (result) {
                    is CheckResult.Success -> s.copy(checking = false, checkResults = result.checks)
                    is CheckResult.ApiError -> s.copy(
                        checking = false,
                        error = apiErrorMessage(result.code, result.error),
                    )
                    is CheckResult.NetworkError -> s.copy(
                        checking = false,
                        error = networkErrorMessage(result.kind, result.detail),
                    )
                }
            }
        }
    }

    fun refreshVoice() {
        viewModelScope.launch { voiceController.refresh() }
    }

    fun setVoiceMode(mode: String) {
        viewModelScope.launch { voiceController.setMode(mode) }
    }

    class Factory(private val api: RouterApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = Z1ProfilesViewModel(api) as T
    }
}
