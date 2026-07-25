package com.skofqq.domainmanager.ui

import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Dataset
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.CatalogEntry
import com.skofqq.domainmanager.data.PROVIDER_BEHAVIORS
import com.skofqq.domainmanager.data.PROVIDER_FORMATS
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.data.RuleProvider
import com.skofqq.domainmanager.data.guessSourceBehavior
import com.skofqq.domainmanager.data.guessSourceFormat
import com.skofqq.domainmanager.data.providerNameFor
import com.skofqq.domainmanager.data.suggestProviderName
import kotlin.coroutines.cancellation.CancellationException

/** Refresh periods offered by the picker, in seconds — a readable subset of the API's 60..604800. */
private val INTERVAL_OPTIONS = listOf(3600, 21600, 86400, 604800)

private const val STEP_SOURCE = 1
private const val STEP_PARAMS = 2
private const val STEP_GROUP = 3
private const val STEP_CONFIRM = 4
private const val STEP_COUNT = 4

/**
 * Rule-provider groups: the list of what this app added (action=provider_list)
 * plus the step-by-step add flow. Nested one level under the mihomo screen, with
 * its own predictive-back handler so the wizard pops back to the list and the
 * list pops back to mihomo.
 */
@Composable
fun RuleProvidersScreen(viewModel: RuleProvidersViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var addOpen by rememberSaveable { mutableStateOf(false) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    var backSwipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    // Removal outcome (including the "there was nothing to remove" case) is a
    // one-shot toast — the row is already gone from the list by then.
    state.removeMessage?.let { message ->
        val text = message.resolve()
        LaunchedEffect(message) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            viewModel.clearRemoveMessage()
        }
    }

    // Success closes the wizard and reports what actually landed on the router
    // (the router's own answer, not the values typed into the form).
    state.addedProvider?.let { added ->
        val text = stringResource(R.string.provider_added, added.provider, added.group)
        LaunchedEffect(added) {
            addOpen = false
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            viewModel.clearAddResult()
        }
    }

    PredictiveBackHandler(enabled = addOpen) { events ->
        try {
            events.collect {
                backProgress = it.progress
                backSwipeEdge = it.swipeEdge
            }
            addOpen = false
            viewModel.clearAddResult()
        } catch (e: CancellationException) {
            throw e
        } finally {
            backProgress = 0f
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (!addOpen || backProgress > 0f) {
            ProvidersListScreen(
                state = state,
                onBack = onBack,
                onAdd = {
                    viewModel.clearAddResult()
                    // Normally just a file read — the store only reaches GitHub
                    // when its cache is missing or a day old.
                    viewModel.loadCatalog()
                    addOpen = true
                },
                onRemove = { viewModel.remove(it) },
                onRetry = { viewModel.refresh() },
            )
        }
        if (addOpen) {
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
                AddProviderFlow(
                    proxyGroups = state.proxyGroups,
                    proxyGroupsError = state.proxyGroupsError,
                    takenNames = state.providers.orEmpty().map { it.provider },
                    catalog = state.catalog,
                    catalogLoading = state.catalogLoading,
                    catalogStale = state.catalogStale,
                    catalogError = state.catalogError,
                    catalogFetchedAt = state.catalogFetchedAt,
                    onRefreshCatalog = { viewModel.loadCatalog(force = true) },
                    adding = state.adding,
                    addError = state.addError,
                    onSubmit = { provider, url, behavior, format, interval, group, newGroup, fetchProxy ->
                        viewModel.add(
                            provider = provider,
                            url = url,
                            behavior = behavior,
                            format = format,
                            interval = interval,
                            group = group,
                            newGroup = newGroup,
                            fetchProxy = fetchProxy,
                        )
                    },
                    onClose = {
                        addOpen = false
                        viewModel.clearAddResult()
                    },
                )
            }
        }
    }
}

@Composable
private fun ProvidersListScreen(
    state: RuleProvidersUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onRetry: () -> Unit,
) {
    var confirmRemove by rememberSaveable { mutableStateOf<String?>(null) }
    // First provider_list (no cache yet) → skeletons; later refreshes keep the
    // existing rows on screen, same convention as every other list in the app.
    val skeletonBrush = if (state.providers == null && state.error == null) shimmerBrush() else null

    StatusChildScaffold(
        title = stringResource(R.string.rule_providers_title),
        onBack = onBack,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
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
                    stringResource(R.string.rule_providers_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.error?.let { message ->
                item {
                    Column {
                        ErrorCard(message)
                        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                    }
                }
            }
            if (skeletonBrush != null) {
                items(3) {
                    SkeletonBox(
                        skeletonBrush,
                        Modifier.fillMaxWidth().height(96.dp),
                        MaterialTheme.shapes.medium,
                    )
                }
            }
            val providers = state.providers
            if (providers != null && providers.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.rule_providers_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            items(providers.orEmpty(), key = { it.provider }) { provider ->
                ProviderCard(
                    provider = provider,
                    busy = state.removingProvider == provider.provider,
                    onRemove = { confirmRemove = provider.provider },
                )
            }
        }
    }

    confirmRemove?.let { name ->
        val provider = state.providers?.firstOrNull { it.provider == name }
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text(stringResource(R.string.provider_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.provider_delete_text,
                        name,
                        provider?.group.orEmpty(),
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = null
                    onRemove(name)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProviderCard(provider: RuleProvider, busy: Boolean, onRemove: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        provider.provider,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        stringResource(R.string.provider_routed_to, provider.group),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (busy) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            Text(
                provider.url,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                ProviderBadge(behaviorLabel(provider.behavior))
                ProviderBadge(provider.format.uppercase())
                ProviderBadge(intervalLabel(provider.interval))
            }
        }
    }
}

@Composable
private fun ProviderBadge(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.shapes.small,
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

// --- Add flow ---------------------------------------------------------------------

/**
 * Four-step add wizard. Every value is validated here before provider_add is
 * called, so the API's 400s stay theoretical — the ones that CAN'T be predicted
 * from the client (group deleted meanwhile, name taken on the router, a config
 * test failure) are surfaced verbatim on the last step instead of a generic
 * "something went wrong".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProviderFlow(
    proxyGroups: List<String>?,
    proxyGroupsError: UiMessage?,
    takenNames: List<String>,
    catalog: List<CatalogEntry>?,
    catalogLoading: Boolean,
    catalogStale: UiMessage?,
    catalogError: UiMessage?,
    catalogFetchedAt: Long,
    onRefreshCatalog: () -> Unit,
    adding: Boolean,
    addError: UiMessage?,
    onSubmit: (
        provider: String,
        url: String,
        behavior: String,
        format: String,
        interval: Int,
        group: String?,
        newGroup: String?,
        fetchProxy: String?,
    ) -> Unit,
    onClose: () -> Unit,
) {
    // Explicit saveable keys throughout: this wizard lives three conditional
    // branches deep (Status child → mihomo child → providers child), and
    // position-derived keys of same-typed values across sibling branches are
    // exactly the setup where a restore can land in the wrong slot.
    var step by rememberSaveable { mutableIntStateOf(STEP_SOURCE) }
    /** Step 1 sub-mode: the hand-entered URL form instead of the curated list. */
    var customMode by rememberSaveable { mutableStateOf(false) }

    var url by rememberSaveable { mutableStateOf("") }
    var behavior by rememberSaveable { mutableStateOf("domain") }
    var format by rememberSaveable { mutableStateOf("mrs") }
    var name by rememberSaveable { mutableStateOf("") }
    var interval by rememberSaveable {
        mutableIntStateOf(RouterApi.DEFAULT_PROVIDER_INTERVAL)
    }
    var fetchProxy by rememberSaveable { mutableStateOf<String?>(null) }
    var useNewGroup by rememberSaveable { mutableStateOf(false) }
    var group by rememberSaveable { mutableStateOf<String?>(null) }
    var newGroupName by rememberSaveable { mutableStateOf("") }
    /** Once the user picks a type/format by hand, typing more URL must not overwrite it. */
    var pickersTouched by rememberSaveable { mutableStateOf(false) }
    /** Step-1 catalog filter; purely client-side over the loaded snapshot. */
    var catalogQuery by rememberSaveable { mutableStateOf("") }

    // Step-internal back: the wizard walks itself backwards; only the very first
    // screen falls through to the host's predictive-back close.
    BackHandler(enabled = step > STEP_SOURCE || customMode) {
        if (step > STEP_SOURCE) step-- else customMode = false
    }

    val urlValid = url.isNotBlank() && url.startsWith("https://") && url.length > "https://".length
    val nameValid = RouterApi.isValidProviderName(name)
    val nameTaken = name in takenNames
    val newGroupTaken = proxyGroups.orEmpty().any { it.equals(newGroupName.trim(), ignoreCase = true) }
    val groupStepValid = if (useNewGroup) {
        newGroupName.isNotBlank() && !newGroupTaken
    } else {
        group != null
    }
    val canAdvance = when (step) {
        STEP_SOURCE -> customMode && urlValid
        STEP_PARAMS -> nameValid && !nameTaken
        STEP_GROUP -> groupStepValid
        else -> !adding
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.provider_add_title))
                        Text(
                            stringResource(R.string.provider_step, step, STEP_COUNT),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            step > STEP_SOURCE -> step--
                            customMode -> customMode = false
                            else -> onClose()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) }
                },
            )
        },
        bottomBar = {
            // Step 1's catalog advances by tapping a source, so it has no Next
            // button unless the manual form is open.
            if (step > STEP_SOURCE || customMode) {
                Column(Modifier.navigationBarsPadding()) {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (step == STEP_CONFIRM) {
                            Button(
                                onClick = {
                                    onSubmit(
                                        name.trim(),
                                        url.trim(),
                                        behavior,
                                        format,
                                        interval,
                                        if (useNewGroup) null else group,
                                        if (useNewGroup) newGroupName.trim() else null,
                                        fetchProxy,
                                    )
                                },
                                enabled = !adding,
                            ) {
                                if (adding) {
                                    CircularProgressIndicator(
                                        Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                    Spacer(Modifier.size(8.dp))
                                }
                                Text(stringResource(R.string.add))
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (step == STEP_SOURCE && name.isBlank()) {
                                        name = suggestProviderName(url)
                                    }
                                    step++
                                },
                                enabled = canAdvance,
                            ) {
                                Text(stringResource(R.string.next))
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        // Each step is a different form in the same list — without this a step
        // entered while scrolled down would open somewhere in its middle.
        val listState = rememberLazyListState()
        LaunchedEffect(step, customMode) { listState.scrollToItem(0) }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                LinearProgressIndicator(
                    progress = { step.toFloat() / STEP_COUNT },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            when (step) {
                STEP_SOURCE -> if (customMode) {
                    customSourceStep(
                        url = url,
                        onUrl = {
                            url = it
                            // Extension-based guesses, only as a starting point and
                            // only until the user overrides them by hand — a URL
                            // alone can't prove what is inside the file.
                            if (!pickersTouched) {
                                behavior = guessSourceBehavior(it)
                                format = guessSourceFormat(it)
                            }
                        },
                        urlValid = urlValid,
                        behavior = behavior,
                        onBehavior = {
                            behavior = it
                            pickersTouched = true
                        },
                        format = format,
                        onFormat = {
                            format = it
                            pickersTouched = true
                        },
                    )
                } else {
                    catalogStep(
                        catalog = catalog,
                        loading = catalogLoading,
                        stale = catalogStale,
                        error = catalogError,
                        fetchedAt = catalogFetchedAt,
                        query = catalogQuery,
                        onQuery = { catalogQuery = it },
                        onRefresh = onRefreshCatalog,
                        onCustom = { customMode = true },
                        onPick = { entry ->
                            // url comes straight from the catalog entry; behavior
                            // and format are its directory/extension defaults and
                            // stay editable on the next step.
                            url = entry.url
                            behavior = entry.behavior
                            format = entry.format
                            name = providerNameFor(entry)
                            pickersTouched = false
                            step = STEP_PARAMS
                        },
                    )
                }

                STEP_PARAMS -> paramsStep(
                    name = name,
                    onName = { name = it.trim() },
                    nameValid = nameValid,
                    nameTaken = nameTaken,
                    interval = interval,
                    onInterval = { interval = it },
                    proxyGroups = proxyGroups,
                    fetchProxy = fetchProxy,
                    onFetchProxy = { fetchProxy = it },
                )

                STEP_GROUP -> groupStep(
                    proxyGroups = proxyGroups,
                    proxyGroupsError = proxyGroupsError,
                    useNewGroup = useNewGroup,
                    onUseNewGroup = { useNewGroup = it },
                    group = group,
                    onGroup = { group = it },
                    newGroupName = newGroupName,
                    onNewGroupName = { newGroupName = it },
                    newGroupTaken = newGroupTaken,
                )

                else -> confirmStep(
                    name = name,
                    url = url,
                    behavior = behavior,
                    format = format,
                    interval = interval,
                    fetchProxy = fetchProxy,
                    useNewGroup = useNewGroup,
                    group = group,
                    newGroupName = newGroupName,
                    addError = addError,
                )
            }
        }
    }
}

/**
 * Step 1a: the live catalog, searchable. The list is the whole `geo` tree of the
 * rules repo (~10k files), so it is filtered client-side against the already
 * loaded snapshot — typing never costs a request.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.catalogStep(
    catalog: List<CatalogEntry>?,
    loading: Boolean,
    stale: UiMessage?,
    error: UiMessage?,
    fetchedAt: Long,
    query: String,
    onQuery: (String) -> Unit,
    onRefresh: () -> Unit,
    onCustom: () -> Unit,
    onPick: (CatalogEntry) -> Unit,
) {
    // Always the first row, filter or no filter: the catalog only covers this one
    // repo, and every other source in the world goes through here.
    item(key = "custom-url") {
        ElevatedCard(onClick = onCustom, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        stringResource(R.string.provider_custom_url),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.provider_custom_url_desc),
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

    item(key = "catalog-search") {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            label = { Text(stringResource(R.string.catalog_search)) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQuery("") }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.clear_search),
                            )
                        }
                    }
                    IconButton(onClick = onRefresh, enabled = !loading) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.catalog_refresh),
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    // A cache we couldn't refresh is still perfectly usable — say so and move on,
    // never block the flow on it.
    stale?.let { message ->
        item(key = "catalog-stale") {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        stringResource(R.string.catalog_not_updated),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        message.resolve(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (loading && catalog == null) {
        item(key = "catalog-loading") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(
                    stringResource(R.string.catalog_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }

    // Nothing cached and the fetch failed: the custom-URL row above is the way out.
    error?.let { message ->
        item(key = "catalog-error") {
            Column {
                ErrorCard(message)
                TextButton(onClick = onRefresh) { Text(stringResource(R.string.retry)) }
            }
        }
    }

    val entries = catalog
    if (entries != null) {
        val needle = query.trim().lowercase()
        val shown = if (needle.isEmpty()) entries
        else entries.filter { it.name.lowercase().contains(needle) }

        item(key = "catalog-count") {
            Text(
                stringResource(R.string.catalog_count, shown.size, entries.size) +
                    if (fetchedAt > 0L) " · " + relativeTime(fetchedAt) else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (shown.isEmpty()) {
            item(key = "catalog-empty") {
                Text(
                    stringResource(R.string.catalog_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }
        items(shown, key = { "${it.source}/${it.name}" }) { entry ->
            CatalogRow(entry = entry, onClick = { onPick(entry) })
        }
    }
}

@Composable
private fun CatalogRow(entry: CatalogEntry, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Dataset,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    entry.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${entry.source} · ${behaviorLabel(entry.behavior)} · ${entry.format.uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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

/** "2 hours ago" style, localized by the platform. */
@Composable
private fun relativeTime(millis: Long): String = DateUtils.getRelativeTimeSpanString(
    millis,
    System.currentTimeMillis(),
    DateUtils.MINUTE_IN_MILLIS,
).toString()

/** Step 1b: hand-entered source — any https URL in a mihomo-compatible format. */
private fun androidx.compose.foundation.lazy.LazyListScope.customSourceStep(
    url: String,
    onUrl: (String) -> Unit,
    urlValid: Boolean,
    behavior: String,
    onBehavior: (String) -> Unit,
    format: String,
    onFormat: (String) -> Unit,
) {
    item {
        OutlinedTextField(
            value = url,
            onValueChange = onUrl,
            label = { Text(stringResource(R.string.provider_url)) },
            placeholder = { Text("https://example.com/rules.mrs") },
            singleLine = true,
            isError = url.isNotBlank() && !urlValid,
            supportingText = {
                Text(
                    if (url.isNotBlank() && !urlValid) stringResource(R.string.provider_url_invalid)
                    else stringResource(R.string.provider_url_hint)
                )
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    item { SectionLabel(stringResource(R.string.provider_behavior)) }
    item {
        OptionSegmentedRow(
            options = PROVIDER_BEHAVIORS,
            selected = behavior,
            label = { behaviorLabel(it) },
            onSelect = onBehavior,
        )
    }
    item {
        Text(
            stringResource(R.string.provider_behavior_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    item { SectionLabel(stringResource(R.string.provider_format)) }
    item {
        OptionSegmentedRow(
            options = PROVIDER_FORMATS,
            selected = format,
            label = { it.uppercase() },
            onSelect = onFormat,
        )
    }
    item {
        Text(
            stringResource(R.string.provider_format_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Step 2: provider name, refresh period and the optional download proxy-group. */
@OptIn(ExperimentalLayoutApi::class)
private fun androidx.compose.foundation.lazy.LazyListScope.paramsStep(
    name: String,
    onName: (String) -> Unit,
    nameValid: Boolean,
    nameTaken: Boolean,
    interval: Int,
    onInterval: (Int) -> Unit,
    proxyGroups: List<String>?,
    fetchProxy: String?,
    onFetchProxy: (String?) -> Unit,
) {
    item {
        OutlinedTextField(
            value = name,
            onValueChange = onName,
            label = { Text(stringResource(R.string.provider_name)) },
            singleLine = true,
            isError = name.isNotEmpty() && (!nameValid || nameTaken),
            supportingText = {
                Text(
                    when {
                        name.isNotEmpty() && !nameValid -> stringResource(R.string.provider_name_invalid)
                        nameTaken -> stringResource(R.string.provider_name_taken)
                        else -> stringResource(R.string.provider_name_hint)
                    }
                )
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    item { SectionLabel(stringResource(R.string.provider_interval)) }
    item {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            INTERVAL_OPTIONS.forEach { seconds ->
                FilterChip(
                    selected = interval == seconds,
                    onClick = { onInterval(seconds) },
                    label = { Text(intervalLabel(seconds)) },
                    leadingIcon = if (interval == seconds) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                )
            }
        }
    }
    item { SectionLabel(stringResource(R.string.provider_fetch_proxy)) }
    item {
        Text(
            stringResource(R.string.provider_fetch_proxy_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    item {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = fetchProxy == null,
                onClick = { onFetchProxy(null) },
                label = { Text(stringResource(R.string.provider_fetch_proxy_none)) },
            )
            proxyGroups.orEmpty().forEach { groupName ->
                FilterChip(
                    selected = fetchProxy == groupName,
                    onClick = { onFetchProxy(groupName) },
                    label = { Text(groupName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
    }
}

/** Step 3: where matched traffic goes — an existing proxy-group or a brand-new one. */
private fun androidx.compose.foundation.lazy.LazyListScope.groupStep(
    proxyGroups: List<String>?,
    proxyGroupsError: UiMessage?,
    useNewGroup: Boolean,
    onUseNewGroup: (Boolean) -> Unit,
    group: String?,
    onGroup: (String) -> Unit,
    newGroupName: String,
    onNewGroupName: (String) -> Unit,
    newGroupTaken: Boolean,
) {
    item {
        Text(
            stringResource(R.string.provider_group_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    item {
        // Exactly one of group / new_group may be sent — a two-way switch, never
        // both at once, mirroring the API's own either/or contract.
        OptionSegmentedRow(
            options = listOf("existing", "new"),
            selected = if (useNewGroup) "new" else "existing",
            label = {
                if (it == "new") stringResource(R.string.provider_group_new)
                else stringResource(R.string.provider_group_existing)
            },
            onSelect = { onUseNewGroup(it == "new") },
        )
    }
    if (useNewGroup) {
        item {
            OutlinedTextField(
                value = newGroupName,
                onValueChange = onNewGroupName,
                label = { Text(stringResource(R.string.provider_new_group_name)) },
                singleLine = true,
                isError = newGroupTaken,
                supportingText = {
                    Text(
                        if (newGroupTaken) stringResource(R.string.provider_new_group_taken)
                        else stringResource(R.string.provider_new_group_hint)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        proxyGroupsError?.let { item { ErrorCard(it) } }
        val groups = proxyGroups
        if (groups != null && groups.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.provider_no_groups),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(groups.orEmpty(), key = { "grp-$it" }) { candidate ->
            ElevatedCard(onClick = { onGroup(candidate) }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = group == candidate, onClick = { onGroup(candidate) })
                    Text(
                        candidate,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}

/** Step 4: full read-back of what will be sent, plus the router's verbatim refusal if it refuses. */
private fun androidx.compose.foundation.lazy.LazyListScope.confirmStep(
    name: String,
    url: String,
    behavior: String,
    format: String,
    interval: Int,
    fetchProxy: String?,
    useNewGroup: Boolean,
    group: String?,
    newGroupName: String,
    addError: UiMessage?,
) {
    item {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SummaryRow(stringResource(R.string.provider_name), name)
                SummaryRow(stringResource(R.string.provider_url), url, mono = true)
                SummaryRow(stringResource(R.string.provider_behavior), behaviorLabel(behavior))
                SummaryRow(stringResource(R.string.provider_format), format.uppercase())
                SummaryRow(stringResource(R.string.provider_interval), intervalLabel(interval))
                SummaryRow(
                    stringResource(R.string.provider_fetch_proxy),
                    fetchProxy ?: stringResource(R.string.provider_fetch_proxy_none),
                )
                HorizontalDivider()
                SummaryRow(
                    if (useNewGroup) stringResource(R.string.provider_group_new)
                    else stringResource(R.string.provider_group_existing),
                    if (useNewGroup) newGroupName else group.orEmpty(),
                    highlight = true,
                )
            }
        }
    }
    if (useNewGroup) {
        item {
            Text(
                stringResource(R.string.provider_new_group_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    item {
        Text(
            stringResource(R.string.provider_add_slow_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    addError?.let { item { ErrorCard(it) } }
}

@Composable
private fun SummaryRow(label: String, value: String, mono: Boolean = false, highlight: Boolean = false) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (mono) FontFamily.Monospace else null,
            fontWeight = if (highlight) FontWeight.SemiBold else null,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionSegmentedRow(
    options: List<String>,
    selected: String,
    label: @Composable (String) -> String,
    onSelect: (String) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = selected == option,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(label(option), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// --- Shared labels ----------------------------------------------------------------

/** Unknown behaviors are shown raw rather than hidden — the API may gain more. */
@Composable
internal fun behaviorLabel(behavior: String): String = when (behavior) {
    "domain" -> stringResource(R.string.behavior_domain)
    "ipcidr" -> stringResource(R.string.behavior_ipcidr)
    "classical" -> stringResource(R.string.behavior_classical)
    else -> behavior
}

/** Readable refresh period; anything off the preset list falls back to plain hours. */
@Composable
internal fun intervalLabel(seconds: Int): String = when (seconds) {
    3600 -> stringResource(R.string.interval_hour)
    21600 -> stringResource(R.string.interval_6h)
    86400 -> stringResource(R.string.interval_day)
    604800 -> stringResource(R.string.interval_week)
    else -> {
        val hours = (seconds / 3600).coerceAtLeast(1)
        pluralStringResource(R.plurals.n_hours, hours, hours)
    }
}

/**
 * Entry point shown on the mihomo screen. [count] is null until the providers
 * list has been loaded at least once — the card then keeps its generic subtitle
 * rather than claiming a count it doesn't have.
 */
@Composable
internal fun RuleProvidersEntryCard(count: Int?, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Dataset,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    stringResource(R.string.rule_providers_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (count != null) pluralStringResource(R.plurals.n_rule_providers, count, count)
                    else stringResource(R.string.rule_providers_entry_desc),
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
