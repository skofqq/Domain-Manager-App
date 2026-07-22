package com.skofqq.domainmanager.ui

import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.data.VoiceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

const val VOICE_MODE_CUSTOM = "custom"
const val VOICE_MODE_BOLVAN = "bolvan"

data class VoiceModeUiState(
    val mode: String? = null,
    val loading: Boolean = false,
    val error: UiMessage? = null,
)

/**
 * Discord/WhatsApp/Telegram voice-traffic handling toggle (action=voice_status /
 * voice_set), shared by the zapret and zapret2 profile screens — each owns one
 * instance bound to its own engine string. Plain helper, not a ViewModel: the
 * owning ViewModel drives it from its own viewModelScope.
 */
class VoiceModeController(private val api: RouterApi, private val engine: String) {

    private val _state = MutableStateFlow(VoiceModeUiState())
    val state: StateFlow<VoiceModeUiState> = _state

    fun reset() {
        _state.value = VoiceModeUiState()
    }

    suspend fun refresh() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        val result = withContext(Dispatchers.IO) { api.voiceStatus(engine) }
        _state.update { applyResult(it, result) }
    }

    suspend fun setMode(mode: String) {
        if (_state.value.loading || _state.value.mode == mode) return
        _state.update { it.copy(loading = true, error = null) }
        val result = withContext(Dispatchers.IO) { api.voiceSet(engine, mode) }
        _state.update { applyResult(it, result) }
    }

    private fun applyResult(current: VoiceModeUiState, result: VoiceResult): VoiceModeUiState = when (result) {
        // The new mode comes FROM the response, never from the value we sent —
        // the router is the source of truth.
        is VoiceResult.Success -> current.copy(loading = false, mode = result.info.mode)
        is VoiceResult.ApiError ->
            current.copy(loading = false, error = apiErrorMessage(result.code, result.error))
        is VoiceResult.NetworkError ->
            current.copy(loading = false, error = networkErrorMessage(result.kind, result.detail))
    }
}
