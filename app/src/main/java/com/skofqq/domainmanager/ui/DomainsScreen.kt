package com.skofqq.domainmanager.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.DomainStatus
import com.skofqq.domainmanager.util.extractDomain

/** Display names for API target values ("mihomo" / "magitrickle"). */
private fun targetLabel(target: String) = if (target == "mihomo") "mihomo" else "MagiTrickle"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DomainsScreen(viewModel: DomainsViewModel) {
    val state by viewModel.state.collectAsState()
    var input by rememberSaveable { mutableStateOf("") }
    var dialogEntry by remember { mutableStateOf<DomainStatus?>(null) }
    var editEntry by remember { mutableStateOf<DomainStatus?>(null) }
    val focusManager = LocalFocusManager.current
    val clipboard = LocalClipboardManager.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.domains)) },
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
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
