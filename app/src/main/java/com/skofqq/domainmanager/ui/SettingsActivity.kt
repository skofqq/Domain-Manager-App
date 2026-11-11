package com.skofqq.domainmanager.ui

import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.res.vectorResource
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.toShape
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.material.icons.outlined.NetworkPing
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.QrCodeScanner
import com.skofqq.domainmanager.ui.theme.Helm
import androidx.compose.material3.MaterialShapes
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.CompositionLocalProvider
import com.skofqq.domainmanager.ui.theme.LocalHelmColors
import com.skofqq.domainmanager.ui.theme.appPalette
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.BlurOn
import com.skofqq.domainmanager.ui.theme.Tints
import com.skofqq.domainmanager.ui.components.*
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.util.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.LocalActivity
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.FragmentActivity
import coil.compose.SubcomposeAsyncImage
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.ui.theme.appColorScheme
import com.skofqq.domainmanager.data.ApiLog
import com.skofqq.domainmanager.data.HistoryStore
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.data.RouterProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

private const val GITHUB_USERNAME = "skofqq"
private const val GITHUB_PROFILE_URL = "https://github.com/$GITHUB_USERNAME"
private const val GITHUB_REPO_URL = "https://github.com/$GITHUB_USERNAME/Domain-Manager-App"
private const val GITHUB_AVATAR_URL = "https://github.com/$GITHUB_USERNAME.png"

private val TARGET_OPTIONS = listOf("both", "mihomo", "magitrickle")

/** Language tag "" means "follow the system". Labels stay in their own language on purpose. */
/** [label] is the native name of the language (null = "System", from resources); [flag] an emoji. */
private data class LanguageOption(val tag: String, val label: String?, val flag: String)

private val LANGUAGE_OPTIONS = listOf(
    LanguageOption("", null, "🌐"), // label resolved from R.string.language_system
    LanguageOption("en", "English", "🇬🇧"),
    LanguageOption("ru", "Русский", "🇷🇺"),
    LanguageOption("uk", "Українська", "🇺🇦"),
    LanguageOption("de", "Deutsch", "🇩🇪"),
    LanguageOption("es", "Español", "🇪🇸"),
    LanguageOption("fr", "Français", "🇫🇷"),
    LanguageOption("it", "Italiano", "🇮🇹"),
    LanguageOption("tr", "Türkçe", "🇹🇷"),
    LanguageOption("be", "Беларуская", "🇧🇾"),
    LanguageOption("kk", "Қазақша", "🇰🇿"),
    LanguageOption("zh-CN", "简体中文", "🇨🇳"),
    LanguageOption("ja", "日本語", "🇯🇵"),
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
fun SettingsScreen(
    viewModel: SettingsViewModel,
    backupViewModel: BackupViewModel,
    /** Non-null once, right after opening via an App Shortcut — e.g. "diagnostics". */
    initialSubScreen: String? = null,
    onInitialSubScreenConsumed: () -> Unit = {},
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

    // Pops the child screen first; the root's back-to-Domains handler in
    // MainNavigation only fires once this one is disabled. Predictive-back aware:
    // the swipe progress drives a scale/corner-radius/alpha preview of the leaving
    // child (same treatment as the bottom-tab handler in MainNavigation), commits
    // on gesture completion and snaps back on cancel.
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
        // remounts it fresh once the child is gone.
        if (child == null || backProgress > 0f) {
            SettingsRootScreen(onOpen = { subScreen = it })
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
                    "appearance" -> AppearanceSettingsScreen(viewModel, onBack = { subScreen = null })
                    "language" -> LanguageSettingsScreen(onBack = { subScreen = null })
                    "auth" -> AuthSettingsScreen(viewModel, onBack = { subScreen = null })
                    "security" -> SecuritySettingsScreen(viewModel, onBack = { subScreen = null })
                    "backup" -> BackupSettingsScreen(backupViewModel, onBack = { subScreen = null })
                    "history" -> HistorySettingsScreen(viewModel, onBack = { subScreen = null })
                    "diagnostics" -> DiagnosticsSettingsScreen(viewModel, onBack = { subScreen = null })
                    "monitoring" -> MonitoringSettingsScreen(viewModel, onBack = { subScreen = null })
                    "shortcuts" -> ShortcutsSettingsScreen(viewModel, onBack = { subScreen = null })
                    "about" -> AboutSettingsScreen(onBack = { subScreen = null })
                }
            }
        }
    }
}

// --- Root level ---------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsRootScreen(onOpen: (String) -> Unit) {
    HelmScaffold(title = stringResource(R.string.settings)) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(top = 4.dp, bottom = padding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Only composes something while an app update is pending.
            item { UpdateAvailableCard(onOpen = { onOpen("about") }, modifier = Modifier.padding(horizontal = 16.dp)) }
            item {
                SectionCard(title = null) {
                    SettingsRow(
                        Icons.Outlined.Palette,
                        stringResource(R.string.section_appearance),
                        stringResource(R.string.settings_appearance_desc),
                        tint = Tints.purple,
                    ) { onOpen("appearance") }
                    SettingsRow(
                        Icons.Outlined.Translate,
                        stringResource(R.string.section_language),
                        currentLanguageLabel(),
                        tint = Tints.blue,
                        showDivider = false,
                    ) { onOpen("language") }
                }
            }
            item {
                SectionCard(title = null) {
                    SettingsRow(
                        Icons.Outlined.Key,
                        stringResource(R.string.settings_auth_title),
                        stringResource(R.string.settings_auth_desc),
                        tint = Tints.amber,
                    ) { onOpen("auth") }
                    SettingsRow(
                        Icons.Outlined.Fingerprint,
                        stringResource(R.string.section_security),
                        stringResource(R.string.settings_security_desc),
                        tint = Tints.green,
                    ) { onOpen("security") }
                    SettingsRow(
                        Icons.Outlined.CloudUpload,
                        stringResource(R.string.section_backup),
                        stringResource(R.string.settings_backup_desc),
                        tint = Tints.teal,
                    ) { onOpen("backup") }
                    SettingsRow(
                        Icons.Outlined.History,
                        stringResource(R.string.section_history),
                        stringResource(R.string.settings_history_desc),
                        tint = Tints.gray,
                        showDivider = false,
                    ) { onOpen("history") }
                }
            }
            item {
                SectionCard(title = null) {
                    SettingsRow(
                        Icons.Outlined.BugReport,
                        stringResource(R.string.section_diagnostics),
                        stringResource(R.string.settings_diagnostics_desc),
                        tint = Tints.red,
                    ) { onOpen("diagnostics") }
                    SettingsRow(
                        Icons.Outlined.NotificationsActive,
                        stringResource(R.string.section_monitoring),
                        stringResource(R.string.settings_monitoring_desc),
                        tint = Tints.orange,
                    ) { onOpen("monitoring") }
                    SettingsRow(
                        Icons.Outlined.Bolt,
                        stringResource(R.string.section_shortcuts),
                        stringResource(R.string.settings_shortcuts_desc),
                        tint = Tints.amber,
                        showDivider = false,
                    ) { onOpen("shortcuts") }
                }
            }
            item {
                SectionCard(title = null) {
                    SettingsRow(
                        Icons.Outlined.Info,
                        stringResource(R.string.settings_about_title),
                        stringResource(R.string.settings_about_desc),
                        tint = Tints.accent,
                        showDivider = false,
                    ) { onOpen("about") }
                }
            }
        }
    }
}

@Composable
private fun currentLanguageLabel(): String {
    // Keyed on the configuration: the activity is not recreated on a locale change.
    val tag = remember(LocalConfiguration.current) { AppCompatDelegate.getApplicationLocales().toLanguageTags() }
    if (tag.isEmpty()) return stringResource(R.string.language_system)
    val option = LANGUAGE_OPTIONS.firstOrNull { it.tag.isNotEmpty() && tag.startsWith(it.tag) }
        ?: LANGUAGE_OPTIONS.first { it.tag == "en" }
    return "${option.flag}  ${option.label}"
}

// --- Shared child scaffold ------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsChildScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    HelmScaffold(title = title, onBack = onBack, content = content)
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
                ) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = stringResource(R.string.theme_previous),
                    )
                }
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
                ) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = stringResource(R.string.theme_next),
                    )
                }
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
            Spacer(Modifier.height(24.dp))
            HelmCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    SwitchRow(
                        icon = Icons.Outlined.Palette,
                        title = stringResource(R.string.dynamic_color),
                        subtitle = stringResource(R.string.dynamic_color_desc) + if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "" else " " +
                            stringResource(R.string.dynamic_color_hint),
                        checked = viewModel.useDynamicColor,
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                        onCheckedChange = { viewModel.setDynamicColor(it) },
                    )
                    SwitchRow(
                        icon = Icons.Outlined.BlurOn,
                        title = stringResource(R.string.liquid_glass),
                        subtitle = stringResource(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) R.string.liquid_glass_desc
                            else R.string.liquid_glass_unsupported,
                        ),
                        checked = viewModel.liquidGlass && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                        onCheckedChange = { viewModel.updateLiquidGlass(it) },
                    )
                    SwitchRow(
                        icon = Icons.Outlined.DarkMode,
                        title = stringResource(R.string.true_black),
                        subtitle = stringResource(R.string.true_black_desc),
                        checked = viewModel.trueBlack,
                        onCheckedChange = { viewModel.updateTrueBlack(it) },
                    )
                    SwitchRow(
                        icon = Icons.Outlined.Vibration,
                        title = stringResource(R.string.haptic_feedback),
                        subtitle = stringResource(R.string.haptic_feedback_desc),
                        checked = viewModel.hapticFeedback,
                        showDivider = false,
                        onCheckedChange = { viewModel.updateHapticFeedback(it) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
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
    CompositionLocalProvider(LocalHelmColors provides appPalette(dark, useDynamicColor)) { MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography) {
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

private val CircleSwatchDark = Color(0xFF1C1F26)
private val CircleSwatchLight = Color(0xFFF1F0F7)

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
            // Light and Dark are bare colored circles with no glyph and no text —
            // without a name they reach a screen reader as an unlabeled swatch, and
            // "which one is picked" is drawn only as an accent ring. selectable
            // carries both the name and the selected state.
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (option.mode == "system") {
            Icon(
                Icons.Filled.BrightnessAuto,
                contentDescription = null,
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
        val currentTag = remember(LocalConfiguration.current) {
            AppCompatDelegate.getApplicationLocales().toLanguageTags()
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LANGUAGE_OPTIONS.forEach { option ->
                val selected = if (option.tag.isEmpty()) currentTag.isEmpty()
                else currentTag.startsWith(option.tag)
                OptionRow(
                    title = option.label ?: stringResource(R.string.language_system),
                    subtitle = null,
                    selected = selected,
                    leading = option.flag,
                    onClick = { applyLocale(option.tag) },
                )
            }
        }
    }
}

/**
 * MainActivity declares configChanges="locale|layoutDirection", so this does NOT
 * recreate the activity: it gets onConfigurationChanged() and the UI recomposes
 * in the new language in place (see MainActivity.localeConfiguration).
 */
private fun applyLocale(tag: String) {
    AppCompatDelegate.setApplicationLocales(
        if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
        else LocaleListCompat.forLanguageTags(tag)
    )
}

// --- Authorization (router profiles) ----------------------------------------------

/** Sentinel "profile id" for the add flow before the new profile is first saved. */
private const val NEW_PROFILE_ID = "__new__"

/**
 * Two-level Authorization: the profile list (with the active-profile radio —
 * same source of truth as the Domains title-bar switcher) and the single-profile
 * editor holding everything the old one-router form had.
 */
@Composable
private fun AuthSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    var backSwipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    // Pops the editor back to the list before the list pops back to the
    // settings root (this handler sits deeper than SettingsScreen's).
    PredictiveBackHandler(enabled = editingId != null) { events ->
        try {
            events.collect {
                backProgress = it.progress
                backSwipeEdge = it.swipeEdge
            }
            editingId = null
        } catch (e: CancellationException) {
            throw e
        } finally {
            backProgress = 0f
        }
    }
    val editing = editingId
    Box(Modifier.fillMaxSize()) {
        // Peeks the profile list behind the leaving editor while the gesture is
        // in progress. Only mounted during an active swipe, so returning to the
        // list still remounts it fresh once the editor is gone.
        if (editing == null || backProgress > 0f) {
            ProfileListScreen(
                viewModel = viewModel,
                onBack = onBack,
                onEdit = { editingId = it },
                onAdd = { editingId = NEW_PROFILE_ID },
            )
        }
        if (editing != null) {
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
                ProfileEditScreen(viewModel, profileId = editing, onBack = { editingId = null })
            }
        }
    }
}

@Composable
private fun ProfileListScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val profiles by viewModel.profiles.collectAsState()
    val activeId by viewModel.activeProfileId.collectAsState()
    val context = LocalContext.current
    var deleteCandidate by remember { mutableStateOf<RouterProfile?>(null) }
    // Scanned QR whose host matches an existing profile → confirm-update, never
    // a silent duplicate with the same address but a different token.
    var updateCandidate by remember { mutableStateOf<Pair<RouterProfile, SetupPayload>?>(null) }
    val invalidQrMessage = stringResource(R.string.qr_invalid)

    SettingsChildScaffold(stringResource(R.string.settings_auth_title), onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { NoticeCard(title = null, body = stringResource(R.string.settings_profiles_hint)) }
            items(profiles.size, key = { profiles[it].id }) { index ->
                val profile = profiles[index]
                ProfileRow(
                    profile = profile,
                    active = profile.id == activeId,
                    onSetActive = { viewModel.setActiveProfile(profile.id) },
                    onClick = { onEdit(profile.id) },
                    onDelete = { deleteCandidate = profile },
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HelmButton(
                        text = stringResource(R.string.profile_add),
                        onClick = onAdd,
                        icon = Icons.Filled.Add,
                        style = ButtonStyle.Tonal,
                        modifier = Modifier.weight(1f),
                    )
                    HelmButton(
                        text = stringResource(R.string.qr_scan),
                        icon = Icons.Outlined.QrCodeScanner,
                        style = ButtonStyle.Tonal,
                        onClick = {
                            startQrScan(context) { raw ->
                                val payload = raw?.let { parseSetupUri(it) }
                                when {
                                    payload != null -> {
                                        val existing = profiles.firstOrNull {
                                            it.host.equals(payload.host, ignoreCase = true)
                                        }
                                        if (existing != null) {
                                            updateCandidate = existing to payload
                                        } else {
                                            val profile = RouterProfile(
                                                id = UUID.randomUUID().toString(),
                                                // Host doubles as the initial label;
                                                // rename in the editor.
                                                name = payload.host,
                                                host = payload.host,
                                                port = payload.port ?: 80,
                                                fallbackHost = payload.fallbackHost,
                                                fallbackPort = payload.fallbackPort ?: 80,
                                                token = payload.token,
                                                defaultTarget = "both",
                                            )
                                            viewModel.saveProfile(profile)
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.qr_profile_added, profile.name),
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                    }
                                    raw != null ->
                                        Toast.makeText(context, invalidQrMessage, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    deleteCandidate?.let { profile ->
        GlassAlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(stringResource(R.string.profile_delete_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.profile_delete_text, profile.name))
                    if (profiles.size == 1) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.profile_delete_last_warning),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProfile(profile.id)
                    deleteCandidate = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    updateCandidate?.let { (existing, payload) ->
        GlassAlertDialog(
            onDismissRequest = { updateCandidate = null },
            title = { Text(stringResource(R.string.qr_update_title)) },
            text = { Text(stringResource(R.string.qr_update_text, existing.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveProfile(
                        existing.copy(
                            host = payload.host,
                            port = payload.port ?: existing.port,
                            token = payload.token,
                            fallbackHost = payload.fallbackHost,
                            fallbackPort = payload.fallbackPort ?: existing.fallbackPort,
                        )
                    )
                    updateCandidate = null
                }) { Text(stringResource(R.string.update)) }
            },
            dismissButton = {
                TextButton(onClick = { updateCandidate = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ProfileRow(
    profile: RouterProfile,
    active: Boolean,
    onSetActive: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    HelmCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = active, onClick = onSetActive)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name.ifEmpty { profile.host },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${profile.host}:${profile.port}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

// --- Single profile editor (the old one-router form, now per-profile) --------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditScreen(viewModel: SettingsViewModel, profileId: String, onBack: () -> Unit) {
    val isNew = profileId == NEW_PROFILE_ID
    val profiles by viewModel.profiles.collectAsState()
    val existing = remember(profileId) { profiles.firstOrNull { it.id == profileId } }
    // A new profile mints its stable id once, so repeated Saves update the same row.
    val targetId = rememberSaveable(profileId) {
        if (isNew) UUID.randomUUID().toString() else profileId
    }
    var name by rememberSaveable(profileId) { mutableStateOf(existing?.name ?: "") }
    var host by rememberSaveable(profileId) { mutableStateOf(existing?.host ?: "") }
    var port by rememberSaveable(profileId) { mutableStateOf((existing?.port ?: 80).toString()) }
    var fallbackHost by rememberSaveable(profileId) { mutableStateOf(existing?.fallbackHost ?: "") }
    var fallbackPort by rememberSaveable(profileId) { mutableStateOf((existing?.fallbackPort ?: 80).toString()) }
    var token by rememberSaveable(profileId) { mutableStateOf(existing?.token ?: "") }
    var target by rememberSaveable(profileId) { mutableStateOf(existing?.defaultTarget ?: "both") }

    val testState by viewModel.testState.collectAsState()
    var tokenVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(R.string.saved)
    val context = LocalContext.current
    val activity = LocalActivity.current as? FragmentActivity
    val authTitle = stringResource(R.string.auth_to_show_token)
    val qrAuthTitle = stringResource(R.string.auth_to_show_qr)
    var showQr by remember { mutableStateOf(false) }

    // Test-connection result surfaces as a one-shot toast; state is cleared so it
    // doesn't re-fire on recomposition or when returning to this screen.
    LaunchedEffect(testState) {
        val state = testState
        if (state is TestUiState.Done) {
            Toast.makeText(context, state.message.resolve(context), Toast.LENGTH_LONG).show()
            viewModel.clearTestResult()
        }
    }

    val pageTitle = when {
        isNew -> stringResource(R.string.profile_new_title)
        existing != null -> existing.name.ifEmpty { existing.host }
        else -> stringResource(R.string.settings_auth_title)
    }
    HelmScaffold(
        title = pageTitle,
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                HelmSection(
                    title = stringResource(R.string.section_router),
                    icon = Icons.Outlined.Router,
                    tint = Tints.accent,
                    shape = MaterialShapes.Cookie12Sided,
                ) {
                    HelmTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.profile_name)) },
                        placeholder = { Text(stringResource(R.string.profile_name_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HelmTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text(stringResource(R.string.host)) },
                            placeholder = { Text("192.168.1.1") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.weight(2f),
                        )
                        HelmTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text(stringResource(R.string.port)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                HelmSection(
                    title = stringResource(R.string.section_fallback),
                    subtitle = stringResource(R.string.fallback_hint),
                    icon = Icons.Outlined.AltRoute,
                    tint = Tints.purple,
                    shape = MaterialShapes.Clover4Leaf,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HelmTextField(
                            value = fallbackHost,
                            onValueChange = { fallbackHost = it },
                            label = { Text(stringResource(R.string.host)) },
                            placeholder = { Text("100.64.0.1") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.weight(2f),
                        )
                        HelmTextField(
                            value = fallbackPort,
                            onValueChange = { fallbackPort = it },
                            label = { Text(stringResource(R.string.port)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                HelmSection(
                    title = stringResource(R.string.section_auth),
                    icon = Icons.Outlined.Key,
                    tint = Tints.amber,
                    shape = MaterialShapes.Sunny,
                ) {
                    HelmTextField(
                        value = token,
                        onValueChange = { token = it },
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.section_default_target),
                            style = MaterialTheme.typography.titleSmall,
                            color = Helm.colors.text,
                        )
                        PillTabs(
                            labels = TARGET_OPTIONS,
                            selected = TARGET_OPTIONS.indexOf(target).coerceAtLeast(0).toFloat(),
                            onSelect = { target = TARGET_OPTIONS[it] },
                            container = Helm.colors.surface2,
                        )
                    }
                }
            }
            item {
                HelmSection(
                    title = stringResource(R.string.section_qr),
                    subtitle = stringResource(R.string.qr_hint),
                    icon = Icons.Outlined.QrCode2,
                    tint = Tints.teal,
                    shape = MaterialShapes.Cookie6Sided,
                ) {
                    HelmButton(
                        text = stringResource(R.string.qr_show),
                        onClick = {
                            // The QR carries the token in clear text (same secret,
                            // different shape), so it sits behind the SAME device-auth
                            // gate as the token "Show" button. Cancel/fail = no dialog,
                            // no exposure at all.
                            when {
                                viewModel.appLockMode == "token" && activity != null &&
                                    deviceAuthAvailable(activity) ->
                                    promptDeviceAuth(activity, qrAuthTitle) { showQr = true }
                                else -> showQr = true
                            }
                        },
                        icon = Icons.Outlined.QrCode2,
                        style = ButtonStyle.Tonal,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HelmButton(
                        text = stringResource(R.string.save),
                        onClick = {
                            viewModel.saveProfile(
                                RouterProfile(
                                    id = targetId,
                                    // An unnamed profile still needs a recognizable list
                                    // label: fall back to the host.
                                    name = name.trim().ifEmpty { host.trim() },
                                    host = host.trim(),
                                    port = port.toIntOrNull()?.coerceIn(1, 65535) ?: 80,
                                    fallbackHost = fallbackHost.trim(),
                                    fallbackPort = fallbackPort.toIntOrNull()?.coerceIn(1, 65535) ?: 80,
                                    token = token.trim(),
                                    defaultTarget = target,
                                )
                            )
                            scope.launch { snackbarHostState.showSnackbar(savedMessage) }
                        },
                        enabled = host.isNotBlank(),
                        icon = Icons.Filled.Check,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    HelmButton(
                        text = stringResource(
                            if (testState is TestUiState.Testing) R.string.testing
                            else R.string.test_connection
                        ),
                        onClick = { viewModel.testConnection(host, port, token) },
                        loading = testState is TestUiState.Testing,
                        icon = Icons.Outlined.NetworkCheck,
                        style = ButtonStyle.Tonal,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (showQr) {
        // Encodes THIS edited profile's current field values (saved or not) —
        // showing the QR of a non-active profile shows that profile's own data.
        val qrContent = buildSetupUri(
            host = host.trim(),
            port = port.trim(),
            token = token.trim(),
            fallbackHost = fallbackHost.trim(),
            fallbackPort = fallbackPort.trim(),
        )
        // Long-press fallback for scanners that reject the styled look; resets
        // to the styled view on every dialog opening.
        var plainQr by remember { mutableStateOf(false) }
        // Near-fullscreen so the code renders large — easier to scan and the
        // styling actually reads (small-dialog rendering wasted both).
        Dialog(
            onDismissRequest = { showQr = false },
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
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.qr_show_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .pointerInput(Unit) {
                                    detectTapGestures(onLongPress = { plainQr = !plainQr })
                                },
                        ) {
                            Crossfade(targetState = plainQr, label = "qr-style") { plain ->
                                StyledQrCode(
                                    content = qrContent,
                                    plain = plain,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                    Text(
                        stringResource(R.string.qr_plain_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.qr_show_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = { showQr = false },
                        modifier = Modifier.align(Alignment.End),
                    ) { Text(stringResource(R.string.close)) }
                }
            }
        }
    }
}

/**
 * System QR scanner via Play services (no camera permission needed in-app).
 * [onRaw] gets the decoded text, or null when cancelled/unavailable.
 */
private fun startQrScan(context: Context, onRaw: (String?) -> Unit) {
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    GmsBarcodeScanning.getClient(context, options)
        .startScan()
        .addOnSuccessListener { onRaw(it.rawValue) }
        .addOnCanceledListener { onRaw(null) }
        .addOnFailureListener { onRaw(null) }
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
            NoticeCard(title = null, body = stringResource(R.string.lock_explain))
            if (!authAvailable) {
                Spacer(Modifier.height(12.dp))
                NoticeCard(
                    title = null,
                    body = stringResource(R.string.lock_unavailable),
                    tint = Tints.red,
                    icon = Icons.Outlined.Fingerprint,
                )
            }
            Spacer(Modifier.height(12.dp))
            LOCK_OPTIONS.forEach { option ->
                // With no biometrics AND no screen lock there is nothing to require —
                // only "off" stays selectable.
                val enabled = authAvailable || option.mode == "off"
                OptionRow(
                    title = stringResource(option.labelRes),
                    subtitle = stringResource(option.descRes),
                    selected = viewModel.appLockMode == option.mode,
                    enabled = enabled,
                    onClick = { viewModel.setAppLock(option.mode) },
                )
                Spacer(Modifier.height(10.dp))
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HelmSection(
                title = stringResource(R.string.backup_export_title),
                subtitle = stringResource(R.string.backup_export_note),
                icon = Icons.Outlined.CloudUpload,
                tint = Tints.teal,
                shape = MaterialShapes.Cookie12Sided,
            ) {
                state.snapshot?.let { snapshot ->
                    state.snapshotCounts?.let { (routing, zapret2, zapret) ->
                        NoticeCard(
                            title = null,
                            body = stringResource(R.string.backup_export_summary, routing, zapret2, zapret),
                            tint = Tints.green,
                            icon = Icons.Filled.CheckCircle,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HelmButton(
                            text = stringResource(R.string.copy),
                            onClick = {
                                clipboard.setText(AnnotatedString(snapshot))
                                Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                            },
                            icon = Icons.Outlined.ContentCopy,
                            style = ButtonStyle.Tonal,
                            modifier = Modifier.weight(1f),
                        )
                        HelmButton(
                            text = stringResource(R.string.share),
                            onClick = {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, snapshot)
                                }
                                context.startActivity(Intent.createChooser(send, null))
                            },
                            icon = Icons.Outlined.Share,
                            style = ButtonStyle.Tonal,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                HelmButton(
                    text = stringResource(
                        if (state.building) R.string.backup_building else R.string.backup_build
                    ),
                    onClick = { viewModel.buildSnapshot() },
                    enabled = !state.importing,
                    loading = state.building,
                    icon = Icons.Outlined.Inventory2,
                    style = if (state.snapshot == null) ButtonStyle.Primary else ButtonStyle.Neutral,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            HelmSection(
                title = stringResource(R.string.backup_import_title),
                subtitle = stringResource(R.string.backup_import_note),
                icon = Icons.Outlined.CloudDownload,
                tint = Tints.blue,
                shape = MaterialShapes.Cookie9Sided,
            ) {
                HelmTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    minLines = 4,
                    maxLines = 10,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
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
                SectionNote(stringResource(R.string.backup_import_hint_restart))
                val progress = state.importProgress
                if (state.importing && progress != null && progress.second > 0) {
                    LabeledProgress(
                        label = stringResource(R.string.backup_run_import),
                        valueText = stringResource(R.string.backup_progress, progress.first, progress.second),
                        progress = progress.first.toFloat() / progress.second,
                    )
                }
                HelmButton(
                    text = if (state.importing && progress != null) {
                        stringResource(R.string.backup_progress, progress.first, progress.second)
                    } else stringResource(R.string.backup_run_import),
                    onClick = { viewModel.runImport(importText) },
                    enabled = importText.isNotBlank() && !state.building,
                    loading = state.importing,
                    icon = Icons.Outlined.Download,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.importResult?.let {
                    NoticeCard(title = null, body = it.resolve(), tint = Tints.green, icon = Icons.Filled.CheckCircle)
                }
                state.error?.let { ErrorCard(it) }
            }
        }
    }
}

// --- History (local log of added domains) -----------------------------------------

/**
 * Read-only, phone-local history of successful adds (action=add / strat_add).
 * Never synced with the router — it keeps no timestamps of its own.
 */
@Composable
private fun HistorySettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    SettingsChildScaffold(stringResource(R.string.section_history), onBack) { padding ->
        val context = LocalContext.current
        val store = remember { HistoryStore.get(context) }
        val entries by store.entries.collectAsState()
        // History is per router profile — reload for the active one.
        val activeRouterId by viewModel.activeProfileId.collectAsState()
        LaunchedEffect(activeRouterId) {
            withContext(Dispatchers.IO) { store.reload(activeRouterId) }
        }
        val dateFormat = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item {
                NoticeCard(title = null, body = stringResource(R.string.history_note))
                Spacer(Modifier.height(14.dp))
            }
            val list = entries
            if (list != null && list.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.History,
                        title = stringResource(R.string.history_empty),
                        tint = Tints.gray,
                    )
                }
            }
            if (!list.isNullOrEmpty()) {
                // One sliced card: the header, then a lazy item per entry.
                item(key = "history-header") {
                    HelmSectionHeader(
                        title = stringResource(R.string.section_history),
                        icon = Icons.Outlined.History,
                        tint = Tints.blue,
                        shape = MaterialShapes.Cookie9Sided,
                        trailing = { ValuePill(list.size.toString(), Tints.blue) },
                        modifier = Modifier
                            .sectionSlice(first = true, last = false, color = Helm.colors.card)
                            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 8.dp),
                    )
                }
                items(list.size, key = { list[it].id }) { index ->
                    val entry = list[index]
                    val last = index == list.lastIndex
                    Column(
                        Modifier
                            .sectionSlice(first = false, last = last, color = Helm.colors.card)
                            .padding(start = 18.dp, end = 18.dp, bottom = if (last) 10.dp else 0.dp),
                    ) {
                        if (index > 0) SectionDivider()
                        SectionRow(
                            title = entry.domain,
                            subtitle = buildString {
                                append(dateFormat.format(Date(entry.timeMillis)))
                                entry.target?.let { append(" · ").append(it) }
                                entry.engine?.let { engine ->
                                    append(" · ").append(engine)
                                    entry.strategy?.let { append(" #").append(it) }
                                }
                            },
                            leading = {
                                ShapeIcon(
                                    if (entry.engine != null) Icons.Outlined.Shield else Icons.Outlined.Public,
                                    if (entry.engine != null) Tints.orange else Tints.accent,
                                    shape = MaterialShapes.Circle,
                                    size = 36.dp,
                                )
                            },
                        )
                    }
                }
            }
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
        var showRebootConfirm by remember { mutableStateOf(false) }
        val rebootState by viewModel.rebootState.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HelmSection(
                title = stringResource(R.string.diag_timeout_title),
                icon = Icons.Outlined.Timer,
                tint = Tints.blue,
                shape = MaterialShapes.Cookie9Sided,
                trailing = { ValuePill(stringResource(R.string.diag_timeout_value, timeout.roundToInt()), Tints.blue) },
            ) {
                Slider(
                    value = timeout,
                    onValueChange = { timeout = it },
                    // Persisted once per released drag, not per tick.
                    onValueChangeFinished = { viewModel.setHttpTimeout(timeout.roundToInt()) },
                    valueRange = RouterApi.MIN_TIMEOUT_SECONDS.toFloat()..RouterApi.MAX_TIMEOUT_SECONDS.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            HelmSection(
                title = stringResource(R.string.section_router),
                icon = Icons.Outlined.RestartAlt,
                tint = Tints.red,
                shape = MaterialShapes.Cookie4Sided,
            ) {
                HelmButton(
                    text = stringResource(R.string.reboot_router),
                    onClick = { showRebootConfirm = true },
                    enabled = rebootState == null,
                    icon = Icons.Outlined.RestartAlt,
                    style = ButtonStyle.Danger,
                    modifier = Modifier.fillMaxWidth(),
                )
                viewModel.rebootError?.let { ErrorCard(it) }
            }

            HelmSection(
                title = stringResource(R.string.diag_log_title),
                subtitle = stringResource(R.string.diag_log_note),
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                tint = Tints.purple,
                shape = MaterialShapes.Clover4Leaf,
                trailing = {
                    SectionIconButton(
                        Icons.Outlined.ContentCopy,
                        stringResource(R.string.copy),
                        enabled = entries.isNotEmpty(),
                        tint = Tints.purple,
                    ) {
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
                    }
                },
            ) {
                if (entries.isEmpty()) {
                    SectionNote(stringResource(R.string.diag_log_empty))
                }
                entries.forEach { entry ->
                    ApiLogEntryCard(entry, timeFormat)
                }
            }
        }

        // Two-step confirmation: the server fires the reboot immediately with no
        // soft warning of its own, so the dialog carries the full consequences.
        if (showRebootConfirm) {
            GlassAlertDialog(
                onDismissRequest = { showRebootConfirm = false },
                title = { Text(stringResource(R.string.reboot_confirm_title)) },
                text = { Text(stringResource(R.string.reboot_confirm_text)) },
                confirmButton = {
                    TextButton(onClick = {
                        showRebootConfirm = false
                        viewModel.requestReboot()
                    }) {
                        Text(
                            stringResource(R.string.reboot_confirm_button),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRebootConfirm = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }

        rebootState?.let { state ->
            RebootOverlay(
                state = state,
                onCheck = { viewModel.rebootCheckConnection() },
                onDismiss = { viewModel.dismissReboot() },
            )
        }
    }
}

/**
 * Full-screen "router is rebooting" overlay: countdown while the router is down,
 * then a connection check reusing the Test Connection logic. Rendered as an
 * un-dismissable full-size Dialog so it covers the bottom navigation too; the
 * explicit Hide button is the only way out.
 */
@Composable
private fun RebootOverlay(
    state: RebootUiState,
    onCheck: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        val still = reduceMotion()
        val spin = rememberInfiniteTransition(label = "reboot")
        val angle by spin.animateFloat(
            0f, 360f,
            infiniteRepeatable(tween(9000, easing = LinearEasing)),
            label = "rebootAngle",
        )
        val (tint, shape) = when (state) {
            is RebootUiState.CheckDone -> if (state.ok) Tints.green to MaterialShapes.Cookie9Sided
            else Tints.red to MaterialShapes.Cookie4Sided
            else -> Tints.accent to MaterialShapes.Cookie12Sided
        }
        Surface(modifier = Modifier.fillMaxSize(), color = Helm.colors.page) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(140.dp)
                            .graphicsLayer { rotationZ = if (still || state is RebootUiState.CheckDone) 0f else angle }
                            .clip(shape.toShape())
                            .background(tint.bg),
                    )
                    Icon(
                        when {
                            state is RebootUiState.CheckDone && state.ok -> Icons.Filled.Check
                            state is RebootUiState.CheckDone -> Icons.Filled.Close
                            else -> Icons.Filled.RestartAlt
                        },
                        contentDescription = null,
                        tint = tint.fg,
                        modifier = Modifier.size(56.dp),
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.rebooting_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Helm.colors.text,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.rebooting_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Helm.colors.text2,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(28.dp))
                when (state) {
                    is RebootUiState.Requesting -> LoadingIndicator(Modifier.size(64.dp))
                    is RebootUiState.Waiting -> Text(
                        stringResource(R.string.reboot_countdown, state.secondsLeft),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Helm.colors.accent,
                    )
                    is RebootUiState.CheckReady -> {
                        Text(
                            stringResource(R.string.reboot_check_ready),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Helm.colors.text,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        HelmButton(
                            text = stringResource(R.string.test_connection),
                            onClick = onCheck,
                            icon = Icons.Outlined.NetworkCheck,
                        )
                    }
                    is RebootUiState.Checking -> {
                        LoadingIndicator(Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.testing), color = Helm.colors.text2)
                    }
                    is RebootUiState.CheckDone -> {
                        Text(
                            state.message.resolve(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tint.fg,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        if (state.ok) {
                            HelmButton(text = stringResource(R.string.done), onClick = onDismiss, icon = Icons.Filled.Check)
                        } else {
                            HelmButton(text = stringResource(R.string.retry), onClick = onCheck, icon = Icons.Filled.Refresh)
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.hide_overlay))
                }
            }
        }
    }
}

// --- Monitoring (opt-in background WAN/disk/latency checks) -----------------------

@Composable
private fun MonitoringSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    SettingsChildScaffold(stringResource(R.string.section_monitoring), onBack) { padding ->
        val context = LocalContext.current
        var interval by remember { mutableFloatStateOf(viewModel.monitoringIntervalMinutes.toFloat()) }
        val groupNames by viewModel.mihomoGroupNames.collectAsState()
        var selectedGroup by remember { mutableStateOf(viewModel.latencyMonitorGroup()) }
        LaunchedEffect(Unit) { viewModel.loadMihomoGroupNames() }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) {
            // Turn monitoring on either way — a denied permission just means
            // Android itself will suppress the notifications; the background
            // checks (and their WAN/disk state tracking) are still useful.
            viewModel.setMonitoringEnabled(true)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            NoticeCard(title = null, body = stringResource(R.string.monitoring_explain))
            HelmCard(modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(vertical = 6.dp)) {
                SwitchRow(
                    icon = Icons.Outlined.NotificationsActive,
                    title = stringResource(R.string.monitoring_enable),
                    checked = viewModel.monitoringEnabled,
                    showDivider = viewModel.monitoringEnabled,
                    onCheckedChange = { checked ->
                        val needsPermission = checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                        if (needsPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setMonitoringEnabled(checked)
                        }
                    },
                )
                if (viewModel.monitoringEnabled) {
                    SwitchRow(
                        icon = Icons.Outlined.Public,
                        title = stringResource(R.string.monitoring_wan_title),
                        subtitle = stringResource(R.string.monitoring_wan_desc),
                        checked = viewModel.monitorWanIp,
                        onCheckedChange = { viewModel.setMonitorWanIp(it) },
                    )
                    SwitchRow(
                        icon = Icons.Outlined.Storage,
                        title = stringResource(R.string.monitoring_disk_title),
                        subtitle = stringResource(R.string.monitoring_disk_desc),
                        checked = viewModel.monitorDiskSpace,
                        showDivider = false,
                        onCheckedChange = { viewModel.setMonitorDiskSpace(it) },
                    )
                }
            } }

            if (viewModel.monitoringEnabled) {
                HelmSection(
                    title = stringResource(R.string.monitoring_interval_title),
                    icon = Icons.Outlined.Schedule,
                    tint = Tints.blue,
                    shape = MaterialShapes.Sunny,
                    trailing = {
                        ValuePill(stringResource(R.string.monitoring_interval_value, interval.roundToInt()), Tints.blue)
                    },
                ) {
                    Slider(
                        value = interval,
                        onValueChange = { interval = it },
                        onValueChangeFinished = { viewModel.setMonitoringInterval(interval.roundToInt()) },
                        valueRange = 15f..180f,
                        steps = 10,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                HelmSection(
                    title = stringResource(R.string.monitoring_latency_title),
                    subtitle = stringResource(R.string.monitoring_latency_desc),
                    icon = Icons.Outlined.NetworkPing,
                    tint = Tints.purple,
                    shape = MaterialShapes.Flower,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OptionRow(
                            title = stringResource(R.string.monitoring_latency_off),
                            subtitle = null,
                            selected = selectedGroup == null,
                            container = Helm.colors.surface2,
                            onClick = {
                                selectedGroup = null
                                viewModel.setLatencyMonitorGroup(null)
                            },
                        )
                        groupNames?.forEach { name ->
                            OptionRow(
                                title = name,
                                subtitle = null,
                                selected = selectedGroup == name,
                                container = Helm.colors.surface2,
                                onClick = {
                                    selectedGroup = name
                                    viewModel.setLatencyMonitorGroup(name)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Shortcuts (dynamic App Shortcuts, 2-4 user-picked) ----------------------------

@Composable
private fun ShortcutsSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    SettingsChildScaffold(stringResource(R.string.section_shortcuts), onBack) { padding ->
        var selected by remember { mutableStateOf(viewModel.enabledShortcutIds.toSet()) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            NoticeCard(title = null, body = stringResource(R.string.shortcuts_explain))
            HelmSection(
                title = stringResource(R.string.section_shortcuts),
                subtitle = stringResource(R.string.shortcuts_min_max_hint),
                icon = Icons.Outlined.Bolt,
                tint = Tints.amber,
                shape = MaterialShapes.Sunny,
                trailing = { ValuePill("${selected.size}/4", Tints.amber) },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SHORTCUT_CATALOG.forEach { spec ->
                        val checked = spec.id in selected
                        // 2-4 enabled at all times: block the tap that would break either edge.
                        val enabled = if (checked) selected.size > 2 else selected.size < 4
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (checked) Tints.accent.bg else Helm.colors.surface2)
                                .toggleable(
                                    value = checked,
                                    enabled = enabled,
                                    role = Role.Checkbox,
                                    onValueChange = {
                                        val next = if (checked) selected - spec.id else selected + spec.id
                                        selected = next
                                        viewModel.setEnabledShortcutIds(next.toList())
                                    },
                                )
                                .heightIn(min = 56.dp)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(spec.labelRes),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                                color = (if (checked) Helm.colors.accent else Helm.colors.text)
                                    .copy(alpha = if (enabled || checked) 1f else 0.45f),
                                modifier = Modifier.weight(1f),
                            )
                            Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiLogEntryCard(entry: ApiLog.Entry, timeFormat: SimpleDateFormat) {
    var expanded by remember { mutableStateOf(false) }
    val ok = entry.code != null && entry.code < 400
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Helm.colors.surface2)
            .clickable { expanded = !expanded }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ValuePill(
                entry.code?.toString() ?: stringResource(R.string.diag_failed),
                if (ok) Tints.green else Tints.red,
            )
            Text(
                timeFormat.format(Date(entry.timeMillis)),
                style = MaterialTheme.typography.labelMedium,
                color = Helm.colors.text2,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${entry.durationMs} ms",
                style = MaterialTheme.typography.labelMedium,
                color = Helm.colors.text2,
            )
        }
        Text(
            entry.url,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = Helm.colors.text,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
        )
        entry.error?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = Tints.red.fg,
            )
        }
        entry.body?.let { body ->
            Text(
                body,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = Helm.colors.text2,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
            )
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
        val still = reduceMotion()
        val spin = rememberInfiniteTransition(label = "about")
        val angle by spin.animateFloat(
            0f, 360f,
            infiniteRepeatable(tween(24000, easing = LinearEasing)),
            label = "aboutAngle",
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Hero: the app icon on a slowly turning expressive cookie.
            Column(
                Modifier.fillMaxWidth().clip(CardShape).background(Helm.colors.card).padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(132.dp)
                            .graphicsLayer { rotationZ = if (still) 0f else angle }
                            .clip(MaterialShapes.Cookie12Sided.toShape())
                            .background(Tints.accent.bg),
                    )
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1D1D1F)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(84.dp),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Helm.colors.text,
                )
                Spacer(Modifier.height(8.dp))
                ValuePill(stringResource(R.string.about_version, version), Tints.accent)
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.about_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Helm.colors.text2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }

            AppUpdateSection()

            // Source code: GitHub mark links to the project repo.
            EntryCard(
                icon = ImageVector.vectorResource(R.drawable.ic_github),
                title = stringResource(R.string.about_source_code),
                subtitle = GITHUB_REPO_URL.removePrefix("https://"),
                onClick = { context.openUrl(GITHUB_REPO_URL) },
                tint = Tints.gray,
                shape = MaterialShapes.Cookie9Sided,
                trailing = {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = Helm.colors.text2,
                        modifier = Modifier.size(20.dp),
                    )
                },
            )

            // Developer: GitHub avatar + nickname, opens the profile in a browser.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(Helm.colors.card)
                    .clickable { context.openUrl(GITHUB_PROFILE_URL) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Shimmer cookie while the GitHub avatar loads.
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
                        .clip(MaterialShapes.Cookie9Sided.toShape()),
                )
                Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(
                        GITHUB_USERNAME,
                        style = MaterialTheme.typography.titleMedium,
                        color = Helm.colors.text,
                    )
                    Text(
                        stringResource(R.string.developer),
                        style = MaterialTheme.typography.bodySmall,
                        color = Helm.colors.text2,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = Helm.colors.text2,
                    modifier = Modifier.size(20.dp),
                )
            }
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
