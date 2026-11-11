package com.skofqq.domainmanager.ui.theme

import android.os.Build
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.skofqq.domainmanager.ui.components.GlassStyle
import com.skofqq.domainmanager.ui.components.HapticIndication
import com.skofqq.domainmanager.ui.components.LocalGlass

/**
 * Surface palette of the Boxy (BFR) design system: page background, cards on it,
 * secondary surface (icon tiles, fields), hairline outline, text, secondary text, accent.
 */
@Immutable
data class HelmColors(
    val page: Color,
    val card: Color,
    val surface2: Color,
    val outline: Color,
    val text: Color,
    val text2: Color,
    /** Accent for text, icons and outlines: readable on [page] and [card]. */
    val accent: Color,
    /** Brand fill (logo lime) for primary buttons, selected states and highlights. */
    val brand: Color,
    /** Content on [brand]. */
    val onBrand: Color,
    val isDark: Boolean,
)

/** Logo colours: the lime of the icon petals and the ink of its background. */
val BrandLime = Color(0xFFD2DA40)
val BrandInk = Color(0xFF1D1D1F)

private val LightPalette = HelmColors(
    page = Color(0xFFF1F0F7),
    card = Color(0xFFFEFBFF),
    surface2 = Color(0xFFF0F0FA),
    outline = Color.Black.copy(alpha = 0.07f),
    text = Color(0xFF0D1117),
    text2 = Color(0xFF57606A),
    // Logo lime #D2DA40 is too light for text on a light page: the same hue, deepened.
    accent = Color(0xFF5C6500),
    brand = BrandLime,
    onBrand = BrandInk,
    isDark = false,
)

private val DarkPalette = HelmColors(
    page = Color(0xFF0A0C0F),
    card = Color(0xFF1C1F26),
    surface2 = Color(0xFF252930),
    outline = Color.White.copy(alpha = 0.09f),
    text = Color(0xFFF5F7FA),
    text2 = Color(0xFFB4BCC8),
    accent = BrandLime,
    brand = BrandLime,
    onBrand = BrandInk,
    isDark = true,
)

val LocalHelmColors = staticCompositionLocalOf { LightPalette }

/** Entry point to the design system tokens: `Helm.colors.card`, `Helm.colors.text2`, … */
object Helm {
    val colors: HelmColors
        @Composable get() = LocalHelmColors.current
}

/** Boxy type scale: bold large titles, semibold section titles, compact body. */
private val HelmTypography = Typography().let { base ->
    base.copy(
        headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
        headlineMedium = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
        labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    )
}

/** The Boxy palette for a light/dark choice; with dynamic color it is derived from the wallpaper scheme. */
@Composable
fun appPalette(darkTheme: Boolean, useDynamicColor: Boolean, trueBlack: Boolean = false): HelmColors {
    val p = basePalette(darkTheme, useDynamicColor)
    return if (darkTheme && trueBlack) p.copy(page = Color.Black) else p
}

@Composable
private fun basePalette(darkTheme: Boolean, useDynamicColor: Boolean): HelmColors {
    val dynamic = useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    if (!dynamic) return if (darkTheme) DarkPalette else LightPalette
    val context = LocalContext.current
    val scheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    return HelmColors(
        page = scheme.surfaceContainer,
        card = if (darkTheme) scheme.surfaceContainerHigh else scheme.surfaceContainerLowest,
        surface2 = scheme.surfaceContainerHighest,
        outline = scheme.outlineVariant.copy(alpha = 0.5f),
        text = scheme.onSurface,
        text2 = scheme.onSurfaceVariant,
        accent = scheme.primary,
        brand = scheme.primary,
        onBrand = scheme.onPrimary,
        isDark = darkTheme,
    )
}

/**
 * Material color scheme whose surface roles are the Boxy palette, so every stock
 * Material 3 (Expressive) component — buttons, switches, segmented buttons, sheets,
 * text fields, progress indicators — renders in the same system as the custom ones.
 * Used by [DomainManagerTheme] and the Appearance theme previews.
 */
@Composable
fun appColorScheme(darkTheme: Boolean, useDynamicColor: Boolean, trueBlack: Boolean = false): ColorScheme {
    val p = appPalette(darkTheme, useDynamicColor, trueBlack)
    val dynamic = useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current
    val base = when {
        dynamic && darkTheme -> dynamicDarkColorScheme(context)
        dynamic -> dynamicLightColorScheme(context)
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    val accentSoft = p.brand.copy(alpha = if (darkTheme) 0.22f else 0.32f).compositeOver(p.card)
    val red = if (darkTheme) Color(0xFFF85149) else Color(0xFFCF222E)
    return base.copy(
        primary = p.accent,
        onPrimary = if (dynamic) base.onPrimary else if (darkTheme) BrandInk else Color.White,
        primaryContainer = accentSoft,
        onPrimaryContainer = p.accent,
        secondaryContainer = accentSoft,
        onSecondaryContainer = p.text,
        tertiaryContainer = if (dynamic) base.tertiaryContainer else accentSoft,
        background = p.page,
        onBackground = p.text,
        surface = p.page,
        onSurface = p.text,
        surfaceVariant = p.surface2,
        onSurfaceVariant = p.text2,
        surfaceDim = p.page,
        surfaceBright = p.card,
        surfaceContainerLowest = p.card,
        surfaceContainerLow = p.card,
        surfaceContainer = p.card,
        surfaceContainerHigh = if (darkTheme) p.surface2 else p.card,
        surfaceContainerHighest = p.surface2,
        outline = p.text2.copy(alpha = 0.45f).compositeOver(p.card),
        outlineVariant = p.outline.compositeOver(p.card),
        error = red,
        onError = Color.White,
        errorContainer = red.copy(alpha = if (darkTheme) 0.18f else 0.13f).compositeOver(p.card),
        onErrorContainer = red,
    )
}

@Composable
fun DomainManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = false,
    trueBlack: Boolean = false,
    glass: GlassStyle = LocalGlass.current,
    content: @Composable () -> Unit,
) {
    val palette = appPalette(darkTheme, useDynamicColor, trueBlack)
    CompositionLocalProvider(LocalHelmColors provides palette, LocalGlass provides glass) {
        MaterialExpressiveTheme(
            colorScheme = appColorScheme(darkTheme, useDynamicColor, trueBlack),
            typography = HelmTypography,
        ) {
            // Ripple plus a tap vibration for every clickable with the default indication.
            CompositionLocalProvider(
                LocalIndication provides HapticIndication(ripple()),
                content = content,
            )
        }
    }
}
