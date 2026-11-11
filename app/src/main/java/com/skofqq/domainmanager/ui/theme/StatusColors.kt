package com.skofqq.domainmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Semantic status colors for the two states Material3 has no role for: "this is
 * running / reachable / OK" (green) and "this is a favorite" (gold). Everything
 * else — failure, neutral, informational — already maps onto a scheme role
 * (`error`, `outline`, `tertiary`) and must keep using it.
 *
 * Both come in a light- and a dark-surface variant. The single mid-tone
 * `0xFF4CAF50` these replaced was duplicated in eight files and measured 2.5:1
 * against the light surface — below 4.5:1 for the label text it tinted and below
 * 3:1 for the 8dp state dots. The pairs below clear both thresholds in the
 * appearance they're used in.
 */
private val StatusOkLight = Color(0xFF1F883D) // Boxy green (light)
private val StatusOkDark = Color(0xFF3FB950) // Boxy green (dark)

private val StatusFavoriteLight = Color(0xFFBF8700) // Boxy amber (light) — non-text icon tint
private val StatusFavoriteDark = Color(0xFFD29922)

/**
 * Picks the variant by the *rendered* surface rather than the theme flag, so a
 * light-tinted container inside dark mode (or the reverse) still gets a legible
 * tint. Falls back to the theme when the surface sits near the middle.
 */
@Composable
@ReadOnlyComposable
private fun onLightSurface(): Boolean {
    val surface = MaterialTheme.colorScheme.surface
    return if (surface.alpha == 1f) surface.luminance() > 0.5f else !isSystemInDarkTheme()
}

/** Running, reachable, connected, "responds fast". */
val statusOk: Color
    @Composable
    @ReadOnlyComposable
    get() = if (onLightSurface()) StatusOkLight else StatusOkDark

/** Starred device marker — paired with a filled/outlined icon so it is never color-alone. */
val statusFavorite: Color
    @Composable
    @ReadOnlyComposable
    get() = if (onLightSurface()) StatusFavoriteLight else StatusFavoriteDark
