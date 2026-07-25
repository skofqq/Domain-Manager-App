package com.skofqq.domainmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.CatalogEntry
import com.skofqq.domainmanager.data.CatalogFailure
import com.skofqq.domainmanager.data.ProxyGroupsResult
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.data.RuleCatalogResult
import com.skofqq.domainmanager.data.RuleCatalogStore
import com.skofqq.domainmanager.data.RuleProvider
import com.skofqq.domainmanager.data.RuleProviderAddResult
import com.skofqq.domainmanager.data.RuleProviderListResult
import com.skofqq.domainmanager.data.RuleProviderRemoveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RuleProvidersUiState(
    /** Providers added through this app; null until the first provider_list completes. */
    val providers: List<RuleProvider>? = null,
    val isRefreshing: Boolean = false,
    val error: UiMessage? = null,
    /**
     * All proxy-groups on the router (pg_list) — the choices for both the
     * destination group and the optional download group. Null until loaded;
     * loaded together with the list so the add flow never has to wait for it.
     */
    val proxyGroups: List<String>? = null,
    val proxyGroupsError: UiMessage? = null,
    /** Provider with an in-flight provider_remove. */
    val removingProvider: String? = null,
    /** True while provider_add is in flight — the router downloads and config-tests, so it is slow. */
    val adding: Boolean = false,
    val addError: UiMessage? = null,
    /** One-shot: set right after a successful add so the flow can show what landed and close. */
    val addedProvider: RuleProvider? = null,
    /** One-shot toast text for a completed removal (also covers removed=false). */
    val removeMessage: UiMessage? = null,

    /** Live source catalog from the rules repo; null until the first load finishes. */
    val catalog: List<CatalogEntry>? = null,
    val catalogLoading: Boolean = false,
    /** When the shown [catalog] was fetched — 0 while nothing is loaded. */
    val catalogFetchedAt: Long = 0L,
    /** Non-null when [catalog] is a cache that could NOT be refreshed — shown as a notice, not a blocker. */
    val catalogStale: UiMessage? = null,
    /** Only set when there is no catalog to show at all; the custom-URL path still works. */
    val catalogError: UiMessage? = null,
)

class RuleProvidersViewModel(
    private val api: RouterApi,
    private val catalogStore: RuleCatalogStore,
) : ViewModel() {

    private val _state = MutableStateFlow(RuleProvidersUiState())
    val state: StateFlow<RuleProvidersUiState> = _state

    /**
     * Active router switched: the old router's providers and groups mean nothing
     * here. The source catalog is NOT cleared — it describes the rules repo, not
     * the router, so it stays valid across routers.
     */
    fun reset() {
        _state.value = RuleProvidersUiState(
            catalog = _state.value.catalog,
            catalogFetchedAt = _state.value.catalogFetchedAt,
            catalogStale = _state.value.catalogStale,
        )
    }

    /**
     * Loads the source catalog. Without [force] this normally costs nothing but a
     * file read — the store only reaches GitHub when its cache is missing or a day
     * old. [force] is the explicit "refresh catalog" button.
     */
    fun loadCatalog(force: Boolean = false) {
        if (_state.value.catalogLoading) return
        // A cache already on screen stays on screen while a forced refresh runs.
        if (!force && _state.value.catalog != null) return
        viewModelScope.launch {
            _state.update { it.copy(catalogLoading = true, catalogError = null) }
            val result = withContext(Dispatchers.IO) { catalogStore.load(force) }
            _state.update { s ->
                when (result) {
                    is RuleCatalogResult.Fresh -> s.copy(
                        catalogLoading = false,
                        catalog = result.catalog.entries,
                        catalogFetchedAt = result.catalog.fetchedAt,
                        catalogStale = null,
                        catalogError = null,
                    )
                    is RuleCatalogResult.Cached -> s.copy(
                        catalogLoading = false,
                        catalog = result.catalog.entries,
                        catalogFetchedAt = result.catalog.fetchedAt,
                        // failure == null just means the cache was still fresh.
                        catalogStale = result.failure?.let { catalogFailureMessage(it) },
                        catalogError = null,
                    )
                    is RuleCatalogResult.Failed -> s.copy(
                        catalogLoading = false,
                        catalogError = catalogFailureMessage(result.failure),
                    )
                }
            }
        }
    }

    /**
     * provider_list + pg_list in parallel. Only provider_list drives the screen's
     * error card; a failed pg_list just leaves the group pickers empty with their
     * own message, exactly like the Status tab treats its partial failures.
     */
    fun refresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            val providers = async(Dispatchers.IO) { api.ruleProviders() }
            val groups = async(Dispatchers.IO) { api.proxyGroups() }
            val providersResult = providers.await()
            val groupsResult = groups.await()
            _state.update { s ->
                var next = when (providersResult) {
                    is RuleProviderListResult.Success ->
                        s.copy(providers = providersResult.providers, error = null)
                    is RuleProviderListResult.ApiError ->
                        s.copy(error = apiErrorMessage(providersResult.code, providersResult.error))
                    is RuleProviderListResult.NetworkError ->
                        s.copy(error = networkErrorMessage(providersResult.kind, providersResult.detail))
                }
                next = when (groupsResult) {
                    is ProxyGroupsResult.Success ->
                        next.copy(proxyGroups = groupsResult.groups, proxyGroupsError = null)
                    is ProxyGroupsResult.ApiError -> next.copy(
                        proxyGroupsError = apiErrorMessage(groupsResult.code, groupsResult.error),
                    )
                    is ProxyGroupsResult.NetworkError -> next.copy(
                        proxyGroupsError = networkErrorMessage(groupsResult.kind, groupsResult.detail),
                    )
                }
                next.copy(isRefreshing = false)
            }
        }
    }

    /**
     * action=provider_add. Exactly one of [group]/[newGroup] must be non-null —
     * the caller's step-3 UI enforces that, this only forwards it. On success the
     * list is re-fetched so the new row comes from the router, not from local
     * assumptions, and [RuleProvidersUiState.addedProvider] tells the flow to close.
     */
    fun add(
        provider: String,
        url: String,
        behavior: String,
        format: String,
        interval: Int,
        group: String?,
        newGroup: String?,
        fetchProxy: String?,
    ) {
        if (_state.value.adding) return
        viewModelScope.launch {
            _state.update { it.copy(adding = true, addError = null, addedProvider = null) }
            val result = withContext(Dispatchers.IO) {
                api.ruleProviderAdd(
                    provider = provider,
                    url = url,
                    behavior = behavior,
                    format = format,
                    interval = interval,
                    group = group,
                    newGroup = newGroup,
                    fetchProxy = fetchProxy,
                )
            }
            when (result) {
                is RuleProviderAddResult.Success -> {
                    _state.update { it.copy(adding = false, addedProvider = result.provider) }
                    // A brand-new group also has to show up in the pickers next time.
                    refresh()
                }
                is RuleProviderAddResult.ApiError -> _state.update {
                    it.copy(adding = false, addError = apiErrorMessage(result.code, result.error))
                }
                is RuleProviderAddResult.NetworkError -> _state.update {
                    it.copy(adding = false, addError = networkErrorMessage(result.kind, result.detail))
                }
            }
        }
    }

    fun clearAddResult() {
        _state.update { it.copy(addError = null, addedProvider = null) }
    }

    /**
     * action=provider_remove. The row is dropped locally on success and the list
     * re-fetched right after; removed=false (router had nothing to remove) is
     * reported as its own message, not as an error.
     */
    fun remove(provider: String) {
        if (_state.value.removingProvider != null) return
        viewModelScope.launch {
            _state.update { it.copy(removingProvider = provider) }
            val result = withContext(Dispatchers.IO) { api.ruleProviderRemove(provider) }
            when (result) {
                is RuleProviderRemoveResult.Success -> {
                    _state.update { s ->
                        s.copy(
                            removingProvider = null,
                            providers = s.providers?.filterNot { it.provider == provider },
                            removeMessage = if (result.removed) {
                                UiMessage(R.string.provider_removed, listOf(provider))
                            } else {
                                UiMessage(R.string.provider_not_found, listOf(provider))
                            },
                        )
                    }
                    refresh()
                }
                is RuleProviderRemoveResult.ApiError -> _state.update {
                    it.copy(
                        removingProvider = null,
                        removeMessage = apiErrorMessage(result.code, result.error),
                    )
                }
                is RuleProviderRemoveResult.NetworkError -> _state.update {
                    it.copy(
                        removingProvider = null,
                        removeMessage = networkErrorMessage(result.kind, result.detail),
                    )
                }
            }
        }
    }

    fun clearRemoveMessage() {
        _state.update { it.copy(removeMessage = null) }
    }

    class Factory(
        private val api: RouterApi,
        private val catalogStore: RuleCatalogStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RuleProvidersViewModel(api, catalogStore) as T
    }
}

/** GitHub-side failures read differently from router failures — the rate limit especially. */
private fun catalogFailureMessage(failure: CatalogFailure): UiMessage = when (failure) {
    is CatalogFailure.RateLimited -> UiMessage(R.string.err_catalog_rate_limited)
    is CatalogFailure.HttpError -> UiMessage(R.string.err_catalog_http, listOf(failure.code))
    is CatalogFailure.Network -> networkErrorMessage(failure.kind, failure.detail)
}
