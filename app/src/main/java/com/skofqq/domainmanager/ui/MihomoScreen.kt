package com.skofqq.domainmanager.ui

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlin.coroutines.cancellation.CancellationException
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.MihomoConnection
import com.skofqq.domainmanager.data.MihomoGroup
import com.skofqq.domainmanager.data.MihomoNodeInfo

/**
 * mihomo drill-down: Selector groups with node switching, client-computed
 * transfer speed and the live connection list. Connection polling runs only
 * while this screen is resumed — never from the background.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MihomoScreen(
    viewModel: MihomoViewModel,
    ruleProvidersViewModel: RuleProvidersViewModel,
    onBack: () -> Unit,
) {
    // Rule providers are a push screen one level deeper than this one. Its
    // predictive-back handler is registered from inside this composition, so it
    // takes priority over ServicesScreen's child→root handler and back walks
    // providers → mihomo → Status root.
    var providersOpen by rememberSaveable { mutableStateOf(false) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    var backSwipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

    PredictiveBackHandler(enabled = providersOpen) { events ->
        try {
            events.collect {
                backProgress = it.progress
                backSwipeEdge = it.swipeEdge
            }
            providersOpen = false
        } catch (e: CancellationException) {
            throw e
        } finally {
            backProgress = 0f
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (!providersOpen || backProgress > 0f) {
            MihomoRootScreen(
                viewModel = viewModel,
                ruleProvidersViewModel = ruleProvidersViewModel,
                onBack = onBack,
                onOpenRuleProviders = { providersOpen = true },
            )
        }
        if (providersOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val scale = lerp(1f, 0.94f, backProgress)
                        scaleX = scale
                        scaleY = scale
                        translationX = lerp(
                            0f,
                            if (backSwipeEdge == BackEventCompat.EDGE_LEFT) 24f else -24f,
                            backProgress,
                        )
                        alpha = lerp(1f, 0.85f, backProgress)
                        shape = RoundedCornerShape(lerp(0f, 28f, backProgress).dp)
                        clip = true
                    },
            ) {
                RuleProvidersScreen(
                    viewModel = ruleProvidersViewModel,
                    onBack = { providersOpen = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MihomoRootScreen(
    viewModel: MihomoViewModel,
    ruleProvidersViewModel: RuleProvidersViewModel,
    onBack: () -> Unit,
    onOpenRuleProviders: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    // Count only — the providers screen owns the loading; this shows whatever it
    // already knows and a generic subtitle before the first visit.
    val providersState by ruleProvidersViewModel.state.collectAsState()
    // Name, not the object: the sheet must show fresh "now" after a re-fetch.
    var sheetGroupName by rememberSaveable { mutableStateOf<String?>(null) }

    // Which source device the connection list is filtered to; null = all.
    var sourceFilter by rememberSaveable { mutableStateOf<String?>(null) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshGroups()
        viewModel.refreshDeviceNames()
        viewModel.startPolling()
        onPauseOrDispose { viewModel.stopPolling() }
    }

    // Re-test latency for the group whose sheet is currently open, and kick off
    // the separate live IPv6 check (item 10 — never cached, re-fetched on every
    // entry); clear both away when it closes so a reopen never flashes stale data.
    LaunchedEffect(sheetGroupName) {
        val name = sheetGroupName
        if (name != null) {
            viewModel.loadDelays(name)
            viewModel.loadNodeIpv6()
        } else {
            viewModel.clearDelays()
            viewModel.clearNodeIpv6()
        }
    }

    StatusChildScaffold("mihomo", onBack) { padding ->
        val groupsBrush = if (state.groups == null && state.groupsError == null) shimmerBrush() else null

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // --- Traffic (client-side delta between two polls) — kept on top so the
            // live speed is visible without scrolling past the group list. ---
            item { SectionLabel(stringResource(R.string.section_traffic)) }
            item {
                TrafficCard(
                    downSpeed = state.downSpeedBps,
                    upSpeed = state.upSpeedBps,
                    downTotal = state.downloadTotal,
                    upTotal = state.uploadTotal,
                    samples = state.trafficSamples,
                )
            }

            // --- Proxy groups ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel(stringResource(R.string.section_proxy_groups))
                    Spacer(Modifier.weight(1f))
                    ListGridToggle(
                        grid = viewModel.groupsGrid,
                        onChange = { viewModel.setGroupsGridMode(it) },
                    )
                }
            }
            state.groupsError?.let { message ->
                item {
                    Column {
                        ErrorCard(message)
                        TextButton(onClick = { viewModel.refreshGroups() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
            if (groupsBrush != null) {
                items(3) { SkeletonGroupRow(groupsBrush) }
            }
            state.groups?.let { groups ->
                if (viewModel.groupsGrid) {
                    // Same cards and tap behavior, just packed two per row.
                    items(groups.chunked(2), key = { it.first().name }) { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { group ->
                                GroupGridCard(
                                    group = group,
                                    busy = state.selectingGroup == group.name,
                                    onClick = { sheetGroupName = group.name },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                } else {
                    items(groups, key = { it.name }) { group ->
                        GroupCard(
                            group = group,
                            busy = state.selectingGroup == group.name,
                            onClick = { sheetGroupName = group.name },
                        )
                    }
                }
            }

            // --- Rule providers (external rule-sets bound to a proxy group) ---
            item { SectionLabel(stringResource(R.string.section_rule_providers)) }
            item {
                RuleProvidersEntryCard(
                    count = providersState.providers?.size,
                    onClick = onOpenRuleProviders,
                )
            }

            // --- Active connections ---
            item { SectionLabel(stringResource(R.string.section_connections)) }
            state.connectionsError?.let { message ->
                item { ErrorCard(message) }
            }
            val connections = state.connections
            // Per-source-device grouping. IMPORTANT: these numbers cover ONLY the
            // traffic that flows through the mihomo proxy — not the device's whole
            // traffic; the caption below the chips says so explicitly.
            val bySource = connections.orEmpty()
                .filter { it.sourceIP.isNotEmpty() }
                .groupBy { it.sourceIP }
            if (bySource.isNotEmpty()) {
                item {
                    SourceDeviceChips(
                        bySource = bySource,
                        deviceNames = state.deviceNames,
                        selected = sourceFilter,
                        onSelect = { sourceFilter = it },
                    )
                }
                item {
                    Text(
                        stringResource(R.string.mihomo_only_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // A filtered-out device (all its connections closed) resets to "all".
            val activeFilter = sourceFilter?.takeIf { it in bySource }
            val shownConnections = if (activeFilter != null) {
                connections.orEmpty().filter { it.sourceIP == activeFilter }
            } else connections
            if (activeFilter != null) {
                val filtered = bySource[activeFilter].orEmpty()
                item {
                    Text(
                        stringResource(
                            R.string.source_traffic_summary,
                            state.deviceNames[activeFilter] ?: activeFilter,
                            formatBytes(filtered.sumOf { it.download }),
                            formatBytes(filtered.sumOf { it.upload }),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            when {
                shownConnections == null && state.connectionsError == null -> item {
                    val brush = shimmerBrush()
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(4) { SkeletonGroupRow(brush) }
                    }
                }
                shownConnections != null && shownConnections.isEmpty() -> item {
                    Text(
                        stringResource(R.string.no_connections),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                shownConnections != null -> items(shownConnections, key = { it.id }) { conn ->
                    ConnectionCard(
                        connection = conn,
                        sourceName = conn.sourceIP.takeIf { it.isNotEmpty() }
                            ?.let { state.deviceNames[it] ?: it },
                        closing = state.closingId == conn.id,
                        closeEnabled = state.closingId == null,
                        onClose = { viewModel.closeConnection(conn.id) },
                    )
                }
            }
        }
    }

    // Node picker for the tapped group. Resolved from live state so a completed
    // switch immediately re-marks the active node.
    val sheetGroup = state.groups?.firstOrNull { it.name == sheetGroupName }
    if (sheetGroupName != null && sheetGroup != null) {
        ModalBottomSheet(
            onDismissRequest = {
                sheetGroupName = null
                viewModel.clearSelectError()
            },
        ) {
            GroupSheetContent(
                group = sheetGroup,
                nodes = state.nodes,
                selecting = state.selectingGroup == sheetGroup.name,
                selectError = state.selectError,
                nodesGrid = viewModel.nodesGrid,
                onNodesGridChange = { viewModel.setNodesGridMode(it) },
                delayGroup = state.delayGroup,
                delaysLoading = state.delaysLoading,
                nodeDelays = state.nodeDelays,
                delaysError = state.delaysError,
                nodeIpv6 = state.nodeIpv6,
                onPick = { proxy ->
                    viewModel.select(sheetGroup.name, proxy) { sheetGroupName = null }
                },
            )
        }
    }
}

/**
 * List/grid switcher, shared by the Proxy Groups section and the node-picker
 * sheet: a pill track with a filled circle behind whichever mode is active —
 * plain tint alone read as "no visible difference" between the two states.
 */
@Composable
private fun ListGridToggle(grid: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(2.dp),
    ) {
        ToggleIcon(
            selected = !grid,
            icon = Icons.AutoMirrored.Filled.ViewList,
            contentDescription = stringResource(R.string.groups_view_list),
            onClick = { onChange(false) },
        )
        ToggleIcon(
            selected = grid,
            icon = Icons.Filled.GridView,
            contentDescription = stringResource(R.string.groups_view_grid),
            onClick = { onChange(true) },
        )
    }
}

@Composable
private fun ToggleIcon(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(MaterialTheme.colorScheme.primary) else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Compact 2-column variant of [GroupCard]; same content, same tap → node sheet. */
@Composable
private fun GroupGridCard(
    group: MihomoGroup,
    busy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(onClick = onClick, modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                }
            }
            Text(
                text = group.now,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun GroupCard(group: MihomoGroup, busy: Boolean, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = group.now,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GroupSheetContent(
    group: MihomoGroup,
    nodes: Map<String, MihomoNodeInfo>,
    selecting: Boolean,
    selectError: UiMessage?,
    nodesGrid: Boolean,
    onNodesGridChange: (Boolean) -> Unit,
    /** Which group [nodeDelays] belongs to — null/mismatched means "not for this group yet". */
    delayGroup: String?,
    delaysLoading: Boolean,
    nodeDelays: Map<String, Int>?,
    delaysError: UiMessage?,
    /** node → has-IPv6 from the separate live check; null while still loading/not yet run. */
    nodeIpv6: Map<String, Boolean>?,
    onPick: (String) -> Unit,
) {
    // Only trust the delay numbers when they were actually tested for THIS group.
    val delaysForGroup = nodeDelays.takeIf { delayGroup == group.name }
    LazyColumn(
        modifier = Modifier.navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 10.dp).size(18.dp),
                        strokeWidth = 2.dp,
                    )
                }
                ListGridToggle(grid = nodesGrid, onChange = onNodesGridChange)
            }
        }
        selectError?.let { message ->
            item {
                ErrorCard(message, Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            }
        }
        if (delaysLoading && delayGroup == group.name) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }
        }
        delaysError?.let { message ->
            item { ErrorCard(message, Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
        }
        if (nodesGrid) {
            items(group.all.chunked(2), key = { it.first() }) { pair ->
                // height(IntrinsicSize.Max) + fillMaxHeight on each card: without
                // it, a card with extra content (UDP/TFO badges, a delay line)
                // stays taller than its row sibling instead of the shorter card
                // stretching to match.
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    pair.forEach { proxy ->
                        NodeGridCard(
                            proxy = proxy,
                            active = proxy == group.now,
                            info = nodes[proxy],
                            ipv6 = nodeIpv6?.get(proxy),
                            delayMs = delaysForGroup?.get(proxy),
                            delayTested = delaysForGroup != null,
                            enabled = !selecting,
                            onClick = { onPick(proxy) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else {
            items(group.all, key = { it }) { proxy ->
                val active = proxy == group.now
                val info = nodes[proxy]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !selecting && !active) { onPick(proxy) }
                        .padding(horizontal = 20.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = proxy,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        // Protocol/xudp/IPv6 as one pill, plus separate UDP/TFO chips —
                        // straight from mihomo's raw payload, IPv6 from the extra live check.
                        if (info != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 3.dp),
                            ) {
                                ProtocolBadge(info, ipv6 = nodeIpv6?.get(proxy))
                                if (info.udp) NodeBadge("UDP")
                                if (info.tfo) NodeBadge("TFO")
                            }
                        }
                    }
                    NodeDelayText(
                        delayMs = delaysForGroup?.get(proxy),
                        tested = delaysForGroup != null,
                        modifier = Modifier.padding(end = 10.dp),
                    )
                    if (active) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 2-column node card for the grid mode: name, protocol badges, delay, active check. */
@Composable
private fun NodeGridCard(
    proxy: String,
    active: Boolean,
    info: MihomoNodeInfo?,
    ipv6: Boolean?,
    delayMs: Int?,
    delayTested: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        enabled = enabled && !active,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = proxy,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (active) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            if (info != null && (info.type.isNotEmpty() || info.udp || info.tfo)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 3.dp),
                ) {
                    ProtocolBadge(info, ipv6 = ipv6)
                    if (info.udp) NodeBadge("UDP")
                    if (info.tfo) NodeBadge("TFO")
                }
            }
            NodeDelayText(delayMs = delayMs, tested = delayTested, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** ms badge / "not responding" / nothing before the first test — same convention as Diagnostics. */
@Composable
private fun NodeDelayText(delayMs: Int?, tested: Boolean, modifier: Modifier = Modifier) {
    if (!tested) return
    if (delayMs != null) {
        Text(
            text = stringResource(R.string.ms_value, delayMs),
            style = MaterialTheme.typography.labelMedium,
            color = when {
                delayMs < 100 -> NodeDelayGood
                delayMs < 300 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            },
            modifier = modifier,
        )
    } else {
        Text(
            text = stringResource(R.string.node_no_response),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier,
        )
    }
}

private val NodeDelayGood = androidx.compose.ui.graphics.Color(0xFF4CAF50)

@Composable
private fun TrafficCard(
    downSpeed: Long?,
    upSpeed: Long?,
    downTotal: Long?,
    upTotal: Long?,
    samples: List<TrafficSample>,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row {
                TrafficColumn(
                    icon = Icons.Filled.ArrowDownward,
                    labelRes = R.string.traffic_download,
                    speed = downSpeed,
                    total = downTotal,
                    modifier = Modifier.weight(1f),
                )
                TrafficColumn(
                    icon = Icons.Filled.ArrowUpward,
                    labelRes = R.string.traffic_upload,
                    speed = upSpeed,
                    total = upTotal,
                    modifier = Modifier.weight(1f),
                )
            }
            // Live rolling chart over the client-side poll buffer — needs at
            // least two deltas to draw a line.
            if (samples.size >= 2) {
                Spacer(Modifier.height(12.dp))
                TrafficChart(
                    samples = samples,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                )
                Text(
                    stringResource(R.string.traffic_chart_caption),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/**
 * Rolling speed chart over the last ~5 minutes of polls: download as a filled
 * primary line, upload as a tertiary line on the same auto-scaled axis. The
 * buffer fills left-to-right until [samples] hits its cap, then scrolls.
 */
@Composable
private fun TrafficChart(samples: List<TrafficSample>, modifier: Modifier = Modifier) {
    val downColor = MaterialTheme.colorScheme.primary
    val upColor = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = modifier) {
        val max = samples.maxOf { maxOf(it.downBps, it.upBps) }.coerceAtLeast(1L).toFloat()
        // Fixed horizontal scale (the buffer cap), so the chart fills up in real
        // time instead of stretching a few points across the full width.
        val slots = (samples.size - 1).coerceAtLeast(119)
        val stepX = size.width / slots
        fun lineFor(value: (TrafficSample) -> Long): Path {
            val path = Path()
            samples.forEachIndexed { index, sample ->
                val x = index * stepX
                val y = size.height * (1f - value(sample) / max)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            return path
        }
        val lastX = (samples.size - 1) * stepX
        val downLine = lineFor { it.downBps }
        val downFill = Path().apply {
            addPath(downLine)
            lineTo(lastX, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(downFill, downColor.copy(alpha = 0.15f))
        drawPath(downLine, downColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        drawPath(
            lineFor { it.upBps },
            upColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

/**
 * Filter chips for the connection list, one per source device (matched to
 * action=devices by IP) plus "all". Labels show the mihomo-only connection count.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SourceDeviceChips(
    bySource: Map<String, List<MihomoConnection>>,
    deviceNames: Map<String, String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.source_all)) },
        )
        // Busiest devices first — stable, meaningful order between polls.
        bySource.entries
            .sortedByDescending { entry -> entry.value.sumOf { it.download + it.upload } }
            .forEach { (ip, conns) ->
                FilterChip(
                    selected = selected == ip,
                    onClick = { onSelect(if (selected == ip) null else ip) },
                    label = {
                        Text(
                            "${deviceNames[ip] ?: ip} · ${conns.size}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
    }
}

/**
 * Protocol/xudp/IPv6 as ONE pill (item 10): lowercase `type`, "/xudp" if the node
 * supports it, "/IPv6" appended once the separate live check confirms it — never
 * two badges for this. [ipv6] is null while that check hasn't landed yet (or found
 * nothing for this node); the pill quietly renders without the suffix until then,
 * then crossfades in the extended label so the change reads as a light update
 * rather than a jump cut.
 */
@Composable
private fun ProtocolBadge(info: MihomoNodeInfo, ipv6: Boolean?) {
    val label = buildString {
        if (info.type.isNotEmpty()) append(info.type.lowercase())
        if (info.xudp) {
            if (isNotEmpty()) append("/")
            append("xudp")
        }
        if (ipv6 == true) {
            if (isNotEmpty()) append("/")
            append("IPv6")
        }
    }
    if (label.isEmpty()) return
    Crossfade(targetState = label, label = "protocol-badge") { text ->
        NodeBadge(text)
    }
}

/** Tiny outlined capability label (protocol name, UDP, TFO) in the node sheet. */
@Composable
private fun NodeBadge(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

@Composable
private fun TrafficColumn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    labelRes: Int,
    speed: Long?,
    total: Long?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                stringResource(labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            // Two polls are needed before the first delta exists.
            text = if (speed != null) stringResource(R.string.speed_value, formatBytes(speed)) else "…",
            style = MaterialTheme.typography.titleMedium,
        )
        total?.let {
            Text(
                stringResource(R.string.traffic_total, formatBytes(it)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConnectionCard(
    connection: MihomoConnection,
    /** Resolved source device name (or bare IP); null when mihomo sent no sourceIP. */
    sourceName: String?,
    closing: Boolean,
    closeEnabled: Boolean,
    onClose: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = connection.host,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (connection.chains.isNotEmpty()) {
                    Text(
                        text = connection.chains.joinToString(" › "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = buildString {
                        append("↓ ${formatBytes(connection.download)} · ↑ ${formatBytes(connection.upload)}")
                        if (sourceName != null) append(" · $sourceName")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (closing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(12.dp)
                        .size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                IconButton(onClick = onClose, enabled = closeEnabled) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close_connection),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SkeletonGroupRow(brush: androidx.compose.ui.graphics.Brush) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SkeletonBox(brush, Modifier.fillMaxWidth(0.5f).height(16.dp))
            SkeletonBox(brush, Modifier.fillMaxWidth(0.35f).height(12.dp))
        }
    }
}
