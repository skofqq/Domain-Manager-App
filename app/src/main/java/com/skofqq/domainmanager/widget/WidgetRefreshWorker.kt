package com.skofqq.domainmanager.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.PrefsStore
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.data.ServiceListResult
import com.skofqq.domainmanager.data.ServiceStatus
import com.skofqq.domainmanager.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** service name → its dot view in the fixed widget layout. */
private val SERVICE_DOTS = mapOf(
    "mihomo" to R.id.widget_dot_mihomo,
    "magitrickle" to R.id.widget_dot_magitrickle,
    "zapret" to R.id.widget_dot_zapret,
    "zapret2" to R.id.widget_dot_zapret2,
)

/**
 * One svc_list poll fanned out to every placed widget instance. Failures (router
 * unreachable, phone off Wi-Fi) render all dots grey — an honest "unknown", not
 * a fake "stopped".
 *
 * `AppWidgetManager.updateAppWidget()` swaps the widget's ENTIRE RemoteViews
 * tree, which the launcher re-inflates in one go — every dot and label flashes
 * for a frame even when its own value didn't change. Since the schedule already
 * ticks every 30 min (the platform's own updatePeriodMillis floor) regardless of
 * whether svc_list actually changed, that alone was enough to read as periodic
 * "blinking". The fix is the same in both directions: only call
 * updateAppWidget() when the rendered state (service dots + which widgets exist)
 * is actually different from last time — a same-state tick repaints nothing.
 */
class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val manager = AppWidgetManager.getInstance(applicationContext)
        val ids = manager.getAppWidgetIds(
            ComponentName(applicationContext, ServicesWidgetProvider::class.java)
        )
        if (ids.isEmpty()) return Result.success()

        val result = withContext(Dispatchers.IO) {
            RouterApi(PrefsStore(applicationContext), applicationContext).listServices()
        }
        val services = (result as? ServiceListResult.Success)?.services

        val signature = renderSignature(services, ids)
        val prefs = applicationContext.getSharedPreferences(WIDGET_STATE_PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_SIGNATURE, null) == signature) return Result.success()
        prefs.edit().putString(KEY_LAST_SIGNATURE, signature).apply()

        val views = buildViews(applicationContext, services)
        ids.forEach { id -> manager.updateAppWidget(id, views) }
        return Result.success()
    }

    /** Captures everything [buildViews] actually renders, so an identical result never repaints. */
    private fun renderSignature(services: List<ServiceStatus>?, ids: IntArray): String {
        val servicesPart = services
            ?.sortedBy { it.service }
            ?.joinToString(";") { "${it.service}:${it.running}:${it.enabled}" }
            ?: "unreachable"
        return "${ids.sorted()}|$servicesPart"
    }

    private fun buildViews(context: Context, services: List<ServiceStatus>?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_services)
        SERVICE_DOTS.forEach { (name, viewId) ->
            val status = services?.firstOrNull { it.service == name }
            views.setImageViewResource(
                viewId,
                when {
                    status == null -> R.drawable.widget_dot_grey
                    status.running -> R.drawable.widget_dot_green
                    // Same client-side derivation as the Status tab: stopped but
                    // enabled = should be running = failed.
                    status.enabled -> R.drawable.widget_dot_red
                    else -> R.drawable.widget_dot_grey
                },
            )
        }
        views.setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        return views
    }

    companion object {
        private const val UNIQUE_WORK = "widget_refresh"
        private const val WIDGET_STATE_PREFS = "widget_state"
        private const val KEY_LAST_SIGNATURE = "last_signature"

        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build(),
            )
        }
    }
}
