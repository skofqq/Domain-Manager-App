package com.skofqq.domainmanager.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.PredictiveBackHandler
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.core.os.LocaleListCompat
import coil.compose.AsyncImage
import com.skofqq.domainmanager.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val GITHUB_USERNAME = "skofqq"
private const val GITHUB_PROFILE_URL = "https://github.com/$GITHUB_USERNAME"
private const val GITHUB_REPO_URL = "https://github.com/$GITHUB_USERNAME/Domain-Manager-App"
private const val GITHUB_AVATAR_URL = "https://github.com/$GITHUB_USERNAME.png"

private val TARGET_OPTIONS = listOf("both", "mihomo", "magitrickle")

/** Language tag "" means "follow the system". Labels stay in their own language on purpose. */
private data class LanguageOption(val tag: String, val label: String?)

private val LANGUAGE_OPTIONS = listOf(
    LanguageOption("", null), // label resolved from R.string.language_system
    LanguageOption("en", "English"),
    LanguageOption("ru", "Русский"),
)

private data class ThemeOption(val mode: String, @StringRes val labelRes: Int)

private val THEME_OPTIONS = listOf(
    ThemeOption("system", R.string.theme_system),
    ThemeOption("light", R.string.theme_light),
    ThemeOption("dark", R.string.theme_dark),
)

/**
 * Two-level settings: a root list of groups, each opening its own child screen.
 * Child state survives recreation (language switch recreates the activity).
 */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    var subScreen by rememberSaveable { mutableStateOf<String?>(null) }

    // Pops the child screen first; the root's back-to-Domains handler in
    // MainNavigation only fires once this one is disabled. Predictive-back aware:
    // commits on gesture completion, no-ops when the swipe is cancelled.
    PredictiveBackHandler(enabled = subScreen != null) { events ->
        try {
            events.collect { }
            subScreen = null
        } catch (e: CancellationException) {
            throw e
        }
    }

    when (subScreen) {
        "appearance" -> AppearanceSettingsScreen(viewModel, onBack = { subScreen = null })
        "language" -> LanguageSettingsScreen(onBack = { subScreen = null })
        "auth" -> AuthSettingsScreen(viewModel, onBack = { subScreen = null })
        "about" -> AboutSettingsScreen(onBack = { subScreen = null })
        else -> SettingsRootScreen(onOpen = { subScreen = it })
    }
}

// --- Root level ---------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsRootScreen(onOpen: (String) -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item {
                SettingsItem(
                    icon = Icons.Outlined.Palette,
                    title = stringResource(R.string.section_appearance),
                    subtitle = stringResource(R.string.settings_appearance_desc),
                    onClick = { onOpen("appearance") },
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Translate,
                    title = stringResource(R.string.section_language),
                    subtitle = currentLanguageLabel(),
                    onClick = { onOpen("language") },
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Key,
                    title = stringResource(R.string.settings_auth_title),
                    subtitle = stringResource(R.string.settings_auth_desc),
                    onClick = { onOpen("auth") },
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.settings_about_title),
                    subtitle = stringResource(R.string.settings_about_desc),
                    onClick = { onOpen("about") },
                )
            }
        }
    }
}

@Composable
private fun currentLanguageLabel(): String {
    val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    return when {
        tag.isEmpty() -> stringResource(R.string.language_system)
        tag.startsWith("ru") -> "Русский"
        else -> "English"
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp),
        )
        Column(modifier = Modifier.padding(start = 20.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// --- Shared child scaffold ------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsChildScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding -> content(padding) }
}

// --- Appearance -----------------------------------------------------------------

@Composable
private fun AppearanceSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    SettingsChildScaffold(stringResource(R.string.section_appearance), onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The app theme applies instantly on selection, so the preview simply
            // renders with the live MaterialTheme colors.
            ThemePreviewCard()
            Spacer(Modifier.height(20.dp))
            // Same segmented pill control as "Default target" and "Language".
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                THEME_OPTIONS.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = viewModel.themeMode == option.mode,
                        onClick = { viewModel.setTheme(option.mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = THEME_OPTIONS.size),
                        icon = {},
                    ) { Text(stringResource(option.labelRes), maxLines = 1) }
                }
            }
            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        stringResource(R.string.dynamic_color),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        stringResource(R.string.dynamic_color_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.dynamic_color_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = viewModel.useDynamicColor,
                    onCheckedChange = { viewModel.setDynamicColor(it) },
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                )
            }
        }
    }
}

/** Miniature phone mock rendered with the live theme colors. */
@Composable
private fun ThemePreviewCard() {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .width(190.dp)
            .height(330.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(2.dp, cs.outlineVariant, RoundedCornerShape(28.dp))
            .background(cs.background)
            .padding(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.domains),
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .border(1.dp, cs.outline, RoundedCornerShape(8.dp)),
            )
            repeat(3) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(cs.surfaceContainerHigh)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(cs.primary),
                    )
                    Box(
                        Modifier
                            .width(70.dp)
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(cs.onSurfaceVariant.copy(alpha = 0.5f)),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (index == 0) cs.secondaryContainer else cs.surfaceContainerHigh),
                    )
                }
            }
        }
    }
}

// --- Language --------------------------------------------------------------------

@Composable
private fun LanguageSettingsScreen(onBack: () -> Unit) {
    SettingsChildScaffold(stringResource(R.string.section_language), onBack) { padding ->
        val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(vertical = 8.dp),
        ) {
            LANGUAGE_OPTIONS.forEach { option ->
                val selected = if (option.tag.isEmpty()) currentTag.isEmpty()
                else currentTag.startsWith(option.tag)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { applyLocale(option.tag) }
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected, onClick = { applyLocale(option.tag) })
                    Text(
                        option.label ?: stringResource(R.string.language_system),
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

private fun applyLocale(tag: String) {
    AppCompatDelegate.setApplicationLocales(
        if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
        else LocaleListCompat.forLanguageTags(tag)
    )
}

// --- Authorization (router address, token, default target) -----------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val testState by viewModel.testState.collectAsState()
    var tokenVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val savedMessage = stringResource(R.string.saved)
    val context = LocalContext.current

    // Test-connection result surfaces as a one-shot toast; state is cleared so it
    // doesn't re-fire on recomposition or when returning to this screen.
    LaunchedEffect(testState) {
        val state = testState
        if (state is TestUiState.Done) {
            Toast.makeText(context, state.message.resolve(context), Toast.LENGTH_LONG).show()
            viewModel.clearTestResult()
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_auth_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { SectionLabel(stringResource(R.string.section_router)) }
            item {
                OutlinedTextField(
                    value = viewModel.host,
                    onValueChange = { viewModel.host = it },
                    label = { Text(stringResource(R.string.host)) },
                    placeholder = { Text("192.168.1.1") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = viewModel.port,
                    onValueChange = { viewModel.port = it },
                    label = { Text(stringResource(R.string.port)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { HorizontalDivider() }
            item { SectionLabel(stringResource(R.string.section_auth)) }
            item {
                OutlinedTextField(
                    value = viewModel.token,
                    onValueChange = { viewModel.token = it },
                    label = { Text(stringResource(R.string.token)) },
                    singleLine = true,
                    visualTransformation = if (tokenVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { tokenVisible = !tokenVisible }) {
                            Text(stringResource(if (tokenVisible) R.string.hide else R.string.show))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { HorizontalDivider() }
            item { SectionLabel(stringResource(R.string.section_default_target)) }
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    TARGET_OPTIONS.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = viewModel.target == option,
                            onClick = { viewModel.target = option },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = TARGET_OPTIONS.size),
                            icon = {},
                        ) { Text(option, maxLines = 1) }
                    }
                }
            }
            item { HorizontalDivider() }
            item {
                Button(
                    onClick = {
                        viewModel.save()
                        scope.launch { snackbarHostState.showSnackbar(savedMessage) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.save)) }
            }
            item {
                OutlinedButton(
                    onClick = { viewModel.testConnection() },
                    enabled = testState !is TestUiState.Testing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (testState is TestUiState.Testing) R.string.testing
                            else R.string.test_connection
                        )
                    )
                }
            }
        }
    }
}

// --- About ------------------------------------------------------------------------

@Composable
private fun AboutSettingsScreen(onBack: () -> Unit) {
    SettingsChildScaffold(stringResource(R.string.settings_about_title), onBack) { padding ->
        val context = LocalContext.current
        val version = remember {
            runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull() ?: "?"
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // App header: name + version, GitHub mark links to the project repo.
            ElevatedCard(
                onClick = { context.openUrl(GITHUB_REPO_URL) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1D1D1F)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp),
                    ) {
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(R.string.about_version, version),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            stringResource(R.string.about_source_code),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_github),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            // Developer: GitHub avatar + nickname, opens the profile in a browser.
            ElevatedCard(
                onClick = { context.openUrl(GITHUB_PROFILE_URL) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = GITHUB_AVATAR_URL,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                    )
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            GITHUB_USERNAME,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(R.string.developer),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Text(
                stringResource(R.string.about_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
    }
}

private fun Context.openUrl(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
    )
}
