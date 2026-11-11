package com.skofqq.domainmanager.ui.components

import androidx.compose.material.icons.filled.Bolt
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.graphics.shapes.RoundedPolygon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skofqq.domainmanager.ui.reduceMotion
import com.skofqq.domainmanager.ui.theme.Helm
import com.skofqq.domainmanager.ui.theme.Tint
import com.skofqq.domainmanager.ui.theme.Tints

// Expressive building blocks of the Helm design system (Boxy palette + Material 3 Expressive
// shapes and motion). Sections are flat 24 dp cards; their identity comes from a shaped,
// tinted icon (MaterialShapes cookies, flowers, …) rather than from colour floods.

/** Icon inside a Material Expressive polygon (cookie, flower, sunny…) on the soft [tint] background. */
@Composable
fun ShapeIcon(
    icon: ImageVector,
    tint: Tint,
    modifier: Modifier = Modifier,
    shape: RoundedPolygon = MaterialShapes.Cookie9Sided,
    size: Dp = 44.dp,
) {
    Box(
        modifier.size(size).clip(shape.toShape()).background(tint.bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(size * 0.5f), tint = tint.fg)
    }
}

/**
 * Section card: shaped icon + title (+ subtitle) header with an optional trailing slot,
 * then the section body. Meant for full-width use inside a padded list (no own side margin).
 */
@Composable
fun HelmSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    tint: Tint = Tints.accent,
    shape: RoundedPolygon = MaterialShapes.Cookie9Sided,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Helm.colors.card)
            .padding(vertical = 16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                ShapeIcon(icon, tint, shape = shape)
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = Helm.colors.text)
                if (!subtitle.isNullOrEmpty()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Helm.colors.text2)
                }
            }
            if (trailing != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), content = trailing)
            }
        }
        Spacer(Modifier.size(14.dp))
        Column(
            Modifier.fillMaxWidth().padding(horizontal = contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** Visual weight of a [HelmButton]. */
enum class ButtonStyle { Primary, Tonal, Danger, Success, Neutral }

/**
 * Expressive button: Material 3 shape morph on press (round → squircle), Boxy colours,
 * built-in busy state that swaps the icon for a spinner.
 */
@Composable
fun HelmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    val (bg, fg) = when (style) {
        ButtonStyle.Primary -> Helm.colors.brand to Helm.colors.onBrand
        ButtonStyle.Tonal -> Tints.accent.bg to Tints.accent.fg
        ButtonStyle.Danger -> Tints.red.bg to Tints.red.fg
        ButtonStyle.Success -> Tints.green.bg to Tints.green.fg
        ButtonStyle.Neutral -> Helm.colors.surface2 to Helm.colors.text
    }
    Button(
        onClick = withTap(onClick),
        enabled = enabled && !loading,
        modifier = modifier.heightIn(min = 48.dp),
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.buttonColors(
            containerColor = bg,
            contentColor = fg,
            disabledContainerColor = if (loading) bg else bg.copy(alpha = bg.alpha * 0.4f).let { if (style == ButtonStyle.Primary) Helm.colors.surface2 else it },
            disabledContentColor = if (loading) fg else Helm.colors.text2.copy(alpha = 0.6f),
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = fg)
            Spacer(Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Pill chip: accent-tinted with a check when [selected], neutral otherwise. */
@Composable
fun HelmChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val bg by animateColorAsState(if (selected) Tints.accent.bg else Helm.colors.surface2, label = "chipBg")
    val fg by animateColorAsState(if (selected) Helm.colors.accent else Helm.colors.text, label = "chipFg")
    Row(
        modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(enabled = enabled, role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val lead = if (selected) Icons.Filled.Check else icon
        if (lead != null) {
            Icon(lead, null, Modifier.size(16.dp), tint = fg)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = fg.copy(alpha = if (enabled) 1f else 0.4f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Wrapping row of [HelmChip]s. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipFlow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

/** Single-line scrolling row of chips (for long, unbounded lists like proxy groups). */
@Composable
fun ChipScroller(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** Indeterminate wavy progress, shown while a long router call runs. */
@Composable
fun WorkingStrip(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        LinearWavyProgressIndicator(modifier.fillMaxWidth().padding(vertical = 4.dp), color = Helm.colors.accent, trackColor = Helm.colors.surface2)
    }
}

/** Big centred number with a caption (speed test result, counters). */
@Composable
fun BigStat(value: String, label: String, modifier: Modifier = Modifier, tint: Tint = Tints.accent) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(tint.bg).padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = tint.fg)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Helm.colors.text2, textAlign = TextAlign.Center)
    }
}

/** Compact metric tile: shaped icon, value and caption on the secondary surface. Use in a 2-column grid. */
@Composable
fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Tint,
    modifier: Modifier = Modifier,
    shape: RoundedPolygon = MaterialShapes.Cookie6Sided,
    monospace: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Helm.colors.surface2)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ShapeIcon(icon, tint, shape = shape, size = 36.dp)
        Column {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = if (monospace) FontFamily.Monospace else null,
                color = Helm.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(label, style = MaterialTheme.typography.bodySmall, color = Helm.colors.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Two [StatTile]-like cells per row. */
@Composable
fun TileGrid(items: List<@Composable (Modifier) -> Unit>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { it(Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * Row inside a section: optional leading slot, title + supporting text, optional trailing slot.
 * The bread-and-butter list row of section cards.
 */
@Composable
fun SectionRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    monospaceSubtitle: Boolean = false,
    titleColor: Color = Helm.colors.text,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = titleColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = if (monospaceSubtitle) FontFamily.Monospace else null,
                    color = Helm.colors.text2,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

/** Monospace output block (logs, traceroute, config snippets) on the secondary surface. */
@Composable
fun MonoBlock(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Helm.colors.surface2).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content,
    )
}

/** Hairline divider between rows inside a section body. */
@Composable
fun SectionDivider() {
    androidx.compose.material3.HorizontalDivider(color = Helm.colors.outline)
}

/** Secondary explanatory text inside a section. */
@Composable
fun SectionNote(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodyMedium, color = Helm.colors.text2)
}

/**
 * Empty / not-installed state: a slowly turning expressive shape behind the icon (still when the
 * system "remove animations" setting is on), title, body and an optional action.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String? = null,
    modifier: Modifier = Modifier,
    tint: Tint = Tints.accent,
    shape: RoundedPolygon = MaterialShapes.Cookie12Sided,
    action: (@Composable () -> Unit)? = null,
) {
    val still = reduceMotion()
    val turn = rememberInfiniteTransition(label = "empty")
    val angle by turn.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "emptyAngle",
    )
    Column(
        modifier.fillMaxWidth().clip(CardShape).background(Helm.colors.card).padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(96.dp)
                    .graphicsLayer { rotationZ = if (still) 0f else angle }
                    .clip(shape.toShape())
                    .background(tint.bg),
            )
            Icon(icon, null, Modifier.size(40.dp), tint = tint.fg)
        }
        Spacer(Modifier.size(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = Helm.colors.text, textAlign = TextAlign.Center)
        if (body != null) {
            Spacer(Modifier.size(6.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Helm.colors.text2, textAlign = TextAlign.Center)
        }
        if (action != null) {
            Spacer(Modifier.size(18.dp))
            action()
        }
    }
}

/** Small "value pill" for the trailing side of a row (latency, counts, sizes). */
@Composable
fun ValuePill(text: String, tint: Tint, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(tint.bg).padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = tint.fg, maxLines = 1, fontSize = 12.sp)
    }
}

/** Label on the left, value on the right, for facts inside a section body. */
@Composable
fun KeyValueRow(label: String, value: String, modifier: Modifier = Modifier, monospace: Boolean = false, valueColor: Color = Helm.colors.text) {
    Row(modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Helm.colors.text2)
        Spacer(Modifier.width(16.dp))
        Text(
            value,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            fontFamily = if (monospace) FontFamily.Monospace else null,
            color = valueColor,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Determinate wavy progress with a label and percentage above it (bootstrap, downloads). */
@Composable
fun LabeledProgress(label: String, valueText: String, progress: Float, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        KeyValueRow(label, valueText)
        LinearWavyProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = Helm.colors.accent,
            trackColor = Helm.colors.surface2,
        )
    }
}

/** Round tonal icon button for section headers (add, refresh, …), with the expressive press morph. */
@Composable
fun SectionIconButton(icon: ImageVector, contentDescription: String?, enabled: Boolean = true, tint: Tint = Tints.accent, onClick: () -> Unit) {
    androidx.compose.material3.FilledTonalIconButton(
        onClick = withTap(onClick),
        enabled = enabled,
        shapes = androidx.compose.material3.IconButtonDefaults.shapes(),
        colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(containerColor = tint.bg, contentColor = tint.fg),
    ) {
        Icon(icon, contentDescription)
    }
}

/** Card that opens another page: shaped tinted icon, title, summary, chevron (or a custom trailing slot). */
@Composable
fun EntryCard(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Tint = Tints.accent,
    shape: RoundedPolygon = MaterialShapes.Cookie9Sided,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Helm.colors.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShapeIcon(icon, tint, shape = shape)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Helm.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Helm.colors.text2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        if (trailing != null) trailing() else Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            Modifier.size(22.dp),
            tint = Helm.colors.text2.copy(alpha = 0.75f),
        )
    }
}

/**
 * Number stepper: tonal −/+ buttons with the expressive press morph around a value that rolls
 * up or down when it changes. [changed] tints the value with the accent (pending, not applied yet).
 */
@Composable
fun Stepper(
    value: Int,
    onChange: (Int) -> Unit,
    min: Int,
    max: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    changed: Boolean = false,
    decreaseLabel: String? = null,
    increaseLabel: String? = null,
) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(Helm.colors.surface2).padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(androidx.compose.material.icons.Icons.Filled.Remove, decreaseLabel, enabled && value > min) { onChange((value - 1).coerceAtLeast(min)) }
        androidx.compose.animation.AnimatedContent(
            targetState = value,
            transitionSpec = {
                val up = targetState > initialState
                (androidx.compose.animation.slideInVertically { if (up) it else -it } + fadeIn()) togetherWith
                    (androidx.compose.animation.slideOutVertically { if (up) -it else it } + fadeOut())
            },
            label = "stepper",
            modifier = Modifier.width(40.dp),
        ) { v ->
            Text(
                v.toString(),
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (changed) Helm.colors.accent else Helm.colors.text,
                textAlign = TextAlign.Center,
            )
        }
        StepButton(androidx.compose.material.icons.Icons.Filled.Add, increaseLabel, enabled && value < max) { onChange((value + 1).coerceAtMost(max)) }
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String?, enabled: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.FilledIconButton(
        onClick = withTap(onClick),
        enabled = enabled,
        shapes = androidx.compose.material3.IconButtonDefaults.shapes(),
        colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
            containerColor = Helm.colors.card,
            contentColor = Helm.colors.text,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Helm.colors.text2.copy(alpha = 0.35f),
        ),
        modifier = Modifier.size(40.dp),
    ) {
        Icon(icon, description, Modifier.size(20.dp))
    }
}

/**
 * Card that unfolds in place: shaped icon, title, summary, optional trailing pill and a chevron
 * that turns with a spring; the body expands with the expressive spatial motion.
 */
@Composable
fun ExpandableCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    tint: Tint = Tints.accent,
    shape: RoundedPolygon = MaterialShapes.Cookie9Sided,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val turn by androidx.compose.animation.core.animateFloatAsState(
        if (expanded) 90f else 0f,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "chevron",
    )
    Column(modifier.fillMaxWidth().clip(CardShape).background(Helm.colors.card)) {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onToggle).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                ShapeIcon(icon, tint, shape = shape)
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Helm.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!subtitle.isNullOrEmpty()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Helm.colors.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                Modifier.size(22.dp).graphicsLayer { rotationZ = turn },
                tint = Helm.colors.text2.copy(alpha = 0.75f),
            )
        }
        AnimatedVisibility(
            expanded,
            enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
}

/** The palette cycled through for lists of equal items (groups, devices), so neighbours differ. */
@Composable
fun cycleTint(index: Int): Tint = when (index % 6) {
    0 -> Tints.blue
    1 -> Tints.purple
    2 -> Tints.teal
    3 -> Tints.orange
    4 -> Tints.green
    else -> Tints.amber
}

/** Expressive shapes cycled the same way as [cycleTint]. */
fun cycleShape(index: Int): RoundedPolygon = when (index % 6) {
    0 -> MaterialShapes.Cookie9Sided
    1 -> MaterialShapes.Clover4Leaf
    2 -> MaterialShapes.Sunny
    3 -> MaterialShapes.Cookie6Sided
    4 -> MaterialShapes.Flower
    else -> MaterialShapes.Cookie12Sided
}

/** Wizard progress: one capsule per step; the current one stretches, done ones fill with the accent. */
@Composable
fun StepIndicator(current: Int, count: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..count) {
            val weight by androidx.compose.animation.core.animateFloatAsState(
                if (i == current) 3f else 1f,
                MaterialTheme.motionScheme.defaultSpatialSpec(),
                label = "stepWeight",
            )
            val color by animateColorAsState(
                if (i <= current) Helm.colors.accent else Helm.colors.surface2,
                label = "stepColor",
            )
            Box(Modifier.weight(weight).height(8.dp).clip(RoundedCornerShape(50)).background(color))
        }
    }
}

/**
 * One slice of a section card split across several lazy items (long, live lists): the first slice
 * gets the top corners, the last one the bottom corners, the middle ones none, so the slices read
 * as one card while each row stays a separate lazy item.
 */
fun Modifier.sectionSlice(first: Boolean, last: Boolean, color: Color): Modifier {
    val r = 24.dp
    val shape = RoundedCornerShape(
        topStart = if (first) r else 0.dp,
        topEnd = if (first) r else 0.dp,
        bottomStart = if (last) r else 0.dp,
        bottomEnd = if (last) r else 0.dp,
    )
    return this.fillMaxWidth().clip(shape).background(color)
}

/** Header of a section card on its own (for sliced sections): shaped icon, title, subtitle, trailing. */
@Composable
fun HelmSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    tint: Tint = Tints.accent,
    shape: RoundedPolygon = MaterialShapes.Cookie9Sided,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            ShapeIcon(icon, tint, shape = shape)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Helm.colors.text)
            if (!subtitle.isNullOrEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Helm.colors.text2)
            }
        }
        if (trailing != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), content = trailing)
        }
    }
}

/** Two-state segmented icon switch (list / grid and the like) on the secondary surface. */
@Composable
fun IconToggle(
    first: ImageVector,
    second: ImageVector,
    secondSelected: Boolean,
    onChange: (Boolean) -> Unit,
    firstLabel: String,
    secondLabel: String,
) {
    Row(Modifier.clip(RoundedCornerShape(50)).background(Helm.colors.surface2).padding(3.dp)) {
        listOf(false to (first to firstLabel), true to (second to secondLabel)).forEach { (value, pair) ->
            val selected = secondSelected == value
            val bg by animateColorAsState(if (selected) Helm.colors.brand else Color.Transparent, label = "toggleBg")
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(50)).background(bg)
                    .clickable(role = Role.RadioButton) { onChange(value) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(pair.first, pair.second, Modifier.size(18.dp), tint = if (selected) Helm.colors.onBrand else Helm.colors.text2)
            }
        }
    }
}

/**
 * Latency pill that is also the button to re-measure it: "123 ms" tinted green/amber/red,
 * a red "timeout" for 0, a neutral "test" prompt when there is no figure yet, and an
 * expressive loading indicator while a test runs.
 */
@Composable
fun DelayPill(ms: Int?, testing: Boolean, onTest: () -> Unit, modifier: Modifier = Modifier) {
    val tint = when {
        ms == null -> Tint(Helm.colors.text2, Helm.colors.surface2)
        ms == 0 -> Tints.red
        ms < 150 -> Tints.green
        ms < 400 -> Tints.amber
        else -> Tints.red
    }
    val label = when {
        ms == null -> androidx.compose.ui.res.stringResource(com.skofqq.domainmanager.R.string.delay_test)
        ms == 0 -> androidx.compose.ui.res.stringResource(com.skofqq.domainmanager.R.string.delay_timeout)
        else -> androidx.compose.ui.res.stringResource(com.skofqq.domainmanager.R.string.ms_value, ms)
    }
    val bg by animateColorAsState(tint.bg, label = "delayBg")
    Row(
        modifier
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(enabled = !testing, role = Role.Button, onClickLabel = androidx.compose.ui.res.stringResource(com.skofqq.domainmanager.R.string.delay_test), onClick = onTest)
            .padding(start = 8.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            if (testing) {
                androidx.compose.material3.LoadingIndicator(Modifier.size(22.dp), color = tint.fg)
            } else {
                Icon(Icons.Filled.Bolt, null, Modifier.size(16.dp), tint = tint.fg)
            }
        }
        Spacer(Modifier.width(4.dp))
        androidx.compose.animation.AnimatedContent(targetState = label, label = "delayText") { text ->
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = tint.fg, maxLines = 1)
        }
    }
}

/**
 * Pull-to-refresh with the Material 3 Expressive morphing loading indicator in a card-coloured
 * puck instead of the plain spinner. Same parameters as PullToRefreshBox.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HelmPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val state = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.LoadingIndicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = Helm.colors.card,
                color = Helm.colors.accent,
            )
        },
        content = content,
    )
}
