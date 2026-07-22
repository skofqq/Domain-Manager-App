package com.skofqq.domainmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.CheckEntry
import com.skofqq.domainmanager.data.CheckResult
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.data.Z2Profile
import com.skofqq.domainmanager.data.Z2ProfileListResult
import com.skofqq.domainmanager.data.Z2RollbackResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Z2ProfilesUiState(
    /** Always exactly 9 entries once loaded — the router's fixed key set "1".."9". */
    val profiles: List<Z2Profile>? = null,
    /** key → value the user picked but hasn't applied yet; empty = nothing staged. */
    val pending: Map<String, Int> = emptyMap(),
    val isRefreshing: Boolean = false,
    val applying: Boolean = false,
    val rollingBack: Boolean = false,
    val checking: Boolean = false,
    val checkResults: List<CheckEntry>? = null,
    val error: UiMessage? = null,
) {
    val hasChanges: Boolean get() = pending.isNotEmpty()

    /** Value shown on a row: the staged edit if any, else the router's last-known value. */
    fun shownValue(profile: Z2Profile): Int = pending[profile.key] ?: profile.strategy
}

/**
 * zapret2 profile switcher — the LuCI "zapret2 strategies" tab equivalent: 9 fixed
 * traffic-class slots, each with its own 1..max strategy range, plus the separate
 * voice-traffic mode toggle. Edits to the 9 slots are staged locally in
 * [Z2ProfilesUiState.pending] and sent as ONE z2profile_apply on explicit confirm —
 * an active zapret2 restarts its daemon on every apply, so this must never fire
 * per stepper tap. See [Z1ProfilesViewModel] for zapret v1's counterpart (4
 * categories, no proto tag, no rollback, immediate per-connection effect).
 */
class Z2ProfilesViewModel(private val api: RouterApi) : ViewModel() {

    private val _state = MutableStateFlow(Z2ProfilesUiState())
    val state: StateFlow<Z2ProfilesUiState> = _state

    private val voiceController = VoiceModeController(api, ENGINE_ZAPRET2)
    val voice: StateFlow<VoiceModeUiState> = voiceController.state

    /** One-shot: rollback outcome, including the neutral "nothing to roll back" case. */
    private val _toast = MutableStateFlow<UiMessage?>(null)
    val toast: StateFlow<UiMessage?> = _toast

    fun clearToast() {
        _toast.value = null
    }

    /** Active router switched: forget everything and let the screen's own entry re-fetch. */
    fun reset() {
        _state.value = Z2ProfilesUiState()
        voiceController.reset()
    }

    fun refresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            val result = withContext(Dispatchers.IO) { api.z2ProfileList() }
            _state.update { s ->
                when (result) {
                    is Z2ProfileListResult.Success -> s.copy(
                        isRefreshing = false,
                        profiles = result.profiles,
                        // A fresh list is the router's truth — abandon stale local edits.
                        pending = emptyMap(),
                    )
                    is Z2ProfileListResult.ApiError -> s.copy(
                        isRefreshing = false,
                        error = apiErrorMessage(result.code, result.error),
                    )
                    is Z2ProfileListResult.NetworkError -> s.copy(
                        isRefreshing = false,
                        error = networkErrorMessage(result.kind, result.detail),
                    )
                }
            }
        }
    }

    /** Stages a local edit for one profile row — never talks to the network. */
    fun setPending(key: String, value: Int) {
        val profile = _state.value.profiles?.firstOrNull { it.key == key } ?: return
        val clamped = value.coerceIn(1, profile.max)
        _state.update { s ->
            val next = if (clamped == profile.strategy) s.pending - key else s.pending + (key to clamped)
            s.copy(pending = next)
        }
    }

    /** Sends only the changed key:strategy pairs — z2profile_apply doesn't require all 9. */
    fun apply() {
        val s = _state.value
        if (s.applying || s.pending.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(applying = true, error = null) }
            val result = withContext(Dispatchers.IO) { api.z2ProfileApply(s.pending) }
            _state.update { cur ->
                when (result) {
                    is Z2ProfileListResult.Success -> cur.copy(
                        applying = false,
                        profiles = result.profiles,
                        pending = emptyMap(),
                    )
                    is Z2ProfileListResult.ApiError -> cur.copy(
                        applying = false,
                        error = apiErrorMessage(result.code, result.error),
                    )
                    is Z2ProfileListResult.NetworkError -> cur.copy(
                        applying = false,
                        error = networkErrorMessage(result.kind, result.detail),
                    )
                }
            }
        }
    }

    fun rollback() {
        if (_state.value.rollingBack) return
        viewModelScope.launch {
            _state.update { it.copy(rollingBack = true, error = null) }
            val result = withContext(Dispatchers.IO) { api.z2ProfileRollback() }
            when (result) {
                is Z2RollbackResult.Success -> {
                    // rolled_back:false is a normal "nothing to undo" answer, not an
                    // error — both outcomes surface as the same neutral one-shot toast.
                    _toast.value = if (result.info.rolledBack) {
                        UiMessage(R.string.z2_rollback_done)
                    } else {
                        UiMessage(R.string.z2_rollback_nothing)
                    }
                    _state.update { it.copy(rollingBack = false, pending = emptyMap()) }
                    // Rollback doesn't return the profile list itself — re-sync so the
                    // rows show the reverted values instead of stale ones.
                    if (result.info.rolledBack) refresh()
                }
                is Z2RollbackResult.ApiError -> _state.update {
                    it.copy(rollingBack = false, error = apiErrorMessage(result.code, result.error))
                }
                is Z2RollbackResult.NetworkError -> _state.update {
                    it.copy(rollingBack = false, error = networkErrorMessage(result.kind, result.detail))
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
        override fun <T : ViewModel> create(modelClass: Class<T>): T = Z2ProfilesViewModel(api) as T
    }
}
