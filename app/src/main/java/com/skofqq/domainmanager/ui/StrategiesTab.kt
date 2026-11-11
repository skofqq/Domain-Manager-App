package com.skofqq.domainmanager.ui

import androidx.compose.material.icons.filled.ContentPaste
import com.skofqq.domainmanager.ui.theme.Tints
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.toShape
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Dns
import com.skofqq.domainmanager.ui.theme.Helm
import com.skofqq.domainmanager.ui.components.*
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.StrategyEntry
import com.skofqq.domainmanager.util.extractDomain
import com.skofqq.domainmanager.ui.theme.statusOk
import kotlinx.coroutines.launch

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
            HelmButton(
                text = stringResource(R.string.retry),
                onClick = { viewModel.refreshActiveEngine() },
                icon = Icons.Filled.Refresh,
                style = ButtonStyle.Tonal,
                modifier = Modifier.fillMaxWidth(),
            )
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
                // column height and its bottom (the list's own padding) ends up
                // off-screen.
                EngineStrategiesPage(
                    viewModel,
                    shown,
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        EmptyState(
            icon = Icons.Outlined.PowerSettingsNew,
            title = stringResource(R.string.strat_no_engine),
            body = stringResource(R.string.strat_no_engine_hint),
            tint = Tints.orange,
            shape = MaterialShapes.Cookie9Sided,
            action = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HelmButton(
                        text = stringResource(R.string.open_status),
                        onClick = onOpenStatus,
                        icon = Icons.Outlined.Dns,
                    )
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EngineStrategiesPage(
    viewModel: StrategiesViewModel,
    engine: String,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state(engine).collectAsState()
    val min = viewModel.minStrategy(engine)
    // One field, two jobs: the text filters the list live AND is the input for
    // the add action (+ / IME) — same merged bar the Routing page uses.
    var searchQuery by rememberSaveable(engine) { mutableStateOf("") }
    var editEntry by remember { mutableStateOf<StrategyEntry?>(null) }
    val focusManager = LocalFocusManager.current
    val clipboard = LocalClipboardManager.current

    // Keyed on engine: the escape hatch swaps the shown engine in place.
    LaunchedEffect(engine) { viewModel.refresh(engine) }

    fun submit() {
        val q = searchQuery.trim()
        if (q.isBlank()) return
        focusManager.clearFocus()
        // Added at the engine's minimum strategy — there is deliberately no
        // strategy picker at add time; tapping the row edits it afterwards.
        if (viewModel.addDomain(engine, q, min)) searchQuery = ""
    }

    // First strat_list for this engine → skeleton rows; later refreshes keep the
    // pull indicator over the existing list.
    val firstLoad = state.domains == null && state.error == null
    val skeletonBrush = if (firstLoad) shimmerBrush() else null

    Box(modifier = modifier) {
        HelmPullToRefreshBox(
            isRefreshing = state.isRefreshing && !firstLoad,
            onRefresh = {
                viewModel.refresh(engine)
                viewModel.refreshActiveEngine()
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // The list runs under the floating search: room for it and the bottom bar.
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp + FloatingSearchHeight + (LocalBottomBarPadding.current ?: 0.dp)),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.error?.let { message ->
                    item { ErrorCard(message, onRetry = { viewModel.refresh(engine) }) }
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
                            DomainsEmptyState(
                                query = searchQuery,
                                emptyTitleRes = R.string.empty_strat_title,
                                emptyBodyRes = R.string.no_strat_domains,
                                listEmpty = domains.isEmpty(),
                                addEnabled = state.busyDomain == null && state.max != null,
                                onAdd = ::submit,
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
        // Same floating search-and-add as the Routing page.
        FloatingSearch(
            query = searchQuery,
            onQuery = { searchQuery = it },
            hint = stringResource(R.string.search_or_add_domain),
            onDone = ::submit,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            if (searchQuery.isEmpty()) {
                GlassAction(
                    icon = Icons.Filled.ContentPaste,
                    description = stringResource(R.string.paste_from_clipboard),
                    onClick = {
                        val clip = clipboard.getText()?.text?.trim().orEmpty()
                        if (clip.isNotBlank()) searchQuery = extractDomain(clip) ?: clip
                    },
                )
            } else {
                GlassAction(
                    icon = Icons.Filled.Add,
                    description = stringResource(R.string.add_domain),
                    onClick = ::submit,
                    enabled = state.busyDomain == null && state.max != null,
                    accent = true,
                )
            }
        }
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

    HelmBottomSheet(
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
            HelmButton(
                text = stringResource(R.string.apply),
                enabled = value != entry.strategy,
                icon = Icons.Filled.Check,
                onClick = {
                    // Single strat_set on explicit confirmation.
                    viewModel.setStrategy(engine, entry.domain, value)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Informational (not interactive) status line. The engine name itself is not
 * repeated here — it is already visible in the top-bar dropdown.
 */
@Composable
private fun EngineInfoBanner(running: Boolean, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()) {
        StatusPill(
            text = stringResource(
                if (running) R.string.strat_showing_active else R.string.strat_showing_inactive
            ),
            color = if (running) statusOk else Helm.colors.text2,
        )
    }
}

/** Sticky section header: "Strategy 5 · 12 domains" (or "Shared pool" for the 0 of zapret2). */
@Composable
private fun StrategyGroupHeader(engine: String, strategy: Int, count: Int) {
    val sharedPool = engine == ENGINE_ZAPRET2 && strategy == 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Helm.colors.page)
            .padding(top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // The strategy number on its own expressive cookie, cycling colours per group.
        Box(
            Modifier.size(32.dp).clip(cycleShape(strategy).toShape()).background(cycleTint(strategy).bg),
            contentAlignment = Alignment.Center,
        ) {
            if (sharedPool) {
                Icon(Icons.Outlined.Shuffle, null, Modifier.size(16.dp), tint = cycleTint(strategy).fg)
            } else {
                Text(
                    strategy.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = cycleTint(strategy).fg,
                )
            }
        }
        Text(
            text = if (sharedPool) stringResource(R.string.strategy_pool_short)
            else stringResource(R.string.strategy_n, strategy),
            style = MaterialTheme.typography.titleMedium,
            color = Helm.colors.text,
        )
        Text(
            text = pluralStringResource(R.plurals.n_domains, count, count),
            style = MaterialTheme.typography.labelMedium,
            color = Helm.colors.text2,
        )
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
                ) {
                    Icon(
                        Icons.Filled.Remove,
                        contentDescription = stringResource(R.string.strategy_previous),
                    )
                }
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
                        shape = RoundedCornerShape(22.dp),
                        containerColor = Helm.colors.card,
                        shadowElevation = 12.dp,
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
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.strategy_next),
                    )
                }
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
    HelmCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
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
            ValuePill(entry.strategy.toString(), cycleTint(entry.strategy))
            if (busy) {
                LoadingIndicator(Modifier.padding(4.dp).size(40.dp))
            } else {
                IconButton(onClick = onDelete, enabled = actionsEnabled) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.remove_domain),
                        tint = if (actionsEnabled) Tints.red.fg
                        else Helm.colors.text2.copy(alpha = 0.38f),
                    )
                }
            }
        }
    }
}

