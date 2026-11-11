package com.skofqq.domainmanager.ui

import com.skofqq.domainmanager.ui.theme.Helm
import com.skofqq.domainmanager.ui.theme.Tints
import androidx.compose.material3.MaterialShapes
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.NetworkPing
import com.skofqq.domainmanager.ui.components.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.TraceHop
import com.skofqq.domainmanager.ui.theme.statusOk
import java.util.Locale

/**
 * Network diagnostics, all run BY THE ROUTER: ping/traceroute for an arbitrary
 * host, WAN speed test and the mihomo tunnel latency test. [wanIp] (when known
 * from sys_info) becomes a quick preset approximating "my provider".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel,
    wanIp: String?,
    onBack: () -> Unit,
    /** True right after opening via the "speedtest" App Shortcut — auto-runs it once. */
    autoRunSpeedtest: Boolean = false,
    onAutoRunSpeedtestConsumed: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    var host by rememberSaveable { mutableStateOf("8.8.8.8") }

    // Fresh entry into this screen (not a recomposition — LaunchedEffect(Unit)
    // only reruns when the composable actually leaves and comes back): last
    // visit's ping/traceroute results shouldn't still be showing.
    LaunchedEffect(Unit) { viewModel.resetPingTraceroute() }

    LifecycleResumeEffect(Unit) {
        viewModel.loadGroups()
        onPauseOrDispose { }
    }

    // Speed test isn't destructive and needs no confirmation — the shortcut
    // fires it straight away instead of just landing on this screen.
    LaunchedEffect(autoRunSpeedtest) {
        if (autoRunSpeedtest) {
            viewModel.runSpeedtest()
            onAutoRunSpeedtestConsumed()
        }
    }

    StatusChildScaffold(stringResource(R.string.diag_net_title), onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // --- Ping / traceroute -------------------------------------------------
            item {
                HelmSection(
                    title = stringResource(R.string.diag_host_section),
                    icon = Icons.Outlined.NetworkPing,
                    tint = Tints.blue,
                    shape = MaterialShapes.Cookie9Sided,
                ) {
                    HelmTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text(stringResource(R.string.host)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ChipFlow {
                        HelmChip("8.8.8.8", selected = host == "8.8.8.8", onClick = { host = "8.8.8.8" })
                        // WAN IP as the closest available stand-in for "my provider".
                        if (!wanIp.isNullOrEmpty()) {
                            HelmChip(
                                stringResource(R.string.diag_preset_wan, wanIp),
                                selected = host == wanIp,
                                onClick = { host = wanIp },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HelmButton(
                            text = stringResource(R.string.diag_ping),
                            onClick = { viewModel.runPing(host) },
                            enabled = host.isNotBlank(),
                            loading = state.pingRunning,
                            style = ButtonStyle.Primary,
                            modifier = Modifier.weight(1f),
                        )
                        HelmButton(
                            text = stringResource(R.string.diag_traceroute),
                            onClick = { viewModel.runTraceroute(host) },
                            enabled = host.isNotBlank(),
                            loading = state.tracerouteRunning,
                            style = ButtonStyle.Tonal,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    state.pingError?.let { ErrorCard(it) }
                    state.ping?.let { ping ->
                        val tint = if (ping.reachable) Tints.green else Tints.red
                        SectionRow(
                            title = ping.host,
                            subtitle = if (ping.reachable) {
                                stringResource(
                                    R.string.ping_result_ok,
                                    ping.lossPercent,
                                    String.format(Locale.US, "%.1f", ping.avgMs ?: 0.0),
                                )
                            } else {
                                stringResource(R.string.ping_result_fail)
                            },
                            leading = {
                                ShapeIcon(
                                    if (ping.reachable) Icons.Filled.Check else Icons.Filled.Close,
                                    tint,
                                    shape = MaterialShapes.Circle,
                                    size = 40.dp,
                                )
                            },
                            trailing = ping.avgMs?.takeIf { ping.reachable }?.let { avg ->
                                { ValuePill(stringResource(R.string.ms_value, avg.toInt()), delayTint(avg.toInt())) }
                            },
                        )
                    }
                    state.tracerouteError?.let { ErrorCard(it) }
                    state.traceroute?.let { hops ->
                        Text(
                            text = stringResource(R.string.trace_title, state.tracerouteHost.orEmpty()),
                            style = MaterialTheme.typography.titleSmall,
                            color = Helm.colors.text,
                        )
                        MonoBlock { hops.forEach { hop -> TraceHopRow(hop) } }
                    }
                }
            }

            // --- WAN speed test ----------------------------------------------------
            item {
                HelmSection(
                    title = stringResource(R.string.diag_speedtest_title),
                    subtitle = stringResource(R.string.diag_speedtest_note),
                    icon = Icons.Outlined.Speed,
                    tint = Tints.teal,
                    shape = MaterialShapes.Sunny,
                ) {
                    state.speedtest?.let { speed ->
                        BigStat(
                            value = stringResource(
                                R.string.speed_mbps,
                                String.format(Locale.US, "%.1f", speed.downloadMbps),
                            ),
                            label = stringResource(R.string.diag_speedtest_result_label),
                            tint = Tints.teal,
                        )
                    }
                    // A long synchronous call on the router (up to ~25 s): an honest
                    // indeterminate indicator, no pretend progress numbers.
                    WorkingStrip(state.speedtestRunning)
                    state.speedtestError?.let { ErrorCard(it) }
                    HelmButton(
                        text = stringResource(
                            if (state.speedtestRunning) R.string.diag_speedtest_running
                            else R.string.diag_speedtest_run
                        ),
                        onClick = { viewModel.runSpeedtest() },
                        loading = state.speedtestRunning,
                        icon = Icons.Filled.PlayArrow,
                        style = ButtonStyle.Tonal,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // --- Tunnel (mihomo group delay) ---------------------------------------
            item {
                HelmSection(
                    title = stringResource(R.string.diag_tunnel_title),
                    subtitle = stringResource(R.string.diag_tunnel_note),
                    icon = Icons.Outlined.Hub,
                    tint = Tints.purple,
                    shape = MaterialShapes.Flower,
                ) {
                    state.groups?.let { groups ->
                        ChipFlow {
                            groups.forEach { group ->
                                HelmChip(
                                    text = group.name,
                                    selected = state.selectedGroup == group.name,
                                    onClick = { viewModel.selectGroup(group.name) },
                                )
                            }
                        }
                    }
                    HelmButton(
                        text = stringResource(
                            if (state.delayRunning) R.string.diag_tunnel_running
                            else R.string.diag_tunnel_run
                        ),
                        onClick = { viewModel.runGroupDelay() },
                        enabled = state.selectedGroup != null,
                        loading = state.delayRunning,
                        icon = Icons.Filled.PlayArrow,
                        style = ButtonStyle.Tonal,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    WorkingStrip(state.delayRunning)
                    state.delayError?.let { ErrorCard(it) }
                    val delays = state.delays
                    val delayNodes = state.delayNodes
                    if (delays != null && delayNodes != null) {
                        Column {
                            delayNodes.forEachIndexed { index, node ->
                                if (index > 0) SectionDivider()
                                val ms = delays[node]
                                SectionRow(
                                    title = node,
                                    trailing = {
                                        if (ms != null) {
                                            ValuePill(stringResource(R.string.ms_value, ms), delayTint(ms))
                                        } else {
                                            // Absent from the answer = no reply within
                                            // the 5 s test window of mihomo.
                                            ValuePill(stringResource(R.string.node_no_response), Tints.red)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun delayColor(ms: Int) = when {
    ms < 100 -> statusOk
    ms < 300 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}

@Composable
private fun TraceHopRow(hop: TraceHop) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = hop.hop.toString(),
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
        )
        Text(
            // ip and ms are null together — the hop didn't answer ("*").
            text = hop.ip ?: "*",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = if (hop.ip == null) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        hop.ms?.let { ms ->
            Text(
                text = stringResource(
                    R.string.ms_value_frac,
                    String.format(Locale.US, "%.1f", ms),
                ),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun delayTint(ms: Int) = when {
    ms < 100 -> Tints.green
    ms < 300 -> Tints.amber
    else -> Tints.red
}
