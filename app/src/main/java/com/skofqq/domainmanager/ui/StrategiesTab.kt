package com.skofqq.domainmanager.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.MagitrickleGroup
import com.skofqq.domainmanager.data.StrategyEntry
import com.skofqq.domainmanager.util.extractDomain
import kotlinx.coroutines.launch

private val RunningGreen = Color(0xFF4CAF50)

/**
 * "Strategies" page of the Domains tab. zapret and zapret2 are mutually exclusive
 * server-side, so on every opening svc_list decides which engine's list to show.
 * [engineOverride] comes from the top-bar dropdown in DomainsScreen (per-visit
 * peek at the other engine, never persisted — DomainsScreen resets it on leaving
 * this page).
 */
@Composable
fun StrategiesTab(
    viewModel: StrategiesViewModel,
    engineOverride: String?,
    onOpenStatus: () -> Unit,
    magitrickleGroups: List<MagitrickleGroup>?,
) {
    val activeState by viewModel.activeEngineState.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshActiveEngine() }

    when (val active = activeState) {
        // svc_list in flight before the very first determination: skeleton of the
        // page shape (banner + search + rows) instead of a bare spinner.
        ActiveEngineUiState.Loading -> {
            val brush = shimmerBrush()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SkeletonBox(
                    brush,
                    Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .height(32.dp),
                    RoundedCornerShape(16.dp),
                )
                SkeletonBox(
                    brush,
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    CircleShape,
                )
                repeat(6) { SkeletonStrategyRow(brush) }
            }
        }

        is ActiveEngineUiState.Error -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ErrorCard(active.message)
            FilledTonalButton(
                onClick = { viewModel.refreshActiveEngine() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.retry)) }
        }

        ActiveEngineUiState.NoneRunning -> NoEngineRunningState(
            onOpenStatus = onOpenStatus,
            onRetry = { viewModel.refreshActiveEngine() },
        )

        is ActiveEngineUiState.Running -> {
            val shown = engineOverride ?: active.engine
            Column(modifier = Modifier.fillMaxSize()) {
                EngineInfoBanner(
                    running = shown == active.engine,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                )
                // weight(1f), not fillMaxSize: without it the page measures at full
                // column height, pushing its bottom (list padding + FAB anchor)
                // off-screen — the FAB then covered the last row.
                EngineStrategiesPage(
                    viewModel,
                    shown,
                    magitrickleGroups,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}

/** Both engines stopped: nothing to configure strategies against. */
@Composable
private fun NoEngineRunningState(onOpenStatus: () -> Unit, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.PowerSettingsNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.strat_no_engine),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.strat_no_engine_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        FilledTonalButton(onClick = onOpenStatus) {
            Text(stringResource(R.string.open_status))
        }
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EngineStrategiesPage(
    viewModel: StrategiesViewModel,
    engine: String,
    magitrickleGroups: List<MagitrickleGroup>?,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state(engine).collectAsState()
    val min = viewModel.minStrategy(engine)
    var searchQuery by rememberSaveable(engine) { mutableStateOf("") }
    var showAddSheet by rememberSaveable(engine) { mutableStateOf(false) }
    var editEntry by remember { mutableStateOf<StrategyEntry?>(null) }

    // Keyed on engine: the escape hatch swaps the shown engine in place.
    LaunchedEffect(engine) { viewModel.refresh(engine) }

    // First strat_list for this engine → skeleton rows; later refreshes keep the
    // pull indicator over the existing list.
    val firstLoad = state.domains == null && state.error == null
    val skeletonBrush = if (firstLoad) shimmerBrush() else null

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            DomainSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                // Same search-to-list gap as the Routing page (8dp here + the
                // list's 8dp top padding).
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
            )
            PullToRefreshBox(
                isRefreshing = state.isRefreshing && !firstLoad,
                onRefresh = {
                    viewModel.refresh(engine)
                    viewModel.refreshActiveEngine()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // Extra bottom room so the FAB never covers the last row.
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.error?.let { message ->
                        item { ErrorCard(message) }
                    }

                    if (skeletonBrush != null) {
                        items(6) { SkeletonStrategyRow(skeletonBrush) }
                    }

                    val domains = state.domains
                    if (domains != null) {
                        val query = searchQuery.trim()
                        val filtered =
                            if (query.isEmpty()) domains
                            else domains.filter { it.domain.contains(query, ignoreCase = true) }
                        if (filtered.isEmpty() && !state.isRefreshing) {
                            item {
                                ListHint(
                                    if (domains.isEmpty()) R.string.no_strat_domains
                                    else R.string.search_no_results
                                )
                            }
                        }
                        // Grouped by strategy number — locked.tsv on the router has the
                        // same structure. Entries stay alphabetical inside a group (the
                        // viewmodel keeps the list sorted by domain).
                        filtered.groupBy { it.strategy }.toSortedMap().forEach { (strategy, entries) ->
                            stickyHeader {
                                StrategyGroupHeader(engine, strategy, entries.size)
                            }
                            // No item keys on purpose: zapret v1 can legitimately report
                            // the same domain under two hostlist strategies, and duplicate
                            // LazyColumn keys crash on scroll. Positional identity is fine.
                            items(entries) { entry ->
                                StrategyRow(
                                    entry = entry,
                                    busy = state.busyDomain == entry.domain,
                                    actionsEnabled = state.busyDomain == null,
                                    onClick = { editEntry = entry },
                                    // Delete is button-only by design: horizontal swipes
                                    // are taken by the nested pagers.
                                    onDelete = { viewModel.removeDomain(engine, entry.domain) },
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_domain))
        }
    }

    if (showAddSheet) {
        AddDomainSheet(
            viewModel = viewModel,
            engine = engine,
            state = state,
            min = min,
            magitrickleGroups = magitrickleGroups,
            onDismiss = { showAddSheet = false },
        )
    }

    editEntry?.let { entry ->
        EditStrategySheet(
            viewModel = viewModel,
            engine = engine,
            entry = entry,
            max = state.max,
            min = min,
            onDismiss = { editEntry = null },
        )
    }
}

/** Bottom sheet with the add form: domain field + clipboard paste + strategy control. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDomainSheet(
    viewModel: StrategiesViewModel,
    engine: String,
    state: EngineStrategiesUiState,
    min: Int,
    magitrickleGroups: List<MagitrickleGroup>?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val clipboard = LocalClipboardManager.current
    var input by rememberSaveable(engine) { mutableStateOf("") }
    var newStrategy by rememberSaveable(engine) { mutableIntStateOf(min) }
    val max = state.max
    val chosen = if (max != null) newStrategy.coerceIn(min, max) else newStrategy

    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.add_domain),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(R.string.domain)) },
                placeholder = { Text(stringResource(R.string.domain_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            // URL-looking clip → registrable domain (same heuristic as
                            // the share flow); plain text → paste as-is for editing.
                            val clip = clipboard.getText()?.text?.trim().orEmpty()
                            if (clip.isNotBlank()) {
                                input = extractDomain(clip) ?: clip
                            }
                        },
                    ) {
                        Icon(
                            Icons.Filled.ContentPaste,
                            contentDescription = stringResource(R.string.paste_from_clipboard),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            // Domain already routed elsewhere via a MagiTrickle group (not Custom —
            // that one's covered by the router's own routing state instead).
            val existingGroup = remember(input, magitrickleGroups) {
                matchMagitrickleGroup(magitrickleGroups, input)
            }
            existingGroup?.let { groupName ->
                Text(
                    text = stringResource(R.string.domain_in_group, groupName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            StrategyControl(
                value = chosen,
                min = min,
                max = max,
                engine = engine,
                onChange = { newStrategy = it },
            )
            state.error?.let { ErrorCard(it) }
            FilledTonalButton(
                onClick = {
                    focusManager.clearFocus()
                    // Single strat_add with the final values — an active engine
                    // restarts its daemon on every mutation.
                    if (viewModel.addDomain(engine, input, chosen)) {
                        input = ""
                        close()
                    }
                },
                enabled = input.isNotBlank() && state.busyDomain == null && max != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.add)) }
        }
    }
}

/** Bottom sheet for changing the strategy of an existing domain. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditStrategySheet(
    viewModel: StrategiesViewModel,
    engine: String,
    entry: StrategyEntry,
    max: Int?,
    min: Int,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val effectiveMax = max ?: entry.strategy.coerceAtLeast(min)
    var value by remember(entry) { mutableIntStateOf(entry.strategy.coerceIn(min, effectiveMax)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = entry.domain,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            StrategyControl(
                value = value,
                min = min,
                max = effectiveMax,
                engine = engine,
                onChange = { value = it },
            )
            FilledTonalButton(
                enabled = value != entry.strategy,
                onClick = {
                    // Single strat_set on explicit confirmation.
                    viewModel.setStrategy(engine, entry.domain, value)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.apply)) }
        }
    }
}

/**
 * Informational (not interactive) status line. The engine name itself is not
 * repeated here — it is already visible in the top-bar dropdown.
 */
@Composable
private fun EngineInfoBanner(running: Boolean, modifier: Modifier = Modifier) {
    Surface(
        color = if (running) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (running) RunningGreen else MaterialTheme.colorScheme.outline),
            )
            Text(
                text = stringResource(
                    if (running) R.string.strat_showing_active else R.string.strat_showing_inactive
                ),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (running) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Collapsed M3 SearchBar filtering the domain list by substring as you type.
 * Shared with the Routing tab so both Domains pages get the same search look.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DomainSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = {},
                expanded = false,
                onExpandedChange = {},
                placeholder = { Text(stringResource(R.string.search_domains)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.clear_search),
                            )
                        }
                    }
                } else null,
            )
        },
        expanded = false,
        onExpandedChange = {},
        // Default SearchBar insets add status-bar top padding even under a
        // TopAppBar — that was the visible gap above the search field.
        windowInsets = WindowInsets(0),
        modifier = modifier,
    ) {}
}

/** Sticky section header: "Strategy 5 · 12 domains" (or "Shared pool" for zapret2's 0). */
@Composable
private fun StrategyGroupHeader(engine: String, strategy: Int, count: Int) {
    val sharedPool = engine == ENGINE_ZAPRET2 && strategy == 0
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (sharedPool) stringResource(R.string.strategy_pool_short)
                else stringResource(R.string.strategy_n, strategy),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "· " + pluralStringResource(R.plurals.n_domains, count, count),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Strategy number control: big tappable value between expressive −/+ icon buttons.
 * Tapping the number opens a dropdown for a direct jump to any value. Never talks to
 * the network — the chosen value is sent once by the caller's explicit Add/Apply.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StrategyControl(
    value: Int,
    min: Int,
    max: Int?,
    engine: String,
    onChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.strategy),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (max == null) {
            // Range unknown until the first strat_list answer arrives.
            Text(
                text = stringResource(R.string.strategy_range_loading),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            var pickerOpen by remember { mutableStateOf(false) }
            val menuScroll = rememberScrollState()
            val density = LocalDensity.current
            LaunchedEffect(pickerOpen) {
                // Open the dropdown scrolled to the current value (48dp per menu item).
                if (pickerOpen) {
                    menuScroll.scrollTo(with(density) { (48.dp.toPx() * (value - min)).toInt() })
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FilledTonalIconButton(
                    onClick = { onChange((value - 1).coerceAtLeast(min)) },
                    shapes = IconButtonDefaults.shapes(),
                    enabled = value > min,
                ) { Icon(Icons.Filled.Remove, contentDescription = null) }
                Box {
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.large)
                            .clickable(onClickLabel = stringResource(R.string.pick_strategy)) {
                                pickerOpen = true
                            }
                            .widthIn(min = 88.dp)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    DropdownMenu(
                        expanded = pickerOpen,
                        onDismissRequest = { pickerOpen = false },
                        scrollState = menuScroll,
                        modifier = Modifier.heightIn(max = 320.dp),
                    ) {
                        (min..max).forEach { n ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (engine == ENGINE_ZAPRET2 && n == 0) {
                                            "0 — " + stringResource(R.string.strategy_pool_short)
                                        } else n.toString()
                                    )
                                },
                                trailingIcon = if (n == value) {
                                    { Icon(Icons.Filled.Check, contentDescription = null) }
                                } else null,
                                onClick = {
                                    onChange(n)
                                    pickerOpen = false
                                },
                            )
                        }
                    }
                }
                FilledTonalIconButton(
                    onClick = { onChange((value + 1).coerceAtMost(max)) },
                    shapes = IconButtonDefaults.shapes(),
                    enabled = value < max,
                ) { Icon(Icons.Filled.Add, contentDescription = null) }
            }
            if (engine == ENGINE_ZAPRET2 && value == 0) {
                Text(
                    text = stringResource(R.string.strategy_pool),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Single-line row: domain + strategy badge + delete. Dense — lists reach 40-50 rows. */
@Composable
private fun StrategyRow(
    entry: StrategyEntry,
    busy: Boolean,
    actionsEnabled: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardDefaults.elevatedShape)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = entry.domain,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = CircleShape,
            ) {
                Text(
                    text = entry.strategy.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp),
                    strokeWidth = 2.5.dp,
                )
            } else {
                IconButton(onClick = onDelete, enabled = actionsEnabled) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.remove_domain),
                        tint = if (actionsEnabled) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ListHint(textRes: Int) {
    Text(
        text = stringResource(textRes),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}
