package com.skofqq.domainmanager.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skofqq.domainmanager.ui.theme.Helm

private val SheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

/**
 * Bottom sheet in the Boxy liquid-glass style: a translucent page-coloured sheet with a light rim along its
 * rounded top, over the blurred app. Same parameters as Material's ModalBottomSheet (drop-in).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelmBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val glass = LocalGlass.current
    val blurBehind = glass.enabled && glass.sheetBlur && Build.VERSION.SDK_INT >= 31
    DisposableEffect(Unit) {
        SheetBlurState.open++
        onDispose { SheetBlurState.open-- }
    }
    val frosted = glass.enabled && glass.elements && Build.VERSION.SDK_INT >= 31
    val rim = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = if (Helm.colors.isDark) 0.22f else 0.75f), Color.White.copy(alpha = 0f)),
        endY = 160f,
    )
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = SheetShape,
        containerColor = when {
            frosted -> Helm.colors.page.copy(alpha = if (glass.translucent) 0.8f else 0.9f)
            blurBehind -> Helm.colors.page.copy(alpha = 0.9f)
            else -> Helm.colors.page
        },
        contentColor = Helm.colors.text,
        scrimColor = if (blurBehind) Color.Black.copy(alpha = 0.2f) else BottomSheetDefaults.ScrimColor,
        // The drag handle is drawn inside the rim below instead.
        dragHandle = null,
    ) {
        // The rim lives inside the sheet's own content, together with the handle. Passed
        // as ModalBottomSheet's modifier it landed on the full-screen container instead
        // of the sheet surface and drew a thin light line along the top of the screen.
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (frosted) Modifier.border(1.dp, rim, SheetShape) else Modifier),
        ) {
            BottomSheetDefaults.DragHandle(Modifier.align(Alignment.CenterHorizontally))
            content()
        }
    }
}

/** Card-like group inside a sheet. */
@Composable
fun SheetGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Helm.colors.card),
        content = content,
    )
}

/** Two Boxy buttons at the bottom of a sheet or form: neutral secondary, accent primary. */
@Composable
fun SheetButtons(
    primary: String,
    onPrimary: () -> Unit,
    secondary: String? = null,
    onSecondary: () -> Unit = {},
    primaryEnabled: Boolean = true,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (secondary != null) {
            SheetButton(secondary, Helm.colors.card, Helm.colors.text, Modifier.weight(1f), true, onSecondary)
        }
        SheetButton(primary, Helm.colors.brand, Helm.colors.onBrand, Modifier.weight(1f), primaryEnabled, onPrimary)
    }
}

@Composable
fun SheetButton(text: String, bg: Color, fg: Color, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = 50.dp).clip(RoundedCornerShape(18.dp))
            .background(if (enabled) bg else bg.copy(alpha = 0.4f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

/** Vertical breathing room between sheet sections. */
@Composable
fun SheetSpacer() = Spacer(Modifier.height(16.dp))

/**
 * Boxy action pill: soft [tint] background with the tint as text (Start = green, Stop = red, …).
 * 44 dp tall so it keeps the minimum touch target.
 */
@Composable
fun TintButton(
    text: String,
    tint: com.skofqq.domainmanager.ui.theme.Tint,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) tint.bg else tint.bg.copy(alpha = tint.bg.alpha * 0.5f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (enabled) tint.fg else tint.fg.copy(alpha = 0.45f)
        if (icon != null) {
            androidx.compose.material3.Icon(icon, null, Modifier.padding(end = 6.dp).size(18.dp), tint = fg)
        }
        Text(text, color = fg, fontWeight = FontWeight.SemiBold, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
    }
}

/** Status pill: a dot (or [icon]) and a label on the soft background of [color]. */
@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = if (Helm.colors.isDark) 0.18f else 0.12f))
            .padding(start = 9.dp, end = 11.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            androidx.compose.material3.Icon(icon, null, Modifier.size(14.dp), tint = color)
        } else {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(color))
        }
        androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
        Text(text, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color, maxLines = 1)
    }
}
