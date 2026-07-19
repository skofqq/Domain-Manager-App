package com.skofqq.domainmanager.ui

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
import java.util.Locale

/** Green shared with the services' running indicator. */
private val ReachableGreen = androidx.compose.ui.graphics.Color(0xFF4CAF50)

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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // --- Ping / traceroute -------------------------------------------------
            item { SectionLabel(stringResource(R.string.diag_host_section)) }
            item {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(stringResource(R.string.host)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestionChip(onClick = { host = "8.8.8.8" }, label = { Text("8.8.8.8") })
                    // WAN IP as the closest available stand-in for "my provider".
                    if (!wanIp.isNullOrEmpty()) {
                        SuggestionChip(
                            onClick = { host = wanIp },
                            label = { Text(stringResource(R.string.diag_preset_wan, wanIp)) },
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { viewModel.runPing(host) },
                        enabled = !state.pingRunning && host.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (state.pingRunning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.diag_ping))
                    }
                    FilledTonalButton(
                        onClick = { viewModel.runTraceroute(host) },
                        enabled = !state.tracerouteRunning && host.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (state.tracerouteRunning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.diag_traceroute))
                    }
                }
            }
            state.pingError?.let { item { ErrorCard(it) } }
            state.ping?.let { ping ->
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                if (ping.reachable) Icons.Filled.Check else Icons.Filled.Close,
                                contentDescription = null,
                                tint = if (ping.reachable) ReachableGreen else MaterialTheme.colorScheme.error,
                            )
                            Column {
                                Text(
                                    text = ping.host,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = if (ping.reachable) {
                                        stringResource(
                                            R.string.ping_result_ok,
                                            ping.lossPercent,
                                            String.format(Locale.US, "%.1f", ping.avgMs ?: 0.0),
                                        )
                                    } else {
                                        stringResource(R.string.ping_result_fail)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            state.tracerouteError?.let { item { ErrorCard(it) } }
            val hops = state.traceroute
            if (hops != null) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Text(
                                text = stringResource(
                                    R.string.trace_title,
                                    state.tracerouteHost.orEmpty(),
                                ),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(Modifier.height(6.dp))
                            hops.forEach { hop -> TraceHopRow(hop) }
                        }
                    }
                }
            }

            // --- WAN speed test ----------------------------------------------------
            item { HorizontalDivider() }
            item { SectionLabel(stringResource(R.string.diag_speedtest_title)) }
            item {
                Text(
                    stringResource(R.string.diag_speedtest_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                OutlinedButton(
                    onClick = { viewModel.runSpeedtest() },
                    enabled = !state.speedtestRunning,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (state.speedtestRunning) R.string.diag_speedtest_running
                            else R.string.diag_speedtest_run
                        )
                    )
                }
            }
            if (state.speedtestRunning) {
                // A long synchronous call on the router (up to ~25 s) — an honest
                // indeterminate indicator, no pretend progress numbers.
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }
            state.speedtestError?.let { item { ErrorCard(it) } }
            state.speedtest?.let { speed ->
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.speed_mbps,
                                    String.format(Locale.US, "%.1f", speed.downloadMbps),
                                ),
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = stringResource(R.string.diag_speedtest_result_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // --- Tunnel (mihomo group delay) ---------------------------------------
            item { HorizontalDivider() }
            item { SectionLabel(stringResource(R.string.diag_tunnel_title)) }
            item {
                Text(
                    stringResource(R.string.diag_tunnel_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.groups?.let { groups ->
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        groups.forEach { group ->
                            FilterChip(
                                selected = state.selectedGroup == group.name,
                                onClick = { viewModel.selectGroup(group.name) },
                                label = { Text(group.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            )
                        }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { viewModel.runGroupDelay() },
                    enabled = !state.delayRunning && state.selectedGroup != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (state.delayRunning) R.string.diag_tunnel_running
                            else R.string.diag_tunnel_run
                        )
                    )
                }
            }
            if (state.delayRunning) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }
            state.delayError?.let { item { ErrorCard(it) } }
            val delays = state.delays
            val delayNodes = state.delayNodes
            if (delays != null && delayNodes != null) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            delayNodes.forEach { node ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = node,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    val ms = delays[node]
                                    if (ms != null) {
                                        Text(
                                            text = stringResource(R.string.ms_value, ms),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = delayColor(ms),
                                        )
                                    } else {
                                        // Absent from the answer = didn't respond
                                        // within mihomo's 5 s test window.
                                        Text(
                                            text = stringResource(R.string.node_no_response),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
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
    ms < 100 -> ReachableGreen
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
