package com.skofqq.domainmanager.ui

import com.skofqq.domainmanager.ui.theme.Helm
import com.skofqq.domainmanager.ui.theme.Tints
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Folder
import com.skofqq.domainmanager.ui.components.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.MagitrickleGroup

/**
 * Read-only overview of EVERY MagiTrickle rule group configured on the router —
 * not just Custom (the one this app otherwise manages), but Anime/Block/
 * Cloudflare/… too, whatever exists there. Tap a group to see its rules.
 */
@Composable
fun MagitrickleGroupsScreen(viewModel: MagitrickleGroupsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    StatusChildScaffold(stringResource(R.string.magitrickle_groups_title), onBack) { padding ->
        val firstLoad = state.groups == null && state.error == null
        val skeletonBrush = if (firstLoad) shimmerBrush() else null

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.error?.let { message ->
                item {
                    ErrorCard(message, onRetry = { viewModel.refresh() })
                }
            }
            if (skeletonBrush != null) {
                items(5) { SkeletonMagitrickleGroupRow(skeletonBrush) }
            }
            state.groups?.let { groups ->
                if (groups.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.FolderOff,
                            title = stringResource(R.string.magitrickle_groups_empty),
                            tint = Tints.gray,
                        )
                    }
                }
                itemsIndexed(groups, key = { _, group -> group.id }) { index, group ->
                    MagitrickleGroupRow(
                        group = group,
                        index = index,
                        expanded = expandedId == group.id,
                        onToggle = { expandedId = if (expandedId == group.id) null else group.id },
                    )
                }
            }
        }
    }
}

@Composable
private fun MagitrickleGroupRow(group: MagitrickleGroup, index: Int, expanded: Boolean, onToggle: () -> Unit) {
    val enabledRules = group.rules.count { it.enable }
    ExpandableCard(
        title = group.name,
        subtitle = pluralStringResource(R.plurals.n_rules, group.rules.size, group.rules.size),
        expanded = expanded,
        onToggle = onToggle,
        icon = Icons.Outlined.Folder,
        tint = cycleTint(index),
        shape = cycleShape(index),
        trailing = { ValuePill(enabledRules.toString(), cycleTint(index)) },
    ) {
        if (group.rules.isEmpty()) {
            SectionNote(stringResource(R.string.magitrickle_group_no_rules))
        } else {
            MonoBlock {
                group.rules.forEach { rule ->
                    Text(
                        text = rule.rule,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        // Disabled rules stay visible (they are real router state)
                        // but visually de-emphasized.
                        color = if (rule.enable) Helm.colors.text else Helm.colors.text2,
                        textDecoration = if (rule.enable) null else TextDecoration.LineThrough,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SkeletonMagitrickleGroupRow(brush: androidx.compose.ui.graphics.Brush) {
    HelmCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SkeletonBox(brush, Modifier.fillMaxWidth(0.5f).height(16.dp))
            SkeletonBox(brush, Modifier.fillMaxWidth(0.3f).height(12.dp))
        }
    }
}
