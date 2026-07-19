package com.skofqq.domainmanager.ui

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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.error?.let { message ->
                item {
                    Column {
                        ErrorCard(message)
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
            if (skeletonBrush != null) {
                items(5) { SkeletonMagitrickleGroupRow(skeletonBrush) }
            }
            state.groups?.let { groups ->
                if (groups.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.magitrickle_groups_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                }
                items(groups, key = { it.id }) { group ->
                    MagitrickleGroupRow(
                        group = group,
                        expanded = expandedId == group.id,
                        onToggle = { expandedId = if (expandedId == group.id) null else group.id },
                    )
                }
            }
        }
    }
}

@Composable
private fun MagitrickleGroupRow(group: MagitrickleGroup, expanded: Boolean, onToggle: () -> Unit) {
    ElevatedCard(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        group.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        pluralStringResource(R.plurals.n_rules, group.rules.size, group.rules.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(if (expanded) 90f else 0f),
                )
            }
            if (expanded) {
                if (group.rules.isEmpty()) {
                    Text(
                        stringResource(R.string.magitrickle_group_no_rules),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        group.rules.forEach { rule ->
                            Text(
                                text = rule.rule,
                                style = MaterialTheme.typography.bodyMedium,
                                // Disabled rules stay visible (they're real router
                                // state) but visually de-emphasized.
                                color = if (rule.enable) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = if (rule.enable) null else TextDecoration.LineThrough,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonMagitrickleGroupRow(brush: androidx.compose.ui.graphics.Brush) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SkeletonBox(brush, Modifier.fillMaxWidth(0.5f).height(16.dp))
            SkeletonBox(brush, Modifier.fillMaxWidth(0.3f).height(12.dp))
        }
    }
}
