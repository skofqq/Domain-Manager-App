package com.skofqq.domainmanager.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** A foreground colour with its soft background, as used by Boxy badges, icon tiles and action pills. */
@Immutable
data class Tint(val fg: Color, val bg: Color)

object Tints {
    private fun pair(light: Color, dark: Color, isDark: Boolean): Tint {
        val fg = if (isDark) dark else light
        return Tint(fg, fg.copy(alpha = if (isDark) 0.18f else 0.13f))
    }

    val green @Composable get() = pair(Color(0xFF1F883D), Color(0xFF3FB950), Helm.colors.isDark)
    val red @Composable get() = pair(Color(0xFFCF222E), Color(0xFFF85149), Helm.colors.isDark)
    val amber @Composable get() = pair(Color(0xFFBF8700), Color(0xFFD29922), Helm.colors.isDark)
    val blue @Composable get() = pair(Color(0xFF0969DA), Color(0xFF58A6FF), Helm.colors.isDark)
    val teal @Composable get() = pair(Color(0xFF0E9F8E), Color(0xFF2FD1B9), Helm.colors.isDark)
    val purple @Composable get() = pair(Color(0xFF8250DF), Color(0xFFBC8CFF), Helm.colors.isDark)
    val orange @Composable get() = pair(Color(0xFFD1602A), Color(0xFFF0883E), Helm.colors.isDark)
    val gray @Composable get() = pair(Color(0xFF57606A), Color(0xFFB4BCC8), Helm.colors.isDark)

    /** The accent of the current palette as a tint pair. */
    /** Brand pair: readable accent text on a soft logo-lime wash. */
    val accent @Composable get() = Tint(Helm.colors.accent, Helm.colors.brand.copy(alpha = if (Helm.colors.isDark) 0.18f else 0.32f))
}
