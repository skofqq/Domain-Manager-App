package com.skofqq.domainmanager.ui

import com.skofqq.domainmanager.ui.theme.Helm
import com.skofqq.domainmanager.ui.theme.Tints
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Tune
import com.skofqq.domainmanager.ui.components.*
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.CheckEntry
import com.skofqq.domainmanager.data.Z2Profile
import com.skofqq.domainmanager.ui.theme.statusOk

/**
 * zapret2 profile switcher: 9 fixed traffic-class slots, each with its own
 * personal 1..max strategy range, plus the separate voice-traffic mode toggle.
 * Only reachable from the Status tab's zapret2 card while zapret2 is the
 * detected running engine (see ServicesScreen's onOpen wiring) — zapret v1 has
 * no equivalent concept.
 */
@Composable
fun Z2ProfilesScreen(viewModel: Z2ProfilesViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val voice by viewModel.voice.collectAsState()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        viewModel.refreshVoice()
        onPauseOrDispose { }
    }

    val toast by viewModel.toast.collectAsState()
    LaunchedEffect(toast) {
        toast?.let {
            Toast.makeText(context, it.resolve(context), Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    StatusChildScaffold(stringResource(R.string.z2_profiles_title), onBack) { padding ->
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
                    item { SkeletonBox(skeletonBrush, Modifier.fillMaxWidth().height(420.dp), CardShape) }
                }

                state.profiles?.let { profiles ->
                    item {
                        ProfilesSection(pendingCount = state.pending.size) {
                            profiles.forEachIndexed { index, profile ->
                                if (index > 0) SectionDivider()
                                ProfileRow(
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

                    item {
                        ProfileActionsSection(
                            hint = stringResource(R.string.z2_apply_hint),
                            canApply = state.hasChanges,
                            applying = state.applying,
                            onApply = { viewModel.apply() },
                            rollingBack = state.rollingBack,
                            onRollback = { viewModel.rollback() },
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

/** Card holding every profile row; the subtitle counts the edits not yet applied. */
@Composable
internal fun ProfilesSection(pendingCount: Int, content: @Composable () -> Unit) {
    HelmSection(
        title = stringResource(R.string.z_profiles_section),
        subtitle = if (pendingCount > 0) {
            pluralStringResource(R.plurals.n_pending_changes, pendingCount, pendingCount)
        } else null,
        icon = Icons.Outlined.Tune,
        tint = Tints.accent,
        shape = MaterialShapes.Cookie9Sided,
        trailing = if (pendingCount > 0) {
            { ValuePill(pendingCount.toString(), Tints.accent) }
        } else null,
    ) {
        Column { content() }
    }
}

/**
 * Apply / rollback / reachability check, shared by both zapret profile screens.
 * [onRollback] null = the engine has no undo (zapret v1).
 */
@Composable
internal fun ProfileActionsSection(
    hint: String?,
    canApply: Boolean,
    applying: Boolean,
    onApply: () -> Unit,
    rollingBack: Boolean = false,
    onRollback: (() -> Unit)? = null,
    checking: Boolean,
    onCheck: () -> Unit,
    checks: List<CheckEntry>?,
) {
    HelmSection(
        title = stringResource(R.string.z_actions_section),
        subtitle = hint,
        icon = Icons.Outlined.Bolt,
        tint = Tints.orange,
        shape = MaterialShapes.Sunny,
    ) {
        HelmButton(
            text = stringResource(R.string.apply),
            onClick = onApply,
            enabled = canApply,
            loading = applying,
            icon = Icons.Filled.Check,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (onRollback != null) {
                HelmButton(
                    text = stringResource(R.string.z2_rollback),
                    onClick = onRollback,
                    loading = rollingBack,
                    icon = Icons.AutoMirrored.Filled.Undo,
                    style = ButtonStyle.Danger,
                    modifier = Modifier.weight(1f),
                )
            }
            HelmButton(
                text = stringResource(R.string.check_reachability),
                onClick = onCheck,
                loading = checking,
                icon = Icons.Outlined.TravelExplore,
                style = ButtonStyle.Tonal,
                modifier = Modifier.weight(1f),
            )
        }
        checks?.let { CheckResults(it) }
    }
}

/** Shared by both zapret profile screens (identical look, only the wired engine differs). */
@Composable
internal fun VoiceCard(voice: VoiceModeUiState, onSelect: (String) -> Unit) {
    HelmSection(
        title = stringResource(R.string.voice_title),
        icon = Icons.Outlined.RecordVoiceOver,
        tint = Tints.purple,
        shape = MaterialShapes.Clover4Leaf,
        trailing = if (voice.loading) {
            { LoadingIndicator(Modifier.size(36.dp)) }
        } else null,
    ) {
        val options = listOf(
            VOICE_MODE_CUSTOM to R.string.voice_custom,
            VOICE_MODE_BOLVAN to R.string.voice_bolvan,
        )
        if (voice.mode != null || !voice.loading) {
            PillTabs(
                labels = options.map { stringResource(it.second) },
                selected = options.indexOfFirst { it.first == voice.mode }.coerceAtLeast(0).toFloat(),
                onSelect = { onSelect(options[it].first) },
                enabled = !voice.loading,
                container = Helm.colors.surface2,
            )
        }
        voice.error?.let { ErrorCard(it) }
    }
}

@Composable
private fun ProfileRow(
    profile: Z2Profile,
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
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = profile.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Helm.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ProtoBadge(profile.proto)
                Text(
                    text = stringResource(R.string.profile_caption, profile.key, profile.strategy),
                    style = MaterialTheme.typography.bodySmall,
                    color = Helm.colors.text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Stepper(
            value = value,
            onChange = onChange,
            min = 1,
            max = profile.max,
            enabled = enabled,
            changed = changed,
            decreaseLabel = stringResource(R.string.strategy_previous),
            increaseLabel = stringResource(R.string.strategy_next),
        )
    }
}

@Composable
private fun ProtoBadge(proto: String) {
    val label = when (proto) {
        "tls" -> stringResource(R.string.z2_proto_tls)
        "udp" -> stringResource(R.string.z2_proto_udp)
        "http" -> stringResource(R.string.z2_proto_http)
        else -> proto.uppercase()
    }
    val tint = when (proto) {
        "tls" -> Tints.blue
        "udp" -> Tints.purple
        "http" -> Tints.teal
        else -> Tints.gray
    }
    ValuePill(label, tint)
}

/** Reachability results: one row per probe with a pass/fail shape and the HTTP code. */
@Composable
internal fun CheckResults(checks: List<CheckEntry>) {
    MonoBlock {
        checks.forEach { check ->
            // Pass/fail lives in the colour and the glyph: the HTTP code next to it is
            // "200" or "—", which is not a verdict. Say it in words for TalkBack.
            val okLabel = stringResource(
                if (check.ok) R.string.a11y_check_ok else R.string.a11y_check_failed
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .semantics(mergeDescendants = true) { contentDescription = okLabel },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (check.ok) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                    contentDescription = null,
                    tint = if (check.ok) Tints.green.fg else Tints.red.fg,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = check.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Helm.colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = check.code.ifEmpty { "—" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = if (check.ok) Helm.colors.text2 else Tints.red.fg,
                )
            }
        }
    }
}
