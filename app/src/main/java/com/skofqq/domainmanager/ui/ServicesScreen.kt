package com.skofqq.domainmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.ServiceStatus

private fun serviceLabel(service: String) =
    if (service == "magitrickle") "MagiTrickle" else service

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(viewModel: ServicesViewModel) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.status)) },
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        // First svc_list (no cache yet) → 4 skeleton cards; later refreshes keep
        // the regular indicator over the existing cards.
        val firstLoad = state.services == null && state.error == null
        val skeletonBrush = if (firstLoad) shimmerBrush() else null

        PullToRefreshBox(
            isRefreshing = state.isRefreshing && !firstLoad,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.error?.let { message ->
                    item { ErrorCard(message) }
                }
                if (skeletonBrush != null) {
                    // mihomo / magitrickle / zapret / zapret2
                    items(4) { SkeletonServiceCard(skeletonBrush) }
                }
                val services = state.services
                if (services != null) {
                    items(services, key = { it.service }) { svc ->
                        ServiceCard(
                            status = svc,
                            busy = state.busyService == svc.service,
                            actionsEnabled = state.busyService == null,
                            onStart = { viewModel.start(svc.service) },
                            onStop = { viewModel.stop(svc.service) },
                            onRestart = { viewModel.restart(svc.service) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceCard(
    status: ServiceStatus,
    busy: Boolean,
    actionsEnabled: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = serviceLabel(status.service),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                // svc_start/stop/restart can block up to ~5 s on the router.
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
            // running (live procd state) and enabled (autostart on next boot)
            // are independent flags — always show both.
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RunningIndicator(running = status.running, enabled = status.enabled)
                AutostartIndicator(status.enabled)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!status.running) {
                    FilledTonalButton(
                        onClick = onStart,
                        enabled = actionsEnabled,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.svc_start)) }
                } else {
                    OutlinedButton(
                        onClick = onStop,
                        enabled = actionsEnabled,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.svc_stop)) }
                    FilledTonalButton(
                        onClick = onRestart,
                        enabled = actionsEnabled,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.svc_restart)) }
                }
            }
        }
    }
}

/** Green when the service runs. */
private val RunningGreen = Color(0xFF4CAF50)

@Composable
private fun RunningIndicator(running: Boolean, enabled: Boolean) {
    // Derived on the client from the two existing flags:
    // running → green; stopped but enabled → red (should be running — failed);
    // stopped and disabled → grey (intentionally off).
    val (tint, labelRes) = when {
        running -> RunningGreen to R.string.svc_running
        enabled -> MaterialTheme.colorScheme.error to R.string.svc_failed
        else -> MaterialTheme.colorScheme.outline to R.string.svc_stopped
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(tint),
        )
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = tint,
        )
    }
}

@Composable
private fun AutostartIndicator(enabled: Boolean) {
    val tint = if (enabled) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = if (enabled) Icons.Filled.Check else Icons.Filled.Close,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(R.string.svc_autostart),
            style = MaterialTheme.typography.labelMedium,
            color = tint,
        )
    }
}
