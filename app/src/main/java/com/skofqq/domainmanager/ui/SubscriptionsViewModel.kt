package com.skofqq.domainmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.MihomoSubscription
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.data.SubscriptionAddResult
import com.skofqq.domainmanager.data.SubscriptionListResult
import com.skofqq.domainmanager.data.SubscriptionRemoveResult
import com.skofqq.domainmanager.data.SubscriptionUpdateResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SubscriptionsUiState(
    /** null until the first subscription_list completes. */
    val subscriptions: List<MihomoSubscription>? = null,
    val isRefreshing: Boolean = false,
    val error: UiMessage? = null,
    /** Subscription with an in-flight subscription_remove. */
    val removingSubscription: String? = null,
    /** True while subscription_add / subscription_update is in flight — both are slow. */
    val saving: Boolean = false,
    val saveError: UiMessage? = null,
    /** One-shot: set right after a successful add/update so the sheet can close. */
    val savedSubscription: MihomoSubscription? = null,
    /** One-shot toast text for a completed removal (also covers removed=false). */
    val removeMessage: UiMessage? = null,
)

/**
 * mihomo proxy-provider subscriptions. Every subscription feeds the same shared
 * node pool all proxy-groups already draw from — adding one makes its nodes
 * selectable everywhere, there is no per-group binding to configure.
 */
class SubscriptionsViewModel(private val api: RouterApi) : ViewModel() {

    private val _state = MutableStateFlow(SubscriptionsUiState())
    val state: StateFlow<SubscriptionsUiState> = _state

    /** Active router switched: the old router's subscriptions mean nothing here. */
    fun reset() {
        _state.value = SubscriptionsUiState()
    }

    fun refresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            val result = withContext(Dispatchers.IO) { api.subscriptions() }
            _state.update { s ->
                when (result) {
                    is SubscriptionListResult.Success ->
                        s.copy(isRefreshing = false, subscriptions = result.subscriptions, error = null)
                    is SubscriptionListResult.ApiError -> s.copy(
                        isRefreshing = false,
                        error = apiErrorMessage(result.code, result.error),
                    )
                    is SubscriptionListResult.NetworkError -> s.copy(
                        isRefreshing = false,
                        error = networkErrorMessage(result.kind, result.detail),
                    )
                }
            }
        }
    }

    /**
     * action=subscription_add. The caller's sheet has already validated the name
     * and URL against the router's own rules, so a rejection here is a genuine
     * router-side conflict (name taken, config test failed).
     */
    fun add(name: String, url: String) {
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, saveError = null, savedSubscription = null) }
            val result = withContext(Dispatchers.IO) { api.subscriptionAdd(name, url) }
            when (result) {
                is SubscriptionAddResult.Success -> {
                    _state.update { it.copy(saving = false, savedSubscription = result.subscription) }
                    refresh()
                }
                is SubscriptionAddResult.ApiError -> _state.update {
                    it.copy(saving = false, saveError = apiErrorMessage(result.code, result.error))
                }
                is SubscriptionAddResult.NetworkError -> _state.update {
                    it.copy(saving = false, saveError = networkErrorMessage(result.kind, result.detail))
                }
            }
        }
    }

    /**
     * action=subscription_update — changes only the URL; the name is immutable on
     * the router (a "rename" is remove + add). Slow: mihomo re-fetches the node
     * list right away instead of waiting for the next scheduled refresh.
     */
    fun update(name: String, url: String) {
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, saveError = null, savedSubscription = null) }
            val result = withContext(Dispatchers.IO) { api.subscriptionUpdate(name, url) }
            when (result) {
                is SubscriptionUpdateResult.Success -> {
                    _state.update { it.copy(saving = false, savedSubscription = result.subscription) }
                    refresh()
                }
                is SubscriptionUpdateResult.ApiError -> _state.update {
                    it.copy(saving = false, saveError = apiErrorMessage(result.code, result.error))
                }
                is SubscriptionUpdateResult.NetworkError -> _state.update {
                    it.copy(saving = false, saveError = networkErrorMessage(result.kind, result.detail))
                }
            }
        }
    }

    fun clearSaveResult() {
        _state.update { it.copy(saveError = null, savedSubscription = null) }
    }

    /**
     * action=subscription_remove. The base subscription is gated in the UI (its
     * row has no delete action at all), so this is never called for it.
     * removed=false — the router had nothing to remove — is its own message, not
     * an error.
     */
    fun remove(name: String) {
        if (_state.value.removingSubscription != null) return
        viewModelScope.launch {
            _state.update { it.copy(removingSubscription = name) }
            val result = withContext(Dispatchers.IO) { api.subscriptionRemove(name) }
            when (result) {
                is SubscriptionRemoveResult.Success -> {
                    _state.update { s ->
                        s.copy(
                            removingSubscription = null,
                            subscriptions = s.subscriptions?.filterNot { it.name == name },
                            removeMessage = if (result.removed) {
                                UiMessage(R.string.subscription_removed, listOf(name))
                            } else {
                                UiMessage(R.string.subscription_not_found, listOf(name))
                            },
                        )
                    }
                    refresh()
                }
                is SubscriptionRemoveResult.ApiError -> _state.update {
                    it.copy(
                        removingSubscription = null,
                        removeMessage = apiErrorMessage(result.code, result.error),
                    )
                }
                is SubscriptionRemoveResult.NetworkError -> _state.update {
                    it.copy(
                        removingSubscription = null,
                        removeMessage = networkErrorMessage(result.kind, result.detail),
                    )
                }
            }
        }
    }

    fun clearRemoveMessage() {
        _state.update { it.copy(removeMessage = null) }
    }

    class Factory(private val api: RouterApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SubscriptionsViewModel(api) as T
    }
}
