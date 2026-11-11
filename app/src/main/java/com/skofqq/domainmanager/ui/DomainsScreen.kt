package com.skofqq.domainmanager.ui

import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SearchOff
import com.skofqq.domainmanager.ui.theme.Tint
import com.skofqq.domainmanager.ui.theme.Tints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material3.toShape
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.outlined.Router
import com.skofqq.domainmanager.ui.theme.Helm
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilledIconButton
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import com.skofqq.domainmanager.ui.components.*
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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
import com.skofqq.domainmanager.ui.theme.statusOk
import kotlinx.coroutines.launch

/** Display names for API target values ("mihomo" / "magitrickle"). */
private fun targetLabel(target: String) = if (target == "mihomo") "mihomo" else "MagiTrickle"

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
    var open by remember { mutableStateOf(false) }
    Box {
        // Glass pill trigger, like the router pill and Boxy's header chips.
        GlassPill(
            text = shown,
            onClick = { open = true },
            trailing = Icons.Filled.KeyboardArrowDown,
        )
        // Liquid glass menu that springs out of the pill's corner.
        GlassMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(ENGINE_ZAPRET2, ENGINE_ZAPRET).forEach { engine ->
                val running = engine == active
                val selected = engine == shown
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.RadioButton) {
                            onSelect(engine)
                            open = false
                        }
                        .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (running) statusOk else Helm.colors.text2.copy(alpha = 0.5f)),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(engine, style = MaterialTheme.typography.bodyLarge, color = Helm.colors.text)
                        Text(
                            stringResource(if (running) R.string.engine_running else R.string.engine_stopped),
                            style = MaterialTheme.typography.bodySmall,
                            color = Helm.colors.text2,
                        )
                    }
                    if (selected) {
                        Spacer(Modifier.width(10.dp))
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Helm.colors.accent)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Helm.colors.page)
            .statusBarsPadding(),
    ) {
        // Active router as a glass pill above the title (same spot as the back pill on
        // sub-pages): always visible, the active router matters to every page of every tab.
        if (routerProfiles.isNotEmpty()) {
            RouterInfoBar(
                profiles = routerProfiles,
                activeId = activeRouterId,
                onClick = { showRouterSheet = true },
            )
        }
        PageHeader(title = stringResource(R.string.domains)) {
            // Compact "zapret2 ▾" dropdown pinned to the right edge — only while the
            // Strategies page shows an engine list; irrelevant on Routing.
            if (onStrategiesPage && activeEngine != null && shownEngine != null) {
                EngineSwitcher(
                    shown = shownEngine,
                    active = activeEngine,
                    onSelect = { engineOverride = it },
                )
            }
        }
        PillTabs(
            labels = listOf(stringResource(R.string.tab_routing), stringResource(R.string.tab_strategies)),
            selected = (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f),
            onSelect = { scope.launch { pagerState.animateScrollToPage(it) } },
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp),
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> RoutingTab(domainsViewModel, magitrickleGroupsState.groups)
                1 -> StrategiesTab(strategiesViewModel, engineOverride, onOpenStatus)
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
    GlassPill(
        text = active.name.ifEmpty { active.host },
        supporting = "${active.host}:${active.port}",
        onClick = onClick,
        leading = Icons.Outlined.Router,
        trailing = Icons.Filled.KeyboardArrowDown,
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp),
    )
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

    // Re-fetched every time the sheet opens: versions/update-availability can
    // change on the router between visits and are cheap to re-read.
    LaunchedEffect(Unit) { viewModel.loadVersions() }

    var pendingUpdateConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(state.updateSucceeded) {
        if (state.updateSucceeded) viewModel.clearUpdateSucceeded()
    }

    HelmBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // --- Router picker ---------------------------------------------------
            HelmSectionHeader(
                title = stringResource(R.string.router_sheet_routers),
                subtitle = pluralStringResource(R.plurals.n_routers, profiles.size, profiles.size),
                icon = Icons.Outlined.Router,
                tint = Tints.accent,
                shape = MaterialShapes.Cookie12Sided,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                profiles.forEachIndexed { index, profile ->
                    RouterChoiceCard(
                        profile = profile,
                        index = index,
                        selected = profile.id == activeId,
                        onClick = { onSelectRouter(profile.id) },
                    )
                }
            }

            // --- Installed services + mihomo update -----------------------------
            HelmSection(
                title = stringResource(R.string.router_sheet_versions),
                icon = Icons.Outlined.Inventory2,
                tint = Tints.teal,
                shape = MaterialShapes.Sunny,
                trailing = if (state.versionsLoading && state.versions != null) {
                    { LoadingIndicator(Modifier.size(36.dp)) }
                } else null,
            ) {
                when {
                    state.versionsLoading && state.versions == null -> {
                        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            LoadingIndicator(Modifier.size(56.dp))
                        }
                    }
                    state.versionsError != null -> ErrorCard(state.versionsError!!)
                    else -> {
                        val versions = state.versions
                        TileGrid(
                            listOf(
                                Triple(R.string.svc_name_mihomo, versions?.mihomo, Icons.Outlined.Hub to MaterialShapes.Cookie9Sided),
                                Triple(R.string.svc_name_magitrickle, versions?.magitrickle, Icons.Outlined.AltRoute to MaterialShapes.Clover4Leaf),
                                Triple(R.string.svc_name_zapret, versions?.zapret, Icons.Outlined.Shield to MaterialShapes.Cookie6Sided),
                                Triple(R.string.svc_name_zapret2, versions?.zapret2, Icons.Outlined.Shield to MaterialShapes.Flower),
                            ).mapIndexed { i, (nameRes, version, look) ->
                                @Composable { m: Modifier ->
                                    val known = !version.isNullOrBlank()
                                    StatTile(
                                        label = stringResource(nameRes),
                                        value = if (known) version!! else stringResource(R.string.version_unknown),
                                        icon = look.first,
                                        tint = if (known) cycleTint(i) else Tints.gray,
                                        shape = look.second,
                                        monospace = known,
                                        modifier = m,
                                    )
                                }
                            },
                        )
                        MihomoUpdateSection(
                            state = state,
                            onCheckUpdate = { viewModel.checkMihomoUpdate() },
                            onRequestUpdate = { pendingUpdateConfirm = true },
                        )
                    }
                }
            }
        }
    }

    if (pendingUpdateConfirm) {
        GlassAlertDialog(
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

/**
 * One router in the picker. The active one fills with the accent, its shaped icon
 * springs up and turns into a fuller cookie; the others stay calm and neutral.
 */
@Composable
private fun RouterChoiceCard(
    profile: RouterProfile,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val motion = MaterialTheme.motionScheme
    val bg by animateColorAsState(
        if (selected) Tints.accent.bg.compositeOver(Helm.colors.card) else Helm.colors.card,
        motion.defaultEffectsSpec(),
        label = "routerBg",
    )
    val iconScale by animateFloatAsState(if (selected) 1f else 0.86f, motion.fastSpatialSpec(), label = "routerScale")
    val iconTurn by animateFloatAsState(if (selected) 0f else -30f, motion.defaultSpatialSpec(), label = "routerTurn")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (selected) 28.dp else 22.dp))
            .background(bg)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.graphicsLayer {
                scaleX = iconScale
                scaleY = iconScale
                rotationZ = iconTurn
            },
        ) {
            ShapeIcon(
                Icons.Outlined.Router,
                if (selected) Tint(Helm.colors.onBrand, Helm.colors.brand) else cycleTint(index),
                shape = if (selected) MaterialShapes.Cookie12Sided else MaterialShapes.Cookie6Sided,
                size = 48.dp,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                profile.name.ifEmpty { profile.host },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) Helm.colors.accent else Helm.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${profile.host}:${profile.port}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = Helm.colors.text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (profile.fallbackHost.isNotBlank()) {
                Text(
                    "↪ ${profile.fallbackHost}:${profile.fallbackPort}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Helm.colors.text2.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        AnimatedVisibility(
            visible = selected,
            enter = scaleIn(motion.fastSpatialSpec()) + fadeIn(motion.fastEffectsSpec()),
            exit = scaleOut(motion.fastSpatialSpec()) + fadeOut(motion.fastEffectsSpec()),
        ) {
            Box(
                Modifier.size(32.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(Helm.colors.brand),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Helm.colors.onBrand, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * Only mihomo has an update flow; the other three services just show their version.
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
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionNote(stringResource(R.string.mihomo_updating))
            WorkingStrip(true)
        }
        return
    }

    if (info != null && info.updateAvailable) {
        // Update available: an accent banner with the new version, the confirm-gated
        // update button and the link to the release notes.
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Tints.accent.bg).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShapeIcon(Icons.Filled.SystemUpdate, Tint(Helm.colors.onBrand, Helm.colors.brand), shape = MaterialShapes.Sunny, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.update_to_version, info.latest),
                        style = MaterialTheme.typography.titleMedium,
                        color = Helm.colors.accent,
                    )
                    Text(
                        "mihomo ${info.current} → ${info.latest}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Helm.colors.text2,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HelmButton(
                    text = stringResource(R.string.update),
                    onClick = onRequestUpdate,
                    icon = Icons.Filled.SystemUpdate,
                    modifier = Modifier.weight(1f),
                )
                HelmButton(
                    text = stringResource(R.string.whats_new_in, info.latest),
                    onClick = {
                        val url = "https://github.com/MetaCubeX/mihomo/releases/tag/${info.latest}"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    style = ButtonStyle.Neutral,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    } else {
        // The button never disappears: "check for updates", then "up to date vX"
        // (still tappable to recheck).
        HelmButton(
            text = when {
                info != null && !state.checkingUpdate -> stringResource(R.string.mihomo_up_to_date, info.current)
                else -> stringResource(R.string.check_for_updates)
            },
            onClick = onCheckUpdate,
            loading = state.checkingUpdate,
            icon = if (info != null) Icons.Filled.Check else Icons.Filled.Refresh,
            style = if (info != null) ButtonStyle.Success else ButtonStyle.Tonal,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    state.checkUpdateError?.let { ErrorCard(it) }
    state.updateError?.let { ErrorCard(it) }
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

    // Domain already routed elsewhere via a MagiTrickle group (not the Custom
    // one: that is already covered by the mihomo/MagiTrickle row indicators).
    val existingGroup = remember(query, magitrickleGroups) { matchMagitrickleGroup(magitrickleGroups, query) }

    Box(modifier = Modifier.fillMaxSize()) {
        HelmPullToRefreshBox(
            isRefreshing = state.isRefreshing && !firstLoad,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // The list runs under the floating search: room for it and the bottom bar.
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp + FloatingSearchHeight + (LocalBottomBarPadding.current ?: 0.dp)),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.error?.let { message ->
                    item { ErrorCard(message, onRetry = { viewModel.refresh() }) }
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
                            DomainsEmptyState(
                                query = query,
                                emptyTitleRes = R.string.empty_domains_title,
                                emptyBodyRes = R.string.no_domains,
                                listEmpty = domains.isEmpty(),
                                addEnabled = state.busyDomain == null,
                                onAdd = ::submit,
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
        // One field, two jobs, floating over the list right above the bottom bar (Boxy
        // panel style): the text filters live AND is the input for add (+ / IME);
        // paste while it is empty.
        FloatingSearch(
            query = query,
            onQuery = { query = it },
            hint = stringResource(R.string.search_or_add_domain),
            note = existingGroup?.let { stringResource(R.string.domain_in_group, it) },
            onDone = ::submit,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            if (query.isEmpty()) {
                GlassAction(
                    icon = Icons.Filled.ContentPaste,
                    description = stringResource(R.string.paste_from_clipboard),
                    onClick = {
                        // URL-looking clip: registrable domain (same heuristic as the
                        // share flow); plain text: paste as-is for manual editing.
                        val clip = clipboard.getText()?.text?.trim().orEmpty()
                        if (clip.isNotBlank()) query = extractDomain(clip) ?: clip
                    },
                )
            } else {
                GlassAction(
                    icon = Icons.Filled.Add,
                    description = stringResource(R.string.add_domain),
                    onClick = ::submit,
                    enabled = state.busyDomain == null,
                    accent = true,
                )
            }
        }
    }

    dialogEntry?.let { entry ->
        val present = if (entry.mihomo) "mihomo" else "magitrickle"
        val missing = if (entry.mihomo) "magitrickle" else "mihomo"
        GlassAlertDialog(
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
        GlassAlertDialog(
            onDismissRequest = { editEntry = null },
            title = { Text(stringResource(R.string.edit_domain)) },
            text = {
                HelmTextField(
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
    HelmCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            // Editing used to hang off an unlabeled long press with a no-op tap in
            // front of it — the row rippled and did nothing, and the only way in was
            // undiscoverable. Tap now opens the editor, matching the Strategies rows,
            // and the long press keeps working under the same name.
            .combinedClickable(
                onClickLabel = stringResource(R.string.edit_domain),
                onLongClickLabel = stringResource(R.string.edit_domain),
                onClick = onLongPress,
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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TargetIndicator("mihomo", entry.mihomo)
                    TargetIndicator("MagiTrickle", entry.magitrickle)
                }
            }
            if (busy) {
                LoadingIndicator(Modifier.padding(4.dp).size(40.dp))
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
    // Sighted users read the state off the check/cross glyph and the tint; neither
    // reaches a screen reader, which would otherwise hear the bare target name in
    // both states. Merge the pair into one node that spells the state out.
    val stateLabel = stringResource(
        if (active) R.string.a11y_target_added else R.string.a11y_target_not_added,
        name,
    )
    StatusPill(
        text = name,
        color = if (active) Helm.colors.accent else Helm.colors.text2.copy(alpha = 0.8f),
        icon = if (active) Icons.Filled.Check else Icons.Filled.Close,
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = stateLabel },
    )
}

/**
 * Empty list or no search match on the Routing and Strategies pages, in the expressive
 * empty-state style. A miss with text typed offers to add exactly that text right away.
 */
@Composable
internal fun DomainsEmptyState(
    query: String,
    emptyTitleRes: Int,
    emptyBodyRes: Int,
    listEmpty: Boolean,
    addEnabled: Boolean,
    onAdd: () -> Unit,
) {
    if (listEmpty && query.isBlank()) {
        EmptyState(
            icon = Icons.Outlined.Public,
            title = stringResource(emptyTitleRes),
            body = stringResource(emptyBodyRes),
            tint = Tints.accent,
            shape = MaterialShapes.Cookie12Sided,
            modifier = Modifier.padding(top = 8.dp),
        )
    } else {
        EmptyState(
            icon = Icons.Outlined.SearchOff,
            title = stringResource(R.string.search_empty_title),
            body = stringResource(R.string.search_no_results),
            tint = Tints.purple,
            shape = MaterialShapes.Flower,
            modifier = Modifier.padding(top = 8.dp),
            action = {
                HelmButton(
                    text = stringResource(R.string.add_domain_named, query.trim()),
                    onClick = onAdd,
                    enabled = addEnabled,
                    icon = Icons.Filled.Add,
                )
            },
        )
    }
}
