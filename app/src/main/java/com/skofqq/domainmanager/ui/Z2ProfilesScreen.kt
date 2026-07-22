package com.skofqq.domainmanager.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.CheckEntry
import com.skofqq.domainmanager.data.Z2Profile

private val ReachableGreen = Color(0xFF4CAF50)

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

        PullToRefreshBox(
            isRefreshing = state.isRefreshing && !firstLoad,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.error?.let { message ->
                    item { ErrorCard(message) }
                }

                if (skeletonBrush != null) {
                    items(9) { SkeletonProfileRow(skeletonBrush) }
                }

                state.profiles?.let { profiles ->
                    items(profiles, key = { it.key }) { profile ->
                        ProfileRow(
                            profile = profile,
                            value = state.shownValue(profile),
                            changed = state.pending.containsKey(profile.key),
                            enabled = !state.applying,
                            onChange = { viewModel.setPending(profile.key, it) },
                        )
                    }

                    item { Spacer(Modifier.height(4.dp)) }
                    item {
                        VoiceCard(
                            voice = voice,
                            onSelect = { viewModel.setVoiceMode(it) },
                        )
                    }

                    item { Spacer(Modifier.height(4.dp)) }
                    item {
                        FilledTonalButton(
                            onClick = { viewModel.apply() },
                            enabled = state.hasChanges && !state.applying,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (state.applying) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text(stringResource(R.string.apply))
                            }
                        }
                    }
                    item {
                        Text(
                            text = stringResource(R.string.z2_apply_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.rollback() },
                                enabled = !state.rollingBack,
                                modifier = Modifier.weight(1f),
                            ) {
                                if (state.rollingBack) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(stringResource(R.string.z2_rollback))
                                }
                            }
                            OutlinedButton(
                                onClick = { viewModel.check() },
                                enabled = !state.checking,
                                modifier = Modifier.weight(1f),
                            ) {
                                if (state.checking) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(stringResource(R.string.check_reachability))
                                }
                            }
                        }
                    }

                    state.checkResults?.let { checks ->
                        item { CheckResultsCard(checks) }
                    }
                }
            }
        }
    }
}

/** Shared by both zapret profile screens (identical look — only the wired engine differs). */
@Composable
internal fun VoiceCard(voice: VoiceModeUiState, onSelect: (String) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.voice_title),
                style = MaterialTheme.typography.titleSmall,
            )
            if (voice.mode == null && voice.loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(
                        VOICE_MODE_CUSTOM to R.string.voice_custom,
                        VOICE_MODE_BOLVAN to R.string.voice_bolvan,
                    )
                    options.forEachIndexed { index, (mode, labelRes) ->
                        SegmentedButton(
                            selected = voice.mode == mode,
                            onClick = { onSelect(mode) },
                            enabled = !voice.loading,
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        ) { Text(stringResource(labelRes)) }
                    }
                }
            }
            voice.error?.let { ErrorCard(it) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileRow(
    profile: Z2Profile,
    value: Int,
    changed: Boolean,
    enabled: Boolean,
    onChange: (Int) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.bodyLarge,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilledTonalIconButton(
                    onClick = { onChange((value - 1).coerceAtLeast(1)) },
                    shapes = IconButtonDefaults.shapes(),
                    enabled = enabled && value > 1,
                ) { Icon(Icons.Filled.Remove, contentDescription = null) }
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (changed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 28.dp),
                )
                FilledTonalIconButton(
                    onClick = { onChange((value + 1).coerceAtMost(profile.max)) },
                    shapes = IconButtonDefaults.shapes(),
                    enabled = enabled && value < profile.max,
                ) { Icon(Icons.Filled.Add, contentDescription = null) }
            }
        }
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
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

/** Shared by both zapret profile screens — checkconn's response shape is engine-agnostic. */
@Composable
internal fun CheckResultsCard(checks: List<CheckEntry>) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            checks.forEach { check ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = if (check.ok) ReachableGreen else MaterialTheme.colorScheme.error,
                        shape = CircleShape,
                        modifier = Modifier.size(8.dp),
                    ) {}
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = check.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = check.code.ifEmpty { "—" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (check.ok) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
            }
        }
    }
}
