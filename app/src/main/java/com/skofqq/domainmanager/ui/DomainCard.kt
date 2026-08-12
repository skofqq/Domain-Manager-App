package com.skofqq.domainmanager.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skofqq.domainmanager.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DomainStatusCard(
    status: StatusUiState,
    defaultTarget: String,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = status,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
        label = "status-card",
    ) { s ->
        when (s) {
            is StatusUiState.Idle -> Box(modifier.fillMaxWidth())

            is StatusUiState.Loading -> Box(
                modifier = modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is StatusUiState.Error -> ElevatedCard(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Text(
                    text = s.message.resolve(),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            is StatusUiState.Loaded -> ElevatedCard(modifier = modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.status),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusChip(name = "mihomo", active = s.mihomo)
                        StatusChip(name = "MagiTrickle", active = s.magitrickle)
                    }
                    Spacer(Modifier.height(4.dp))
                    // "Present" is judged against the selected target, not always both —
                    // with target=mihomo an added domain must offer Remove, not Add.
                    val presentOnTarget = when (defaultTarget) {
                        "mihomo" -> s.mihomo
                        "magitrickle" -> s.magitrickle
                        else -> s.mihomo && s.magitrickle
                    }
                    if (presentOnTarget) {
                        OutlinedButton(
                            onClick = onRemove,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Text(stringResource(R.string.remove_from, defaultTarget))
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onAdd,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.add_to, defaultTarget))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Read-only presence badge. It used to be a [FilterChip] with an empty `onClick`,
 * which put a focusable, ripple-ing control that did nothing into the tab order —
 * and left "added" vs "not added" to the tint and the glyph. It is a plain surface
 * now, named for what it reports.
 */
@Composable
private fun StatusChip(name: String, active: Boolean) {
    val container = if (active) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceContainerHighest
    val content = if (active) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    val stateLabel = stringResource(
        if (active) R.string.a11y_target_added else R.string.a11y_target_not_added,
        name,
    )
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = stateLabel
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (active) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(name, style = MaterialTheme.typography.labelLarge)
        }
    }
}
