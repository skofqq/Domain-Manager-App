package com.skofqq.domainmanager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.rememberDynamicColorScheme

/** Logo accent — seed for the whole non-dynamic color scheme. */
private val BrandSeed = Color(0xFFD2DA40)

@Composable
fun DomainManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        useDynamicColor && supportsDynamic && darkTheme -> dynamicDarkColorScheme(context)
        useDynamicColor && supportsDynamic -> dynamicLightColorScheme(context)
        // Dynamic color off (or unavailable): full Material3 scheme generated from
        // the brand seed, so secondary/tertiary/surfaces stay harmonized with it.
        else -> rememberDynamicColorScheme(seedColor = BrandSeed, isDark = darkTheme, isAmoled = false)
    }
    MaterialExpressiveTheme(colorScheme = colorScheme, content = content)
}
