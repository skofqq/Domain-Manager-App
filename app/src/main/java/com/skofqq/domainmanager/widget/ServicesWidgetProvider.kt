package com.skofqq.domainmanager.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/**
 * Home-screen widget: four status dots for the managed router services.
 * The widget's own 30-minute schedule (updatePeriodMillis) lands here, and every
 * onUpdate delegates the actual svc_list poll to a WorkManager one-time job — no
 * foreground service, no polling of our own.
 */
class ServicesWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        WidgetRefreshWorker.enqueue(context)
    }
}
