package com.skofqq.domainmanager.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import com.skofqq.domainmanager.ui.theme.Helm
import com.skofqq.domainmanager.ui.theme.Tint
import com.skofqq.domainmanager.ui.theme.Tints
import androidx.compose.material3.MaterialShapes
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.VpnLock
import com.skofqq.domainmanager.ui.components.*
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.TorStatus
import com.skofqq.domainmanager.data.TorTestInfo
import com.skofqq.domainmanager.ui.theme.statusOk

/**
 * Tor drill-down, reached from the Tor service card on the Status tab. Covers
 * everything the plain service card can't: bootstrap progress, a real
 * connectivity check, "new identity" (fresh circuits without a restart) and the
 * bridge list.
 */
@Composable
fun TorScreen(viewModel: TorViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var addOpen by rememberSaveable { mutableStateOf(false) }
    var confirmRemove by rememberSaveable { mutableStateOf<String?>(null) }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    // "New identity" and bridge saves report as one-shot toasts — by the time
    // they land the list on screen already shows the new state.
    state.message?.let { message ->
        val text = message.resolve()
        LaunchedEffect(message) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    val installed = state.status?.installed != false
    val skeletonBrush = if (state.status == null && state.error == null) shimmerBrush() else null

    StatusChildScaffold(
        title = stringResource(R.string.tor_title),
        onBack = onBack,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            state.error?.let { message ->
                item {
                    ErrorCard(message, onRetry = { viewModel.refresh() })
                }
            }
            if (skeletonBrush != null) {
                item {
                    SkeletonBox(
                        skeletonBrush,
                        Modifier.fillMaxWidth().height(220.dp),
                        CardShape,
                    )
                }
            }
            // installed=false is the whole answer from a router without Tor:
            // nothing below it would have any data to show.
            if (!installed) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.VpnLock,
                        title = stringResource(R.string.tor_not_installed),
                        body = stringResource(R.string.tor_not_installed_hint),
                        tint = Tints.purple,
                    )
                }
                return@LazyColumn
            }
            state.status?.let { status ->
                item {
                    TorStatusSection(
                        status = status,
                        actionsEnabled = !state.busyService && !state.savingBridges,
                        busy = state.busyService,
                        onStart = { viewModel.start() },
                        onStop = { viewModel.stop() },
                        onRestart = { viewModel.restart() },
                    )
                }

                // --- New identity ---------------------------------------------
                item {
                    HelmSection(
                        title = stringResource(R.string.tor_new_identity),
                        subtitle = stringResource(R.string.tor_new_identity_hint),
                        icon = Icons.Outlined.Shuffle,
                        tint = Tints.teal,
                        shape = MaterialShapes.Clover4Leaf,
                    ) {
                        HelmButton(
                            text = stringResource(R.string.tor_new_identity),
                            onClick = { viewModel.newIdentity() },
                            enabled = !state.busyService,
                            loading = state.newnymRunning,
                            icon = Icons.Outlined.Shuffle,
                            style = ButtonStyle.Tonal,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // --- Connectivity test ----------------------------------------
                item {
                    HelmSection(
                        title = stringResource(R.string.tor_test_title),
                        subtitle = stringResource(R.string.tor_test_hint),
                        icon = Icons.Outlined.TravelExplore,
                        tint = Tints.blue,
                        shape = MaterialShapes.Sunny,
                    ) {
                        state.testResult?.let { result -> TorTestResult(result) }
                        state.testError?.let { ErrorCard(it) }
                        HelmButton(
                            text = stringResource(
                                if (state.testRunning) R.string.tor_testing
                                else R.string.tor_test_connection
                            ),
                            onClick = { viewModel.testConnection() },
                            loading = state.testRunning,
                            icon = Icons.Filled.PlayArrow,
                            style = ButtonStyle.Primary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // --- Bridges ---------------------------------------------------
                item {
                    val bridges = state.bridges
                    HelmSection(
                        title = stringResource(R.string.tor_bridges_title),
                        subtitle = stringResource(R.string.tor_bridges_hint),
                        icon = Icons.Outlined.Route,
                        tint = Tints.orange,
                        shape = MaterialShapes.Cookie6Sided,
                        trailing = if (bridges != null) {
                            {
                                SectionIconButton(
                                    Icons.Filled.Add,
                                    stringResource(R.string.tor_add_bridge),
                                    enabled = !state.savingBridges,
                                ) { addOpen = true }
                            }
                        } else null,
                    ) {
                        state.bridgesError?.let { ErrorCard(it) }
                        // One combined busy state: tor_bridges_set only rewrites torrc,
                        // the restart right after it is what makes the change real.
                        if (state.savingBridges) {
                            SectionNote(stringResource(R.string.tor_bridges_saving))
                        }
                        WorkingStrip(state.savingBridges)
                        if (bridges != null && bridges.isEmpty()) {
                            SectionNote(stringResource(R.string.tor_no_bridges))
                        }
                        bridges?.forEach { bridge ->
                            BridgeRow(
                                bridge = bridge,
                                actionsEnabled = !state.savingBridges,
                                onRemove = { confirmRemove = bridge },
                            )
                        }
                    }
                }
            }
        }
    }

    if (addOpen) {
        AddBridgeSheet(
            onAdd = { line -> if (viewModel.addBridge(line)) addOpen = false },
            onDismiss = { addOpen = false },
        )
    }

    confirmRemove?.let { bridge ->
        GlassAlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text(stringResource(R.string.tor_bridge_delete_title)) },
            text = { Text(stringResource(R.string.tor_bridge_delete_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = null
                    viewModel.removeBridge(bridge)
                }) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

/** Header section: state pills, bootstrap progress, connection facts and the svc_* controls. */
@Composable
private fun TorStatusSection(
    status: TorStatus,
    actionsEnabled: Boolean,
    busy: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
) {
    HelmSection(
        title = "Tor",
        icon = Icons.Outlined.VpnLock,
        tint = if (status.running) Tints.green else Tints.gray,
        shape = MaterialShapes.Cookie12Sided,
        trailing = if (busy) {
            { androidx.compose.material3.LoadingIndicator(Modifier.size(40.dp)) }
        } else null,
    ) {
        // running (live procd state) and enabled (autostart on next boot) are
        // independent flags: same treatment as the service cards.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RunningIndicator(running = status.running, enabled = status.enabled)
            AutostartIndicator(status.enabled)
        }
        // Only meaningful while tor is up and still climbing to 100.
        if (status.running && status.bootstrap < 100) {
            LabeledProgress(
                label = stringResource(R.string.tor_bootstrap),
                valueText = stringResource(R.string.tor_bootstrap_progress, status.bootstrap),
                progress = status.bootstrap / 100f,
            )
        }
        Column {
            KeyValueRow(
                stringResource(R.string.tor_bridges_label),
                pluralStringResource(R.plurals.n_tor_bridges, status.bridges, status.bridges),
            )
            // The tor PROCESS uptime: it resets on every restart, it is not a
            // "time since the last new identity" counter.
            if (status.running) {
                KeyValueRow(stringResource(R.string.tor_uptime), formatUptime(status.uptimeSeconds))
            }
            if (status.socksPort > 0) {
                KeyValueRow(
                    stringResource(R.string.tor_socks_port),
                    if (status.lanIp.isEmpty()) status.socksPort.toString() else "${status.lanIp}:${status.socksPort}",
                    monospace = true,
                )
            }
            if (status.pacUrl.isNotEmpty()) {
                KeyValueRow(stringResource(R.string.tor_pac_url), status.pacUrl, monospace = true)
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

/**
 * Result of one tor_test. ok=false is a legitimate "not connected" verdict
 * (not bootstrapped, bridges unreachable): a failure state, not an error toast.
 */
@Composable
private fun TorTestResult(result: TorTestInfo) {
    val tint = if (result.ok) Tints.green else Tints.red
    val subtitle = when {
        result.ok && result.ip.isNotEmpty() -> listOfNotNull(result.ip, countryLabel(result.country)).joinToString("  ·  ")
        !result.ok -> stringResource(R.string.tor_test_failed_hint)
        else -> null
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(tint.bg).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShapeIcon(
            if (result.ok) Icons.Filled.Check else Icons.Filled.Close,
            Tint(tint.fg, tint.fg.copy(alpha = 0.18f)),
            shape = if (result.ok) MaterialShapes.Cookie9Sided else MaterialShapes.Cookie4Sided,
            size = 40.dp,
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(if (result.ok) R.string.tor_test_ok else R.string.tor_test_failed),
                style = MaterialTheme.typography.titleMedium,
                color = tint.fg,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = if (result.ok) FontFamily.Monospace else null,
                    ),
                    color = Helm.colors.text,
                )
            }
        }
    }
}

/**
 * "LU" → "🇱🇺 LU" by mapping the two ASCII letters onto regional indicator
 * symbols. Returns null for the router's "?" placeholder and anything that isn't
 * a plain two-letter code.
 */
private fun countryLabel(code: String): String? {
    if (code.length != 2 || !code.all { it in 'A'..'Z' || it in 'a'..'z' }) return null
    val flag = code.uppercase().map { Character.toChars(0x1F1E6 + (it - 'A')).concatToString() }
        .joinToString("")
    return "$flag ${code.uppercase()}"
}

/**
 * One bridge line. The string is completely opaque (obfx4 fingerprint, cert,
 * flags) — shown monospace and ellipsized, never parsed or reformatted.
 */
@Composable
private fun BridgeRow(bridge: String, actionsEnabled: Boolean, onRemove: () -> Unit) {
    var expanded by rememberSaveable(bridge) { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Helm.colors.surface2)
            .clickable { expanded = !expanded }
            .padding(start = 14.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = bridge,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = Helm.colors.text,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove, enabled = actionsEnabled) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = if (actionsEnabled) Tints.red.fg else Helm.colors.text2.copy(alpha = 0.38f),
            )
        }
    }
}

/**
 * Paste-friendly single-field sheet. Bridge lines are opaque strings from the
 * Tor Project's distributor, so there is nothing to parse out of the clipboard —
 * it goes in exactly as copied.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBridgeSheet(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val clipboard = LocalClipboardManager.current
    var input by rememberSaveable { mutableStateOf("") }

    HelmBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.tor_add_bridge),
                style = MaterialTheme.typography.titleLarge,
            )
            HelmTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(R.string.tor_bridge_line)) },
                placeholder = { Text(stringResource(R.string.tor_bridge_placeholder)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                maxLines = 4,
                trailingIcon = {
                    IconButton(onClick = {
                        val clip = clipboard.getText()?.text?.trim().orEmpty()
                        if (clip.isNotBlank()) input = clip
                    }) {
                        Icon(
                            Icons.Filled.ContentPaste,
                            contentDescription = stringResource(R.string.paste_from_clipboard),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.tor_bridge_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HelmButton(
                text = stringResource(R.string.add),
                onClick = { onAdd(input) },
                enabled = input.isNotBlank(),
                icon = Icons.Filled.Add,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
