package com.skofqq.domainmanager.ui

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * True when the system "Remove animations" accessibility setting is on
 * (Developer options → Animator duration scale = 0, or Settings →
 * Accessibility → Remove animations, which writes the same value).
 *
 * Compose honors this automatically for one-shot transitions, but not for
 * `rememberInfiniteTransition` — an indefinite loop keeps running at full
 * amplitude and is exactly the motion this setting exists to stop. Anything
 * built on an infinite transition has to check this itself.
 */
@Composable
fun reduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}
