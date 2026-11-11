package com.skofqq.domainmanager.ui.components

import com.skofqq.domainmanager.ui.theme.Tints
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.ui.theme.Helm
import com.skofqq.domainmanager.ui.theme.Tint

/** Corner radius of Boxy cards. */
val CardShape = RoundedCornerShape(24.dp)

/** Colors of a flat Boxy card; [container] overrides the card surface (error/accent tinted cards). */
@Composable
fun helmCardColors(container: Color = Helm.colors.card): CardColors =
    CardDefaults.cardColors(containerColor = container, contentColor = Helm.colors.text)

/** Flat rounded Boxy card (no elevation, card surface on the page background). Drop-in for ElevatedCard/Card. */
@Composable
fun HelmCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardShape,
    colors: CardColors = helmCardColors(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier, shape, colors, CardDefaults.cardElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp, 0.dp), border, content)
}

/** Clickable variant of [HelmCard]. */
@Composable
fun HelmCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardShape,
    colors: CardColors = helmCardColors(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        onClick, modifier, enabled, shape, colors,
        CardDefaults.cardElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp, 0.dp), border, content = content,
    )
}

/** Large page title with an optional summary and trailing actions on the same row. */
@Composable
fun PageHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                color = Helm.colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = Helm.colors.text2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Rounded card with an optional section title and subtitle, like the Boxy settings groups. */
@Composable
fun SectionCard(
    title: String?,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(CardShape)
            .background(Helm.colors.card)
            .padding(vertical = if (title != null) 16.dp else 6.dp),
    ) {
        if (title != null) {
            Column(Modifier.padding(horizontal = 18.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = Helm.colors.text)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Helm.colors.text2)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        content()
    }
}

/** Small uppercase-free section label above a group of cards. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        color = Helm.colors.text2,
    )
}

/** 44 dp rounded tile holding a row icon; [tint] colours it (soft background + foreground). */
@Composable
fun IconTile(icon: ImageVector, tint: Tint? = null, enabled: Boolean = true, modifier: Modifier = Modifier) {
    Box(
        modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(tint?.bg ?: Helm.colors.surface2),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = (tint?.fg ?: Helm.colors.text).copy(alpha = if (enabled) 1f else 0.4f))
    }
}

@Composable
private fun RowTexts(title: String, subtitle: String?, enabled: Boolean, modifier: Modifier) {
    Column(modifier) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = Helm.colors.text.copy(alpha = if (enabled) 1f else 0.45f),
        )
        if (!subtitle.isNullOrEmpty()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Helm.colors.text2.copy(alpha = if (enabled) 1f else 0.45f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Navigation row: icon tile, title, subtitle and a chevron. */
@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    showDivider: Boolean = true,
    tint: Tint? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(icon, tint)
            Spacer(Modifier.width(14.dp))
            RowTexts(title, subtitle, true, Modifier.weight(1f))
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(22.dp),
                    tint = Helm.colors.text2.copy(alpha = 0.75f),
                )
            }
        }
        if (showDivider) RowDivider()
    }
}

/** Switch row with an icon tile. */
@Composable
fun SwitchRow(
    icon: ImageVector?,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    showDivider: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val toggle = withTap1(onCheckedChange)
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) }
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                IconTile(icon, enabled = enabled)
                Spacer(Modifier.width(14.dp))
            }
            RowTexts(title, subtitle, enabled, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Switch(checked = checked, onCheckedChange = toggle, enabled = enabled)
        }
        if (showDivider) RowDivider(if (icon != null) 76.dp else 18.dp)
    }
}

@Composable
fun RowDivider(start: androidx.compose.ui.unit.Dp = 76.dp) {
    HorizontalDivider(Modifier.padding(start = start, end = 18.dp), color = Helm.colors.outline)
}

/** Round glass action button used in page headers. */
@Composable
fun HeaderAction(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color = Helm.colors.text,
    onClick: () -> Unit,
) {
    GlassSurface(RoundedCornerShape(50), Helm.colors.card, 22.dp, Modifier.size(48.dp), element = true) {
        Box(Modifier.matchParentSize().clickable(onClick = onClick, role = Role.Button), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription, Modifier.size(22.dp), tint = tint)
        }
    }
}

data class Choice<T>(val value: T, val title: String, val subtitle: String? = null)

/** Single-choice glass dialog with the selected option highlighted. */
@Composable
fun <T> ChoiceDialog(
    title: String,
    choices: List<Choice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = withTap(onDismiss)) { Text(stringResource(R.string.cancel)) } },
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                choices.forEach { choice ->
                    OptionRow(choice.title, choice.subtitle, choice.value == selected, container = Helm.colors.surface2) {
                        onSelect(choice.value)
                    }
                }
            }
        },
    )
}

/** Selectable option row (radio replacement): accent-tinted with a check when selected. */
@Composable
fun OptionRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    enabled: Boolean = true,
    container: Color = Helm.colors.card,
    /** Emoji (a flag) drawn on a round tile before the title. */
    leading: String? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Tints.accent.bg else container)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(50)).background(Helm.colors.surface2),
                contentAlignment = Alignment.Center,
            ) {
                Text(leading, fontSize = 22.sp)
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Helm.colors.text.copy(alpha = if (enabled) 1f else 0.45f))
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Helm.colors.text2.copy(alpha = if (enabled) 1f else 0.45f))
            }
        }
        if (selected) Icon(Icons.Filled.Check, null, tint = Helm.colors.accent)
    }
}

/** "Important" note: info tile, accent title and body on a soft accent background. */
@Composable
fun NoticeCard(title: String?, body: String, modifier: Modifier = Modifier, tint: Tint? = null, icon: ImageVector = Icons.Outlined.Info) {
    val t = tint ?: Tints.accent
    val fg = t.fg
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(if (tint == null) t.bg else fg.copy(alpha = 0.10f)).padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(fg.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(22.dp), tint = fg)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, color = fg)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Helm.colors.text)
        }
    }
}

/** Small rounded badge (LAN / NET / SUB style). */
@Composable
fun HelmBadge(text: String, tint: Tint, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(tint.bg).padding(horizontal = 11.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = tint.fg, maxLines = 1)
    }
}

/** Label on the left, value on the right. */
@Composable
fun InfoRow(label: String, value: String, valueColor: Color = Helm.colors.text) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = Helm.colors.text2)
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            Modifier.weight(1.4f),
            style = MaterialTheme.typography.labelLarge,
            color = valueColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
