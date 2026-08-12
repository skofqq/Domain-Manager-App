package com.skofqq.domainmanager.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Logo geometry from ic_splash_icon.xml (108x108 viewport).
private const val VIEWPORT = 108f
private const val PATH_NODE_A =
    "M64.6976 52.0998L55.7635 52.0545L55.7937 43.1959C55.8097 38.3093 59.794 34.325 " +
        "64.6816 34.308C69.5681 34.292 73.5251 38.249 73.5855 43.2119C73.5694 48.0985 " +
        "69.5842 52.0838 64.6976 52.0998Z"
private const val PATH_NODE_B =
    "M43.3081 55.6816L52.1667 55.6514L52.1356 64.5091C52.1205 69.3966 48.1362 73.3809 " +
        "43.2477 73.3969C38.3612 73.413 34.4042 69.456 34.3438 64.493C34.3608 59.7583 " +
        "38.4215 55.6976 43.3081 55.6816Z"

// Own-center pivots of the two nodes (viewport units).
private val PIVOT_A = Offset(64.67f, 43.2f)
private val PIVOT_B = Offset(43.25f, 64.54f)

// The nodes sit on the (+1,-1)/(-1,+1) diagonal; 0.7071 = 1/sqrt(2).
private const val DIAGONAL = 0.7071f

/** How far apart (viewport units) and how twisted the nodes start. */
private const val START_OFFSET = 15f
private const val START_ROTATION_DEG = -18f

private val NODE_COLOR = Color(0xFFD2DA40)
private val NODE_FLASH_COLOR = Color(0xFFF4F9B0)
private val BACKGROUND_DARK = Color(0xFF1D1D1F)
private val BACKGROUND_LIGHT = Color(0xFFEFF0E8)

/**
 * Post-splash intro: the two logo halves start slightly apart and twisted, converge
 * with an ease-in, flash accent-colored on contact ("connection established") with a
 * spring overshoot, then settle into the static logo. Runs ~1.4 s total and calls
 * [onFinished] once done.
 *
 * Honors the system "remove animations" setting (animator duration scale = 0):
 * shows the static logo briefly instead of animating.
 */
@Composable
fun SplashIntroScreen(
    darkTheme: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = reduceMotion()

    // 1 = fully apart/twisted, 0 = docked.
    val separation = remember { Animatable(if (reduceMotion) 0f else 1f) }
    // 0..1 flash intensity on contact.
    val pulse = remember { Animatable(0f) }
    // Whole-logo overshoot bounce after docking.
    val logoScale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            delay(500)
        } else {
            separation.animateTo(0f, tween(durationMillis = 620, easing = FastOutSlowInEasing))
            coroutineScope {
                launch {
                    pulse.animateTo(1f, tween(durationMillis = 110, easing = LinearEasing))
                    pulse.animateTo(0f, tween(durationMillis = 500, easing = LinearOutSlowInEasing))
                }
                launch {
                    logoScale.animateTo(1.07f, tween(durationMillis = 110, easing = LinearEasing))
                    logoScale.animateTo(
                        1f,
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    )
                }
            }
            delay(120)
        }
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (darkTheme) BACKGROUND_DARK else BACKGROUND_LIGHT),
        contentAlignment = Alignment.Center,
    ) {
        SplashLogo(
            separation = separation.value,
            pulse = pulse.value,
            logoScale = logoScale.value,
            modifier = Modifier.size(180.dp),
        )
    }
}

@Composable
private fun SplashLogo(
    separation: Float,
    pulse: Float,
    logoScale: Float,
    modifier: Modifier = Modifier,
) {
    val pathA = remember { PathParser().parsePathString(PATH_NODE_A).toPath() }
    val pathB = remember { PathParser().parsePathString(PATH_NODE_B).toPath() }

    Canvas(modifier) {
        val s = size.minDimension / VIEWPORT
        val nodeColor = lerp(NODE_COLOR, NODE_FLASH_COLOR, pulse * 0.65f)

        // Contact flash: a soft radial glow between the halves.
        if (pulse > 0.01f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NODE_COLOR.copy(alpha = 0.55f * pulse), Color.Transparent),
                    center = center,
                    radius = (30f + 18f * pulse) * s,
                ),
                radius = (30f + 18f * pulse) * s,
                center = center,
            )
        }

        val dx = separation * START_OFFSET * DIAGONAL
        val rotation = separation * START_ROTATION_DEG

        withTransform({
            scale(logoScale, logoScale, pivot = center)
            scale(s, s, pivot = Offset.Zero)
        }) {
            // Same rotation for both halves keeps the composition point-symmetric
            // (node B is node A rotated 180° about the logo center) all the way in.
            withTransform({
                translate(left = dx, top = -dx)
                rotate(degrees = rotation, pivot = PIVOT_A)
            }) { drawPath(pathA, nodeColor) }

            withTransform({
                translate(left = -dx, top = dx)
                rotate(degrees = rotation, pivot = PIVOT_B)
            }) { drawPath(pathB, nodeColor) }
        }
    }
}
