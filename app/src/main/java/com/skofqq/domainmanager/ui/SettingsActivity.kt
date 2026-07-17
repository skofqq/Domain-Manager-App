package com.skofqq.domainmanager.ui

import android.app.Activity
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Fingerprint
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.LocalActivity
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.FragmentActivity
import coil.compose.SubcomposeAsyncImage
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.ui.theme.appColorScheme
import com.skofqq.domainmanager.data.ApiLog
import com.skofqq.domainmanager.data.RouterApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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
fun SettingsScreen(viewModel: SettingsViewModel, backupViewModel: BackupViewModel) {
    var subScreen by rememberSaveable { mutableStateOf<String?>(null) }
    var backProgress by remember { mutableFloatStateOf(0f) }

    // Pops the child screen first; the root's back-to-Domains handler in
    // MainNavigation only fires once this one is disabled. Predictive-back aware:
    // the swipe progress drives a scale/alpha preview of the leaving child (same
    // treatment as the bottom-tab handler in MainNavigation), commits on gesture
    // completion and snaps back on cancel.
    PredictiveBackHandler(enabled = subScreen != null) { events ->
        try {
            events.collect { backProgress = it.progress }
            subScreen = null
        } catch (e: CancellationException) {
            throw e
        } finally {
            backProgress = 0f
        }
    }

    val child = subScreen
    if (child == null) {
        SettingsRootScreen(onOpen = { subScreen = it })
    } else {
        Box(
            modifier = Modifier.graphicsLayer {
                val scale = lerp(1f, 0.94f, backProgress)
                scaleX = scale
                scaleY = scale
                alpha = lerp(1f, 0.85f, backProgress)
            },
        ) {
            when (child) {
                "appearance" -> AppearanceSettingsScreen(viewModel, onBack = { subScreen = null })
                "language" -> LanguageSettingsScreen(onBack = { subScreen = null })
                "auth" -> AuthSettingsScreen(viewModel, onBack = { subScreen = null })
                "security" -> SecuritySettingsScreen(viewModel, onBack = { subScreen = null })
                "backup" -> BackupSettingsScreen(backupViewModel, onBack = { subScreen = null })
                "diagnostics" -> DiagnosticsSettingsScreen(viewModel, onBack = { subScreen = null })
                "about" -> AboutSettingsScreen(onBack = { subScreen = null })
            }
        }
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
                    icon = Icons.Outlined.Fingerprint,
                    title = stringResource(R.string.section_security),
                    subtitle = stringResource(R.string.settings_security_desc),
                    onClick = { onOpen("security") },
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.CloudUpload,
                    title = stringResource(R.string.section_backup),
                    subtitle = stringResource(R.string.settings_backup_desc),
                    onClick = { onOpen("backup") },
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.BugReport,
                    title = stringResource(R.string.section_diagnostics),
                    subtitle = stringResource(R.string.settings_diagnostics_desc),
                    onClick = { onOpen("diagnostics") },
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
        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState(
            initialPage = THEME_OPTIONS.indexOfFirst { it.mode == viewModel.themeMode }
                .coerceAtLeast(0),
        ) { THEME_OPTIONS.size }

        // Two-way sync: a swipe (or chevron/circle-driven scroll) that SETTLES on a
        // page applies that theme for real; the selected circle follows themeMode.
        LaunchedEffect(pagerState.settledPage) {
            val mode = THEME_OPTIONS[pagerState.settledPage].mode
            if (viewModel.themeMode != mode) viewModel.setTheme(mode)
        }

        fun scrollTo(page: Int) {
            scope.launch {
                pagerState.animateScrollToPage(page.coerceIn(0, THEME_OPTIONS.lastIndex))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Swipeable preview card + side chevrons as a tap alternative. The pager
            // spans the full width between the chevrons (each page centers its card),
            // so the swipe is caught well outside the card itself.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(
                    onClick = { scrollTo(pagerState.currentPage - 1) },
                    enabled = pagerState.currentPage > 0,
                ) { Icon(Icons.Filled.ChevronLeft, contentDescription = null) }
                HorizontalPager(
                    state = pagerState,
                    pageSpacing = 16.dp,
                    modifier = Modifier.weight(1f),
                ) { page ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        ThemePreviewPage(THEME_OPTIONS[page], viewModel.useDynamicColor)
                    }
                }
                IconButton(
                    onClick = { scrollTo(pagerState.currentPage + 1) },
                    enabled = pagerState.currentPage < THEME_OPTIONS.lastIndex,
                ) { Icon(Icons.Filled.ChevronRight, contentDescription = null) }
            }
            Spacer(Modifier.height(14.dp))
            PagerDots(current = pagerState.currentPage, count = THEME_OPTIONS.size)
            Spacer(Modifier.height(12.dp))
            // Round theme selectors (reference style), replacing the old pill row.
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                THEME_OPTIONS.forEachIndexed { index, option ->
                    ThemeCircleSelector(
                        option = option,
                        selected = viewModel.themeMode == option.mode,
                        onClick = {
                            viewModel.setTheme(option.mode)
                            scrollTo(index)
                        },
                    )
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

/**
 * One pager page: a filled container-colored card with the "phone screen" surface
 * (skeleton preview) on top and the theme label embedded in the colored bottom
 * zone — a single visual asset per the reference. The System page resolves to the
 * device's REAL current system theme, not a static look.
 */
@Composable
private fun ThemePreviewPage(option: ThemeOption, useDynamicColor: Boolean) {
    val dark = when (option.mode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val scheme = appColorScheme(darkTheme = dark, useDynamicColor = useDynamicColor)
    MaterialTheme(colorScheme = scheme) {
        Column(
            modifier = Modifier
                .width(228.dp)
                .height(390.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(scheme.primaryContainer),
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp)
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(scheme.surface),
            ) { ThemePreviewSkeleton() }
            Text(
                text = stringResource(option.labelRes),
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onPrimaryContainer,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 11.dp),
            )
        }
    }
}

/**
 * Mini Domains-style skeleton inside the preview "screen": search bar, three
 * placeholder rows (dot + title bar + badge), bottom-nav dots. Colors and the
 * shimmer come from the wrapping per-page MaterialTheme.
 */
@Composable
private fun ThemePreviewSkeleton() {
    val cs = MaterialTheme.colorScheme
    val brush = shimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SkeletonBox(brush, Modifier.fillMaxWidth().height(20.dp), CircleShape)
        repeat(3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(cs.surfaceContainerHigh)
                    .padding(horizontal = 8.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(cs.primary),
                )
                SkeletonBox(brush, Modifier.weight(1f).height(8.dp))
                SkeletonBox(brush, Modifier.width(18.dp).height(10.dp), CircleShape)
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
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (index == 0) cs.secondaryContainer else cs.surfaceContainerHigh),
                )
            }
        }
    }
}

/** Standard 3-dot page indicator; the active dot is bigger and accent-colored. */
@Composable
private fun PagerDots(current: Int, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(count) { index ->
            val active = index == current
            Box(
                modifier = Modifier
                    .size(if (active) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
            )
        }
    }
}

private val CircleSwatchDark = Color(0xFF1D1D1F)
private val CircleSwatchLight = Color(0xFFF4F4EC)

/**
 * Round theme selector (reference style): dark filled circle with an auto icon for
 * System, light filled circle for Light, solid dark circle for Dark; the selected
 * one gets a thicker accent ring.
 */
@Composable
private fun ThemeCircleSelector(option: ThemeOption, selected: Boolean, onClick: () -> Unit) {
    val label = stringResource(option.labelRes)
    Box(
        modifier = Modifier
            .size(52.dp)
            .then(
                if (selected) {
                    Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else Modifier
            )
            .padding(6.dp)
            .clip(CircleShape)
            .background(if (option.mode == "light") CircleSwatchLight else CircleSwatchDark)
            .then(
                if (option.mode == "light") {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                } else Modifier
            )
            .clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (option.mode == "system") {
            Icon(
                Icons.Filled.BrightnessAuto,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// --- Language --------------------------------------------------------------------

@Composable
private fun LanguageSettingsScreen(onBack: () -> Unit) {
    SettingsChildScaffold(stringResource(R.string.section_language), onBack) { padding ->
        val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val activity = LocalActivity.current
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
                        .clickable { applyLocale(activity, option.tag) }
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected, onClick = { applyLocale(activity, option.tag) })
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

private fun applyLocale(activity: Activity?, tag: String) {
    AppCompatDelegate.setApplicationLocales(
        if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
        else LocaleListCompat.forLanguageTags(tag)
    )
    // setApplicationLocales() recreates the activity; the default relaunch has no
    // transition, which reads as a flash. Crossfade it instead.
    when {
        activity == null -> Unit
        Build.VERSION.SDK_INT >= 34 -> {
            activity.overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_OPEN,
                android.R.anim.fade_in,
                android.R.anim.fade_out,
            )
            activity.overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                android.R.anim.fade_in,
                android.R.anim.fade_out,
            )
        }
        else -> @Suppress("DEPRECATION")
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
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
    val activity = LocalActivity.current as? FragmentActivity
    val authTitle = stringResource(R.string.auth_to_show_token)

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
                        TextButton(onClick = {
                            when {
                                tokenVisible -> tokenVisible = false
                                // "token" lock mode: confirm with biometrics/PIN before revealing.
                                viewModel.appLockMode == "token" && activity != null &&
                                    deviceAuthAvailable(activity) ->
                                    promptDeviceAuth(activity, authTitle) { tokenVisible = true }
                                else -> tokenVisible = true
                            }
                        }) {
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

// --- Security (biometric / PIN lock) ----------------------------------------------

private data class LockOption(
    val mode: String,
    @StringRes val labelRes: Int,
    @StringRes val descRes: Int,
)

private val LOCK_OPTIONS = listOf(
    LockOption("off", R.string.lock_mode_off, R.string.lock_mode_off_desc),
    LockOption("token", R.string.lock_mode_token, R.string.lock_mode_token_desc),
    LockOption("app", R.string.lock_mode_app, R.string.lock_mode_app_desc),
)

@Composable
private fun SecuritySettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    SettingsChildScaffold(stringResource(R.string.section_security), onBack) { padding ->
        val context = LocalContext.current
        val authAvailable = remember { deviceAuthAvailable(context) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                stringResource(R.string.lock_explain),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!authAvailable) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.lock_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(12.dp))
            LOCK_OPTIONS.forEach { option ->
                // With no biometrics AND no screen lock there is nothing to require —
                // only "off" stays selectable.
                val enabled = authAvailable || option.mode == "off"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = enabled) { viewModel.setAppLock(option.mode) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = viewModel.appLockMode == option.mode,
                        onClick = { viewModel.setAppLock(option.mode) },
                        enabled = enabled,
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            stringResource(option.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (enabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        )
                        Text(
                            stringResource(option.descRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// --- Backup (export / import of the router lists) ---------------------------------

@Composable
private fun BackupSettingsScreen(viewModel: BackupViewModel, onBack: () -> Unit) {
    SettingsChildScaffold(stringResource(R.string.section_backup), onBack) { padding ->
        val state by viewModel.state.collectAsState()
        val context = LocalContext.current
        val clipboard = LocalClipboardManager.current
        val copiedMessage = stringResource(R.string.copied)
        var importText by rememberSaveable { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionLabel(stringResource(R.string.backup_export_title))
            Text(
                stringResource(R.string.backup_export_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { viewModel.buildSnapshot() },
                enabled = !state.building && !state.importing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (state.building) R.string.backup_building else R.string.backup_build
                    )
                )
            }
            state.snapshot?.let { snapshot ->
                state.snapshotCounts?.let { (routing, zapret2, zapret) ->
                    Text(
                        stringResource(R.string.backup_export_summary, routing, zapret2, zapret),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(snapshot))
                            Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.copy)) }
                    OutlinedButton(
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, snapshot)
                            }
                            context.startActivity(Intent.createChooser(send, null))
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.share)) }
                }
            }

            HorizontalDivider()
            SectionLabel(stringResource(R.string.backup_import_title))
            Text(
                stringResource(R.string.backup_import_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = importText,
                onValueChange = { importText = it },
                minLines = 4,
                maxLines = 10,
                trailingIcon = {
                    IconButton(onClick = {
                        clipboard.getText()?.text?.trim()?.takeIf { it.isNotEmpty() }
                            ?.let { importText = it }
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
                stringResource(R.string.backup_import_hint_restart),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { viewModel.runImport(importText) },
                enabled = importText.isNotBlank() && !state.importing && !state.building,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val progress = state.importProgress
                Text(
                    if (state.importing && progress != null) {
                        stringResource(R.string.backup_progress, progress.first, progress.second)
                    } else stringResource(R.string.backup_run_import)
                )
            }
            state.importResult?.let {
                Text(
                    it.resolve(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            state.error?.let { ErrorCard(it) }
        }
    }
}

// --- Diagnostics (timeout + request log) ------------------------------------------

@Composable
private fun DiagnosticsSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    SettingsChildScaffold(stringResource(R.string.section_diagnostics), onBack) { padding ->
        val entries by ApiLog.flow.collectAsState()
        val context = LocalContext.current
        val clipboard = LocalClipboardManager.current
        val copiedMessage = stringResource(R.string.copied)
        var timeout by remember { mutableStateOf(viewModel.httpTimeoutSeconds.toFloat()) }
        val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionLabel(stringResource(R.string.diag_timeout_title))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Slider(
                    value = timeout,
                    onValueChange = { timeout = it },
                    // Persisted once per released drag, not per tick.
                    onValueChangeFinished = { viewModel.setHttpTimeout(timeout.roundToInt()) },
                    valueRange = RouterApi.MIN_TIMEOUT_SECONDS.toFloat()..RouterApi.MAX_TIMEOUT_SECONDS.toFloat(),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.diag_timeout_value, timeout.roundToInt()),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SectionLabel(stringResource(R.string.diag_log_title))
                TextButton(
                    enabled = entries.isNotEmpty(),
                    onClick = {
                        val text = entries.joinToString("\n\n") { e ->
                            buildString {
                                append(timeFormat.format(Date(e.timeMillis)))
                                append("  ")
                                append(e.code?.toString() ?: "FAIL")
                                append("  ")
                                append(e.durationMs)
                                append(" ms\n")
                                append(e.url)
                                e.error?.let { append("\n").append(it) }
                                e.body?.let { append("\n").append(it) }
                            }
                        }
                        clipboard.setText(AnnotatedString(text))
                        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                    },
                ) { Text(stringResource(R.string.copy)) }
            }
            Text(
                stringResource(R.string.diag_log_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (entries.isEmpty()) {
                Text(
                    stringResource(R.string.diag_log_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
            entries.forEach { entry ->
                ApiLogEntryCard(entry, timeFormat)
            }
        }
    }
}

@Composable
private fun ApiLogEntryCard(entry: ApiLog.Entry, timeFormat: SimpleDateFormat) {
    var expanded by remember { mutableStateOf(false) }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    timeFormat.format(Date(entry.timeMillis)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    entry.code?.toString() ?: stringResource(R.string.diag_failed),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        entry.code == null -> MaterialTheme.colorScheme.error
                        entry.code < 400 -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.error
                    },
                )
                Text(
                    "${entry.durationMs} ms",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                entry.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
            )
            entry.error?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            entry.body?.let { body ->
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                )
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
                    // Shimmer circle while the GitHub avatar loads.
                    SubcomposeAsyncImage(
                        model = GITHUB_AVATAR_URL,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        loading = {
                            SkeletonBox(
                                brush = shimmerBrush(),
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                            )
                        },
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
