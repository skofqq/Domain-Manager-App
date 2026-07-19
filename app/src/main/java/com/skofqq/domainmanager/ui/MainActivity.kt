package com.skofqq.domainmanager.ui

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.drop
import androidx.compose.runtime.collectAsState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.HistoryStore
import com.skofqq.domainmanager.data.PrefsStore
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.ui.theme.DomainManagerTheme
import com.skofqq.domainmanager.worker.RouterMonitorWorker

class MainActivity : AppCompatActivity() {
    private val prefs by lazy { PrefsStore(this) }
    private val api by lazy { RouterApi(prefs, applicationContext) }
    private val history by lazy { HistoryStore.get(this) }
    private val domainsViewModel by viewModels<DomainsViewModel> { DomainsViewModel.Factory(api, history) }
    private val strategiesViewModel by viewModels<StrategiesViewModel> { StrategiesViewModel.Factory(api, history) }
    private val servicesViewModel by viewModels<ServicesViewModel> { ServicesViewModel.Factory(api) }
    private val mihomoViewModel by viewModels<MihomoViewModel> { MihomoViewModel.Factory(api) }
    private val diagnosticsViewModel by viewModels<DiagnosticsViewModel> { DiagnosticsViewModel.Factory(api) }
    private val magitrickleGroupsViewModel by viewModels<MagitrickleGroupsViewModel> {
        MagitrickleGroupsViewModel.Factory(api)
    }
    private val routerInfoViewModel by viewModels<RouterInfoViewModel> { RouterInfoViewModel.Factory(api) }
    private val settingsViewModel by viewModels<SettingsViewModel> {
        SettingsViewModel.Factory(prefs, api, applicationContext)
    }
    private val backupViewModel by viewModels<BackupViewModel> { BackupViewModel.Factory(api) }

    /**
     * "shortcut_action" from a tapped App Shortcut's intent — read once in
     * onCreate, refreshed by onNewIntent when the activity is already on top
     * (singleTop). MainNavigation consumes and clears it via [consumeShortcutAction].
     */
    private var pendingShortcutAction by mutableStateOf<String?>(null)

    /** Only set for the "zapret_restart" shortcut — which engine it was published for. */
    private var pendingShortcutZapretEngine by mutableStateOf<String?>(null)

    private fun consumeShortcutAction() {
        pendingShortcutAction = null
        pendingShortcutZapretEngine = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingShortcutAction = intent.getStringExtra(EXTRA_SHORTCUT_ACTION)
        pendingShortcutZapretEngine = intent.getStringExtra(EXTRA_SHORTCUT_ZAPRET_ENGINE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        applyStartupWindowBackground()
        pendingShortcutAction = intent?.getStringExtra(EXTRA_SHORTCUT_ACTION)
        pendingShortcutZapretEngine = intent?.getStringExtra(EXTRA_SHORTCUT_ZAPRET_ENGINE)
        // Ensure the enabled shortcuts are actually published — covers first
        // launch after install/update, not only after a Settings change. The
        // zapret_restart entry needs a fresh svc_list to know which engine is
        // running, so it only appears once the Compose-side refresh below lands.
        AppShortcuts.publish(applicationContext, prefs.enabledShortcutIds)
        // Re-affirm the monitor schedule on every cold start too — WorkManager
        // survives app updates on its own, but this is a cheap safety net.
        if (prefs.monitoringEnabled) {
            RouterMonitorWorker.reschedule(applicationContext, prefs.monitoringIntervalMinutes)
        }
        setContent {
            val darkTheme = isDarkTheme(settingsViewModel.themeMode)
            // Active router switched (title-bar chip or the Settings radio):
            // every screen drops the old router's data and re-fetches — the
            // skeleton states show, never the previous router's numbers.
            LaunchedEffect(Unit) {
                prefs.activeProfileIdFlow.drop(1).collect {
                    domainsViewModel.reset()
                    strategiesViewModel.reset()
                    servicesViewModel.reset()
                    mihomoViewModel.reset()
                    diagnosticsViewModel.reset()
                    magitrickleGroupsViewModel.reset()
                    routerInfoViewModel.reset()
                }
            }
            // Keeps the "zapret_restart" shortcut's label pointed at whichever
            // engine is actually running — refreshed on every cold start and every
            // time svc_list changes (service start/stop/restart from any screen).
            LaunchedEffect(Unit) { servicesViewModel.refresh() }
            val shortcutServicesState by servicesViewModel.state.collectAsState()
            LaunchedEffect(shortcutServicesState.services) {
                val services = shortcutServicesState.services ?: return@LaunchedEffect
                val activeZapret = services.firstOrNull { it.service in ZAPRET_ENGINES && it.running }?.service
                AppShortcuts.publish(applicationContext, prefs.enabledShortcutIds, activeZapret)
            }
            // Re-style system bars when the in-app theme diverges from the system one,
            // otherwise status-bar icons lose contrast on a forced light/dark background.
            LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                )
            }
            DomainManagerTheme(
                darkTheme = darkTheme,
                useDynamicColor = settingsViewModel.useDynamicColor,
            ) {
                // Keep the window background in exact sync with the composed theme,
                // so any later recreation starts from the right color.
                val windowBackground = MaterialTheme.colorScheme.background
                LaunchedEffect(windowBackground) {
                    window.setBackgroundDrawable(ColorDrawable(windowBackground.toArgb()))
                }
                // Cold start: system splash → animated intro → Domains. Saveable so a
                // recreation (language/theme change) never replays the intro.
                var introDone by rememberSaveable { mutableStateOf(false) }
                // App lock: asked once per process when the "app" mode is on at start.
                // Initialized from the pref so enabling the mode mid-session never
                // locks the user out of the session they enabled it in.
                var unlocked by rememberSaveable {
                    mutableStateOf(settingsViewModel.appLockMode != "app")
                }
                Crossfade(
                    targetState = introDone,
                    animationSpec = tween(durationMillis = 350),
                    label = "intro-crossfade",
                ) { done ->
                    if (done) {
                        if (unlocked) {
                            MainNavigation(
                                domainsViewModel = domainsViewModel,
                                strategiesViewModel = strategiesViewModel,
                                servicesViewModel = servicesViewModel,
                                mihomoViewModel = mihomoViewModel,
                                diagnosticsViewModel = diagnosticsViewModel,
                                magitrickleGroupsViewModel = magitrickleGroupsViewModel,
                                routerInfoViewModel = routerInfoViewModel,
                                settingsViewModel = settingsViewModel,
                                backupViewModel = backupViewModel,
                                pendingShortcutAction = pendingShortcutAction,
                                pendingShortcutZapretEngine = pendingShortcutZapretEngine,
                                onShortcutActionConsumed = ::consumeShortcutAction,
                            )
                        } else {
                            AppLockScreen(onUnlocked = { unlocked = true })
                        }
                    } else {
                        SplashIntroScreen(
                            darkTheme = darkTheme,
                            onFinished = { introDone = true },
                        )
                    }
                }
            }
        }
    }

    /**
     * The XML windowBackground follows the SYSTEM night mode, but the in-app theme
     * can be forced light/dark (and dynamic color shifts the real background) — on
     * a recreation (language switch!) the first frame would flash the wrong color.
     * Paint the window with the effective theme's background before the first draw;
     * the composed theme then refines it to the exact color.
     */
    private fun applyStartupWindowBackground() {
        val dark = when (prefs.themeMode) {
            "light" -> false
            "dark" -> true
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        }
        val color = if (prefs.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // M3 dynamic background ≈ system neutral1 tone 99 (light) / tone 10 (dark).
            getColor(
                if (dark) android.R.color.system_neutral1_900
                else android.R.color.system_neutral1_10
            )
        } else {
            getColor(if (dark) R.color.window_background_dark else R.color.window_background_light)
        }
        window.setBackgroundDrawable(ColorDrawable(color))
    }
}

/** Maps the "system" | "light" | "dark" preference to an effective dark flag. */
@Composable
fun isDarkTheme(themeMode: String): Boolean = when (themeMode) {
    "light" -> false
    "dark" -> true
    else -> isSystemInDarkTheme()
}

private data class TabSpec(
    @StringRes val labelRes: Int,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector,
)

private const val TAB_DOMAINS = 0
private const val TAB_STATUS = 1
private const val TAB_SETTINGS = 2

private val ZAPRET_ENGINES = setOf("zapret", "zapret2")

private val TABS = listOf(
    TabSpec(R.string.domains, Icons.Filled.Public, Icons.Outlined.Public),
    TabSpec(R.string.status, Icons.Filled.Dns, Icons.Outlined.Dns),
    TabSpec(R.string.settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
private fun MainNavigation(
    domainsViewModel: DomainsViewModel,
    strategiesViewModel: StrategiesViewModel,
    servicesViewModel: ServicesViewModel,
    mihomoViewModel: MihomoViewModel,
    diagnosticsViewModel: DiagnosticsViewModel,
    magitrickleGroupsViewModel: MagitrickleGroupsViewModel,
    routerInfoViewModel: RouterInfoViewModel,
    settingsViewModel: SettingsViewModel,
    backupViewModel: BackupViewModel,
    pendingShortcutAction: String?,
    pendingShortcutZapretEngine: String?,
    onShortcutActionConsumed: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_DOMAINS) }
    var backProgress by remember { mutableFloatStateOf(0f) }

    // Every confirmation-requiring shortcut only NAVIGATES — reboot/mihomo-update
    // land on the screen that already carries the explicit confirm dialog, nothing
    // fires on its own. Speed test and zapret restart are the exception: neither
    // is destructive, so both run immediately, no extra tap.
    var openDevicesFromShortcut by remember { mutableStateOf(false) }
    var openDiagnosticsFromShortcut by remember { mutableStateOf(false) }
    var openMihomoUpdateFromShortcut by remember { mutableStateOf(false) }
    var openSpeedtestSubscreenFromShortcut by remember { mutableStateOf(false) }
    var autoRunSpeedtestFromShortcut by remember { mutableStateOf(false) }
    LaunchedEffect(pendingShortcutAction) {
        when (pendingShortcutAction) {
            "status" -> selectedTab = TAB_STATUS
            "devices" -> { selectedTab = TAB_STATUS; openDevicesFromShortcut = true }
            "reboot" -> { selectedTab = TAB_SETTINGS; openDiagnosticsFromShortcut = true }
            "mihomo_update" -> { selectedTab = TAB_DOMAINS; openMihomoUpdateFromShortcut = true }
            "speedtest" -> {
                selectedTab = TAB_STATUS
                openSpeedtestSubscreenFromShortcut = true
                autoRunSpeedtestFromShortcut = true
            }
            "zapret_restart" -> {
                selectedTab = TAB_STATUS
                pendingShortcutZapretEngine?.let { servicesViewModel.restart(it) }
            }
            else -> return@LaunchedEffect
        }
        onShortcutActionConsumed()
    }

    // Domains is the single root of the back graph: back from any other tab returns
    // there, and back from Domains leaves the app (no handler enabled → the system
    // predictive back-to-home animation plays). Nested Settings screens register
    // their own handler deeper in the composition, which takes priority.
    PredictiveBackHandler(enabled = selectedTab != TAB_DOMAINS) { events ->
        try {
            events.collect { backProgress = it.progress }
            selectedTab = TAB_DOMAINS
        } catch (e: CancellationException) {
            throw e
        } finally {
            backProgress = 0f
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                TABS.forEachIndexed { index, tab ->
                    val selected = selectedTab == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                if (selected) tab.filledIcon else tab.outlinedIcon,
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
        // Status-bar inset is handled by each tab's own TopAppBar; padding it here
        // too would double the gap above the title.
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Predictive back preview: the leaving tab shrinks slightly with the
                // swipe, hinting at the return to Domains.
                .graphicsLayer {
                    val scale = lerp(1f, 0.94f, backProgress)
                    scaleX = scale
                    scaleY = scale
                    alpha = lerp(1f, 0.85f, backProgress)
                },
        ) {
            when (selectedTab) {
                TAB_DOMAINS -> {
                    val routerProfiles by settingsViewModel.profiles.collectAsState()
                    val activeRouterId by settingsViewModel.activeProfileId.collectAsState()
                    DomainsScreen(
                        domainsViewModel,
                        strategiesViewModel,
                        magitrickleGroupsViewModel,
                        routerInfoViewModel,
                        routerProfiles = routerProfiles,
                        activeRouterId = activeRouterId,
                        onSelectRouter = { settingsViewModel.setActiveProfile(it) },
                        onOpenStatus = { selectedTab = TAB_STATUS },
                        autoOpenRouterSheet = openMihomoUpdateFromShortcut,
                        onAutoOpenRouterSheetConsumed = { openMihomoUpdateFromShortcut = false },
                    )
                }
                TAB_STATUS -> ServicesScreen(
                    servicesViewModel,
                    mihomoViewModel,
                    diagnosticsViewModel,
                    magitrickleGroupsViewModel,
                    initialSubScreen = when {
                        openDevicesFromShortcut -> "devices"
                        openSpeedtestSubscreenFromShortcut -> "diagnostics"
                        else -> null
                    },
                    onInitialSubScreenConsumed = {
                        openDevicesFromShortcut = false
                        openSpeedtestSubscreenFromShortcut = false
                    },
                    autoRunSpeedtest = autoRunSpeedtestFromShortcut,
                    onAutoRunSpeedtestConsumed = { autoRunSpeedtestFromShortcut = false },
                )
                TAB_SETTINGS -> SettingsScreen(
                    settingsViewModel,
                    backupViewModel,
                    initialSubScreen = if (openDiagnosticsFromShortcut) "diagnostics" else null,
                    onInitialSubScreenConsumed = { openDiagnosticsFromShortcut = false },
                )
            }
        }
    }
}
