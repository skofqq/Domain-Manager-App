package com.skofqq.domainmanager.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.AppRelease
import com.skofqq.domainmanager.data.AppUpdater
import com.skofqq.domainmanager.data.InstallEvent
import com.skofqq.domainmanager.ui.components.ButtonStyle
import com.skofqq.domainmanager.ui.components.EntryCard
import com.skofqq.domainmanager.ui.components.HelmButton
import com.skofqq.domainmanager.ui.components.HelmSection
import com.skofqq.domainmanager.ui.components.LabeledProgress
import com.skofqq.domainmanager.ui.components.NoticeCard
import com.skofqq.domainmanager.ui.components.SwitchRow
import com.skofqq.domainmanager.ui.components.TintButton
import com.skofqq.domainmanager.ui.theme.Helm
import com.skofqq.domainmanager.ui.theme.Tints
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: AppRelease) : UpdateState
    data class Downloading(val release: AppRelease, val progress: Float) : UpdateState
    /** Downloaded and verified; waiting for the user (or for the install permission). */
    data class Ready(val release: AppRelease, val apk: File, val needsPermission: Boolean) : UpdateState
    data class Installing(val release: AppRelease, val apk: File) : UpdateState
    data class Failed(val release: AppRelease?, val message: UiMessage) : UpdateState
}

/**
 * Activity-scoped (obtained with viewModel() from any screen, and by MainActivity
 * for the daily background check), so the Settings root card and the About
 * screen share one state.
 */
class AppUpdateViewModel(app: Application) : AndroidViewModel(app) {
    private val updater = AppUpdater(app)
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state
    private var job: Job? = null

    var autoCheck by mutableStateOf(updater.autoCheck)
        private set

    val installedVersion: String get() = updater.installedVersion

    init {
        viewModelScope.launch {
            AppUpdater.installEvents.collect { event ->
                val current = _state.value as? UpdateState.Installing ?: return@collect
                when (event) {
                    null, InstallEvent.AwaitingUser, InstallEvent.Success -> Unit
                    InstallEvent.Aborted -> _state.value = UpdateState.Ready(current.release, current.apk, false)
                    InstallEvent.Conflict -> _state.value =
                        UpdateState.Failed(current.release, UiMessage(R.string.update_err_conflict))
                    is InstallEvent.Failed -> _state.value = UpdateState.Failed(
                        current.release,
                        UiMessage(R.string.update_err_install, detail = event.message),
                    )
                }
                if (event != null && event != InstallEvent.AwaitingUser) AppUpdater.consumeInstallEvent()
            }
        }
    }

    fun updateAutoCheck(value: Boolean) {
        updater.autoCheck = value
        autoCheck = value
    }

    /** Silent daily check on app start: failures stay quiet, only a newer release surfaces. */
    fun checkIfDue() {
        if (!updater.isCheckDue || _state.value != UpdateState.Idle) return
        check(silent = true)
    }

    fun check(silent: Boolean = false) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            if (!silent) _state.value = UpdateState.Checking
            _state.value = try {
                val release = updater.fetchNewerRelease()
                when {
                    release == null -> if (silent) UpdateState.Idle else UpdateState.UpToDate
                    else -> updater.downloadedApk(release)
                        ?.let { UpdateState.Ready(release, it, !updater.canInstall()) }
                        ?: UpdateState.Available(release)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (silent) UpdateState.Idle
                else UpdateState.Failed(null, UiMessage(R.string.update_err_check, detail = e.message))
            }
        }
    }

    fun downloadAndInstall(release: AppRelease) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            _state.value = UpdateState.Downloading(release, 0f)
            val apk = try {
                updater.download(release) { p -> _state.value = UpdateState.Downloading(release, p) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppUpdater.InvalidApkException) {
                _state.value = UpdateState.Failed(release, UiMessage(R.string.update_err_bad_apk))
                return@launch
            } catch (e: Exception) {
                _state.value = UpdateState.Failed(release, UiMessage(R.string.update_err_download, detail = e.message))
                return@launch
            }
            installNow(release, apk)
        }
    }

    /** Re-tries the install of an already downloaded APK (e.g. after granting the permission). */
    fun install() {
        val ready = _state.value as? UpdateState.Ready ?: return
        if (job?.isActive == true) return
        job = viewModelScope.launch { installNow(ready.release, ready.apk) }
    }

    /** Called on resume: the user may have just granted "Install unknown apps". */
    fun onResumed() {
        val ready = _state.value as? UpdateState.Ready ?: return
        if (ready.needsPermission && updater.canInstall()) install()
    }

    private suspend fun installNow(release: AppRelease, apk: File) {
        if (!updater.canInstall()) {
            _state.value = UpdateState.Ready(release, apk, needsPermission = true)
            return
        }
        AppUpdater.consumeInstallEvent()
        _state.value = UpdateState.Installing(release, apk)
        try {
            updater.install(apk)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.value = UpdateState.Failed(release, UiMessage(R.string.update_err_install, detail = e.message))
        }
    }
}

/** Card on the Settings root, shown only while an update is pending. */
@Composable
fun UpdateAvailableCard(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val vm: AppUpdateViewModel = viewModel()
    val state by vm.state.collectAsState()
    val release = when (val s = state) {
        is UpdateState.Available -> s.release
        is UpdateState.Downloading -> s.release
        is UpdateState.Ready -> s.release
        is UpdateState.Installing -> s.release
        else -> null
    } ?: return
    EntryCard(
        icon = Icons.Outlined.SystemUpdate,
        title = stringResource(R.string.update_available_short, release.version),
        subtitle = stringResource(R.string.update_available_hint),
        onClick = onOpen,
        tint = Tints.accent,
        shape = MaterialShapes.SoftBurst,
        modifier = modifier,
    )
}

/** The "App update" section of the About screen. */
@Composable
fun AppUpdateSection() {
    val vm: AppUpdateViewModel = viewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.onResumed() }

    HelmSection(
        title = stringResource(R.string.update_section),
        subtitle = stringResource(R.string.update_current_version, vm.installedVersion),
        icon = Icons.Outlined.SystemUpdate,
        tint = Tints.accent,
        shape = MaterialShapes.SoftBurst,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (val s = state) {
                UpdateState.Idle, UpdateState.Checking -> HelmButton(
                    text = stringResource(
                        if (s == UpdateState.Checking) R.string.update_checking else R.string.check_for_updates
                    ),
                    onClick = { vm.check() },
                    loading = s == UpdateState.Checking,
                    icon = Icons.Outlined.Refresh,
                    style = ButtonStyle.Tonal,
                    modifier = Modifier.fillMaxWidth(),
                )

                UpdateState.UpToDate -> {
                    NoticeCard(
                        title = null,
                        body = stringResource(R.string.update_up_to_date),
                        tint = Tints.green,
                        icon = Icons.Outlined.CheckCircle,
                    )
                    HelmButton(
                        text = stringResource(R.string.check_for_updates),
                        onClick = { vm.check() },
                        icon = Icons.Outlined.Refresh,
                        style = ButtonStyle.Neutral,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is UpdateState.Available -> {
                    ReleaseInfo(s.release)
                    HelmButton(
                        text = stringResource(
                            R.string.update_download_install,
                            Formatter.formatShortFileSize(context, s.release.apkSize),
                        ),
                        onClick = { vm.downloadAndInstall(s.release) },
                        icon = Icons.Filled.Download,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is UpdateState.Downloading -> {
                    ReleaseInfo(s.release)
                    LabeledProgress(
                        label = stringResource(R.string.update_downloading),
                        valueText = "${(s.progress * 100).toInt()}%",
                        progress = s.progress,
                    )
                }

                is UpdateState.Ready -> {
                    ReleaseInfo(s.release)
                    if (s.needsPermission) {
                        NoticeCard(
                            title = stringResource(R.string.update_permission_title),
                            body = stringResource(R.string.update_permission_body),
                            tint = Tints.amber,
                            icon = Icons.Outlined.Security,
                        )
                        HelmButton(
                            text = stringResource(R.string.update_permission_open),
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        Uri.parse("package:${context.packageName}"),
                                    )
                                )
                            },
                            icon = Icons.Outlined.Security,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        HelmButton(
                            text = stringResource(R.string.update_install),
                            onClick = { vm.install() },
                            icon = Icons.Filled.InstallMobile,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                is UpdateState.Installing -> {
                    ReleaseInfo(s.release)
                    HelmButton(
                        text = stringResource(R.string.update_installing),
                        onClick = {},
                        loading = true,
                        icon = Icons.Filled.InstallMobile,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is UpdateState.Failed -> {
                    s.release?.let { ReleaseInfo(it) }
                    ErrorCard(s.message)
                    HelmButton(
                        text = stringResource(R.string.retry),
                        onClick = { if (s.release != null) vm.downloadAndInstall(s.release) else vm.check() },
                        icon = Icons.Outlined.Refresh,
                        style = ButtonStyle.Tonal,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TintButton(
                        text = stringResource(R.string.update_open_github),
                        tint = Tints.gray,
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(s.release?.pageUrl ?: AppUpdater.RELEASES_URL))
                        )
                    }
                }
            }
            SwitchRow(
                icon = null,
                title = stringResource(R.string.update_auto_check),
                subtitle = stringResource(R.string.update_auto_check_desc),
                checked = vm.autoCheck,
                showDivider = false,
                onCheckedChange = vm::updateAutoCheck,
            )
        }
    }
}

@Composable
private fun ReleaseInfo(release: AppRelease) {
    var expanded by rememberSaveable(release.version) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.update_available_title, release.version),
            style = MaterialTheme.typography.titleMedium,
            color = Helm.colors.text,
        )
        if (release.notes.isNotBlank()) {
            Text(
                release.notes.cleanMarkdown(),
                style = MaterialTheme.typography.bodySmall,
                color = Helm.colors.text2,
                maxLines = if (expanded) Int.MAX_VALUE else 6,
                overflow = TextOverflow.Ellipsis,
            )
            TintButton(
                text = stringResource(if (expanded) R.string.update_notes_less else R.string.update_notes_more),
                tint = Tints.gray,
            ) { expanded = !expanded }
        }
    }
}

/** Release notes are Markdown; plain text reads better than raw `##` and `**`. */
private fun String.cleanMarkdown(): String = lines().joinToString("\n") { line ->
    line.trimEnd()
        .replace(Regex("^#{1,6}\\s*"), "")
        .replace(Regex("^\\s*[-*]\\s+"), "• ")
        .replace("**", "")
        .replace("`", "")
}.replace(Regex("\n{3,}"), "\n\n").trim()
