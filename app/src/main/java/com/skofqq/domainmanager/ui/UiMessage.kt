package com.skofqq.domainmanager.ui

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
    // 502: the mihomo controller itself is down — a distinct case from our API failing.
    "mihomo_unreachable" -> UiMessage(R.string.err_mihomo_unreachable)
    "mihomo_select_failed" -> UiMessage(R.string.err_mihomo_select_failed)
    else -> UiMessage(R.string.err_generic, listOf(code, error))
}

/** Standard error surface used under lists/forms across screens. */
@Composable
fun ErrorCard(message: UiMessage, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Text(
            text = message.resolve(),
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

fun networkErrorMessage(kind: NetFailure, detail: String?): UiMessage = when (kind) {
    NetFailure.TIMEOUT -> UiMessage(R.string.err_timeout, detail = detail)
    NetFailure.UNREACHABLE -> UiMessage(R.string.err_unreachable, detail = detail)
    NetFailure.UNKNOWN_HOST -> UiMessage(R.string.err_unknown_host, detail = detail)
    NetFailure.INVALID_ADDRESS -> UiMessage(R.string.err_invalid_router_address, listOf(detail ?: ""))
    NetFailure.EMPTY_RESPONSE -> UiMessage(R.string.err_empty_response)
    NetFailure.OTHER ->
        if (detail.isNullOrBlank()) UiMessage(R.string.err_network)
        else UiMessage(R.string.err_raw, listOf(detail))
}
