package com.skofqq.domainmanager.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * Debug-only harness: loops the splash intro animation, alternating dark/light
 * theme on each pass. Launch with:
 * adb shell am start -n com.skofqq.domainmanager/.ui.SplashPreviewActivity
 */
class SplashPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            var iteration by remember { mutableIntStateOf(0) }
            key(iteration) {
                SplashIntroScreen(
                    darkTheme = iteration % 2 == 0,
                    onFinished = {},
                )
            }
            LaunchedEffect(iteration) {
                delay(2600)
                iteration++
            }
        }
    }
}
