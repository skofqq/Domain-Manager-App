package com.skofqq.domainmanager.ui.components

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.ui.theme.Helm

/**
 * Room the floating bottom bar takes at the bottom of the screen (bar + its margin + the system
 * navigation bar). Null outside the main tabs, where only the system navigation bar is padded.
 */
val LocalBottomBarPadding = compositionLocalOf<Dp?> { null }

/** Compact glass "← Back" pill used on pages opened from another page. */
@Composable
fun BackPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassSurface(RoundedCornerShape(50), Helm.colors.card, 18.dp, modifier.padding(start = 12.dp, top = 8.dp), element = true) {
        Row(
            Modifier.clickable(onClick = onClick, role = Role.Button).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp), tint = Helm.colors.text)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.back), style = MaterialTheme.typography.labelLarge, color = Helm.colors.text)
        }
    }
}

/** Back pill (when [onBack] is set), then the large page title with actions on the same row. */
@Composable
fun SubPageHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(Modifier.fillMaxWidth().background(Helm.colors.page).statusBarsPadding()) {
        if (onBack != null) BackPill(onBack)
        PageHeader(title, subtitle, actions = actions)
    }
}

/**
 * Page in the Boxy layout: page background, back pill + large title header, content below it.
 * Bottom padding leaves room for the floating bottom bar on the main tabs.
 */
@Composable
fun HelmScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    header: (@Composable () -> Unit)? = null,
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val bar = LocalBottomBarPadding.current
    val insets = if (bar != null) WindowInsets(bottom = bar) else WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
    Scaffold(
        modifier = modifier,
        topBar = header ?: { SubPageHeader(title, subtitle, onBack, actions) },
        floatingActionButton = floatingActionButton,
        snackbarHost = snackbarHost,
        containerColor = Helm.colors.page,
        contentColor = Helm.colors.text,
        contentWindowInsets = insets,
    ) { padding -> content(padding) }
}

/** Search field on the page background: card-coloured pill with a search icon and a clear button. */
@Composable
fun PageSearchField(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    container: Color = Helm.colors.card,
) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(container).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, Modifier.size(20.dp), tint = Helm.colors.text2)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, color = Helm.colors.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicTextField(
                value,
                onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Helm.colors.text),
                cursorBrush = SolidColor(Helm.colors.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(50)).clickable(role = Role.Button) { onChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, stringResource(R.string.cancel), Modifier.size(20.dp), tint = Helm.colors.text2)
            }
        }
    }
}

/**
 * Text field in the Boxy rounded style: filled secondary surface, 16 dp corners, no underline.
 * Same parameters as Material's OutlinedTextField, so it is a drop-in replacement.
 */
@Composable
fun HelmTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null,
    suffix: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    container: Color = Helm.colors.surface2,
) {
    val c = Helm.colors
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            disabledContainerColor = container.copy(alpha = 0.6f),
            errorContainerColor = MaterialTheme.colorScheme.errorContainer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            focusedLabelColor = c.accent,
            unfocusedLabelColor = c.text2,
            cursorColor = c.accent,
            focusedTextColor = c.text,
            unfocusedTextColor = c.text,
            focusedPlaceholderColor = c.text2,
            unfocusedPlaceholderColor = c.text2,
        ),
    )
}

/**
 * Segmented pill tabs in the Boxy style: a card-coloured capsule with a highlight that slides
 * with [selected] (fractional while a pager is swiped) — the same motion as the bottom bar.
 */
@Composable
fun PillTabs(
    labels: List<String>,
    selected: Float,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = Helm.colors.card,
) {
    val select = withTap1(onSelect)
    val colors = Helm.colors
    val shape = RoundedCornerShape(50)
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier.fillMaxWidth().height(48.dp).clip(shape).background(container).padding(4.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
    ) {
        val itemWidth = maxWidth / labels.size
        val position by androidx.compose.animation.core.animateFloatAsState(
            selected,
            androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 500f),
            label = "pillTab",
        )
        Box(
            Modifier
                .offset(x = itemWidth * position)
                .width(itemWidth)
                .fillMaxHeight()
                .clip(shape)
                .background(colors.brand.copy(alpha = if (colors.isDark) 0.22f else 0.38f)),
        )
        Row(Modifier.fillMaxSize()) {
            labels.forEachIndexed { index, label ->
                val active = kotlin.math.abs(position - index) < 0.5f
                val tint by androidx.compose.animation.animateColorAsState(if (active) colors.accent else colors.text2, label = "pillTint")
                Box(
                    Modifier
                        .width(itemWidth)
                        .fillMaxHeight()
                        .clip(shape)
                        .clickable(enabled = enabled, role = Role.Tab) { select(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) androidx.compose.ui.text.font.FontWeight.SemiBold else null,
                        color = tint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Glass pill with a leading icon, a label and an optional trailing chevron (router switcher, filters). */
@Composable
fun GlassPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailing: androidx.compose.ui.graphics.vector.ImageVector? = null,
    supporting: String? = null,
) {
    GlassSurface(RoundedCornerShape(50), Helm.colors.card, 22.dp, modifier, element = true) {
        Row(
            Modifier.clickable(onClick = onClick, role = Role.Button)
                .padding(start = if (leading != null) 12.dp else 16.dp, end = if (trailing != null) 10.dp else 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Icon(leading, null, Modifier.size(20.dp), tint = Helm.colors.accent)
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f, fill = false)) {
                Text(text, style = MaterialTheme.typography.labelLarge, color = Helm.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (supporting != null) {
                    Text(supporting, style = MaterialTheme.typography.labelMedium, color = Helm.colors.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(4.dp))
                Icon(trailing, null, Modifier.size(20.dp), tint = Helm.colors.text2)
            }
        }
    }
}
