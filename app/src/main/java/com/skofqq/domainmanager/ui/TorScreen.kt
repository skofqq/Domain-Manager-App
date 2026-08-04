package com.skofqq.domainmanager.ui

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
import androidx.compose.ui.graphics.Color
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

/** Same green the service cards use for a running daemon. */
private val TorOkGreen = Color(0xFF4CAF50)

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
        floatingActionButton = {
            // Bridges are the only thing on this screen you add — no FAB before
            // the first list arrives, and none at all without Tor installed.
            if (installed && state.bridges != null) {
                ExtendedFloatingActionButton(
                    onClick = { addOpen = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.tor_add_bridge)) },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.error?.let { message ->
                item {
                    Column {
                        ErrorCard(message)
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
            if (skeletonBrush != null) {
                item {
                    SkeletonBox(
                        skeletonBrush,
                        Modifier.fillMaxWidth().height(160.dp),
                        MaterialTheme.shapes.medium,
                    )
                }
            }
            // installed=false is the whole answer from a router without Tor —
            // nothing below it would have any data to show.
            if (!installed) {
                item { TorNotInstalledCard() }
                return@LazyColumn
            }
            state.status?.let { status ->
                item { TorStatusCard(status) }
                item {
                    TorServiceButtons(
                        running = status.running,
                        enabled = !state.busyService && !state.savingBridges,
                        busy = state.busyService,
                        onStart = { viewModel.start() },
                        onStop = { viewModel.stop() },
                        onRestart = { viewModel.restart() },
                    )
                }

                // --- New identity ---------------------------------------------
                item { SectionLabel(stringResource(R.string.tor_new_identity)) }
                item {
                    Text(
                        stringResource(R.string.tor_new_identity_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { viewModel.newIdentity() },
                        enabled = !state.newnymRunning && !state.busyService,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.newnymRunning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.tor_new_identity))
                    }
                }

                // --- Connectivity test ----------------------------------------
                item { SectionLabel(stringResource(R.string.tor_test_title)) }
                item {
                    Text(
                        stringResource(R.string.tor_test_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    FilledTonalButton(
                        onClick = { viewModel.testConnection() },
                        enabled = !state.testRunning,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.testRunning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            stringResource(
                                if (state.testRunning) R.string.tor_testing
                                else R.string.tor_test_connection
                            )
                        )
                    }
                }
                state.testError?.let { item { ErrorCard(it) } }
                state.testResult?.let { result -> item { TorTestCard(result) } }

                // --- Bridges ---------------------------------------------------
                item { SectionLabel(stringResource(R.string.tor_bridges_title)) }
                item {
                    Text(
                        stringResource(R.string.tor_bridges_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.bridgesError?.let { item { ErrorCard(it) } }
                // One combined busy state: tor_bridges_set only rewrites torrc,
                // the restart right after it is what makes the change real.
                if (state.savingBridges) {
                    item {
                        Column {
                            Text(
                                stringResource(R.string.tor_bridges_saving),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                val bridges = state.bridges
                if (bridges != null && bridges.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.tor_no_bridges),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                }
                items(bridges.orEmpty(), key = { it }) { bridge ->
                    BridgeRow(
                        bridge = bridge,
                        actionsEnabled = !state.savingBridges,
                        onRemove = { confirmRemove = bridge },
                    )
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
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text(stringResource(R.string.tor_bridge_delete_title)) },
            text = { Text(stringResource(R.string.tor_bridge_delete_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = null
                    viewModel.removeBridge(bridge)
                }) {
                    Text(stringResource(R.string.delete))
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

@Composable
private fun TorNotInstalledCard() {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                stringResource(R.string.tor_not_installed),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.tor_not_installed_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TorStatusCard(status: TorStatus) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // running (live procd state) and enabled (autostart on next boot) are
            // independent flags — same treatment as the service cards.
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RunningIndicator(running = status.running, enabled = status.enabled)
                AutostartIndicator(status.enabled)
            }
            // Only meaningful while tor is up and still climbing to 100 — a
            // finished bootstrap needs no bar.
            if (status.running && status.bootstrap < 100) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TorRow(
                        label = stringResource(R.string.tor_bootstrap),
                        value = stringResource(R.string.tor_bootstrap_progress, status.bootstrap),
                    )
                    LinearProgressIndicator(
                        progress = { status.bootstrap / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            TorRow(
                label = stringResource(R.string.tor_bridges_label),
                value = pluralStringResource(R.plurals.n_tor_bridges, status.bridges, status.bridges),
            )
            // The tor PROCESS's uptime — it resets on every restart, it is not a
            // "time since the last new identity" counter.
            if (status.running) {
                TorRow(
                    label = stringResource(R.string.tor_uptime),
                    value = formatUptime(status.uptimeSeconds),
                )
            }
            if (status.socksPort > 0) {
                TorRow(
                    label = stringResource(R.string.tor_socks_port),
                    value = if (status.lanIp.isEmpty()) status.socksPort.toString()
                    else "${status.lanIp}:${status.socksPort}",
                    monospace = true,
                )
            }
            if (status.pacUrl.isNotEmpty()) {
                TorRow(
                    label = stringResource(R.string.tor_pac_url),
                    value = status.pacUrl,
                    monospace = true,
                )
            }
        }
    }
}

@Composable
private fun TorRow(label: String, value: String, monospace: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

/** Same start/stop/restart row the service cards use, driving the generic svc_* actions. */
@Composable
private fun TorServiceButtons(
    running: Boolean,
    enabled: Boolean,
    busy: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!running) {
            FilledTonalButton(
                onClick = onStart,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.svc_start)) }
        } else {
            OutlinedButton(
                onClick = onStop,
                enabled = enabled,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.svc_stop)) }
            FilledTonalButton(
                onClick = onRestart,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.svc_restart)) }
        }
        // svc_* can block up to ~5 s on the router while procd settles.
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}

/**
 * Result of one tor_test. ok=false is a legitimate "not connected" verdict
 * (not bootstrapped, bridges unreachable) — a failure state on the card, not an
 * error toast.
 */
@Composable
private fun TorTestCard(result: TorTestInfo) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                if (result.ok) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                tint = if (result.ok) TorOkGreen else MaterialTheme.colorScheme.error,
            )
            Column {
                Text(
                    text = if (result.ok) stringResource(R.string.tor_test_ok)
                    else stringResource(R.string.tor_test_failed),
                    style = MaterialTheme.typography.titleSmall,
                )
                if (result.ok && result.ip.isNotEmpty()) {
                    Text(
                        text = result.ip,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // "?" = the exit node was reached but the geo lookup failed.
                    val country = countryLabel(result.country)
                    if (country != null) {
                        Text(
                            text = country,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (!result.ok) {
                    Text(
                        text = stringResource(R.string.tor_test_failed_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
    ElevatedCard(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = bridge,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRemove, enabled = actionsEnabled) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = if (actionsEnabled) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
            }
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

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
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
            OutlinedTextField(
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
            FilledTonalButton(
                onClick = { onAdd(input) },
                enabled = input.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.add)) }
        }
    }
}
