package com.skofqq.domainmanager.ui

import com.skofqq.domainmanager.ui.theme.Helm
import com.skofqq.domainmanager.ui.theme.Tints
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material3.MaterialShapes
import androidx.compose.material.icons.outlined.Devices
import com.skofqq.domainmanager.ui.components.*
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.DeviceUnknown
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.LanDevice
import com.skofqq.domainmanager.ui.theme.statusFavorite
import com.skofqq.domainmanager.ui.theme.statusOk

/**
 * Shared scaffold for the Status tab's push screens (Devices, mihomo): inline
 * large title + back arrow, same shape as the Settings child screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatusChildScaffold(
    title: String,
    onBack: () -> Unit,
    /** Optional FAB slot — only the rule-providers child uses one so far. */
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    HelmScaffold(
        title = title,
        onBack = onBack,
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

// --- Device type icons ------------------------------------------------------------

/**
 * Icon keys, in the order the manual picker shows them. The router's type field is
 * free text from a user-maintained DHCP tag (new values appear without warning), so
 * [guessDeviceIcon] matches keywords, never exact strings.
 */
private val DEVICE_ICON_KEYS = listOf(
    "speaker", "bulb", "phone", "computer", "laptop", "router", "zigbee", "printer",
    "socket", "tv", "storage", "camera", "generic",
)

@Composable
private fun deviceIcon(key: String): ImageVector = when (key) {
    "speaker" -> Icons.Outlined.Speaker
    "bulb" -> Icons.Outlined.Lightbulb
    "phone" -> Icons.Outlined.Smartphone
    // Custom asset (Material "personal_video" glyph) — distinct from "laptop"
    // below and from the stock Material "Computer"/"Laptop" glyphs, per the
    // user-supplied reference icon.
    "computer" -> ImageVector.vectorResource(id = R.drawable.ic_device_pc)
    "laptop" -> Icons.Outlined.Laptop
    "router" -> Icons.Outlined.Router
    // Signal-arcs hub glyph — distinct from the Wi-Fi router icon above.
    "zigbee" -> Icons.Outlined.Sensors
    "printer" -> Icons.Outlined.Print
    "socket" -> Icons.Outlined.Power
    // Play-triangle-on-screen glyph — distinct from the "computer" glyph above
    // (both used to be plain-monitor shapes and were indistinguishable).
    "tv" -> Icons.Outlined.LiveTv
    "storage" -> Icons.Outlined.Storage
    "camera" -> Icons.Outlined.Videocam
    else -> Icons.Outlined.DeviceUnknown
}

/** Short caption shown under each icon in the picker (item 3). */
private fun deviceIconLabelRes(key: String): Int = when (key) {
    "speaker" -> R.string.device_type_speaker
    "bulb" -> R.string.device_type_bulb
    "phone" -> R.string.device_type_phone
    "computer" -> R.string.device_type_pc
    "laptop" -> R.string.device_type_laptop
    "router" -> R.string.device_type_router
    "zigbee" -> R.string.device_type_zigbee
    "printer" -> R.string.device_type_printer
    "socket" -> R.string.device_type_socket
    "tv" -> R.string.device_type_tv
    "storage" -> R.string.device_type_storage
    "camera" -> R.string.device_type_camera
    else -> R.string.device_type_generic
}

/** Case-insensitive keyword match; anything unmatched (incl. empty) → "generic". */
private fun guessDeviceIcon(type: String): String {
    val t = type.lowercase()
    return when {
        "speaker" in t -> "speaker"
        "lamp" in t || "bulb" in t || "light" in t -> "bulb"
        "phone" in t -> "phone"
        "laptop" in t -> "laptop"
        "pc" in t || "computer" in t || "desktop" in t -> "computer"
        // Checked before the generic "gateway" router match below — a ZigBee
        // gateway tag also contains "gateway" and would otherwise be misread
        // as a plain Wi-Fi router.
        "zigbee" in t -> "zigbee"
        "router" in t || "gateway" in t -> "router"
        "printer" in t -> "printer"
        "socket" in t || "plug" in t -> "socket"
        "tv" in t -> "tv"
        "storage" in t || "nas" in t -> "storage"
        "camera" in t -> "camera"
        else -> "generic"
    }
}

// --- Screen -----------------------------------------------------------------------

/** Read-only list of the router's LAN clients; long-press a row to pick its icon. */
@Composable
fun DevicesScreen(viewModel: ServicesViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val scan by viewModel.deviceScan.collectAsState()
    val iconOverrides by viewModel.deviceIcons.collectAsState()
    val favorites by viewModel.favoriteDeviceMacs.collectAsState()
    var pickerDevice by remember { mutableStateOf<LanDevice?>(null) }
    val context = LocalContext.current

    // Sweep once per arrival of a (new) device list: screen opening and every
    // pull-to-refresh produce a fresh list instance, re-triggering the scan.
    LaunchedEffect(state.devices) {
        if (state.devices != null) viewModel.scanDevices()
    }

    // Wake-on-LAN result as a one-shot toast: "packet sent", never a promise the
    // device actually woke — the protocol gives no confirmation.
    val wakeMessage by viewModel.wakeMessage.collectAsState()
    LaunchedEffect(wakeMessage) {
        wakeMessage?.let {
            Toast.makeText(context, it.resolve(context), Toast.LENGTH_SHORT).show()
            viewModel.clearWakeMessage()
        }
    }

    StatusChildScaffold(stringResource(R.string.devices_title), onBack) { padding ->
        val firstLoad = state.devices == null
        val skeletonBrush = if (firstLoad) shimmerBrush() else null

        HelmPullToRefreshBox(
            isRefreshing = state.isRefreshing && !firstLoad,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (skeletonBrush != null) {
                    items(6) { SkeletonDeviceRow(skeletonBrush) }
                }
                if (scan.scanning) {
                    item { ScanProgressRow(done = scan.done, total = scan.total) }
                }
                val devices = state.devices
                if (devices != null) {
                    // Favorites first, original router order preserved within each group.
                    val (starred, others) = devices.partition { it.mac in favorites }
                    val groups = listOfNotNull(
                        starred.takeIf { it.isNotEmpty() }?.let { true to it },
                        others.takeIf { it.isNotEmpty() }?.let { false to it },
                    )
                    groups.forEach { (isFavorites, list) ->
                        item(key = if (isFavorites) "favorites" else "all") {
                            val onlineCount = list.count { scan.online[it.mac] == true }
                            HelmSection(
                                title = stringResource(if (isFavorites) R.string.devices_favorites else R.string.devices_all),
                                subtitle = if (scan.online.isNotEmpty()) {
                                    stringResource(R.string.devices_online_of, onlineCount, list.size)
                                } else {
                                    pluralStringResource(R.plurals.n_devices_online, list.size, list.size)
                                },
                                icon = if (isFavorites) Icons.Filled.Star else Icons.Outlined.Devices,
                                tint = if (isFavorites) Tints.amber else Tints.teal,
                                shape = if (isFavorites) MaterialShapes.Sunny else MaterialShapes.Cookie9Sided,
                                trailing = { ValuePill(list.size.toString(), if (isFavorites) Tints.amber else Tints.teal) },
                            ) {
                                Column {
                                    list.forEachIndexed { index, device ->
                                        if (index > 0) SectionDivider()
                                        val iconKey = iconOverrides[device.mac] ?: guessDeviceIcon(device.type)
                                        DeviceCard(
                                            device = device,
                                            // Manual pick wins over the type-keyword guess.
                                            iconKey = iconKey,
                                            online = scan.online[device.mac],
                                            favorite = device.mac in favorites,
                                            onToggleFavorite = { viewModel.toggleFavoriteDevice(device.mac) },
                                            // WoL only makes sense for PCs/laptops; the resolved icon
                                            // (guess or manual pick) is the closest signal we have.
                                            onWake = if (iconKey == "computer") {
                                                { viewModel.wake(device.mac) }
                                            } else null,
                                            onLongPress = { pickerDevice = device },
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

    pickerDevice?.let { device ->
        DeviceIconPickerDialog(
            device = device,
            currentOverride = iconOverrides[device.mac],
            onPick = { key ->
                viewModel.setDeviceIcon(device.mac, key)
                pickerDevice = null
            },
            onDismiss = { pickerDevice = null },
        )
    }
}

/** Small "checking devices…" strip shown while the router-side ping sweep runs. */
@Composable
private fun ScanProgressRow(done: Int, total: Int) {
    Column(
        Modifier.fillMaxWidth().clip(CardShape).background(Helm.colors.card).padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        LabeledProgress(
            label = stringResource(R.string.device_scan_label),
            valueText = stringResource(R.string.device_scan_progress, done, total),
            progress = if (total > 0) done.toFloat() / total else 0f,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceCard(
    device: LanDevice,
    iconKey: String,
    /** null = not checked (yet): no indicator at all. */
    online: Boolean?,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    /** Non-null only for wakeable (computer-like) devices. */
    onWake: (() -> Unit)?,
    onLongPress: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val copiedMessage = stringResource(R.string.copied)
    fun copy(value: String) {
        clipboard.setText(AnnotatedString(value))
        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            // Tap and long press both open the icon picker, under the same name.
            .combinedClickable(
                onClickLabel = stringResource(R.string.device_icon_title),
                onLongClickLabel = stringResource(R.string.device_icon_title),
                onClick = onLongPress,
                onLongClick = onLongPress,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The shape carries the device, the colour carries the ping verdict.
        val tint = when (online) {
            true -> Tints.green
            false -> Tints.gray
            null -> Tints.accent
        }
        Box {
            ShapeIcon(deviceIcon(iconKey), tint, shape = MaterialShapes.Cookie9Sided, size = 44.dp)
            // "Online now" verdict from the router-side ping sweep, stated in words for
            // TalkBack instead of leaving it to the colour.
            if (online != null) {
                val onlineLabel = stringResource(
                    if (online) R.string.a11y_device_online else R.string.a11y_device_offline
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Helm.colors.card)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(if (online) statusOk else Helm.colors.text2.copy(alpha = 0.5f))
                        .semantics { contentDescription = onlineLabel },
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        ) {
            Text(
                // DHCP hostnames cannot contain spaces, so users type dashes:
                // display-only swap, the source string stays untouched.
                text = device.name.replace('-', ' ')
                    .ifEmpty { stringResource(R.string.device_unknown) },
                style = MaterialTheme.typography.titleMedium,
                color = if (device.name.isEmpty()) Helm.colors.text2 else Helm.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Stacked, not side-by-side: on narrow rows a shared line would wrap the
            // MAC mid-address. Tap a value to copy it; the padding lifts each line to
            // a comfortable touch target.
            Text(
                text = device.ip,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = Helm.colors.text2,
                modifier = Modifier
                    .clickable(onClickLabel = stringResource(R.string.copy_ip)) { copy(device.ip) }
                    .padding(vertical = 4.dp),
            )
            Text(
                text = device.mac,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = Helm.colors.text2,
                modifier = Modifier
                    .clickable(onClickLabel = stringResource(R.string.copy_mac)) { copy(device.mac) }
                    .padding(vertical = 4.dp),
            )
        }
        if (onWake != null) {
            SectionIconButton(
                Icons.Outlined.PowerSettingsNew,
                stringResource(R.string.wake_device),
                tint = Tints.accent,
                onClick = onWake,
            )
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                if (favorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = stringResource(
                    if (favorite) R.string.unfavorite_device else R.string.favorite_device
                ),
                tint = if (favorite) statusFavorite else Helm.colors.text2,
            )
        }
    }
}

/**
 * Manual icon pick for one device, stored locally by MAC ([onPick] null = back to
 * the automatic keyword guess). Fullscreen (item 4) — the labelled grid (item 3)
 * needs more room than a small centered dialog, and may need to scroll.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeviceIconPickerDialog(
    device: LanDevice,
    currentOverride: String?,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 20.dp),
            shape = RoundedCornerShape(30.dp),
            color = com.skofqq.domainmanager.ui.theme.Helm.colors.page,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                Text(
                    stringResource(R.string.device_icon_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    device.name.replace('-', ' ').ifEmpty { device.mac },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )
                FlowRow(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DEVICE_ICON_KEYS.forEach { key ->
                        val selected = key == currentOverride
                        Column(
                            modifier = Modifier
                                .width(78.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onPick(key) }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Selected one: accent fill on a fuller cookie; others: calm, neutral.
                            ShapeIcon(
                                deviceIcon(key),
                                if (selected) com.skofqq.domainmanager.ui.theme.Tint(Helm.colors.onBrand, Helm.colors.brand)
                                else com.skofqq.domainmanager.ui.theme.Tint(Helm.colors.text2, Helm.colors.card),
                                shape = if (selected) MaterialShapes.Cookie12Sided else MaterialShapes.Cookie9Sided,
                                size = 52.dp,
                            )
                            Text(
                                text = stringResource(deviceIconLabelRes(key)),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    // Clears the manual pick — the keyword guess applies again.
                    TextButton(onClick = { onPick(null) }) {
                        Text(stringResource(R.string.device_icon_auto))
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonDeviceRow(brush: androidx.compose.ui.graphics.Brush) {
    HelmCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonBox(brush, Modifier.size(40.dp), CircleShape)
            Column(
                modifier = Modifier.padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SkeletonBox(brush, Modifier.fillMaxWidth(0.45f).height(16.dp))
                SkeletonBox(brush, Modifier.fillMaxWidth(0.7f).height(12.dp))
            }
        }
    }
}
