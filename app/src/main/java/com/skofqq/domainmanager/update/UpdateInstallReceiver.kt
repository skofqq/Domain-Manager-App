package com.skofqq.domainmanager.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.skofqq.domainmanager.data.AppUpdater
import com.skofqq.domainmanager.data.InstallEvent

/**
 * Receives the PackageInstaller session status for an in-app update. When the
 * system needs confirmation it hands back an intent for its install dialog, which
 * is started from here — the app is in the foreground (the user just tapped
 * "Install"), so the activity start is allowed.
 */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirm == null) {
                    AppUpdater.post(InstallEvent.Failed(null))
                    return
                }
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                AppUpdater.post(InstallEvent.AwaitingUser)
                runCatching { context.startActivity(confirm) }
                    .onFailure { AppUpdater.post(InstallEvent.Failed(it.message)) }
            }
            PackageInstaller.STATUS_SUCCESS -> AppUpdater.post(InstallEvent.Success)
            PackageInstaller.STATUS_FAILURE_ABORTED -> AppUpdater.post(InstallEvent.Aborted)
            PackageInstaller.STATUS_FAILURE_CONFLICT,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> AppUpdater.post(InstallEvent.Conflict)
            else -> AppUpdater.post(
                InstallEvent.Failed(intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))
            )
        }
    }
}
