package com.skofqq.domainmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.ApiResult
import com.skofqq.domainmanager.data.DomainStatus
import com.skofqq.domainmanager.data.ListResult
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.util.extractDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DomainsUiState(
    /** null until the first list load completes (or fails). */
    val domains: List<DomainStatus>? = null,
    val isRefreshing: Boolean = false,
    /** Domain with an in-flight add/remove call; its row shows a spinner. */
    val busyDomain: String? = null,
    val error: UiMessage? = null,
)

class DomainsViewModel(private val api: RouterApi) : ViewModel() {

    private val _state = MutableStateFlow(DomainsUiState())
    val state: StateFlow<DomainsUiState> = _state

    val defaultTarget: String get() = api.prefs.defaultTarget

    fun refresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            val result = withContext(Dispatchers.IO) { api.listDomains() }
            _state.update { s ->
                when (result) {
                    is ListResult.Success -> s.copy(
                        isRefreshing = false,
                        domains = result.domains.sortedBy { it.domain },
                    )
                    is ListResult.ApiError -> s.copy(
                        isRefreshing = false,
                        error = apiErrorMessage(result.code, result.error),
                    )
                    is ListResult.NetworkError -> s.copy(
                        isRefreshing = false,
                        error = networkErrorMessage(result.kind, result.detail),
                    )
                }
            }
        }
    }

    /** Returns false when [input] doesn't contain a recognizable domain (field keeps its text). */
    fun addDomain(input: String): Boolean {
        val domain = extractDomain(input)
        if (domain == null) {
            _state.update { it.copy(error = UiMessage(R.string.err_bad_domain)) }
            return false
        }
        mutate(domain, action = "add", target = defaultTarget)
        return true
    }

    fun addToTarget(domain: String, target: String) = mutate(domain, "add", target)
    fun removeFromTarget(domain: String, target: String) = mutate(domain, "remove", target)

    /**
     * Rename via remove(old) + add(new) — no dedicated API endpoint. The target is
     * derived from the old entry's flags so editing never changes which systems the
     * domain belongs to. Finishes with a full list re-sync.
     */
    fun editDomain(entry: DomainStatus, newInput: String) {
        val newDomain = extractDomain(newInput)
        if (newDomain == null) {
            _state.update { it.copy(error = UiMessage(R.string.err_bad_domain)) }
            return
        }
        if (newDomain == entry.domain || _state.value.busyDomain != null) return
        val target = when {
            entry.mihomo && entry.magitrickle -> "both"
            entry.mihomo -> "mihomo"
            else -> "magitrickle"
        }
        viewModelScope.launch {
            _state.update { it.copy(busyDomain = entry.domain, error = null) }

            val removed = withContext(Dispatchers.IO) { api.callApi(entry.domain, "remove", target) }
            val removeError = when (removed) {
                is ApiResult.Success -> null
                is ApiResult.ApiError -> apiErrorMessage(removed.code, removed.error)
                is ApiResult.NetworkError -> networkErrorMessage(removed.kind, removed.detail)
            }
            if (removeError != null) {
                _state.update { it.copy(busyDomain = null, error = removeError) }
                return@launch
            }

            val added = withContext(Dispatchers.IO) { api.callApi(newDomain, "add", target) }
            _state.update { s ->
                when (added) {
                    is ApiResult.Success -> s.copy(busyDomain = null)
                    is ApiResult.ApiError -> s.copy(
                        busyDomain = null,
                        error = apiErrorMessage(added.code, added.error),
                    )
                    is ApiResult.NetworkError -> s.copy(
                        busyDomain = null,
                        error = networkErrorMessage(added.kind, added.detail),
                    )
                }
            }
            // Old domain is already removed at this point even if add failed —
            // re-sync the list from the router so the screen shows the truth.
            refresh()
        }
    }

    private fun mutate(domain: String, action: String, target: String) {
        if (_state.value.busyDomain != null) return
        viewModelScope.launch {
            _state.update { it.copy(busyDomain = domain, error = null) }
            val result = withContext(Dispatchers.IO) { api.callApi(domain, action, target) }
            _state.update { s ->
                when (result) {
                    // The response carries the post-action flags — patch the row in place
                    // instead of re-fetching the whole list.
                    is ApiResult.Success -> s.copy(
                        busyDomain = null,
                        domains = s.domains.updatedWith(result.status),
                    )
                    is ApiResult.ApiError -> s.copy(
                        busyDomain = null,
                        error = apiErrorMessage(result.code, result.error),
                    )
                    is ApiResult.NetworkError -> s.copy(
                        busyDomain = null,
                        error = networkErrorMessage(result.kind, result.detail),
                    )
                }
            }
        }
    }

    private fun List<DomainStatus>?.updatedWith(status: DomainStatus): List<DomainStatus>? {
        if (this == null) return null
        val rest = filter { it.domain != status.domain }
        return if (!status.mihomo && !status.magitrickle) rest
        else (rest + status).sortedBy { it.domain }
    }

    class Factory(private val api: RouterApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DomainsViewModel(api) as T
    }
}
