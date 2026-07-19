package com.skofqq.domainmanager.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.DiskInfoResult
import com.skofqq.domainmanager.data.GroupDelayResult
import com.skofqq.domainmanager.data.MihomoProxiesResult
import com.skofqq.domainmanager.data.NotificationHelper
import com.skofqq.domainmanager.data.PrefsStore
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.data.SysInfoResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Opt-in, battery-friendly periodic sanity check (Settings → Мониторинг): WAN IP
 * change, disk-space warning, mihomo latency degradation for one chosen group.
 * Runs on WorkManager's own schedule (NetworkType.CONNECTED, no other special
 * constraint — this is a background convenience check, not a realtime monitor).
 *
 * Scoped to the ACTIVE router profile only: flipping the active-profile pointer
 * from a background worker to probe other saved profiles would race with
 * whatever the user has open in the foreground, so multi-router users get this
 * for whichever router they last had selected.
 */
class RouterMonitorWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = PrefsStore(applicationContext)
        if (!prefs.monitoringEnabled) return@withContext Result.success()
        val profile = prefs.activeProfile() ?: return@withContext Result.success()
        val api = RouterApi(prefs, applicationContext)
        var state = prefs.monitorState(profile.id)

        if (prefs.monitorWanIp) state = checkWanIp(api, state)
        if (prefs.monitorDiskSpace) state = checkDisk(api, state)
        state.latencyGroup?.let { group -> state = checkLatency(api, group, state) }

        prefs.setMonitorState(profile.id, state)
        Result.success()
    }

    /** Real change only: WAN going briefly empty (down) then back to the SAME ip must never look like a change. */
    private fun checkWanIp(api: RouterApi, state: PrefsStore.RouterMonitorState): PrefsStore.RouterMonitorState {
        val info = (api.sysInfo() as? SysInfoResult.Success)?.info ?: return state
        var next = state
        if (info.wanIp.isNotEmpty()) {
            if (state.lastWanIp.isNotEmpty() && info.wanIp != state.lastWanIp) {
                notifyWanChange(state.lastWanIp, info.wanIp)
            }
            next = next.copy(lastWanIp = info.wanIp)
        }
        if (info.wanIpv6.isNotEmpty()) {
            if (state.lastWanIpv6.isNotEmpty() && info.wanIpv6 != state.lastWanIpv6) {
                notifyWanChange(state.lastWanIpv6, info.wanIpv6)
            }
            next = next.copy(lastWanIpv6 = info.wanIpv6)
        }
        return next
    }

    private fun notifyWanChange(old: String, new: String) {
        NotificationHelper.notify(
            applicationContext,
            applicationContext.getString(R.string.notif_wan_changed_title),
            applicationContext.getString(R.string.notif_wan_changed_text, old, new),
        )
    }

    /** At most one alert per calendar day for the same (still-crossed) threshold. */
    private fun checkDisk(api: RouterApi, state: PrefsStore.RouterMonitorState): PrefsStore.RouterMonitorState {
        val info = (api.diskInfo() as? DiskInfoResult.Success)?.info ?: return state
        if (info.usedPercent < 90) return state
        val today = System.currentTimeMillis() / TimeUnit.DAYS.toMillis(1)
        if (state.diskAlertEpochDay == today) return state
        NotificationHelper.notify(
            applicationContext,
            applicationContext.getString(R.string.notif_disk_title),
            applicationContext.getString(R.string.notif_disk_text, info.usedPercent),
        )
        return state.copy(diskAlertEpochDay = today)
    }

    /**
     * Compares the CURRENT active node's latency against the last known-good
     * baseline for [group]. A node swap (user picked a different node manually)
     * resets the baseline silently — that's not degradation. Once an alert has
     * fired, it stays latched (no repeat every run) until the node recovers.
     */
    private fun checkLatency(
        api: RouterApi,
        group: String,
        state: PrefsStore.RouterMonitorState,
    ): PrefsStore.RouterMonitorState {
        val activeNode = (api.mihomoProxies() as? MihomoProxiesResult.Success)?.groups
            ?.firstOrNull { it.name == group }?.now ?: return state
        val delays = (api.mihomoGroupDelay(group) as? GroupDelayResult.Success)?.delays
            ?: return state // transient failure — don't corrupt the stored baseline
        val currentMs = delays[activeNode]

        if (state.latencyNode != null && state.latencyNode != activeNode) {
            // Different node than last time we checked — new baseline, no notification.
            return state.copy(latencyNode = activeNode, latencyBaselineMs = currentMs, latencyAlertActive = false)
        }

        if (currentMs == null) {
            if (!state.latencyAlertActive) {
                NotificationHelper.notify(
                    applicationContext,
                    applicationContext.getString(R.string.notif_latency_title),
                    applicationContext.getString(R.string.notif_latency_down_text, activeNode),
                )
            }
            return state.copy(latencyNode = activeNode, latencyAlertActive = true)
        }

        val baseline = state.latencyBaselineMs
        return when {
            baseline == null -> state.copy(latencyNode = activeNode, latencyBaselineMs = currentMs, latencyAlertActive = false)
            currentMs > baseline * 2 -> {
                if (!state.latencyAlertActive) {
                    NotificationHelper.notify(
                        applicationContext,
                        applicationContext.getString(R.string.notif_latency_title),
                        applicationContext.getString(
                            R.string.notif_latency_degraded_text, activeNode, baseline, currentMs,
                        ),
                    )
                }
                // Baseline stays put until recovery — a second worse reading
                // must not re-baseline itself as "normal".
                state.copy(latencyNode = activeNode, latencyAlertActive = true)
            }
            else -> state.copy(latencyNode = activeNode, latencyBaselineMs = currentMs, latencyAlertActive = false)
        }
    }

    companion object {
        private const val UNIQUE_WORK = "router_monitor"

        /** (Re)schedules the periodic check at the user's configured interval. Safe to call repeatedly. */
        fun reschedule(context: Context, intervalMinutes: Int) {
            val request = PeriodicWorkRequestBuilder<RouterMonitorWorker>(
                intervalMinutes.toLong().coerceAtLeast(15), TimeUnit.MINUTES,
            ).setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            ).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
        }
    }
}
