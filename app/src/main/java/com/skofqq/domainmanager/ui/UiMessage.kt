package com.skofqq.domainmanager.ui

import com.skofqq.domainmanager.ui.theme.Tint
import com.skofqq.domainmanager.ui.theme.Tints
import com.skofqq.domainmanager.ui.theme.Helm
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import com.skofqq.domainmanager.ui.components.*
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.NetFailure

/**
 * A localizable message: resource id + optional format args, resolved in composition.
 * [detail] is a raw technical string (exception text) appended on its own line.
 */
data class UiMessage(
    @StringRes val res: Int,
    val args: List<Any> = emptyList(),
    val detail: String? = null,
) {
    @Composable
    fun resolve(): String {
        val base = if (args.isEmpty()) stringResource(res) else stringResource(res, *args.toTypedArray())
        return if (detail.isNullOrBlank()) base else "$base\n\n$detail"
    }

    /** Just the message, without the technical [detail] (shown separately on error cards). */
    @Composable
    fun resolveBase(): String = if (args.isEmpty()) stringResource(res) else stringResource(res, *args.toTypedArray())

    /** Non-composable variant for toasts and other out-of-composition consumers. */
    fun resolve(context: Context): String {
        val base = if (args.isEmpty()) context.getString(res)
        else context.getString(res, *args.toTypedArray())
        return if (detail.isNullOrBlank()) base else "$base\n\n$detail"
    }
}

fun apiErrorMessage(code: Int, error: String): UiMessage = when (error) {
    "bad_token" -> UiMessage(R.string.err_bad_token)
    "token_not_configured" -> UiMessage(R.string.err_token_not_configured)
    "bad_domain" -> UiMessage(R.string.err_bad_domain)
    "empty_domain" -> UiMessage(R.string.err_empty_domain)
    "bad_service" -> UiMessage(R.string.err_bad_service)
    "bad_host" -> UiMessage(R.string.err_bad_host)
    "bad_mac" -> UiMessage(R.string.err_bad_mac)
    "etherwake_not_installed" -> UiMessage(R.string.err_etherwake_missing)
    "bad_engine" -> UiMessage(R.string.err_bad_engine)
    "bad_strategy" -> UiMessage(R.string.err_bad_strategy)
    // Rule providers. group_not_found / group_exists / provider_exists are the
    // ones a user actually hits (the group was changed on the router since the
    // list was fetched, or the name is already in use) — each gets its own text
    // so the add flow never falls back to a generic failure.
    "bad_provider_name" -> UiMessage(R.string.err_bad_provider_name)
    "bad_url" -> UiMessage(R.string.err_bad_url)
    "bad_behavior" -> UiMessage(R.string.err_bad_behavior)
    "bad_format" -> UiMessage(R.string.err_bad_format)
    "bad_interval" -> UiMessage(R.string.err_bad_interval)
    "missing_group" -> UiMessage(R.string.err_missing_group)
    "group_and_new_group_both_set" -> UiMessage(R.string.err_group_both_set)
    "group_not_found" -> UiMessage(R.string.err_group_not_found)
    "bad_new_group" -> UiMessage(R.string.err_bad_new_group)
    "provider_exists" -> UiMessage(R.string.err_provider_exists)
    "group_exists" -> UiMessage(R.string.err_group_exists)
    "mihomo_config_test_failed" -> UiMessage(R.string.err_mihomo_config_test_failed)
    // mihomo subscriptions. The base one is gated in the UI already — this text
    // only ever shows if the router's own list said "removable" and then changed.
    "subscription_exists" -> UiMessage(R.string.err_subscription_exists)
    "subscription_not_found" -> UiMessage(R.string.err_subscription_not_found)
    "cannot_remove_base_subscription" -> UiMessage(R.string.err_subscription_base_protected)
    // Tor control port: "not ready" is transient (tor just started and hasn't
    // written its cookie file yet), so the text says to retry rather than blaming
    // the user's configuration.
    "controlport_not_ready" -> UiMessage(R.string.err_tor_controlport_not_ready)
    "controlport_command_failed" -> UiMessage(R.string.err_tor_controlport_failed)
    // 502: the mihomo controller itself is down — a distinct case from our API failing.
    "mihomo_unreachable" -> UiMessage(R.string.err_mihomo_unreachable)
    "mihomo_select_failed" -> UiMessage(R.string.err_mihomo_select_failed)
    else -> UiMessage(R.string.err_generic, listOf(code, error))
}

/** Standard error surface used under lists/forms across screens. */
@Composable
fun ErrorCard(message: UiMessage, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    val kind = errorKindOf(message.res)
    val red = Tints.red
    val openSettings = LocalOpenRouterSettings.current
    val requestLocalNetwork = LocalRequestLocalNetwork.current
    var showDetail by rememberSaveable(message) { mutableStateOf(false) }
    val (titleRes, icon, shape) = when (kind) {
        ErrorKind.CONNECTION -> Triple(R.string.err_title_connection, Icons.Outlined.WifiOff, MaterialShapes.Cookie4Sided)
        ErrorKind.CONFIG -> Triple(R.string.err_title_config, Icons.Outlined.Dns, MaterialShapes.Cookie6Sided)
        ErrorKind.ACCESS -> Triple(R.string.err_title_access, Icons.Outlined.Key, MaterialShapes.Clover4Leaf)
        ErrorKind.LOCAL_NETWORK -> Triple(R.string.err_title_local_network, Icons.Outlined.Lan, MaterialShapes.Cookie4Sided)
        ErrorKind.OTHER -> Triple(R.string.err_title_other, Icons.Outlined.ErrorOutline, MaterialShapes.Cookie9Sided)
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(red.bg.compositeOver(Helm.colors.card))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            ShapeIcon(icon, Tint(Color.White, red.fg), shape = shape, size = 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = red.fg,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    message.resolveBase(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Helm.colors.text,
                )
            }
        }
        // The raw exception text is useful for a bug report, not for reading:
        // folded away behind "Details".
        val detail = message.detail
        if (!detail.isNullOrBlank()) {
            AnimatedVisibility(
                visible = showDetail,
                enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
                exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(),
            ) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Helm.colors.text2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Helm.colors.card)
                        .padding(12.dp),
                )
            }
        }
        val settingsAction = openSettings.takeIf { kind != ErrorKind.OTHER && kind != ErrorKind.LOCAL_NETWORK }
        val permissionAction = requestLocalNetwork.takeIf { kind == ErrorKind.LOCAL_NETWORK }
        if (permissionAction != null) {
            HelmButton(
                text = stringResource(R.string.local_network_allow),
                onClick = permissionAction,
                icon = Icons.Outlined.Lan,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (onRetry != null || settingsAction != null || !detail.isNullOrBlank()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onRetry != null) {
                    HelmButton(
                        text = stringResource(R.string.retry),
                        onClick = onRetry,
                        icon = Icons.Filled.Refresh,
                        style = ButtonStyle.Danger,
                    )
                }
                if (settingsAction != null) {
                    HelmButton(
                        text = stringResource(R.string.err_open_settings),
                        onClick = settingsAction,
                        icon = Icons.Outlined.Settings,
                        style = ButtonStyle.Neutral,
                    )
                }
                if (!detail.isNullOrBlank()) {
                    TextButton(onClick = { showDetail = !showDetail }) {
                        Text(stringResource(R.string.err_details), color = Helm.colors.text2)
                    }
                }
            }
        }
    }
}

/** What an error is about, for its title, icon and whether "Router settings" helps. */
private enum class ErrorKind { CONNECTION, CONFIG, ACCESS, LOCAL_NETWORK, OTHER }

private fun errorKindOf(@StringRes res: Int): ErrorKind = when (res) {
    R.string.err_timeout, R.string.err_unreachable, R.string.err_unknown_host,
    R.string.err_network, R.string.err_empty_response -> ErrorKind.CONNECTION
    R.string.err_tls, R.string.err_not_api, R.string.err_invalid_router_address -> ErrorKind.CONFIG
    R.string.err_bad_token, R.string.err_token_not_configured -> ErrorKind.ACCESS
    R.string.err_local_network -> ErrorKind.LOCAL_NETWORK
    else -> ErrorKind.OTHER
}

/**
 * Opens Settings → Authorization (the active router's address, port and token) from an
 * error card. Provided by the main screen; null elsewhere (share sheet), where the card
 * simply has no such button.
 */
val LocalOpenRouterSettings = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * Asks for the Android 16+ local network permission (or opens the app settings when
 * the system will not show the dialog again). Provided by MainActivity.
 */
val LocalRequestLocalNetwork = staticCompositionLocalOf<(() -> Unit)?> { null }

fun networkErrorMessage(kind: NetFailure, detail: String?): UiMessage = when (kind) {
    NetFailure.TIMEOUT -> UiMessage(R.string.err_timeout, detail = detail)
    NetFailure.UNREACHABLE -> UiMessage(R.string.err_unreachable, detail = detail)
    NetFailure.UNKNOWN_HOST -> UiMessage(R.string.err_unknown_host, detail = detail)
    NetFailure.INVALID_ADDRESS -> UiMessage(R.string.err_invalid_router_address, listOf(detail ?: ""))
    NetFailure.EMPTY_RESPONSE -> UiMessage(R.string.err_empty_response)
    NetFailure.TLS -> UiMessage(R.string.err_tls, detail = detail)
    NetFailure.NOT_API -> UiMessage(R.string.err_not_api, detail = detail)
    NetFailure.LOCAL_NETWORK_BLOCKED -> UiMessage(R.string.err_local_network, detail = detail)
    NetFailure.OTHER ->
        if (detail.isNullOrBlank()) UiMessage(R.string.err_network)
        else UiMessage(R.string.err_raw, listOf(detail))
}
