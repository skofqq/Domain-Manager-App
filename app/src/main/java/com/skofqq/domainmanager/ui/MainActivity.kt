package com.skofqq.domainmanager.ui

import android.graphics.Color
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CancellationException
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.PrefsStore
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.ui.theme.DomainManagerTheme

class MainActivity : AppCompatActivity() {
    private val prefs by lazy { PrefsStore(this) }
    private val api by lazy { RouterApi(prefs, applicationContext) }
    private val domainsViewModel by viewModels<DomainsViewModel> { DomainsViewModel.Factory(api) }
    private val servicesViewModel by viewModels<ServicesViewModel> { ServicesViewModel.Factory(api) }
    private val settingsViewModel by viewModels<SettingsViewModel> { SettingsViewModel.Factory(prefs, api) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme = isDarkTheme(settingsViewModel.themeMode)
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
                // Cold start: system splash → animated intro → Domains. Saveable so a
                // recreation (language/theme change) never replays the intro.
                var introDone by rememberSaveable { mutableStateOf(false) }
                Crossfade(
                    targetState = introDone,
                    animationSpec = tween(durationMillis = 350),
                    label = "intro-crossfade",
                ) { done ->
                    if (done) {
                        MainNavigation(
                            domainsViewModel = domainsViewModel,
                            servicesViewModel = servicesViewModel,
                            settingsViewModel = settingsViewModel,
                        )
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

private val TABS = listOf(
    TabSpec(R.string.domains, Icons.Filled.Public, Icons.Outlined.Public),
    TabSpec(R.string.status, Icons.Filled.Dns, Icons.Outlined.Dns),
    TabSpec(R.string.settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
private fun MainNavigation(
    domainsViewModel: DomainsViewModel,
    servicesViewModel: ServicesViewModel,
    settingsViewModel: SettingsViewModel,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_DOMAINS) }
    var backProgress by remember { mutableFloatStateOf(0f) }

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
                TAB_DOMAINS -> DomainsScreen(domainsViewModel)
                1 -> ServicesScreen(servicesViewModel)
                2 -> SettingsScreen(settingsViewModel)
            }
        }
    }
}
