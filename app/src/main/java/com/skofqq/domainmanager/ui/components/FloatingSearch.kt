package com.skofqq.domainmanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.ui.theme.Helm
import com.skofqq.domainmanager.ui.theme.Tint

/** Height the floating search takes over the list (pill + its margins), for the list's bottom padding. */
val FloatingSearchHeight = 76.dp

/**
 * The page's search as a liquid-glass pill floating over the bottom of the list, right above the bottom
 * navigation bar, with the page's [actions] beside it as [GlassAction]s (Boxy panel style). The list fades
 * into the page under it, and it rises with the keyboard. [note] is an optional hint line above the pill.
 */
@Composable
fun FloatingSearch(
    query: String,
    onQuery: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    note: String? = null,
    onDone: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val page = Helm.colors.page
    val bottom = LocalBottomBarPadding.current ?: 0.dp
    // With the keyboard open the bar sits right on it: the keyboard covers the room kept for the bottom bar,
    // so the larger of the two is used rather than their sum.
    val density = LocalDensity.current
    val root = LocalView.current.rootView
    // How far the bottom edge of this block is from the bottom of the window.
    var below by remember { mutableIntStateOf(0) }
    val keyboard = with(density) { (WindowInsets.ime.getBottom(density) - below).coerceAtLeast(0).toDp() }
    Column(
        modifier
            .onGloballyPositioned { c ->
                below = (root.height - (c.positionInWindow().y + c.size.height)).toInt().coerceAtLeast(0)
            }
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to page.copy(alpha = 0f), 0.22f to page.copy(alpha = 0.9f), 0.4f to page, 1f to page))
            .padding(top = 20.dp, bottom = maxOf(bottom - 8.dp, keyboard)),
    ) {
        AnimatedVisibility(
            visible = note != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Text(
                note.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = Helm.colors.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Helm.colors.card)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassSurface(
                RoundedCornerShape(50),
                Helm.colors.card,
                28.dp,
                Modifier.weight(1f).fillMaxHeight().floating(),
                element = true,
            ) {
                Row(
                    Modifier.fillMaxSize().padding(start = 18.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Search, null, Modifier.size(20.dp), tint = Helm.colors.text2)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(hint, color = Helm.colors.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = onQuery,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Helm.colors.text),
                            cursorBrush = SolidColor(Helm.colors.accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Uri),
                            keyboardActions = KeyboardActions(onDone = { onDone() }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (query.isNotEmpty()) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(50)).clickable(role = Role.Button) { onQuery("") },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Close, stringResource(R.string.clear_search), Modifier.size(20.dp), tint = Helm.colors.text2)
                        }
                    }
                }
            }
            actions()
        }
    }
}

/** Lifts a pill off the list like the bottom navigation bar: a soft shadow and a thin outline. */
@Composable
private fun Modifier.floating(): Modifier {
    val pill = RoundedCornerShape(50)
    return shadow(10.dp, pill, clip = false).border(1.dp, Helm.colors.outline, pill)
}

/**
 * Round glass button the height of the floating search. [accent] fills it with the accent colour
 * (the main action, e.g. "add"); [busy] swaps the icon for the expressive loading indicator.
 */
@Composable
fun GlassAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    busy: Boolean = false,
    accent: Boolean = false,
    size: Dp = 56.dp,
) {
    val tint = if (accent) Tint(Helm.colors.onBrand, Helm.colors.brand) else Tint(Helm.colors.text, Helm.colors.card)
    GlassSurface(
        RoundedCornerShape(50),
        tint.bg,
        size / 2,
        Modifier.size(size).floating().clip(RoundedCornerShape(50))
            .clickable(enabled = enabled && !busy, role = Role.Button, onClick = withTap(onClick)),
        element = true,
        tintAlpha = if (accent) 0.92f else null,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (busy) {
                androidx.compose.material3.LoadingIndicator(Modifier.size(30.dp), color = tint.fg)
            } else {
                Icon(icon, description, Modifier.size(24.dp), tint = tint.fg.copy(alpha = if (enabled) 1f else 0.35f))
            }
        }
    }
}
