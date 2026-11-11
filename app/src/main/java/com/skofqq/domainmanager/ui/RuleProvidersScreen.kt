package com.skofqq.domainmanager.ui

import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Link
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import com.skofqq.domainmanager.ui.theme.Tints
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material.icons.outlined.Delete
import com.skofqq.domainmanager.ui.theme.Helm
import com.skofqq.domainmanager.ui.components.*
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
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { NoticeCard(title = null, body = stringResource(R.string.rule_providers_hint)) }
            state.error?.let { message ->
                item {
                    ErrorCard(message, onRetry = onRetry)
                }
            }
            if (skeletonBrush != null) {
                item { SkeletonBox(skeletonBrush, Modifier.fillMaxWidth().height(240.dp), CardShape) }
            }
            val providers = state.providers
            if (providers != null && providers.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Dataset,
                        title = stringResource(R.string.rule_providers_empty),
                        tint = Tints.teal,
                        action = {
                            HelmButton(
                                text = stringResource(R.string.add),
                                onClick = onAdd,
                                icon = Icons.Filled.Add,
                            )
                        },
                    )
                }
            }
            if (!providers.isNullOrEmpty()) {
                item {
                    HelmSection(
                        title = pluralStringResource(R.plurals.n_rule_providers, providers.size, providers.size),
                        icon = Icons.Outlined.Dataset,
                        tint = Tints.teal,
                        shape = MaterialShapes.Cookie6Sided,
                        trailing = {
                            SectionIconButton(Icons.Filled.Add, stringResource(R.string.add), tint = Tints.teal, onClick = onAdd)
                        },
                    ) {
                        Column {
                            providers.forEachIndexed { index, provider ->
                                if (index > 0) SectionDivider()
                                ProviderCard(
                                    provider = provider,
                                    busy = state.removingProvider == provider.provider,
                                    onRemove = { confirmRemove = provider.provider },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    confirmRemove?.let { name ->
        val provider = state.providers?.firstOrNull { it.provider == name }
        GlassAlertDialog(
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProviderCard(provider: RuleProvider, busy: Boolean, onRemove: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    provider.provider,
                    style = MaterialTheme.typography.titleMedium,
                    color = Helm.colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.provider_routed_to, provider.group),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Helm.colors.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (busy) {
                LoadingIndicator(Modifier.size(40.dp))
            } else {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = Tints.red.fg,
                    )
                }
            }
        }
        Text(
            provider.url,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = Helm.colors.text2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            ValuePill(behaviorLabel(provider.behavior), Tints.purple)
            ValuePill(provider.format.uppercase(), Tints.blue)
            ValuePill(intervalLabel(provider.interval), Tints.gray)
        }
    }
}

@Composable
private fun ProviderBadge(text: String) {
    ValuePill(text, Tints.gray)
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
        containerColor = Helm.colors.page,
        topBar = {
            SubPageHeader(
                title = stringResource(R.string.provider_add_title),
                subtitle = stringResource(R.string.provider_step, step, STEP_COUNT),
                onBack = {
                    when {
                        step > STEP_SOURCE -> step--
                        customMode -> customMode = false
                        else -> onClose()
                    }
                },
            ) {
                TextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) }
            }
        },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Helm.colors.page)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StepIndicator(current = step, count = STEP_COUNT)
                // Step 1's catalog advances by tapping a source, so it has no Next
                // button unless the manual form is open.
                if (step > STEP_SOURCE || customMode) {
                    if (step == STEP_CONFIRM) {
                        HelmButton(
                            text = stringResource(R.string.add),
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
                            loading = adding,
                            icon = Icons.Filled.Check,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        HelmButton(
                            text = stringResource(R.string.next),
                            onClick = {
                                if (step == STEP_SOURCE && name.isBlank()) {
                                    name = suggestProviderName(url)
                                }
                                step++
                            },
                            enabled = canAdvance,
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            modifier = Modifier.fillMaxWidth(),
                        )
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
        EntryCard(
            icon = Icons.Outlined.Edit,
            title = stringResource(R.string.provider_custom_url),
            subtitle = stringResource(R.string.provider_custom_url_desc),
            onClick = onCustom,
            tint = Tints.accent,
            shape = MaterialShapes.Clover4Leaf,
        )
    }

    item(key = "catalog-search") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PageSearchField(
                value = query,
                onChange = onQuery,
                hint = stringResource(R.string.catalog_search),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            SectionIconButton(
                Icons.Filled.Refresh,
                stringResource(R.string.catalog_refresh),
                enabled = !loading,
                onClick = onRefresh,
            )
        }
    }

    // A cache we could not refresh is still perfectly usable: say so and move on,
    // never block the flow on it.
    stale?.let { message ->
        item(key = "catalog-stale") {
            NoticeCard(
                title = stringResource(R.string.catalog_not_updated),
                body = message.resolve(),
                tint = Tints.amber,
            )
        }
    }

    if (loading && catalog == null) {
        item(key = "catalog-loading") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LoadingIndicator(Modifier.size(64.dp))
                Text(
                    stringResource(R.string.catalog_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Helm.colors.text2,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    // Nothing cached and the fetch failed: the custom-URL row above is the way out.
    error?.let { message ->
        item(key = "catalog-error") {
            ErrorCard(message, onRetry = onRefresh)
        }
    }

    val entries = catalog
    if (entries != null) {
        val needle = query.trim().lowercase()
        val shown = if (needle.isEmpty()) entries
        else entries.filter { it.name.lowercase().contains(needle) }

        item(key = "catalog-count") {
            SectionNote(
                stringResource(R.string.catalog_count, shown.size, entries.size) +
                    if (fetchedAt > 0L) " · " + relativeTime(fetchedAt) else "",
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        if (shown.isEmpty()) {
            item(key = "catalog-empty") {
                EmptyState(
                    icon = Icons.Filled.Search,
                    title = stringResource(R.string.catalog_no_results),
                    tint = Tints.gray,
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
    EntryCard(
        icon = Icons.Outlined.Dataset,
        title = entry.name,
        subtitle = "${entry.source} · ${behaviorLabel(entry.behavior)} · ${entry.format.uppercase()}",
        onClick = onClick,
        tint = when (entry.behavior) {
            "domain" -> Tints.blue
            "ipcidr" -> Tints.purple
            else -> Tints.teal
        },
        shape = when (entry.behavior) {
            "domain" -> MaterialShapes.Cookie9Sided
            "ipcidr" -> MaterialShapes.Cookie6Sided
            else -> MaterialShapes.Clover4Leaf
        },
    )
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
        HelmSection(
            title = stringResource(R.string.provider_url),
            icon = Icons.Outlined.Link,
            tint = Tints.blue,
            shape = MaterialShapes.Cookie9Sided,
        ) {
            HelmTextField(
                value = url,
                onValueChange = onUrl,
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
    }
    item {
        HelmSection(
            title = stringResource(R.string.provider_behavior),
            subtitle = stringResource(R.string.provider_behavior_hint),
            icon = Icons.Outlined.Category,
            tint = Tints.purple,
            shape = MaterialShapes.Clover4Leaf,
        ) {
            OptionSegmentedRow(
                options = PROVIDER_BEHAVIORS,
                selected = behavior,
                label = { behaviorLabel(it) },
                onSelect = onBehavior,
            )
        }
    }
    item {
        HelmSection(
            title = stringResource(R.string.provider_format),
            subtitle = stringResource(R.string.provider_format_hint),
            icon = Icons.Outlined.DataObject,
            tint = Tints.teal,
            shape = MaterialShapes.Cookie6Sided,
        ) {
            OptionSegmentedRow(
                options = PROVIDER_FORMATS,
                selected = format,
                label = { it.uppercase() },
                onSelect = onFormat,
            )
        }
    }
}

/** Step 2: provider name, refresh period and the optional download proxy-group. */
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
        HelmSection(
            title = stringResource(R.string.provider_name),
            icon = Icons.AutoMirrored.Outlined.Label,
            tint = Tints.blue,
            shape = MaterialShapes.Cookie9Sided,
        ) {
            HelmTextField(
                value = name,
                onValueChange = onName,
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
    }
    item {
        HelmSection(
            title = stringResource(R.string.provider_interval),
            icon = Icons.Outlined.Schedule,
            tint = Tints.amber,
            shape = MaterialShapes.Sunny,
        ) {
            ChipFlow {
                INTERVAL_OPTIONS.forEach { seconds ->
                    HelmChip(
                        text = intervalLabel(seconds),
                        selected = interval == seconds,
                        onClick = { onInterval(seconds) },
                    )
                }
            }
        }
    }
    item {
        HelmSection(
            title = stringResource(R.string.provider_fetch_proxy),
            subtitle = stringResource(R.string.provider_fetch_proxy_hint),
            icon = Icons.Outlined.CloudDownload,
            tint = Tints.teal,
            shape = MaterialShapes.Cookie12Sided,
        ) {
            ChipFlow {
                HelmChip(
                    text = stringResource(R.string.provider_fetch_proxy_none),
                    selected = fetchProxy == null,
                    onClick = { onFetchProxy(null) },
                )
                proxyGroups.orEmpty().forEach { groupName ->
                    HelmChip(
                        text = groupName,
                        selected = fetchProxy == groupName,
                        onClick = { onFetchProxy(groupName) },
                    )
                }
            }
        }
    }
}

/** Step 3: where matched traffic goes, an existing proxy-group or a brand-new one. */
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
        HelmSection(
            title = stringResource(R.string.provider_group_title),
            subtitle = stringResource(R.string.provider_group_hint),
            icon = Icons.Outlined.AltRoute,
            tint = Tints.purple,
            shape = MaterialShapes.Flower,
        ) {
            // Exactly one of group / new_group may be sent: a two-way switch, never
            // both at once, mirroring the either/or contract of the API.
            OptionSegmentedRow(
                options = listOf("existing", "new"),
                selected = if (useNewGroup) "new" else "existing",
                label = {
                    if (it == "new") stringResource(R.string.provider_group_new)
                    else stringResource(R.string.provider_group_existing)
                },
                onSelect = { onUseNewGroup(it == "new") },
            )
            if (useNewGroup) {
                HelmTextField(
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
            } else {
                proxyGroupsError?.let { ErrorCard(it) }
                val groups = proxyGroups
                if (groups != null && groups.isEmpty()) {
                    SectionNote(stringResource(R.string.provider_no_groups))
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    groups.orEmpty().forEach { candidate ->
                        OptionRow(
                            title = candidate,
                            subtitle = null,
                            selected = group == candidate,
                            container = Helm.colors.surface2,
                            onClick = { onGroup(candidate) },
                        )
                    }
                }
            }
        }
    }
}

/** Step 4: full read-back of what will be sent, plus the verbatim refusal of the router if it refuses. */
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
        HelmSection(
            title = name,
            subtitle = url,
            icon = Icons.Outlined.Dataset,
            tint = Tints.teal,
            shape = MaterialShapes.Cookie12Sided,
        ) {
            // Where matched traffic goes: the one answer worth seeing first.
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Tints.accent.bg).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Helm.colors.accent)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (useNewGroup) stringResource(R.string.provider_group_new)
                        else stringResource(R.string.provider_group_existing),
                        style = MaterialTheme.typography.bodySmall,
                        color = Helm.colors.text2,
                    )
                    Text(
                        if (useNewGroup) newGroupName else group.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Helm.colors.accent,
                    )
                }
            }
            Column {
                SummaryRow(stringResource(R.string.provider_behavior), behaviorLabel(behavior))
                SummaryRow(stringResource(R.string.provider_format), format.uppercase())
                SummaryRow(stringResource(R.string.provider_interval), intervalLabel(interval))
                SummaryRow(
                    stringResource(R.string.provider_fetch_proxy),
                    fetchProxy ?: stringResource(R.string.provider_fetch_proxy_none),
                )
            }
            if (useNewGroup) SectionNote(stringResource(R.string.provider_new_group_note))
            SectionNote(stringResource(R.string.provider_add_slow_note))
            addError?.let { ErrorCard(it) }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, mono: Boolean = false, highlight: Boolean = false) {
    KeyValueRow(
        label = label,
        value = value,
        monospace = mono,
        valueColor = if (highlight) Helm.colors.accent else Helm.colors.text,
    )
}

/** Small closed set of options (behavior, format, existing/new): the sliding pill segments. */
@Composable
private fun OptionSegmentedRow(
    options: List<String>,
    selected: String,
    label: @Composable (String) -> String,
    onSelect: (String) -> Unit,
) {
    PillTabs(
        labels = options.map { label(it) },
        selected = options.indexOf(selected).coerceAtLeast(0).toFloat(),
        onSelect = { onSelect(options[it]) },
        container = Helm.colors.surface2,
    )
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
    EntryCard(
        icon = Icons.Outlined.Dataset,
        title = stringResource(R.string.rule_providers_title),
        subtitle = if (count != null) pluralStringResource(R.plurals.n_rule_providers, count, count)
        else stringResource(R.string.rule_providers_entry_desc),
        onClick = onClick,
        tint = Tints.teal,
        shape = MaterialShapes.Cookie6Sided,
    )
}
