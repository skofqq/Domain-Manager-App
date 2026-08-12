package com.skofqq.domainmanager.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp

/**
 * First-load skeletons ("cache empty" only — refreshes of already-shown data keep
 * the regular pull-to-refresh indicator so content never blinks back to bones).
 *
 * One brush per screen section, shared by its boxes, so the sweep is synchronized.
 * Pacing is tuned to the app's expressive motion feel — a ~1.2 s accelerate/
 * decelerate pass with a short rest between passes. MotionScheme springs are
 * physics-based one-shots and cannot drive an infinite loop, so the sweep
 * approximates their character with an eased tween instead of a generic
 * constant-speed shimmer.
 *
 * With the system "remove animations" setting on, the sweep is dropped entirely
 * and the placeholders render as flat blocks — an indefinite loop is precisely
 * the motion that setting is meant to stop, and Compose does not suppress
 * infinite transitions on its own.
 */
@Composable
fun shimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    if (reduceMotion()) return SolidColor(base)
    val transition = rememberInfiniteTransition(label = "skeleton-shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, delayMillis = 250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )
    // Local (per-box) coordinates: every placeholder sweeps in phase.
    val sweepWidth = 480f
    val start = progress * (1080f + sweepWidth) - sweepWidth
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(start, 0f),
        end = Offset(start + sweepWidth, sweepWidth * 0.35f),
    )
}

@Composable
fun SkeletonBox(
    brush: Brush,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    Box(modifier = modifier.clip(shape).background(brush))
}

/** Routing row placeholder: domain bar + two indicator dots + action circle. */
@Composable
fun SkeletonDomainRow(brush: Brush) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SkeletonBox(brush, Modifier.weight(1f).height(16.dp))
            SkeletonBox(brush, Modifier.size(14.dp), CircleShape)
            SkeletonBox(brush, Modifier.size(14.dp), CircleShape)
            SkeletonBox(brush, Modifier.size(24.dp), CircleShape)
        }
    }
}

/** Strategies row placeholder: domain bar + strategy badge pill + delete circle. */
@Composable
fun SkeletonStrategyRow(brush: Brush) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 8.dp)
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SkeletonBox(brush, Modifier.weight(1f).height(16.dp))
            SkeletonBox(brush, Modifier.width(34.dp).height(22.dp), CircleShape)
            SkeletonBox(brush, Modifier.size(24.dp), CircleShape)
        }
    }
}

/** zapret2 profile row placeholder: name + badge caption bar, stepper on the right. */
@Composable
fun SkeletonProfileRow(brush: Brush) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 8.dp)
                .height(60.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonBox(brush, Modifier.fillMaxWidth(0.6f).height(14.dp))
                SkeletonBox(brush, Modifier.fillMaxWidth(0.4f).height(10.dp))
            }
            SkeletonBox(brush, Modifier.size(32.dp), CircleShape)
            SkeletonBox(brush, Modifier.width(20.dp).height(14.dp))
            SkeletonBox(brush, Modifier.size(32.dp), CircleShape)
        }
    }
}

/** Status service card placeholder: title, two indicator bars, two action buttons. */
@Composable
fun SkeletonServiceCard(brush: Brush) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SkeletonBox(brush, Modifier.fillMaxWidth(0.4f).height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SkeletonBox(brush, Modifier.width(72.dp).height(12.dp))
                SkeletonBox(brush, Modifier.width(72.dp).height(12.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBox(brush, Modifier.weight(1f).height(40.dp), CircleShape)
                SkeletonBox(brush, Modifier.weight(1f).height(40.dp), CircleShape)
            }
        }
    }
}
