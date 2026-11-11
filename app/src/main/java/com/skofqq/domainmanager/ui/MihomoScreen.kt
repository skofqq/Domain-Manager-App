package com.skofqq.domainmanager.ui

import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.toShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.decode.SvgDecoder
import coil.ImageLoader
import android.content.Context
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.material3.LinearWavyProgressIndicator
import com.skofqq.domainmanager.ui.theme.Helm
import com.skofqq.domainmanager.ui.theme.Tint
import com.skofqq.domainmanager.ui.theme.Tints
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Hub
import com.skofqq.domainmanager.ui.components.*
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
import com.skofqq.domainmanager.ui.theme.statusOk

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
    subscriptionsViewModel: SubscriptionsViewModel,
    onBack: () -> Unit,
) {
    // Rule providers and subscriptions are push screens one level deeper than
    // this one. The predictive-back handler is registered from inside this
    // composition, so it takes priority over ServicesScreen's child→root handler
    // and back walks child → mihomo → Status root.
    var child by rememberSaveable { mutableStateOf<String?>(null) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    var backSwipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

    PredictiveBackHandler(enabled = child != null) { events ->
        try {
            events.collect {
                backProgress = it.progress
                backSwipeEdge = it.swipeEdge
            }
            child = null
        } catch (e: CancellationException) {
            throw e
        } finally {
            backProgress = 0f
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (child == null || backProgress > 0f) {
            MihomoRootScreen(
                viewModel = viewModel,
                ruleProvidersViewModel = ruleProvidersViewModel,
                subscriptionsViewModel = subscriptionsViewModel,
                onBack = onBack,
                onOpenRuleProviders = { child = "providers" },
                onOpenSubscriptions = { child = "subscriptions" },
            )
        }
        if (child != null) {
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
                when (child) {
                    "providers" -> RuleProvidersScreen(
                        viewModel = ruleProvidersViewModel,
                        onBack = { child = null },
                    )
                    "subscriptions" -> SubscriptionsScreen(
                        viewModel = subscriptionsViewModel,
                        onBack = { child = null },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MihomoRootScreen(
    viewModel: MihomoViewModel,
    ruleProvidersViewModel: RuleProvidersViewModel,
    subscriptionsViewModel: SubscriptionsViewModel,
    onBack: () -> Unit,
    onOpenRuleProviders: () -> Unit,
    onOpenSubscriptions: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    // Counts only — each child screen owns its own loading; these show whatever
    // is already known and a generic subtitle before the first visit.
    val providersState by ruleProvidersViewModel.state.collectAsState()
    val subscriptionsState by subscriptionsViewModel.state.collectAsState()
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
        ) {
            // --- Traffic (client-side delta between two polls): kept on top so the
            // live speed is visible without scrolling past the group list. ---
            item {
                TrafficCard(
                    downSpeed = state.downSpeedBps,
                    upSpeed = state.upSpeedBps,
                    downTotal = state.downloadTotal,
                    upTotal = state.uploadTotal,
                    samples = state.trafficSamples,
                )
            }
            item { Spacer(Modifier.height(14.dp)) }

            // --- Proxy groups ---
            item {
                HelmSection(
                    title = stringResource(R.string.section_proxy_groups),
                    subtitle = state.groups?.let { pluralStringResource(R.plurals.n_proxy_groups, it.size, it.size) },
                    icon = Icons.Outlined.Hub,
                    tint = Tints.accent,
                    shape = MaterialShapes.Cookie9Sided,
                    trailing = {
                        SectionIconButton(
                            Icons.Outlined.Speed,
                            stringResource(R.string.delay_test_all),
                            enabled = state.groups != null && state.testingGroups.isEmpty(),
                            tint = Tints.green,
                        ) { viewModel.testAllGroups() }
                        IconToggle(
                            first = Icons.AutoMirrored.Filled.ViewList,
                            second = Icons.Filled.GridView,
                            secondSelected = viewModel.groupsGrid,
                            onChange = { viewModel.setGroupsGridMode(it) },
                            firstLabel = stringResource(R.string.groups_view_list),
                            secondLabel = stringResource(R.string.groups_view_grid),
                        )
                    },
                ) {
                    state.groupsError?.let { message ->
                        ErrorCard(message, onRetry = { viewModel.refreshGroups() })
                    }
                    if (groupsBrush != null) {
                        repeat(3) { SkeletonBox(groupsBrush, Modifier.fillMaxWidth().height(52.dp), RoundedCornerShape(16.dp)) }
                    }
                    state.groups?.let { groups ->
                        if (viewModel.groupsGrid) {
                            // Same content and tap behavior, just packed two per row.
                            TileGrid(
                                groups.mapIndexed { index, group ->
                                    @Composable { m: Modifier ->
                                        GroupGridCard(
                                            group = group,
                                            index = index,
                                            busy = state.selectingGroup == group.name,
                                            delayMs = state.currentDelay(group),
                                            testing = group.name in state.testingGroups,
                                            onTest = { viewModel.testGroup(group.name) },
                                            onClick = { sheetGroupName = group.name },
                                            modifier = m,
                                        )
                                    }
                                },
                            )
                        } else {
                            Column {
                                groups.forEachIndexed { index, group ->
                                    if (index > 0) SectionDivider()
                                    GroupCard(
                                        group = group,
                                        index = index,
                                        busy = state.selectingGroup == group.name,
                                        delayMs = state.currentDelay(group),
                                        testing = group.name in state.testingGroups,
                                        onTest = { viewModel.testGroup(group.name) },
                                        onClick = { sheetGroupName = group.name },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(14.dp)) }

            // --- Subscriptions (the shared node pool every group selects from) and
            // rule providers (external rule-sets bound to a proxy group) ---
            item {
                SubscriptionsEntryCard(
                    count = subscriptionsState.subscriptions?.size,
                    onClick = onOpenSubscriptions,
                )
            }
            item { Spacer(Modifier.height(10.dp)) }
            item {
                RuleProvidersEntryCard(
                    count = providersState.providers?.size,
                    onClick = onOpenRuleProviders,
                )
            }
            item { Spacer(Modifier.height(14.dp)) }

            // --- Active connections: a sliced section, one lazy item per row ---
            val connections = state.connections
            // Per-source-device grouping. IMPORTANT: these numbers cover ONLY the
            // traffic that flows through the mihomo proxy, not the whole traffic of
            // the device; the caption below the chips says so explicitly.
            val bySource = connections.orEmpty()
                .filter { it.sourceIP.isNotEmpty() }
                .groupBy { it.sourceIP }
            // A filtered-out device (all its connections closed) resets to "all".
            val activeFilter = sourceFilter?.takeIf { it in bySource }
            val shownConnections = if (activeFilter != null) {
                connections.orEmpty().filter { it.sourceIP == activeFilter }
            } else connections
            val hasRows = !shownConnections.isNullOrEmpty()
            item(key = "conn-header") {
                Column(
                    Modifier
                        .sectionSlice(first = true, last = !hasRows, color = Helm.colors.card)
                        .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = if (hasRows) 6.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HelmSectionHeader(
                        title = stringResource(R.string.section_connections),
                        subtitle = stringResource(R.string.mihomo_only_note).takeIf { bySource.isNotEmpty() },
                        icon = Icons.Outlined.Lan,
                        tint = Tints.purple,
                        shape = MaterialShapes.Clover4Leaf,
                        trailing = connections?.let { { ValuePill(it.size.toString(), Tints.purple) } },
                    )
                    state.connectionsError?.let { message -> ErrorCard(message) }
                    if (bySource.isNotEmpty()) {
                        SourceDeviceChips(
                            bySource = bySource,
                            deviceNames = state.deviceNames,
                            selected = sourceFilter,
                            onSelect = { sourceFilter = it },
                        )
                    }
                    if (activeFilter != null) {
                        val filtered = bySource[activeFilter].orEmpty()
                        Text(
                            stringResource(
                                R.string.source_traffic_summary,
                                state.deviceNames[activeFilter] ?: activeFilter,
                                formatBytes(filtered.sumOf { it.download }),
                                formatBytes(filtered.sumOf { it.upload }),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Helm.colors.accent,
                        )
                    }
                    when {
                        shownConnections == null && state.connectionsError == null -> {
                            val brush = shimmerBrush()
                            repeat(4) { SkeletonBox(brush, Modifier.fillMaxWidth().height(52.dp), RoundedCornerShape(16.dp)) }
                        }
                        shownConnections != null && shownConnections.isEmpty() ->
                            SectionNote(stringResource(R.string.no_connections))
                    }
                }
            }
            if (shownConnections != null) {
                itemsIndexed(shownConnections, key = { _, conn -> conn.id }) { index, conn ->
                    val last = index == shownConnections.lastIndex
                    Column(
                        Modifier
                            .sectionSlice(first = false, last = last, color = Helm.colors.card)
                            .padding(start = 18.dp, end = 6.dp, bottom = if (last) 10.dp else 0.dp),
                    ) {
                        if (index > 0) SectionDivider()
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
    }

    // Node picker for the tapped group. Resolved from live state so a completed
    // switch immediately re-marks the active node.
    val sheetGroup = state.groups?.firstOrNull { it.name == sheetGroupName }
    if (sheetGroupName != null && sheetGroup != null) {
        HelmBottomSheet(
            onDismissRequest = {
                sheetGroupName = null
                viewModel.clearSelectError()
            },
        ) {
            GroupSheetContent(
                group = sheetGroup,
                index = state.groups.orEmpty().indexOf(sheetGroup).coerceAtLeast(0),
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
    IconToggle(
        first = Icons.AutoMirrored.Filled.ViewList,
        second = Icons.Filled.GridView,
        secondSelected = grid,
        onChange = onChange,
        firstLabel = stringResource(R.string.groups_view_list),
        secondLabel = stringResource(R.string.groups_view_grid),
    )
}

/** Compact 2-column variant of [GroupCard]; same content, same tap: node sheet. */
@Composable
private fun GroupGridCard(
    group: MihomoGroup,
    index: Int,
    busy: Boolean,
    delayMs: Int?,
    testing: Boolean,
    onTest: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Helm.colors.surface2)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GroupIcon(group, index, 36.dp)
            Spacer(Modifier.weight(1f))
            if (busy) LoadingIndicator(Modifier.size(28.dp)) else DelayPill(delayMs, testing, onTest)
        }
        Column {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleSmall,
                color = Helm.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = group.now,
                style = MaterialTheme.typography.bodySmall,
                color = Helm.colors.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun GroupCard(
    group: MihomoGroup,
    index: Int,
    busy: Boolean,
    delayMs: Int?,
    testing: Boolean,
    onTest: () -> Unit,
    onClick: () -> Unit,
) {
    SectionRow(
        title = group.name,
        subtitle = group.now,
        onClick = onClick,
        leading = { GroupIcon(group, index, 40.dp) },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (busy) {
                    LoadingIndicator(Modifier.size(32.dp))
                } else {
                    DelayPill(delayMs, testing, onTest)
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Helm.colors.text2,
                )
            }
        },
    )
}

@Composable
private fun GroupSheetContent(
    group: MihomoGroup,
    /** Position in the group list, so the sheet icon matches the colour of its row. */
    index: Int,
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
                GroupIcon(group, index, 40.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selecting) {
                    LoadingIndicator(Modifier.padding(end = 6.dp).size(36.dp))
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
                LinearWavyProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    color = Helm.colors.accent,
                    trackColor = Helm.colors.surface2,
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
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (active) Tints.accent.bg else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable(enabled = !selecting && !active) { onPick(proxy) }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
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
    HelmCard(
        onClick = onClick,
        enabled = enabled && !active,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = helmCardColors(if (active) Tints.accent.bg.compositeOver(Helm.colors.card) else Helm.colors.card),
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
        ValuePill(
            stringResource(R.string.ms_value, delayMs),
            when {
                delayMs < 100 -> Tints.green
                delayMs < 300 -> Tints.amber
                else -> Tints.red
            },
            modifier,
        )
    } else {
        ValuePill(stringResource(R.string.node_no_response), Tints.red, modifier)
    }
}

@Composable
private fun TrafficCard(
    downSpeed: Long?,
    upSpeed: Long?,
    downTotal: Long?,
    upTotal: Long?,
    samples: List<TrafficSample>,
) {
    HelmSection(
        title = stringResource(R.string.section_traffic),
        icon = Icons.Outlined.SwapVert,
        tint = Tints.teal,
        shape = MaterialShapes.Sunny,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TrafficColumn(
                icon = Icons.Filled.ArrowDownward,
                labelRes = R.string.traffic_download,
                speed = downSpeed,
                total = downTotal,
                tint = Tints.blue,
                modifier = Modifier.weight(1f),
            )
            TrafficColumn(
                icon = Icons.Filled.ArrowUpward,
                labelRes = R.string.traffic_upload,
                speed = upSpeed,
                total = upTotal,
                tint = Tints.purple,
                modifier = Modifier.weight(1f),
            )
        }
        // Live rolling chart over the client-side poll buffer: needs at least two
        // deltas to draw a line.
        if (samples.size >= 2) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Helm.colors.surface2).padding(12.dp),
            ) {
                TrafficChart(
                    samples = samples,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                )
                Text(
                    stringResource(R.string.traffic_chart_caption),
                    style = MaterialTheme.typography.labelSmall,
                    color = Helm.colors.text2,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/**
 * Rolling speed chart over the last ~5 minutes of polls: download as a filled
 * blue line, upload as a purple line on the same auto-scaled axis. The buffer
 * fills left-to-right until [samples] hits its cap, then scrolls.
 */
@Composable
private fun TrafficChart(samples: List<TrafficSample>, modifier: Modifier = Modifier) {
    val downColor = Tints.blue.fg
    val upColor = Tints.purple.fg
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
        drawPath(
            downFill,
            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(downColor.copy(alpha = 0.35f), downColor.copy(alpha = 0f))),
        )
        drawPath(downLine, downColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
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
@Composable
private fun SourceDeviceChips(
    bySource: Map<String, List<MihomoConnection>>,
    deviceNames: Map<String, String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    ChipScroller {
        HelmChip(
            text = stringResource(R.string.source_all),
            selected = selected == null,
            onClick = { onSelect(null) },
        )
        // Busiest devices first: stable, meaningful order between polls.
        bySource.entries
            .sortedByDescending { entry -> entry.value.sumOf { it.download + it.upload } }
            .forEach { (ip, conns) ->
                HelmChip(
                    text = "${deviceNames[ip] ?: ip} · ${conns.size}",
                    selected = selected == ip,
                    onClick = { onSelect(if (selected == ip) null else ip) },
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

/** Tiny capability label (protocol name, UDP, TFO) in the node sheet. */
@Composable
private fun NodeBadge(text: String) {
    ValuePill(text, Tints.gray)
}

@Composable
private fun TrafficColumn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    labelRes: Int,
    speed: Long?,
    total: Long?,
    tint: Tint,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(20.dp)).background(tint.bg).padding(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint.fg,
                modifier = Modifier.size(16.dp),
            )
            Text(
                stringResource(labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = tint.fg,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            // Two polls are needed before the first delta exists.
            text = if (speed != null) stringResource(R.string.speed_value, formatBytes(speed)) else "…",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Helm.colors.text,
        )
        total?.let {
            Text(
                stringResource(R.string.traffic_total, formatBytes(it)),
                style = MaterialTheme.typography.bodySmall,
                color = Helm.colors.text2,
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = connection.host,
                style = MaterialTheme.typography.titleSmall,
                color = Helm.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (connection.chains.isNotEmpty()) {
                Text(
                    text = connection.chains.joinToString(" › "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Helm.colors.accent,
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
                color = Helm.colors.text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (closing) {
            LoadingIndicator(Modifier.padding(4.dp).size(40.dp))
        } else {
            IconButton(onClick = onClose, enabled = closeEnabled) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.close_connection),
                    tint = Helm.colors.text2,
                )
            }
        }
    }
}

@Composable
private fun SkeletonGroupRow(brush: androidx.compose.ui.graphics.Brush) {
    HelmCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SkeletonBox(brush, Modifier.fillMaxWidth(0.5f).height(16.dp))
            SkeletonBox(brush, Modifier.fillMaxWidth(0.35f).height(12.dp))
        }
    }
}

/** One image loader for config icons: PNG/JPEG/WebP out of the box plus SVG; Coil caches on disk. */
private object GroupIconLoader {
    @Volatile private var loader: ImageLoader? = null

    fun get(context: Context): ImageLoader = loader ?: synchronized(this) {
        loader ?: ImageLoader.Builder(context.applicationContext)
            .components { add(SvgDecoder.Factory()) }
            .crossfade(true)
            .build()
            .also { loader = it }
    }
}

/**
 * Icon of a proxy group: the picture from the `icon:` URL of the group in the router's mihomo
 * config, on the same soft shaped tile as everywhere else. Falls back to the generic hub glyph
 * while loading, when the group has no icon or the URL does not load.
 */
@Composable
private fun GroupIcon(group: MihomoGroup, index: Int, size: androidx.compose.ui.unit.Dp) {
    val tint = cycleTint(index)
    val url = group.icon
    if (url == null) {
        ShapeIcon(Icons.Outlined.Hub, tint, shape = cycleShape(index), size = size)
        return
    }
    val context = LocalContext.current
    Box(
        Modifier.size(size).clip(cycleShape(index).toShape()).background(tint.bg),
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context).data(url).build(),
            imageLoader = GroupIconLoader.get(context),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            loading = { Icon(Icons.Outlined.Hub, null, Modifier.size(size * 0.5f), tint = tint.fg) },
            error = { Icon(Icons.Outlined.Hub, null, Modifier.size(size * 0.5f), tint = tint.fg) },
            modifier = Modifier.size(size * 0.62f),
        )
    }
}
