package com.skofqq.domainmanager.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
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
import com.skofqq.domainmanager.data.MagitrickleGroup
import com.skofqq.domainmanager.data.RouterProfile
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
    magitrickleGroupsViewModel: MagitrickleGroupsViewModel,
    routerInfoViewModel: RouterInfoViewModel,
    routerProfiles: List<RouterProfile>,
    activeRouterId: String,
    onSelectRouter: (String) -> Unit,
    onOpenStatus: () -> Unit,
    /** True right after opening via the "mihomo_update" App Shortcut — auto-opens the router sheet once. */
    autoOpenRouterSheet: Boolean = false,
    onAutoOpenRouterSheetConsumed: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    var showRouterSheet by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(autoOpenRouterSheet) {
        if (autoOpenRouterSheet) {
            showRouterSheet = true
            onAutoOpenRouterSheetConsumed()
        }
    }

    // Backs the "already in group X" typing hint on both pages below — loaded
    // once up front so it's ready before the user starts typing.
    val magitrickleGroupsState by magitrickleGroupsViewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        if (magitrickleGroupsState.groups == null) magitrickleGroupsViewModel.refresh()
    }

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
            Column {
                // Full-width router bar — moved out of the cramped title row so
                // it reads as its own destination, not an accessory next to the
                // engine picker. Always visible: the active router matters to
                // every page of every tab.
                if (routerProfiles.isNotEmpty()) {
                    RouterInfoBar(
                        profiles = routerProfiles,
                        activeId = activeRouterId,
                        onClick = { showRouterSheet = true },
                    )
                }
                LargeTopAppBar(
                    title = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                    // The router bar above already claims the status-bar inset —
                    // without zeroing it here the LargeTopAppBar would pad twice.
                    windowInsets = WindowInsets(0),
                    scrollBehavior = scrollBehavior,
                )
            }
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
                    0 -> RoutingTab(domainsViewModel, magitrickleGroupsState.groups)
                    1 -> StrategiesTab(strategiesViewModel, engineOverride, onOpenStatus, magitrickleGroupsState.groups)
                }
            }
        }
    }

    if (showRouterSheet) {
        RouterInfoSheet(
            profiles = routerProfiles,
            activeId = activeRouterId,
            onSelectRouter = onSelectRouter,
            viewModel = routerInfoViewModel,
            onDismiss = { showRouterSheet = false },
        )
    }
}

/**
 * Full-width bar replacing the old cramped title-row pill: the active router's
 * name/host, stretched across the screen, tapping opens [RouterInfoSheet].
 */
@Composable
private fun RouterInfoBar(
    profiles: List<RouterProfile>,
    activeId: String,
    onClick: () -> Unit,
) {
    val active = profiles.firstOrNull { it.id == activeId } ?: profiles.firstOrNull() ?: return
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = active.name.ifEmpty { active.host },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${active.host}:${active.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Router bar's expanded panel: (a) the router-switching list, relocated from
 * the old title-row dropdown, (b) installed versions of the four managed
 * services (item 9 of the "10 features" request), (c) mihomo's check-update /
 * update-with-confirm flow plus a changelog link — the only service with such
 * an action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouterInfoSheet(
    profiles: List<RouterProfile>,
    activeId: String,
    onSelectRouter: (String) -> Unit,
    viewModel: RouterInfoViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Re-fetched every time the sheet opens — versions/update-availability can
    // change on the router between visits and are cheap to re-read.
    LaunchedEffect(Unit) { viewModel.loadVersions() }

    var pendingUpdateConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(state.updateSucceeded) {
        if (state.updateSucceeded) viewModel.clearUpdateSucceeded()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.router_sheet_routers),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            profiles.forEach { profile ->
                val selected = profile.id == activeId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .let {
                            if (selected) it.background(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                            ) else it
                        }
                        .clickable { onSelectRouter(profile.id) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            profile.name.ifEmpty { profile.host },
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${profile.host}:${profile.port}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (selected) Icon(Icons.Filled.Check, contentDescription = null)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = stringResource(R.string.router_sheet_versions),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            if (state.versionsLoading && state.versions == null) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        stringResource(R.string.loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (state.versionsError != null) {
                Text(
                    text = state.versionsError!!.resolve(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                val versions = state.versions
                VersionRow(stringResource(R.string.svc_name_mihomo), versions?.mihomo)
                MihomoUpdateSection(
                    state = state,
                    onCheckUpdate = { viewModel.checkMihomoUpdate() },
                    onRequestUpdate = { pendingUpdateConfirm = true },
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                VersionRow(stringResource(R.string.svc_name_magitrickle), versions?.magitrickle)
                VersionRow(stringResource(R.string.svc_name_zapret), versions?.zapret)
                VersionRow(stringResource(R.string.svc_name_zapret2), versions?.zapret2)
            }
        }
    }

    if (pendingUpdateConfirm) {
        AlertDialog(
            onDismissRequest = { pendingUpdateConfirm = false },
            title = { Text(stringResource(R.string.mihomo_update_confirm_title)) },
            text = { Text(stringResource(R.string.mihomo_update_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingUpdateConfirm = false
                        viewModel.confirmMihomoUpdate()
                    },
                ) { Text(stringResource(R.string.update)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingUpdateConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun VersionRow(name: String, version: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            text = version?.takeIf { it.isNotBlank() } ?: stringResource(R.string.version_unknown),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Only mihomo has an update flow — plain-version rows for the other three
 * services never show these controls.
 */
@Composable
private fun MihomoUpdateSection(
    state: RouterInfoUiState,
    onCheckUpdate: () -> Unit,
    onRequestUpdate: () -> Unit,
) {
    val context = LocalContext.current
    val info = state.updateInfo

    if (state.updating) {
        Row(
            modifier = Modifier.padding(start = 0.dp, top = 2.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Text(
                stringResource(R.string.mihomo_updating),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    // The button itself never disappears — only its label/action changes with
    // the check result: "Проверить обновления" → (up to date: still tappable to
    // recheck) → (update available: becomes "Обновить", now triggers the
    // confirm-gated mihomo_update instead of a recheck). A real Button, not a
    // TextButton — it reads as an action here, not a hyperlink.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (info != null && info.updateAvailable) {
            FilledTonalButton(onClick = onRequestUpdate, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.update_to_version, info.latest))
            }
        } else {
            OutlinedButton(
                onClick = onCheckUpdate,
                enabled = !state.checkingUpdate,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.checkingUpdate) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.check_for_updates))
                } else if (info != null) {
                    Text(stringResource(R.string.mihomo_up_to_date, info.current))
                } else {
                    Text(stringResource(R.string.check_for_updates))
                }
            }
        }
    }

    if (info != null && info.updateAvailable) {
        Text(
            text = stringResource(R.string.whats_new_in, info.latest),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 4.dp, bottom = 8.dp)
                .clickable {
                    val url = "https://github.com/MetaCubeX/mihomo/releases/tag/${info.latest}"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
        )
    }

    state.checkUpdateError?.let {
        Text(
            text = it.resolve(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
    }
    state.updateError?.let {
        Text(
            text = it.resolve(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun RoutingTab(viewModel: DomainsViewModel, magitrickleGroups: List<MagitrickleGroup>?) {
    val state by viewModel.state.collectAsState()
    // One field, two jobs: the text filters the list live AND is the input for
    // the add action (+ / IME) — the pre-search separate "Domain" field is gone.
    var query by rememberSaveable { mutableStateOf("") }
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
        if (query.isBlank()) return
        focusManager.clearFocus()
        if (viewModel.addDomain(query)) query = ""
    }

    // First load (no cache yet) → skeleton rows; refreshes of already-shown data
    // keep the regular indicator over the existing list, never re-skeleton.
    val firstLoad = state.domains == null && state.error == null
    val skeletonBrush = if (firstLoad) shimmerBrush() else null

    Column(modifier = Modifier.fillMaxSize()) {
        // Same pinned SearchBar look as the Strategies page, but this one also
        // carries the old add-field's features: paste while empty, + / IME-action
        // to add the typed domain.
        RoutingSearchAddBar(
            query = query,
            onQueryChange = { query = it },
            addEnabled = query.isNotBlank() && state.busyDomain == null,
            onPaste = {
                // URL-looking clip → registrable domain (same heuristic as the
                // share flow); plain text → paste as-is for manual editing.
                val clip = clipboard.getText()?.text?.trim().orEmpty()
                if (clip.isNotBlank()) {
                    query = extractDomain(clip) ?: clip
                }
            },
            onAdd = ::submit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        // Domain already routed elsewhere via a MagiTrickle group (not the Custom
        // one — that's already covered by the mihomo/MagiTrickle row indicators).
        val existingGroup = remember(query, magitrickleGroups) { matchMagitrickleGroup(magitrickleGroups, query) }
        existingGroup?.let { groupName ->
            Text(
                text = stringResource(R.string.domain_in_group, groupName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
            )
        }
        // Bottom padding: breathing room between the search bar (+ hint, if any)
        // and the first list row (the list's own 8dp top padding alone read as "stuck").
        Spacer(Modifier.height(8.dp))
        PullToRefreshBox(
            isRefreshing = state.isRefreshing && !firstLoad,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.error?.let { message ->
                    item { ErrorCard(message) }
                }

                if (skeletonBrush != null) {
                    items(8) { SkeletonDomainRow(skeletonBrush) }
                }

                val domains = state.domains
                if (domains != null) {
                    val filter = query.trim()
                    val filtered =
                        if (filter.isEmpty()) domains
                        else domains.filter { it.domain.contains(filter, ignoreCase = true) }
                    if (filtered.isEmpty() && !state.isRefreshing) {
                        item {
                            Text(
                                text = stringResource(
                                    if (domains.isEmpty()) R.string.no_domains
                                    else R.string.search_no_results
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    items(filtered, key = { it.domain }) { entry ->
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

/**
 * Routing's single top field: same collapsed SearchBar as Strategies, filtering
 * as you type, plus the old add-field's actions — paste (while empty), clear and
 * add (while filled); the IME action also submits the add.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutingSearchAddBar(
    query: String,
    onQueryChange: (String) -> Unit,
    addEnabled: Boolean,
    onPaste: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { onAdd() },
                expanded = false,
                onExpandedChange = {},
                placeholder = { Text(stringResource(R.string.search_or_add_domain)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    Row {
                        if (query.isEmpty()) {
                            IconButton(onClick = onPaste) {
                                Icon(
                                    Icons.Filled.ContentPaste,
                                    contentDescription = stringResource(R.string.paste_from_clipboard),
                                )
                            }
                        } else {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.clear_search),
                                )
                            }
                            IconButton(onClick = onAdd, enabled = addEnabled) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = stringResource(R.string.add_domain),
                                )
                            }
                        }
                    }
                },
            )
        },
        expanded = false,
        onExpandedChange = {},
        // Default SearchBar insets add status-bar top padding even under a
        // TopAppBar — same suppression as the Strategies search.
        windowInsets = WindowInsets(0),
        modifier = modifier,
    ) {}
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
