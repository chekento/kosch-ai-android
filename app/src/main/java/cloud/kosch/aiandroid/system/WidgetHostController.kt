package cloud.kosch.aiandroid.system

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import cloud.kosch.aiandroid.model.WidgetSizePreset

class WidgetHostController(context: Context) {
    private val appContext = context.applicationContext
    private val host = AppWidgetHost(appContext, HOST_ID)
    private val manager = AppWidgetManager.getInstance(appContext)

    fun startListening() = host.startListening()

    fun stopListening() = host.stopListening()

    fun allocateId(): Int = host.allocateAppWidgetId()

    fun deleteId(appWidgetId: Int) = host.deleteAppWidgetId(appWidgetId)

    fun pickIntent(appWidgetId: Int): Intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }

    fun configurationIntent(appWidgetId: Int): Intent? {
        val configure = manager.getAppWidgetInfo(appWidgetId)?.configure ?: return null
        return Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            component = configure
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
    }

    fun isValid(appWidgetId: Int): Boolean = manager.getAppWidgetInfo(appWidgetId) != null

    /** Portable component identity only; callers must never serialize the Android host id beside it. */
    fun providerComponent(appWidgetId: Int): String? = manager
        .getAppWidgetInfo(appWidgetId)
        ?.provider
        ?.flattenToString()
        ?.takeIf(String::isNotBlank)

    fun createView(context: Context, appWidgetId: Int, preset: WidgetSizePreset): View? {
        val info = manager.getAppWidgetInfo(appWidgetId) ?: return null
        return host.createView(context, appWidgetId, info).apply {
            setAppWidget(appWidgetId, info)
            updateAppWidgetSize(
                Bundle.EMPTY,
                preset.minWidthDp,
                preset.minHeightDp,
                preset.minWidthDp * 2,
                preset.minHeightDp * 2,
            )
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    private companion object {
        const val HOST_ID = 0x4B4F5343 // "KOSC"
    }
}
