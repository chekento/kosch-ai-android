package cloud.kosch.aiandroid.system

import android.appwidget.AppWidgetHost
import android.content.Context

/**
 * Owns the launcher's widget-host identity and lifecycle. Picking, binding and rendering widgets
 * intentionally remain disabled until the workspace can persist every corresponding host ID.
 */
class WidgetHostController(context: Context) {
    private val host = AppWidgetHost(context, HOST_ID)

    fun startListening() = host.startListening()

    fun stopListening() = host.stopListening()

    private companion object {
        const val HOST_ID = 0x4B4F5343 // "KOSC"
    }
}

