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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.MihomoSubscription
import com.skofqq.domainmanager.data.RouterApi

/**
 * mihomo subscriptions: the proxy-provider lists whose nodes make up the shared
 * pool every proxy-group selects from. Nested one level under the mihomo screen.
 * The base subscription baked into the router's config is listed but can be
 * neither edited nor removed — the router rejects it too, but the UI gates it on
 * [MihomoSubscription.removable] rather than relying on that error.
 */
@Composable
fun SubscriptionsScreen(viewModel: SubscriptionsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var addOpen by rememberSaveable { mutableStateOf(false) }
    /** Non-null while the "change URL" sheet is open, holding the edited name. */
    var editName by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmRemove by rememberSaveable { mutableStateOf<String?>(null) }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    // Removal outcome (including "there was nothing to remove") is a one-shot
    // toast — the row is already gone from the list by then.
    state.removeMessage?.let { message ->
        val text = message.resolve()
        LaunchedEffect(message) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            viewModel.clearRemoveMessage()
        }
    }

    // Success closes whichever sheet was open and reports what actually landed on
    // the router (its own answer, not the values typed into the form).
    state.savedSubscription?.let { saved ->
        val text = stringResource(R.string.subscription_saved, saved.name)
        LaunchedEffect(saved) {
            addOpen = false
            editName = null
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            viewModel.clearSaveResult()
        }
    }

    val skeletonBrush = if (state.subscriptions == null && state.error == null) shimmerBrush() else null

    StatusChildScaffold(
        title = stringResource(R.string.subscriptions_title),
        onBack = onBack,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.clearSaveResult()
                    addOpen = true
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add)) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.subscriptions_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
                items(2) {
                    SkeletonBox(
                        skeletonBrush,
                        Modifier.fillMaxWidth().height(96.dp),
                        MaterialTheme.shapes.medium,
                    )
                }
            }
            items(state.subscriptions.orEmpty(), key = { it.name }) { subscription ->
                SubscriptionCard(
                    subscription = subscription,
                    busy = state.removingSubscription == subscription.name,
                    actionsEnabled = state.removingSubscription == null && !state.saving,
                    onEdit = {
                        viewModel.clearSaveResult()
                        editName = subscription.name
                    },
                    onRemove = { confirmRemove = subscription.name },
                )
            }
        }
    }

    if (addOpen) {
        SubscriptionSheet(
            existingName = null,
            initialUrl = "",
            takenNames = state.subscriptions.orEmpty().map { it.name },
            saving = state.saving,
            error = state.saveError,
            onSubmit = { name, url -> viewModel.add(name, url) },
            onDismiss = {
                addOpen = false
                viewModel.clearSaveResult()
            },
        )
    }

    editName?.let { name ->
        val current = state.subscriptions?.firstOrNull { it.name == name }
        SubscriptionSheet(
            existingName = name,
            initialUrl = current?.url.orEmpty(),
            takenNames = emptyList(),
            saving = state.saving,
            error = state.saveError,
            onSubmit = { _, url -> viewModel.update(name, url) },
            onDismiss = {
                editName = null
                viewModel.clearSaveResult()
            },
        )
    }

    confirmRemove?.let { name ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text(stringResource(R.string.subscription_delete_title)) },
            text = { Text(stringResource(R.string.subscription_delete_text, name)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = null
                    viewModel.remove(name)
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

@Composable
private fun SubscriptionCard(
    subscription: MihomoSubscription,
    busy: Boolean,
    actionsEnabled: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    subscription.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (busy) {
                    CircularProgressIndicator(
                        Modifier.padding(end = 12.dp).size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else if (subscription.removable) {
                    IconButton(onClick = onEdit, enabled = actionsEnabled) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.subscription_edit),
                        )
                    }
                    IconButton(onClick = onRemove, enabled = actionsEnabled) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            Text(
                subscription.url,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp, end = 12.dp),
            )
            // The base subscription is part of the router's own config — say why
            // it has no actions instead of just showing an inert row.
            if (!subscription.removable) {
                Text(
                    stringResource(R.string.subscription_base_protected),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 6.dp, end = 12.dp),
                )
            }
        }
    }
}

/**
 * Single-step add/edit sheet: name + URL is the whole model, so there is no
 * wizard here. [existingName] non-null switches it to edit mode — the name is
 * immutable on the router, only the URL can change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionSheet(
    existingName: String?,
    initialUrl: String,
    takenNames: List<String>,
    saving: Boolean,
    error: UiMessage?,
    onSubmit: (name: String, url: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val clipboard = LocalClipboardManager.current
    var name by rememberSaveable(existingName) { mutableStateOf(existingName.orEmpty()) }
    var url by rememberSaveable(existingName) { mutableStateOf(initialUrl) }

    val editing = existingName != null
    val nameValid = editing || RouterApi.isValidProviderName(name)
    val nameTaken = !editing && name in takenNames
    val urlValid = RouterApi.isValidSourceUrl(url)

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
                text = stringResource(
                    if (editing) R.string.subscription_edit_title else R.string.subscription_add_title
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.subscription_name)) },
                placeholder = { Text(stringResource(R.string.subscription_name_placeholder)) },
                singleLine = true,
                // Name is fixed on the router — renaming is remove + add.
                enabled = !editing,
                isError = name.isNotEmpty() && (!nameValid || nameTaken),
                supportingText = {
                    Text(
                        when {
                            editing -> stringResource(R.string.subscription_name_fixed)
                            name.isNotEmpty() && !nameValid ->
                                stringResource(R.string.provider_name_invalid)
                            nameTaken -> stringResource(R.string.err_subscription_exists)
                            else -> stringResource(R.string.provider_name_hint)
                        }
                    )
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(R.string.subscription_url)) },
                placeholder = { Text("https://example.com/sub") },
                singleLine = true,
                isError = url.isNotEmpty() && !urlValid,
                supportingText = {
                    Text(
                        if (url.isNotEmpty() && !urlValid) stringResource(R.string.provider_url_invalid)
                        else stringResource(R.string.provider_url_hint)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                trailingIcon = {
                    IconButton(onClick = {
                        val clip = clipboard.getText()?.text?.trim().orEmpty()
                        if (clip.isNotBlank()) url = clip
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
                stringResource(R.string.subscription_slow_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            error?.let { ErrorCard(it) }
            FilledTonalButton(
                onClick = { onSubmit(name.trim(), url.trim()) },
                enabled = !saving && nameValid && !nameTaken && name.isNotBlank() && urlValid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(if (editing) R.string.apply else R.string.add))
            }
        }
    }
}

/**
 * Entry point shown on the mihomo screen. [count] is null until the list has been
 * loaded at least once — the card then keeps its generic subtitle rather than
 * claiming a count it doesn't have.
 */
@Composable
internal fun SubscriptionsEntryCard(count: Int?, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.CloudSync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    stringResource(R.string.subscriptions_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (count != null) pluralStringResource(R.plurals.n_subscriptions, count, count)
                    else stringResource(R.string.subscriptions_entry_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
