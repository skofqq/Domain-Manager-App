package com.skofqq.domainmanager.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.DomainStatus
import com.skofqq.domainmanager.util.extractDomain
import kotlinx.coroutines.launch

/** Display names for API target values ("mihomo" / "magitrickle"). */
private fun targetLabel(target: String) = if (target == "mihomo") "mihomo" else "MagiTrickle"

private val EngineRunningGreen = Color(0xFF4CAF50)

/** Positions the menu against the RIGHT window edge, just below the trigger. */
private class RightEdgePositionProvider(private val density: Density) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = with(density) {
        IntOffset(
            x = (windowSize.width - popupContentSize.width - 12.dp.roundToPx()).coerceAtLeast(0),
            y = anchorBounds.bottom + 4.dp.roundToPx(),
        )
    }
}

/**
 * Top-bar engine dropdown for the Strategies page: "zapret2 ▾". Both engines are
 * listed with their live state; picking the stopped one only changes which list is
 * displayed for the current visit (the choice is never persisted).
 *
 * Custom Popup instead of material3 DropdownMenu: right-edge anchoring, the app's
 * pill corner radius, and expressive spring show/hide via MaterialTheme.motionScheme
 * (spatial spring for scale, effects spec for fade) — none of which the stock
 * DropdownMenu transition exposes.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EngineSwitcher(
    shown: String,
    active: String,
    onSelect: (String) -> Unit,
) {
    // MutableTransitionState keeps the popup composed until the exit spring ends.
    val menuState = remember { MutableTransitionState(false) }
    val density = LocalDensity.current
    val positionProvider = remember(density) { RightEdgePositionProvider(density) }

    Box {
        // "Minimal bar" trigger: light pill, a solid accent band carrying the
        // engine name, chevron-down at the right edge.
        Surface(
            onClick = { menuState.targetState = true },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Row(
                modifier = Modifier.padding(start = 6.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Text(
                        text = shown,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (menuState.currentState || menuState.targetState) {
            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = { menuState.targetState = false },
                properties = PopupProperties(focusable = true),
            ) {
                val motion = MaterialTheme.motionScheme
                val origin = TransformOrigin(0.9f, 0f)
                AnimatedVisibility(
                    visibleState = menuState,
                    enter = scaleIn(
                        animationSpec = motion.fastSpatialSpec(),
                        initialScale = 0.75f,
                        transformOrigin = origin,
                    ) + fadeIn(animationSpec = motion.fastEffectsSpec()),
                    exit = scaleOut(
                        animationSpec = motion.fastSpatialSpec(),
                        targetScale = 0.75f,
                        transformOrigin = origin,
                    ) + fadeOut(animationSpec = motion.fastEffectsSpec()),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shadowElevation = 3.dp,
                    ) {
                        // Compact: sized to the widest item, never full-width.
                        Column(
                            modifier = Modifier
                                .width(IntrinsicSize.Max)
                                .padding(vertical = 8.dp),
                        ) {
                            listOf(ENGINE_ZAPRET2, ENGINE_ZAPRET).forEach { engine ->
                                val running = engine == active
                                val selected = engine == shown
                                DropdownMenuItem(
                                    text = {
                                        Column(modifier = Modifier.padding(end = 12.dp)) {
                                            Text(engine, style = MaterialTheme.typography.bodyLarge)
                                            Text(
                                                stringResource(
                                                    if (running) R.string.engine_running
                                                    else R.string.engine_stopped
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (running) EngineRunningGreen
                                                    else MaterialTheme.colorScheme.outline
                                                ),
                                        )
                                    },
                                    trailingIcon = if (selected) {
                                        { Icon(Icons.Filled.Check, contentDescription = null) }
                                    } else null,
                                    modifier = if (selected) {
                                        Modifier.background(
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                                        )
                                    } else Modifier,
                                    onClick = {
                                        onSelect(engine)
                                        menuState.targetState = false
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

/**
 * Domains tab: top-level swipeable pages — "Routing" (mihomo/MagiTrickle lists,
 * the original flow) and "Strategies" (per-domain zapret DPI-bypass strategies).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DomainsScreen(
    domainsViewModel: DomainsViewModel,
    strategiesViewModel: StrategiesViewModel,
    onOpenStatus: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    // Engine switcher state for the Strategies page, hoisted here because its
    // trigger lives in the top bar. The override is per-visit: it resets when the
    // user leaves the Strategies page, so the next opening auto-selects the engine
    // that is actually running (StrategiesTab re-checks svc_list on entry).
    val activeState by strategiesViewModel.activeEngineState.collectAsState()
    var engineOverride by remember { mutableStateOf<String?>(null) }
    val onStrategiesPage = pagerState.currentPage == 1
    LaunchedEffect(onStrategiesPage) { if (!onStrategiesPage) engineOverride = null }
    val activeEngine = (activeState as? ActiveEngineUiState.Running)?.engine
    val shownEngine = activeEngine?.let { engineOverride ?: it }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.domains))
                        Spacer(Modifier.weight(1f))
                        // Compact "zapret2 ▾" dropdown pinned to the right edge —
                        // only while the Strategies page shows an engine list;
                        // irrelevant on Routing.
                        if (onStrategiesPage && activeEngine != null && shownEngine != null) {
                            EngineSwitcher(
                                shown = shownEngine,
                                active = activeEngine,
                                onSelect = { engineOverride = it },
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                listOf(R.string.tab_routing, R.string.tab_strategies).forEachIndexed { index, labelRes ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(stringResource(labelRes)) },
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    0 -> RoutingTab(domainsViewModel)
                    1 -> StrategiesTab(strategiesViewModel, engineOverride, onOpenStatus)
                }
            }
        }
    }
}

@Composable
private fun RoutingTab(viewModel: DomainsViewModel) {
    val state by viewModel.state.collectAsState()
    var input by rememberSaveable { mutableStateOf("") }
    var dialogEntry by remember { mutableStateOf<DomainStatus?>(null) }
    var editEntry by remember { mutableStateOf<DomainStatus?>(null) }
    val focusManager = LocalFocusManager.current
    val clipboard = LocalClipboardManager.current

    // Re-fetch whenever the tab is opened or the app comes back to the foreground,
    // so adds/removes made from the Share flow show up without a manual refresh.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    fun submit() {
        if (input.isBlank()) return
        focusManager.clearFocus()
        if (viewModel.addDomain(input)) input = ""
    }

    // First load (no cache yet) → skeleton rows; refreshes of already-shown data
    // keep the regular indicator over the existing list, never re-skeleton.
    val firstLoad = state.domains == null && state.error == null
    val skeletonBrush = if (firstLoad) shimmerBrush() else null

    PullToRefreshBox(
        isRefreshing = state.isRefreshing && !firstLoad,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(stringResource(R.string.domain)) },
                    placeholder = { Text(stringResource(R.string.domain_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    trailingIcon = {
                        Row {
                            IconButton(
                                onClick = {
                                    // URL-looking clip → registrable domain (same
                                    // heuristic as the share flow); plain text →
                                    // paste as-is for manual editing.
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
                            IconButton(
                                onClick = ::submit,
                                enabled = input.isNotBlank() && state.busyDomain == null,
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = stringResource(R.string.add_domain),
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.error?.let { message ->
                item { ErrorCard(message) }
            }

            if (skeletonBrush != null) {
                items(8) { SkeletonDomainRow(skeletonBrush) }
            }

            val domains = state.domains
            if (domains != null) {
                if (domains.isEmpty() && !state.isRefreshing) {
                    item {
                        Text(
                            text = stringResource(R.string.no_domains),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                items(domains, key = { it.domain }) { entry ->
                    DomainRow(
                        entry = entry,
                        busy = state.busyDomain == entry.domain,
                        actionsEnabled = state.busyDomain == null,
                        onLongPress = { editEntry = entry },
                        onDelete = {
                            if (entry.mihomo && entry.magitrickle) {
                                viewModel.removeFromTarget(entry.domain, "both")
                            } else {
                                // Flags disagree (e.g. an old manual MagiTrickle entry):
                                // let the user pick what to change instead of touching both.
                                dialogEntry = entry
                            }
                        },
                    )
                }
            }
        }
    }

    dialogEntry?.let { entry ->
        val present = if (entry.mihomo) "mihomo" else "magitrickle"
        val missing = if (entry.mihomo) "magitrickle" else "mihomo"
        AlertDialog(
            onDismissRequest = { dialogEntry = null },
            title = { Text(entry.domain) },
            text = {
                Column {
                    Text(stringResource(R.string.domain_partial, targetLabel(present)))
                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            viewModel.removeFromTarget(entry.domain, present)
                            dialogEntry = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(R.string.remove_from, targetLabel(present)),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    TextButton(
                        onClick = {
                            viewModel.addToTarget(entry.domain, missing)
                            dialogEntry = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.add_to, targetLabel(missing)))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { dialogEntry = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    editEntry?.let { entry ->
        var editText by remember(entry) { mutableStateOf(entry.domain) }
        AlertDialog(
            onDismissRequest = { editEntry = null },
            title = { Text(stringResource(R.string.edit_domain)) },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    label = { Text(stringResource(R.string.domain)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = editText.isNotBlank(),
                    onClick = {
                        viewModel.editDomain(entry, editText)
                        editEntry = null
                    },
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { editEntry = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DomainRow(
    entry: DomainStatus,
    busy: Boolean,
    actionsEnabled: Boolean,
    onLongPress: () -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardDefaults.elevatedShape)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress,
            ),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = entry.domain,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // The two lists are independent on the router — always show both
                // flags separately so a half-added domain is visible at a glance.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TargetIndicator("mihomo", entry.mihomo)
                    TargetIndicator("MagiTrickle", entry.magitrickle)
                }
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
private fun TargetIndicator(name: String, active: Boolean) {
    val tint = if (active) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = if (active) Icons.Filled.Check else Icons.Filled.Close,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
        )
    }
}
