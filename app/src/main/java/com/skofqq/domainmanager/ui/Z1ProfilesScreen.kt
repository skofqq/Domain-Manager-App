package com.skofqq.domainmanager.ui

import androidx.compose.foundation.layout.width
import com.skofqq.domainmanager.ui.theme.Helm
import androidx.compose.ui.text.font.FontWeight
import com.skofqq.domainmanager.ui.components.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.Z1Profile

/**
 * zapret v1 category switcher: 4 fixed categories (string keys, no proto tag),
 * each with its own 0..max strategy range (0 = a legitimate default state, so
 * unlike [Z2ProfilesScreen] the stepper allows it), plus the same voice-traffic
 * mode toggle. z1profile_apply has no rollback and takes effect immediately per
 * new connection — no restart-warning caption, no "Roll back" button. Only
 * reachable from the Status tab's zapret card while zapret is the detected
 * running engine (see ServicesScreen's onOpen wiring) — the same svc_list-driven
 * gating already used to auto-pick the Strategies engine tab.
 */
@Composable
fun Z1ProfilesScreen(viewModel: Z1ProfilesViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val voice by viewModel.voice.collectAsState()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        viewModel.refreshVoice()
        onPauseOrDispose { }
    }

    StatusChildScaffold(stringResource(R.string.z1_profiles_title), onBack) { padding ->
        val firstLoad = state.profiles == null && state.error == null
        val skeletonBrush = if (firstLoad) shimmerBrush() else null

        HelmPullToRefreshBox(
            isRefreshing = state.isRefreshing && !firstLoad,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                state.error?.let { message ->
                    item { ErrorCard(message) }
                }

                if (skeletonBrush != null) {
                    item { SkeletonBox(skeletonBrush, Modifier.fillMaxWidth().height(260.dp), CardShape) }
                }

                state.profiles?.let { profiles ->
                    item {
                        ProfilesSection(pendingCount = state.pending.size) {
                            profiles.forEachIndexed { index, profile ->
                                if (index > 0) SectionDivider()
                                CategoryRow(
                                    profile = profile,
                                    value = state.shownValue(profile),
                                    changed = state.pending.containsKey(profile.key),
                                    enabled = !state.applying,
                                    onChange = { viewModel.setPending(profile.key, it) },
                                )
                            }
                        }
                    }

                    item {
                        VoiceCard(
                            voice = voice,
                            onSelect = { viewModel.setVoiceMode(it) },
                        )
                    }

                    // No rollback button here: z1profile_apply has no undo, unlike
                    // zapret2's z2profile_rollback.
                    item {
                        ProfileActionsSection(
                            hint = null,
                            canApply = state.hasChanges,
                            applying = state.applying,
                            onApply = { viewModel.apply() },
                            checking = state.checking,
                            onCheck = { viewModel.check() },
                            checks = state.checkResults,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    profile: Z1Profile,
    value: Int,
    changed: Boolean,
    enabled: Boolean,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = profile.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Helm.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // No protocol badge: zapret v1 categories carry no proto tag,
            // unlike zapret2's profile rows.
            Text(
                text = stringResource(R.string.profile_caption, profile.key, profile.strategy),
                style = MaterialTheme.typography.bodySmall,
                color = Helm.colors.text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        // 0 is a legitimate value here (unlike the 1..max floor of zapret2).
        Stepper(
            value = value,
            onChange = onChange,
            min = 0,
            max = profile.max,
            enabled = enabled,
            changed = changed,
            decreaseLabel = stringResource(R.string.strategy_previous),
            increaseLabel = stringResource(R.string.strategy_next),
        )
    }
}
