package com.skofqq.domainmanager.ui

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material3.LoadingIndicator
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VpnLock
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Router
import com.skofqq.domainmanager.ui.theme.Helm
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.PlayArrow
import com.skofqq.domainmanager.ui.theme.Tints
import com.skofqq.domainmanager.ui.components.*
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.DiskInfo
import com.skofqq.domainmanager.data.ServiceStatus
import com.skofqq.domainmanager.data.SysInfo
import com.skofqq.domainmanager.ui.theme.statusOk
import kotlinx.coroutines.CancellationException
import java.util.Locale

/** Only the services whose own branding differs from the router's lowercase id. */
private fun serviceLabel(service: String) = when (service) {
    "magitrickle" -> "MagiTrickle"
    "tor" -> "Tor"
    else -> service
}

/**
 * Status tab host: the root list plus two push screens (Devices, mihomo) with
 * the same predictive-back treatment as the Settings children. The nested
 * handler here takes priority over MainNavigation's tab→Domains handler.
 */
@Composable
fun ServicesScreen(
    viewModel: ServicesViewModel,
    mihomoViewModel: MihomoViewModel,
    diagnosticsViewModel: DiagnosticsViewModel,
    magitrickleGroupsViewModel: MagitrickleGroupsViewModel,
    z2ProfilesViewModel: Z2ProfilesViewModel,
    z1ProfilesViewModel: Z1ProfilesViewModel,
    ruleProvidersViewModel: RuleProvidersViewModel,
    subscriptionsViewModel: SubscriptionsViewModel,
    torViewModel: TorViewModel,
    /** Non-null once, right after opening via an App Shortcut — e.g. "devices". */
    initialSubScreen: String? = null,
    onInitialSubScreenConsumed: () -> Unit = {},
    /** True right after opening via the "speedtest" App Shortcut — auto-runs it once. */
    autoRunSpeedtest: Boolean = false,
    onAutoRunSpeedtestConsumed: () -> Unit = {},
) {
    var subScreen by rememberSaveable { mutableStateOf<String?>(null) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    var backSwipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

    LaunchedEffect(initialSubScreen) {
        if (initialSubScreen != null) {
            subScreen = initialSubScreen
            onInitialSubScreenConsumed()
        }
    }

    PredictiveBackHandler(enabled = subScreen != null) { events ->
        try {
            events.collect {
                backProgress = it.progress
                backSwipeEdge = it.swipeEdge
            }
            subScreen = null
        } catch (e: CancellationException) {
            throw e
        } finally {
            backProgress = 0f
        }
    }

    val child = subScreen
    Box(Modifier.fillMaxSize()) {
        // Peeks the root behind the leaving child while the gesture is in
        // progress, same as the system draws for cross-activity predictive back.
        // Only mounted during an active swipe, so returning to the root still
        // remounts it fresh (and re-triggers its refresh) once the child is gone.
        if (child == null || backProgress > 0f) {
            StatusRootScreen(
                viewModel = viewModel,
                onOpenDevices = { subScreen = "devices" },
                onOpenMihomo = { subScreen = "mihomo" },
                onOpenDiagnostics = { subScreen = "diagnostics" },
                onOpenMagitrickleGroups = { subScreen = "magitrickle_groups" },
                onOpenZ2Profiles = { subScreen = "z2profiles" },
                onOpenZ1Profiles = { subScreen = "z1profiles" },
                onOpenTor = { subScreen = "tor" },
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
                    "devices" -> DevicesScreen(viewModel, onBack = { subScreen = null })
                    "mihomo" -> MihomoScreen(
                        viewModel = mihomoViewModel,
                        ruleProvidersViewModel = ruleProvidersViewModel,
                        subscriptionsViewModel = subscriptionsViewModel,
                        onBack = { subScreen = null },
                    )
                    "tor" -> TorScreen(
                        viewModel = torViewModel,
                        onBack = { subScreen = null },
                    )
                    "diagnostics" -> DiagnosticsScreen(
                        viewModel = diagnosticsViewModel,
                        wanIp = viewModel.state.collectAsState().value.sysInfo?.wanIp?.takeIf { it.isNotEmpty() },
                        onBack = { subScreen = null },
                        autoRunSpeedtest = autoRunSpeedtest,
                        onAutoRunSpeedtestConsumed = onAutoRunSpeedtestConsumed,
                    )
                    "magitrickle_groups" -> MagitrickleGroupsScreen(
                        viewModel = magitrickleGroupsViewModel,
                        onBack = { subScreen = null },
                    )
                    "z2profiles" -> Z2ProfilesScreen(
                        viewModel = z2ProfilesViewModel,
                        onBack = { subScreen = null },
                    )
                    "z1profiles" -> Z1ProfilesScreen(
                        viewModel = z1ProfilesViewModel,
                        onBack = { subScreen = null },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusRootScreen(
    viewModel: ServicesViewModel,
    onOpenDevices: () -> Unit,
    onOpenMihomo: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenMagitrickleGroups: () -> Unit,
    onOpenZ2Profiles: () -> Unit,
    onOpenZ1Profiles: () -> Unit,
    onOpenTor: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val ipRevealed = rememberIpFlipRevealState()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    HelmScaffold(title = stringResource(R.string.status)) { padding ->
        // First svc_list (no cache yet) → skeletons; later refreshes keep the
        // regular indicator over the existing cards.
        val firstLoad = state.services == null && state.error == null
        val skeletonBrush = if (firstLoad) shimmerBrush() else null

        HelmPullToRefreshBox(
            isRefreshing = state.isRefreshing && !firstLoad,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp + (LocalBottomBarPadding.current ?: 0.dp)),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.error?.let { message ->
                    item { ErrorCard(message, onRetry = { viewModel.refresh() }) }
                }
                if (skeletonBrush != null) {
                    item { SkeletonHealthCard(skeletonBrush) }
                    item { SkeletonBox(skeletonBrush, Modifier.fillMaxWidth().height(56.dp), MaterialTheme.shapes.medium) }
                    // mihomo / magitrickle / zapret / zapret2
                    items(4) { SkeletonServiceCard(skeletonBrush) }
                }
                state.sysInfo?.let { info ->
                    item { HealthCard(info, state.diskInfo, ipRevealed) }
                }
                state.devices?.let { devices ->
                    item { DevicesSummaryCard(count = devices.size, onClick = onOpenDevices) }
                }
                if (!firstLoad) {
                    item { DiagnosticsEntryCard(onClick = onOpenDiagnostics) }
                }
                val services = state.services
                if (services != null) {
                    items(services, key = { it.service }) { svc ->
                        ServiceCard(
                            status = svc,
                            busy = state.busyService == svc.service,
                            actionsEnabled = state.busyService == null,
                            onStart = { viewModel.start(svc.service) },
                            onStop = { viewModel.stop(svc.service) },
                            onRestart = { viewModel.restart(svc.service) },
                            onOpenLog = { viewModel.openServiceLog(svc.service) },
                            // mihomo drills into its own screen; MagiTrickle
                            // drills into the read-only groups overview; Tor
                            // into bridges + new identity + connection test.
                            // Each zapret engine's profile switcher is only
                            // reachable while THAT engine is the actually
                            // detected running one — same svc_list-driven gating
                            // the Strategies auto-select uses, just expressed as
                            // "no chevron" here instead of "no tab".
                            onOpen = when {
                                svc.service == "mihomo" -> onOpenMihomo
                                svc.service == "magitrickle" -> onOpenMagitrickleGroups
                                svc.service == "tor" -> onOpenTor
                                svc.service == ENGINE_ZAPRET2 && svc.running -> onOpenZ2Profiles
                                svc.service == ENGINE_ZAPRET && svc.running -> onOpenZ1Profiles
                                else -> null
                            },
                        )
                    }
                }
            }
        }
    }

    val logState by viewModel.serviceLog.collectAsState()
    logState?.let { log ->
        HelmBottomSheet(onDismissRequest = { viewModel.closeServiceLog() }) {
            ServiceLogSheetContent(
                state = log,
                onRefresh = { viewModel.refreshServiceLog() },
            )
        }
    }
}

/**
 * Monospace log-line viewer for one service (action=service_log). An empty,
 * successfully-fetched list is a normal answer for magitrickle/zapret/zapret2 —
 * shown as a plain "nothing found" line, never styled as an error.
 */
@Composable
private fun ServiceLogSheetContent(state: ServiceLogUiState, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 480.dp)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShapeIcon(Icons.Outlined.Article, Tints.purple, shape = MaterialShapes.Clover4Leaf, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.service_log_title, serviceLabel(state.service)),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            if (state.loading) {
                LoadingIndicator(Modifier.size(36.dp))
            } else {
                SectionIconButton(Icons.Filled.Refresh, stringResource(R.string.refresh), tint = Tints.purple, onClick = onRefresh)
            }
        }
        Spacer(Modifier.height(8.dp))
        state.error?.let { ErrorCard(it) }
        val lines = state.lines
        LazyColumn(
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(RoundedCornerShape(18.dp))
                .background(Helm.colors.card),
            contentPadding = PaddingValues(12.dp),
        ) {
            if (lines != null && lines.isEmpty() && !state.loading) {
                item {
                    Text(
                        stringResource(R.string.service_log_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
            lines?.let { list ->
                items(list.size) { index ->
                    val line = list[index]
                    val palette = rememberLogPalette()
                    val level = remember(line) { logLevelOf(line) }
                    val highlighted = remember(line, palette) { highlightLogLine(line, palette) }
                    // Severity bar on the left; errors also get a faint red wash.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (level == LogLevel.ERROR) Tints.red.bg.copy(alpha = Tints.red.bg.alpha * 0.6f)
                                else Color.Transparent,
                            )
                            .height(IntrinsicSize.Min),
                    ) {
                        Box(
                            Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(palette.colorOf(level)),
                        )
                        Text(
                            text = highlighted,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
                        )
                    }
                }
            }
        }
    }
}

// --- Router health header (action=sys_info) ---------------------------------------

/** Shared by the router health card and the Tor screen — both show an uptime_seconds field. */
@Composable
internal fun formatUptime(seconds: Long): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        days > 0 -> stringResource(R.string.dur_days_hours, days, hours)
        hours > 0 -> stringResource(R.string.dur_hours_minutes, hours, minutes)
        else -> stringResource(R.string.dur_minutes, minutes)
    }
}

/**
 * "ImmortalWrt 25.12.0 r37854-4b24da3b4c5c" → "ImmortalWrt 25.12.0": name + version
 * only, the git-revision-hash tail is noise for a status card and is dropped
 * outright (not parsed, just cut after the second whitespace-separated token).
 */
private fun shortFirmware(firmware: String): String {
    val parts = firmware.trim().split(Regex("\\s+"))
    return if (parts.size >= 2) "${parts[0]} ${parts[1]}" else firmware
}

@Composable
private fun HealthCard(info: SysInfo, disk: DiskInfo?, ipRevealed: Boolean) {
    HelmSection(
        title = info.model.ifEmpty { stringResource(R.string.health_model) },
        subtitle = info.firmware.takeIf { it.isNotEmpty() }?.let { shortFirmware(it) },
        icon = Icons.Outlined.Router,
        tint = Tints.accent,
        shape = MaterialShapes.Cookie12Sided,
    ) {
        val tiles = buildList<@Composable (Modifier) -> Unit> {
            add { m ->
                StatTile(
                    label = stringResource(R.string.health_uptime),
                    value = formatUptime(info.uptimeSeconds),
                    icon = Icons.Outlined.Schedule,
                    tint = Tints.blue,
                    shape = MaterialShapes.Cookie6Sided,
                    modifier = m,
                )
            }
            add { m ->
                StatTile(
                    label = stringResource(R.string.health_load),
                    value = String.format(Locale.US, "%.2f · %.2f", info.load1, info.load5),
                    icon = Icons.Outlined.Speed,
                    tint = if (info.load1 >= 2.0) Tints.orange else Tints.teal,
                    shape = MaterialShapes.Sunny,
                    modifier = m,
                )
            }
            // null = the board exposes no cpu-thermal zone: no tile at all.
            info.cpuTempC?.let { temp ->
                add { m ->
                    StatTile(
                        label = stringResource(R.string.health_cpu_temp),
                        value = String.format(Locale.US, "%.1f°C", temp),
                        icon = Icons.Outlined.Thermostat,
                        tint = when {
                            temp >= 80 -> Tints.red
                            temp >= 65 -> Tints.amber
                            else -> Tints.green
                        },
                        shape = MaterialShapes.Flower,
                        modifier = m,
                    )
                }
            }
            add { m ->
                val usedKb = (info.memTotalKb - info.memAvailableKb).coerceAtLeast(0)
                val pct = if (info.memTotalKb > 0) (usedKb * 100 / info.memTotalKb).toInt() else 0
                StatTile(
                    label = "${stringResource(R.string.health_memory)} · ${formatBytes(info.memTotalKb * 1024)}",
                    value = "$pct% · ${formatBytes(usedKb * 1024)}",
                    icon = Icons.Outlined.Memory,
                    tint = if (pct >= 90) Tints.red else Tints.purple,
                    shape = MaterialShapes.Clover4Leaf,
                    modifier = m,
                )
            }
        }
        TileGrid(tiles)
        // null while the first disk_info has not answered yet: the row just does not
        // exist yet, same "hide, don't placeholder" convention as WAN IPv6.
        disk?.let { d ->
            val warning = d.usedPercent >= 90
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KeyValueRow(
                    label = stringResource(R.string.health_disk),
                    value = "${formatBytes(d.usedKb * 1024)} / ${formatBytes(d.totalKb * 1024)} (${d.usedPercent}%)",
                    valueColor = if (warning) Tints.red.fg else Helm.colors.text,
                )
                LinearWavyProgressIndicator(
                    progress = { d.usedPercent / 100f },
                    color = if (warning) Tints.red.fg else Helm.colors.accent,
                    trackColor = Helm.colors.surface2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        // Hidden by default (placeholder dots), revealed only while the phone is
        // flipped face-down, same idea as "flip to shhh". Empty = WAN is down, not
        // an error, so it still renders (as "—") once revealed.
        Column {
            IpRow(
                label = stringResource(R.string.health_wan_ip),
                value = info.wanIp.ifEmpty { "—" },
                revealed = ipRevealed,
            )
            // No IPv6: no row at all (not an "IPv6: —" placeholder).
            if (info.wanIpv6.isNotEmpty()) {
                IpRow(
                    label = stringResource(R.string.health_wan_ipv6),
                    value = info.wanIpv6,
                    revealed = ipRevealed,
                )
            }
        }
    }
}

@Composable
private fun HealthRow(label: String, value: String, monospace: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // The value gets the flexible space: long firmware strings wrap instead of
        // squeezing the label out.
        Text(
            text = value,
            style = if (monospace) {
                MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
            } else MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        )
    }
}

/**
 * IPv4/IPv6 row — same two-column label/value layout as every other
 * [HealthRow] on the card (label left, value right-aligned, same body style),
 * just with the value swappable between the real address and a "•••"
 * placeholder. [revealed] drives that swap; it cross-fades with a light scale
 * so the flip gesture reads as a deliberate reveal rather than a jump cut.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IpRow(label: String, value: String, revealed: Boolean) {
    val motion = MaterialTheme.motionScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AnimatedContent(
            targetState = revealed,
            transitionSpec = {
                (scaleIn(initialScale = 0.85f, animationSpec = motion.fastSpatialSpec()) +
                    fadeIn(animationSpec = motion.fastEffectsSpec()))
                    .togetherWith(
                        scaleOut(targetScale = 0.85f, animationSpec = motion.fastSpatialSpec()) +
                            fadeOut(animationSpec = motion.fastEffectsSpec()),
                    )
            },
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            label = "ip-reveal",
        ) { show ->
            Text(
                text = if (show) value else "•••",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** How far past "flat" (m/s²) the Z axis must sit before a side counts as settled. */
private const val FLIP_FLAT_Z_THRESHOLD = 7f

/** Debounce so one physical flip fires exactly one toggle, not several mid-turn. */
private const val FLIP_DEBOUNCE_MS = 600L

/**
 * Toggles on a physical device flip (screen-up ↔ screen-down) — the "flip to
 * shhh" gesture, read from the accelerometer's Z axis with hysteresis so only
 * a settled face-up/face-down transition counts, not the phone mid-tumble.
 * Purely a data-reveal toggle: never touches screen orientation. The listener
 * only runs while the Status tab is actually resumed, matching the rest of the
 * app's "no background sensing" convention.
 */
@Composable
private fun rememberIpFlipRevealState(): Boolean {
    var revealed by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        // null = not yet settled into either stable side.
        var faceUp: Boolean? = null
        var lastToggleAt = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val z = event.values[2]
                val nowFaceUp = when {
                    z > FLIP_FLAT_Z_THRESHOLD -> true
                    z < -FLIP_FLAT_Z_THRESHOLD -> false
                    else -> return // mid-flip / not flat enough to classify yet
                }
                val now = System.currentTimeMillis()
                if (faceUp != null && faceUp != nowFaceUp && now - lastToggleAt > FLIP_DEBOUNCE_MS) {
                    revealed = !revealed
                    lastToggleAt = now
                }
                faceUp = nowFaceUp
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
        if (sensor != null) sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)

        onPauseOrDispose { sensorManager?.unregisterListener(listener) }
    }

    return revealed
}

@Composable
private fun SkeletonHealthCard(brush: androidx.compose.ui.graphics.Brush) {
    HelmCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(4) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    SkeletonBox(brush, Modifier.fillMaxWidth(0.3f).height(12.dp))
                    SkeletonBox(brush, Modifier.fillMaxWidth(0.35f).height(12.dp))
                }
            }
            SkeletonBox(brush, Modifier.fillMaxWidth().height(4.dp), CircleShape)
        }
    }
}

// --- Connected devices summary (action=devices) -----------------------------------

@Composable
private fun DevicesSummaryCard(count: Int, onClick: () -> Unit) {
    EntryCard(
        icon = Icons.Outlined.Devices,
        title = stringResource(R.string.devices_title),
        subtitle = pluralStringResource(R.plurals.n_devices_online, count, count),
        onClick = onClick,
        tint = Tints.teal,
        shape = MaterialShapes.Cookie9Sided,
        trailing = { ValuePill(count.toString(), Tints.teal) },
    )
}

// --- Network diagnostics entry ----------------------------------------------------

@Composable
private fun DiagnosticsEntryCard(onClick: () -> Unit) {
    EntryCard(
        icon = Icons.Outlined.NetworkCheck,
        title = stringResource(R.string.diag_net_title),
        subtitle = stringResource(R.string.diag_entry_desc),
        onClick = onClick,
        tint = Tints.orange,
        shape = MaterialShapes.Sunny,
    )
}

// --- Service cards ----------------------------------------------------------------

@Composable
private fun ServiceCard(
    status: ServiceStatus,
    busy: Boolean,
    actionsEnabled: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onOpenLog: () -> Unit,
    onOpen: (() -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ServiceIcon(status)
                Spacer(Modifier.width(14.dp))
                Text(
                    style = MaterialTheme.typography.titleLarge,
                    text = serviceLabel(status.service),
                    modifier = Modifier.weight(1f),
                )
                // svc_start/stop/restart can block up to ~5 s on the router.
                if (busy) {
                    LoadingIndicator(Modifier.size(36.dp))
                } else if (onOpen != null) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // running (live procd state) and enabled (autostart on next boot)
            // are independent flags — always show both.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    RunningIndicator(running = status.running, enabled = status.enabled)
                    AutostartIndicator(status.enabled)
                }
                // 18dp glyph, but the button keeps its default 48dp target — pinning
                // the button itself to 32dp put it under the minimum touch size.
                IconButton(onClick = onOpenLog) {
                    Icon(
                        Icons.Outlined.Article,
                        contentDescription = stringResource(R.string.service_log_button),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!status.running) {
                    TintButton(
                        text = stringResource(R.string.svc_start),
                        tint = Tints.green,
                        enabled = actionsEnabled,
                        icon = Icons.Filled.PlayArrow,
                        modifier = Modifier.weight(1f),
                        onClick = onStart,
                    )
                } else {
                    TintButton(
                        text = stringResource(R.string.svc_stop),
                        tint = Tints.red,
                        enabled = actionsEnabled,
                        icon = Icons.Filled.Stop,
                        modifier = Modifier.weight(1f),
                        onClick = onStop,
                    )
                    TintButton(
                        text = stringResource(R.string.svc_restart),
                        tint = Tints.accent,
                        enabled = actionsEnabled,
                        icon = Icons.Filled.Refresh,
                        modifier = Modifier.weight(1f),
                        onClick = onRestart,
                    )
                }
            }
        }
    }
    if (onOpen != null) {
        HelmCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) { content() }
    } else {
        HelmCard(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
internal fun RunningIndicator(running: Boolean, enabled: Boolean) {
    // Derived on the client from the two existing flags:
    // running → green; stopped but enabled → red (should be running — failed);
    // stopped and disabled → grey (intentionally off).
    val (tint, labelRes) = when {
        running -> statusOk to R.string.svc_running
        enabled -> MaterialTheme.colorScheme.error to R.string.svc_failed
        else -> Tints.gray.fg to R.string.svc_stopped
    }
    StatusPill(stringResource(labelRes), tint)
}

@Composable
internal fun AutostartIndicator(enabled: Boolean) {
    val tint = if (enabled) Helm.colors.accent else Tints.gray.fg
    // The label reads "Autostart" in both states — on or off is carried entirely by
    // the check/cross glyph and the tint, so a screen reader hears the same thing
    // either way. Merge the pair into one node that names the state.
    val stateLabel = stringResource(
        if (enabled) R.string.a11y_autostart_on else R.string.a11y_autostart_off
    )
    StatusPill(
        text = stringResource(R.string.svc_autostart),
        color = tint,
        icon = if (enabled) Icons.Filled.Check else Icons.Filled.Close,
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = stateLabel },
    )
}

/** Shaped, state-tinted icon of a service card: the shape is per service, the colour follows its state. */
@Composable
private fun ServiceIcon(status: ServiceStatus) {
    val tint = when {
        status.running -> Tints.green
        status.enabled -> Tints.red
        else -> Tints.gray
    }
    val (icon, shape) = when (status.service) {
        "mihomo" -> Icons.Outlined.Hub to MaterialShapes.Cookie9Sided
        "magitrickle" -> Icons.Outlined.AltRoute to MaterialShapes.Clover4Leaf
        "tor" -> Icons.Outlined.VpnLock to MaterialShapes.Cookie12Sided
        ENGINE_ZAPRET2, ENGINE_ZAPRET -> Icons.Outlined.Shield to MaterialShapes.Cookie6Sided
        else -> Icons.Outlined.Dns to MaterialShapes.Sunny
    }
    ShapeIcon(icon, tint, shape = shape, size = 44.dp)
}
