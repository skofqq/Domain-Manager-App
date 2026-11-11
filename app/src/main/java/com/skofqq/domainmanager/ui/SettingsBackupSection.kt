package com.skofqq.domainmanager.ui

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.SettingsBackup
import com.skofqq.domainmanager.ui.components.ButtonStyle
import com.skofqq.domainmanager.ui.components.HelmButton
import com.skofqq.domainmanager.ui.components.HelmSection
import com.skofqq.domainmanager.ui.components.NoticeCard
import com.skofqq.domainmanager.ui.components.SwitchRow
import com.skofqq.domainmanager.ui.theme.Tints

/**
 * Settings → Backup → "App settings": the app's own configuration as a JSON file,
 * separate from the router-list snapshot above it. Restores from a file picked
 * with the system picker, or from text pasted into the Import field.
 */
@Composable
fun AppSettingsBackupSection(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val activity = LocalActivity.current as? FragmentActivity
    var includeTokens by rememberSaveable { mutableStateOf(true) }
    val authTitle = stringResource(R.string.settings_backup_auth)

    DisposableEffect(Unit) { onDispose { viewModel.clearSettingsBackupMessages() } }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) viewModel.saveSettingsTo(uri, includeTokens) }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.restoreSettingsFrom(uri) }

    /** Tokens leave the encrypted store here — same gate as revealing one or showing the QR. */
    fun guarded(action: () -> Unit) {
        if (includeTokens && viewModel.appLockMode == "token" && activity != null && deviceAuthAvailable(activity)) {
            promptDeviceAuth(activity, authTitle, action)
        } else {
            action()
        }
    }

    HelmSection(
        title = stringResource(R.string.settings_backup_title),
        subtitle = stringResource(R.string.settings_backup_note),
        icon = Icons.Outlined.Tune,
        tint = Tints.purple,
        shape = MaterialShapes.Cookie7Sided,
    ) {
        SwitchRow(
            icon = Icons.Outlined.Key,
            title = stringResource(R.string.settings_backup_tokens),
            checked = includeTokens,
            showDivider = false,
            onCheckedChange = { includeTokens = it },
        )
        if (includeTokens) {
            NoticeCard(
                title = null,
                body = stringResource(R.string.settings_backup_tokens_warning),
                tint = Tints.amber,
                icon = Icons.Outlined.WarningAmber,
            )
        } else {
            NoticeCard(
                title = null,
                body = stringResource(R.string.settings_backup_tokens_desc),
                tint = Tints.gray,
                icon = Icons.Outlined.Key,
            )
        }
        // Full width each: the labels are long in most languages.
        HelmButton(
            text = stringResource(R.string.settings_backup_save),
            onClick = { guarded { saveLauncher.launch(SettingsBackup.fileName()) } },
            icon = Icons.Outlined.Save,
            modifier = Modifier.fillMaxWidth(),
        )
        HelmButton(
            text = stringResource(R.string.share),
            onClick = {
                guarded {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, viewModel.exportSettings(includeTokens))
                    }
                    context.startActivity(Intent.createChooser(send, null))
                }
            },
            icon = Icons.Outlined.Share,
            style = ButtonStyle.Tonal,
            modifier = Modifier.fillMaxWidth(),
        )
        HelmButton(
            text = stringResource(R.string.settings_backup_restore),
            onClick = { openLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
            icon = Icons.Outlined.FileOpen,
            style = ButtonStyle.Neutral,
            modifier = Modifier.fillMaxWidth(),
        )
        viewModel.settingsBackupMessage?.let {
            NoticeCard(title = null, body = it.resolve(), tint = Tints.green, icon = Icons.Filled.CheckCircle)
        }
        viewModel.settingsBackupError?.let { ErrorCard(it) }
    }
}
