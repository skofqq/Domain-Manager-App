package com.skofqq.domainmanager.data

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Android 16+ Local Network Protection: without ACCESS_LOCAL_NETWORK (shown to the
 * user as "Nearby devices") every connection to a LAN address — the router — is
 * silently dropped and looks exactly like a connect timeout. A fresh install
 * starts without it, so the app has to ask.
 */
object LocalNetworkAccess {
    const val PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

    /** Only where the platform actually defines the permission; older Androids need nothing. */
    fun isDefined(context: Context): Boolean = runCatching {
        context.packageManager.getPermissionInfo(PERMISSION, 0)
    }.isSuccess

    /** True when the router is unreachable because of the missing permission, not the network. */
    fun isBlocked(context: Context): Boolean =
        isDefined(context) &&
            ContextCompat.checkSelfPermission(context, PERMISSION) != PackageManager.PERMISSION_GRANTED
}
