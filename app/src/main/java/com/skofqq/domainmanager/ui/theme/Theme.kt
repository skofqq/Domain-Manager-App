package com.skofqq.domainmanager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.rememberDynamicColorScheme

/** Logo accent — seed for the whole non-dynamic color scheme. */
private val BrandSeed = Color(0xFFD2DA40)

/**
 * Resolves the app's color scheme for an arbitrary light/dark choice — used both
 * by [DomainManagerTheme] and by the Appearance theme previews, which render all
 * three variants side by side regardless of the live theme.
 */
@Composable
fun appColorScheme(darkTheme: Boolean, useDynamicColor: Boolean): ColorScheme {
    val context = LocalContext.current
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return when {
        useDynamicColor && supportsDynamic && darkTheme -> dynamicDarkColorScheme(context)
        useDynamicColor && supportsDynamic -> dynamicLightColorScheme(context)
        // Dynamic color off (or unavailable): full Material3 scheme generated from
        // the brand seed, so secondary/tertiary/surfaces stay harmonized with it.
        else -> rememberDynamicColorScheme(seedColor = BrandSeed, isDark = darkTheme, isAmoled = false)
    }
}

@Composable
fun DomainManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialExpressiveTheme(
        colorScheme = appColorScheme(darkTheme, useDynamicColor),
        content = content,
    )
}
