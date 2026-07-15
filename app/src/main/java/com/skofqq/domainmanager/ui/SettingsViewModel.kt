package com.skofqq.domainmanager.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skofqq.domainmanager.data.PrefsStore
import com.skofqq.domainmanager.data.RouterApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(private val prefs: PrefsStore, private val api: RouterApi) : ViewModel() {

    var host by mutableStateOf(prefs.routerHost)
    var port by mutableStateOf(prefs.routerPort.toString())
    var token by mutableStateOf(prefs.token)
    var target by mutableStateOf(prefs.defaultTarget)

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult

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
            _testResult.value = "Testing…"
            _testResult.value = withContext(Dispatchers.IO) { api.testConnection(h, p, t) }
        }
    }

    class Factory(private val prefs: PrefsStore, private val api: RouterApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(prefs, api) as T
    }
}
