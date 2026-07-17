package com.skofqq.domainmanager.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.PrefsStore
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.data.TestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class TestUiState {
    data object Testing : TestUiState()
    data class Done(val ok: Boolean, val message: UiMessage) : TestUiState()
}

class SettingsViewModel(private val prefs: PrefsStore, private val api: RouterApi) : ViewModel() {

    var host by mutableStateOf(prefs.routerHost)
    var port by mutableStateOf(prefs.routerPort.toString())
    var token by mutableStateOf(prefs.token)
    var target by mutableStateOf(prefs.defaultTarget)

    var useDynamicColor by mutableStateOf(prefs.useDynamicColor)
        private set

    fun setDynamicColor(value: Boolean) {
        useDynamicColor = value
        prefs.useDynamicColor = value
    }

    var themeMode by mutableStateOf(prefs.themeMode)
        private set

    fun setTheme(value: String) {
        themeMode = value
        prefs.themeMode = value
    }

    /** "off" | "token" | "app" — see PrefsStore.appLockMode. Persisted immediately. */
    var appLockMode by mutableStateOf(prefs.appLockMode)
        private set

    fun setAppLock(value: String) {
        appLockMode = value
        prefs.appLockMode = value
    }

    /** Router API timeout, seconds. Persisted immediately; RouterApi picks it up per call. */
    var httpTimeoutSeconds by mutableStateOf(prefs.httpTimeoutSeconds)
        private set

    fun setHttpTimeout(value: Int) {
        val clamped = value.coerceIn(RouterApi.MIN_TIMEOUT_SECONDS, RouterApi.MAX_TIMEOUT_SECONDS)
        httpTimeoutSeconds = clamped
        prefs.httpTimeoutSeconds = clamped
    }

    private val _testState = MutableStateFlow<TestUiState?>(null)
    val testState: StateFlow<TestUiState?> = _testState

    /** Called by the UI after the result toast is shown, so it stays one-shot. */
    fun clearTestResult() {
        _testState.value = null
    }

    fun save() {
        prefs.routerHost = host.trim()
        prefs.routerPort = port.toIntOrNull()?.coerceIn(1, 65535) ?: 80
        prefs.token = token.trim()
        prefs.defaultTarget = target
    }

    fun testConnection() {
        val h = host.trim()
        val p = port.toIntOrNull()?.coerceIn(1, 65535) ?: 80
        val t = token.trim()
        viewModelScope.launch {
            _testState.value = TestUiState.Testing
            val result = withContext(Dispatchers.IO) { api.testConnection(h, p, t) }
            _testState.value = when (result) {
                is TestResult.Connected -> TestUiState.Done(true, UiMessage(R.string.test_connected))
                is TestResult.ConnectedBadToken -> TestUiState.Done(true, UiMessage(R.string.test_connected_bad_token))
                is TestResult.ConnectedNoToken -> TestUiState.Done(true, UiMessage(R.string.test_connected_no_token))
                is TestResult.HttpError -> TestUiState.Done(false, UiMessage(R.string.test_http_error, listOf(result.code)))
                is TestResult.Failed -> TestUiState.Done(false, networkErrorMessage(result.kind, result.detail))
            }
        }
    }

    class Factory(private val prefs: PrefsStore, private val api: RouterApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(prefs, api) as T
    }
}
