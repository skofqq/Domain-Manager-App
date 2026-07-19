package com.skofqq.domainmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.HistoryStore
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.data.ServiceListResult
import com.skofqq.domainmanager.data.StrategyEntry
import com.skofqq.domainmanager.data.StrategyListResult
import com.skofqq.domainmanager.data.StrategyResult
import com.skofqq.domainmanager.util.extractDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val ENGINE_ZAPRET2 = "zapret2"
const val ENGINE_ZAPRET = "zapret"

data class EngineStrategiesUiState(
    /** Strategy upper bound from strat_list (dynamic for zapret2, 17 for zapret v1). */
    val max: Int? = null,
    /** null until the first strat_list completes. */
    val domains: List<StrategyEntry>? = null,
    val isRefreshing: Boolean = false,
    /** Domain with an in-flight strat_* mutation; its row shows a spinner. */
    val busyDomain: String? = null,
    val error: UiMessage? = null,
)

/** Which zapret engine is running, per the last svc_list. The two are mutually exclusive server-side. */
sealed class ActiveEngineUiState {
    /** No svc_list answer yet (first opening in this process). */
    data object Loading : ActiveEngineUiState()
    data object NoneRunning : ActiveEngineUiState()
    data class Running(val engine: String) : ActiveEngineUiState()
    data class Error(val message: UiMessage) : ActiveEngineUiState()
}

/**
 * Per-domain DPI-bypass strategies for the two zapret engines. Mutations
 * (strat_add/strat_set/strat_remove) restart the engine's daemon when it is
 * currently running, so they are dispatched only from explicit confirm actions —
 * one call per press, never per input change.
 */
class StrategiesViewModel(
    private val api: RouterApi,
    private val history: HistoryStore? = null,
) : ViewModel() {

    private val zapret2State = MutableStateFlow(EngineStrategiesUiState())
    private val zapretState = MutableStateFlow(EngineStrategiesUiState())

    /**
     * Re-determined via svc_list on every opening of the Strategies page. Stays on the
     * previous determination while a re-check is in flight (no Loading flicker), so
     * [ActiveEngineUiState.Loading] only shows before the very first answer.
     */
    private val _activeEngineState = MutableStateFlow<ActiveEngineUiState>(ActiveEngineUiState.Loading)
    val activeEngineState: StateFlow<ActiveEngineUiState> = _activeEngineState

    fun state(engine: String): StateFlow<EngineStrategiesUiState> = flowOf(engine)

    private fun flowOf(engine: String) =
        if (engine == ENGINE_ZAPRET) zapretState else zapret2State

    /** Minimum valid strategy: zapret v1 has no "no personal lock" state, so 0 is forbidden. */
    fun minStrategy(engine: String): Int = if (engine == ENGINE_ZAPRET) 1 else 0

    /** Active router switched: forget both engines' lists and re-determine from scratch. */
    fun reset() {
        zapret2State.value = EngineStrategiesUiState()
        zapretState.value = EngineStrategiesUiState()
        _activeEngineState.value = ActiveEngineUiState.Loading
        refreshActiveEngine()
    }

    /** svc_list → which zapret engine is running now (picks the list the page shows). */
    fun refreshActiveEngine() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { api.listServices() }
            _activeEngineState.value = when (result) {
                is ServiceListResult.Success -> {
                    val zapret2 = result.services.firstOrNull { it.service == ENGINE_ZAPRET2 }
                    val zapret = result.services.firstOrNull { it.service == ENGINE_ZAPRET }
                    when {
                        zapret2?.running == true -> ActiveEngineUiState.Running(ENGINE_ZAPRET2)
                        zapret?.running == true -> ActiveEngineUiState.Running(ENGINE_ZAPRET)
                        else -> ActiveEngineUiState.NoneRunning
                    }
                }
                is ServiceListResult.ApiError ->
                    ActiveEngineUiState.Error(apiErrorMessage(result.code, result.error))
                is ServiceListResult.NetworkError ->
                    ActiveEngineUiState.Error(networkErrorMessage(result.kind, result.detail))
            }
        }
    }

    fun refresh(engine: String) {
        val flow = flowOf(engine)
        if (flow.value.isRefreshing) return
        viewModelScope.launch {
            flow.update { it.copy(isRefreshing = true, error = null) }
            val result = withContext(Dispatchers.IO) { api.listStrategies(engine) }
            flow.update { s ->
                when (result) {
                    is StrategyListResult.Success -> s.copy(
                        isRefreshing = false,
                        max = result.max,
                        domains = result.domains.sortedBy { it.domain },
                    )
                    is StrategyListResult.ApiError -> s.copy(
                        isRefreshing = false,
                        error = apiErrorMessage(result.code, result.error),
                    )
                    is StrategyListResult.NetworkError -> s.copy(
                        isRefreshing = false,
                        error = networkErrorMessage(result.kind, result.detail),
                    )
                }
            }
        }
    }

    /** Returns false when the input isn't a recognizable domain (field keeps its text). */
    fun addDomain(engine: String, input: String, strategy: Int): Boolean {
        val domain = extractDomain(input)
        if (domain == null) {
            flowOf(engine).update { it.copy(error = UiMessage(R.string.err_bad_domain)) }
            return false
        }
        mutate(engine, domain, "strat_add", strategy)
        return true
    }

    fun setStrategy(engine: String, domain: String, strategy: Int) =
        mutate(engine, domain, "strat_set", strategy)

    fun removeDomain(engine: String, domain: String) =
        mutate(engine, domain, "strat_remove", null)

    private fun mutate(engine: String, domain: String, action: String, strategy: Int?) {
        val flow = flowOf(engine)
        if (flow.value.busyDomain != null) return

        // Client-side range check before hitting the router: an invalid value would
        // still cost a pointless daemon restart on an active engine.
        if (strategy != null) {
            val max = flow.value.max
            if (strategy < minStrategy(engine) || (max != null && strategy > max)) {
                flow.update { it.copy(error = UiMessage(R.string.err_bad_strategy)) }
                return
            }
        }

        viewModelScope.launch {
            flow.update { it.copy(busyDomain = domain, error = null) }
            val result = withContext(Dispatchers.IO) { api.strategyAction(engine, domain, action, strategy) }
            if (action == "strat_add" && result is StrategyResult.Success) {
                withContext(Dispatchers.IO) {
                    history?.logStrategy(api.prefs.activeProfileId, result.domain, engine, result.strategy)
                }
            }
            when (result) {
                is StrategyResult.Success -> {
                    if (action != "strat_remove" && result.strategy == null) {
                        // Router accepted the call but couldn't confirm the value —
                        // re-sync instead of showing a guess.
                        flow.update { it.copy(busyDomain = null, error = UiMessage(R.string.err_strategy_unconfirmed)) }
                        refresh(engine)
                    } else {
                        flow.update { s ->
                            s.copy(busyDomain = null, domains = s.domains.patched(result.domain, result.strategy))
                        }
                    }
                }
                is StrategyResult.ApiError -> flow.update {
                    it.copy(busyDomain = null, error = apiErrorMessage(result.code, result.error))
                }
                is StrategyResult.NetworkError -> flow.update {
                    it.copy(busyDomain = null, error = networkErrorMessage(result.kind, result.detail))
                }
            }
        }
    }

    private fun List<StrategyEntry>?.patched(domain: String, strategy: Int?): List<StrategyEntry>? {
        if (this == null) return null
        val rest = filter { it.domain != domain }
        return if (strategy == null) rest
        else (rest + StrategyEntry(domain, strategy)).sortedBy { it.domain }
    }

    class Factory(
        private val api: RouterApi,
        private val history: HistoryStore? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = StrategiesViewModel(api, history) as T
    }
}
