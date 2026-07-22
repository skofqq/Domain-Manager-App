package com.skofqq.domainmanager.ui

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
import kotlinx.coroutines.CancellationException
import java.util.Locale

private fun serviceLabel(service: String) =
    if (service == "magitrickle") "MagiTrickle" else service

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
                    "mihomo" -> MihomoScreen(mihomoViewModel, onBack = { subScreen = null })
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
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val ipRevealed = rememberIpFlipRevealState()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.status)) },
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        // First svc_list (no cache yet) → skeletons; later refreshes keep the
        // regular indicator over the existing cards.
        val firstLoad = state.services == null && state.error == null
        val skeletonBrush = if (firstLoad) shimmerBrush() else null

        PullToRefreshBox(
            isRefreshing = state.isRefreshing && !firstLoad,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
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
                            // drills into the read-only groups overview. Each
                            // zapret engine's profile switcher is only reachable
                            // while THAT engine is the actually detected running
                            // one — same svc_list-driven gating the Strategies
                            // auto-select uses, just expressed as "no chevron"
                            // here instead of "no tab".
                            onOpen = when {
                                svc.service == "mihomo" -> onOpenMihomo
                                svc.service == "magitrickle" -> onOpenMagitrickleGroups
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
        ModalBottomSheet(onDismissRequest = { viewModel.closeServiceLog() }) {
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
            Text(
                text = stringResource(R.string.service_log_title, serviceLabel(state.service)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                TextButton(onClick = onRefresh) { Text(stringResource(R.string.refresh)) }
            }
        }
        Spacer(Modifier.height(8.dp))
        state.error?.let { ErrorCard(it) }
        val lines = state.lines
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
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
                    Text(
                        text = list[index],
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.padding(vertical = 3.dp),
                    )
                }
            }
        }
    }
}

// --- Router health header (action=sys_info) ---------------------------------------

@Composable
private fun formatUptime(seconds: Long): String {
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
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (info.model.isNotEmpty()) {
                HealthRow(label = stringResource(R.string.health_model), value = info.model)
            }
            if (info.firmware.isNotEmpty()) {
                HealthRow(label = stringResource(R.string.health_firmware), value = shortFirmware(info.firmware))
            }
            HealthRow(
                label = stringResource(R.string.health_uptime),
                value = formatUptime(info.uptimeSeconds),
            )
            HealthRow(
                label = stringResource(R.string.health_load),
                value = String.format(Locale.US, "%.2f · %.2f · %.2f", info.load1, info.load5, info.load15),
            )
            // null = the board exposes no cpu-thermal zone — hide the row entirely,
            // same convention as the WAN IPv6 row above.
            info.cpuTempC?.let { temp ->
                HealthRow(
                    label = stringResource(R.string.health_cpu_temp),
                    value = String.format(Locale.US, "%.1f°C", temp),
                )
            }
            // Hidden by default (placeholder dots) — revealed only while the phone
            // is flipped face-down, same idea as "flip to shhh". Empty = WAN is
            // down, not an error, so it still renders (as "—") once revealed.
            IpRow(
                label = stringResource(R.string.health_wan_ip),
                value = info.wanIp.ifEmpty { "—" },
                revealed = ipRevealed,
            )
            // No IPv6 → no row at all (not an "IPv6: —" placeholder).
            if (info.wanIpv6.isNotEmpty()) {
                IpRow(
                    label = stringResource(R.string.health_wan_ipv6),
                    value = info.wanIpv6,
                    revealed = ipRevealed,
                )
            }
            val usedKb = (info.memTotalKb - info.memAvailableKb).coerceAtLeast(0)
            HealthRow(
                label = stringResource(R.string.health_memory),
                value = "${formatBytes(usedKb * 1024)} / ${formatBytes(info.memTotalKb * 1024)}",
            )
            LinearProgressIndicator(
                progress = {
                    if (info.memTotalKb > 0) usedKb.toFloat() / info.memTotalKb else 0f
                },
                modifier = Modifier.fillMaxWidth(),
            )
            // null while the first disk_info hasn't answered yet — row just doesn't
            // exist yet, same "hide, don't placeholder" convention as WAN IPv6.
            disk?.let { d ->
                val warning = d.usedPercent >= 90
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (warning) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(
                            text = stringResource(R.string.health_disk),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (warning) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "${formatBytes(d.usedKb * 1024)} / ${formatBytes(d.totalKb * 1024)} (${d.usedPercent}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (warning) MaterialTheme.colorScheme.error else Color.Unspecified,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp),
                    )
                }
                LinearProgressIndicator(
                    progress = { d.usedPercent / 100f },
                    color = if (warning) MaterialTheme.colorScheme.error else ProgressIndicatorDefaults.linearColor,
                    modifier = Modifier.fillMaxWidth(),
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
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Devices,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = pluralStringResource(R.plurals.n_devices_online, count, count),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// --- Network diagnostics entry ----------------------------------------------------

@Composable
private fun DiagnosticsEntryCard(onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.NetworkCheck,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.diag_net_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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
                Text(
                    text = serviceLabel(status.service),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                // svc_start/stop/restart can block up to ~5 s on the router.
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
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
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                    RunningIndicator(running = status.running, enabled = status.enabled)
                    AutostartIndicator(status.enabled)
                }
                IconButton(onClick = onOpenLog, modifier = Modifier.size(32.dp)) {
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
                    FilledTonalButton(
                        onClick = onStart,
                        enabled = actionsEnabled,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.svc_start)) }
                } else {
                    OutlinedButton(
                        onClick = onStop,
                        enabled = actionsEnabled,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.svc_stop)) }
                    FilledTonalButton(
                        onClick = onRestart,
                        enabled = actionsEnabled,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.svc_restart)) }
                }
            }
        }
    }
    if (onOpen != null) {
        ElevatedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) { content() }
    } else {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

/** Green when the service runs. */
private val RunningGreen = Color(0xFF4CAF50)

@Composable
private fun RunningIndicator(running: Boolean, enabled: Boolean) {
    // Derived on the client from the two existing flags:
    // running → green; stopped but enabled → red (should be running — failed);
    // stopped and disabled → grey (intentionally off).
    val (tint, labelRes) = when {
        running -> RunningGreen to R.string.svc_running
        enabled -> MaterialTheme.colorScheme.error to R.string.svc_failed
        else -> MaterialTheme.colorScheme.outline to R.string.svc_stopped
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(tint),
        )
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = tint,
        )
    }
}

@Composable
private fun AutostartIndicator(enabled: Boolean) {
    val tint = if (enabled) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = if (enabled) Icons.Filled.Check else Icons.Filled.Close,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(R.string.svc_autostart),
            style = MaterialTheme.typography.labelMedium,
            color = tint,
        )
    }
}
