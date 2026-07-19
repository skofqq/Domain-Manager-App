package com.skofqq.domainmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skofqq.domainmanager.data.MagitrickleGroup
import com.skofqq.domainmanager.data.MagitrickleGroupsResult
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.util.extractDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MagitrickleGroupsUiState(
    /** null until the first magitrickle_groups completes (or fails). */
    val groups: List<MagitrickleGroup>? = null,
    val isRefreshing: Boolean = false,
    val error: UiMessage? = null,
)

/**
 * ALL MagiTrickle rule groups (action=magitrickle_groups), not just the Custom
 * group this app otherwise manages. Backs two things: the read-only groups
 * overview screen, and the "already in group X" typing hint on the Domains tab.
 */
class MagitrickleGroupsViewModel(private val api: RouterApi) : ViewModel() {

    private val _state = MutableStateFlow(MagitrickleGroupsUiState())
    val state: StateFlow<MagitrickleGroupsUiState> = _state

    /** Active router switched: forget the old router's groups. */
    fun reset() {
        _state.value = MagitrickleGroupsUiState()
    }

    fun refresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            val result = withContext(Dispatchers.IO) { api.magitrickleGroups() }
            _state.update { s ->
                when (result) {
                    is MagitrickleGroupsResult.Success -> s.copy(isRefreshing = false, groups = result.groups)
                    is MagitrickleGroupsResult.ApiError -> s.copy(
                        isRefreshing = false,
                        error = apiErrorMessage(result.code, result.error),
                    )
                    is MagitrickleGroupsResult.NetworkError -> s.copy(
                        isRefreshing = false,
                        error = networkErrorMessage(result.kind, result.detail),
                    )
                }
            }
        }
    }

    class Factory(private val api: RouterApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MagitrickleGroupsViewModel(api) as T
    }
}

/**
 * First group (other than Custom — that one already has its own dedicated
 * mihomo/MagiTrickle indicators on the domain row) whose rules already list
 * [input] as a domain. null when there's no match, the groups haven't loaded
 * yet, or [input] doesn't parse as a domain at all — keeps this silent while
 * the field is still just being used to filter/type.
 */
fun matchMagitrickleGroup(groups: List<MagitrickleGroup>?, input: String): String? {
    if (groups.isNullOrEmpty()) return null
    val domain = extractDomain(input) ?: return null
    return groups.firstOrNull { g ->
        !g.name.equals("Custom", ignoreCase = true) &&
            g.rules.any { it.rule.equals(domain, ignoreCase = true) }
    }?.name
}
