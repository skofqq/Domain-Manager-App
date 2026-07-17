package com.skofqq.domainmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.ApiResult
import com.skofqq.domainmanager.data.DomainStatus
import com.skofqq.domainmanager.data.ListResult
import com.skofqq.domainmanager.data.RouterApi
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
import org.json.JSONArray
import org.json.JSONObject

data class BackupUiState(
    val building: Boolean = false,
    /** Pretty-printed snapshot JSON, ready to copy/share. */
    val snapshot: String? = null,
    /** routing / zapret2 / zapret entry counts of the built snapshot. */
    val snapshotCounts: Triple<Int, Int, Int>? = null,
    val importing: Boolean = false,
    /** done / total while an import is running. */
    val importProgress: Pair<Int, Int>? = null,
    /** Final "added · existed · failed" summary (or a parse error). */
    val importResult: UiMessage? = null,
    val error: UiMessage? = null,
)

/**
 * Settings → Backup. Export reads the router state (action=list + strat_list for
 * both engines) into one versioned JSON snapshot; import replays a snapshot (or a
 * plain domain-per-line list) through action=add / strat_add. Entries that already
 * match the router state are skipped — that also avoids pointless daemon restarts
 * on a running zapret engine.
 */
class BackupViewModel(private val api: RouterApi) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state

    private val engines = listOf(ENGINE_ZAPRET2, ENGINE_ZAPRET)

    // --- Export -----------------------------------------------------------------

    fun buildSnapshot() {
        if (_state.value.building) return
        viewModelScope.launch {
            _state.update { it.copy(building = true, error = null, snapshot = null, snapshotCounts = null) }

            val routing = withContext(Dispatchers.IO) { api.listDomains() }
            val routingList = when (routing) {
                is ListResult.Success -> routing.domains
                is ListResult.ApiError -> return@launch fail(apiErrorMessage(routing.code, routing.error))
                is ListResult.NetworkError -> return@launch fail(networkErrorMessage(routing.kind, routing.detail))
            }

            val strategyLists = mutableMapOf<String, List<StrategyEntry>>()
            for (engine in engines) {
                val result = withContext(Dispatchers.IO) { api.listStrategies(engine) }
                strategyLists[engine] = when (result) {
                    is StrategyListResult.Success -> result.domains
                    is StrategyListResult.ApiError -> return@launch fail(apiErrorMessage(result.code, result.error))
                    is StrategyListResult.NetworkError -> return@launch fail(networkErrorMessage(result.kind, result.detail))
                }
            }

            val json = JSONObject().apply {
                put("version", 1)
                put("routing", JSONArray().apply {
                    routingList.forEach { d ->
                        put(JSONObject().apply {
                            put("domain", d.domain)
                            put("mihomo", d.mihomo)
                            put("magitrickle", d.magitrickle)
                        })
                    }
                })
                put("strategies", JSONObject().apply {
                    engines.forEach { engine ->
                        put(engine, JSONArray().apply {
                            strategyLists.getValue(engine).forEach { s ->
                                put(JSONObject().apply {
                                    put("domain", s.domain)
                                    put("strategy", s.strategy)
                                })
                            }
                        })
                    }
                })
            }

            _state.update {
                it.copy(
                    building = false,
                    snapshot = json.toString(2),
                    snapshotCounts = Triple(
                        routingList.size,
                        strategyLists.getValue(ENGINE_ZAPRET2).size,
                        strategyLists.getValue(ENGINE_ZAPRET).size,
                    ),
                )
            }
        }
    }

    private fun fail(message: UiMessage) {
        _state.update { it.copy(building = false, importing = false, importProgress = null, error = message) }
    }

    // --- Import -----------------------------------------------------------------

    private data class RoutingItem(val domain: String, val mihomo: Boolean, val magitrickle: Boolean)
    private data class StrategyItem(val engine: String, val domain: String, val strategy: Int)
    private data class ImportPlan(val routing: List<RoutingItem>, val strategies: List<StrategyItem>)

    /** Snapshot JSON → plan; anything else → domain-per-line with the default target. */
    private fun parse(text: String): ImportPlan? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("{")) {
            return try {
                val json = JSONObject(trimmed)
                val routing = mutableListOf<RoutingItem>()
                json.optJSONArray("routing")?.let { array ->
                    for (i in 0 until array.length()) {
                        val entry = array.getJSONObject(i)
                        routing += RoutingItem(
                            domain = entry.getString("domain"),
                            mihomo = entry.optBoolean("mihomo", false),
                            magitrickle = entry.optBoolean("magitrickle", false),
                        )
                    }
                }
                val strategies = mutableListOf<StrategyItem>()
                json.optJSONObject("strategies")?.let { engines0 ->
                    for (engine in engines) {
                        val array = engines0.optJSONArray(engine) ?: continue
                        for (i in 0 until array.length()) {
                            val entry = array.getJSONObject(i)
                            strategies += StrategyItem(engine, entry.getString("domain"), entry.getInt("strategy"))
                        }
                    }
                }
                ImportPlan(routing, strategies)
            } catch (_: Exception) {
                null
            }
        }
        // Plain list: one domain (or URL — same heuristic as the share flow) per line.
        val mihomo = api.prefs.defaultTarget != "magitrickle"
        val magitrickle = api.prefs.defaultTarget != "mihomo"
        val domains = trimmed.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { extractDomain(it) }
            .distinct()
        if (domains.isEmpty()) return null
        return ImportPlan(domains.map { RoutingItem(it, mihomo, magitrickle) }, emptyList())
    }

    fun runImport(text: String) {
        if (_state.value.importing) return
        val plan = parse(text)
        if (plan == null) {
            _state.update { it.copy(importResult = UiMessage(R.string.backup_parse_error), error = null) }
            return
        }
        val total = plan.routing.size + plan.strategies.size
        if (total == 0) {
            _state.update { it.copy(importResult = UiMessage(R.string.backup_empty), error = null) }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(importing = true, importProgress = 0 to total, importResult = null, error = null)
            }

            // Pre-fetch the current router state so already-present entries are counted
            // as "existed" and skipped without a network call (and without a daemon
            // restart on a running engine).
            val current: Map<String, DomainStatus> = if (plan.routing.isNotEmpty()) {
                when (val result = withContext(Dispatchers.IO) { api.listDomains() }) {
                    is ListResult.Success -> result.domains.associateBy { it.domain }
                    is ListResult.ApiError -> return@launch fail(apiErrorMessage(result.code, result.error))
                    is ListResult.NetworkError -> return@launch fail(networkErrorMessage(result.kind, result.detail))
                }
            } else emptyMap()

            val currentStrategies = mutableMapOf<String, Map<String, Int>>()
            val strategyMax = mutableMapOf<String, Int>()
            for (engine in plan.strategies.map { it.engine }.distinct()) {
                when (val result = withContext(Dispatchers.IO) { api.listStrategies(engine) }) {
                    is StrategyListResult.Success -> {
                        currentStrategies[engine] = result.domains.associate { it.domain to it.strategy }
                        strategyMax[engine] = result.max
                    }
                    is StrategyListResult.ApiError -> return@launch fail(apiErrorMessage(result.code, result.error))
                    is StrategyListResult.NetworkError -> return@launch fail(networkErrorMessage(result.kind, result.detail))
                }
            }

            var added = 0
            var existed = 0
            var failed = 0
            var done = 0
            fun progress() {
                done++
                _state.update { it.copy(importProgress = done to total) }
            }

            for (item in plan.routing) {
                val target = when {
                    item.mihomo && item.magitrickle -> "both"
                    item.mihomo -> "mihomo"
                    item.magitrickle -> "magitrickle"
                    else -> null
                }
                val cur = current[item.domain]
                when {
                    target == null -> failed++
                    cur != null &&
                        (!item.mihomo || cur.mihomo) &&
                        (!item.magitrickle || cur.magitrickle) -> existed++
                    else -> {
                        val result = withContext(Dispatchers.IO) { api.callApi(item.domain, "add", target) }
                        if (result is ApiResult.Success) added++ else failed++
                    }
                }
                progress()
            }

            for (item in plan.strategies) {
                val min = if (item.engine == ENGINE_ZAPRET) 1 else 0
                val max = strategyMax[item.engine]
                when {
                    item.strategy < min || (max != null && item.strategy > max) -> failed++
                    currentStrategies[item.engine]?.get(item.domain) == item.strategy -> existed++
                    else -> {
                        val result = withContext(Dispatchers.IO) {
                            api.strategyAction(item.engine, item.domain, "strat_add", item.strategy)
                        }
                        if (result is StrategyResult.Success) added++ else failed++
                    }
                }
                progress()
            }

            _state.update {
                it.copy(
                    importing = false,
                    importProgress = null,
                    importResult = UiMessage(R.string.backup_result, listOf(added, existed, failed)),
                )
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(error = null, importResult = null) }
    }

    class Factory(private val api: RouterApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = BackupViewModel(api) as T
    }
}
