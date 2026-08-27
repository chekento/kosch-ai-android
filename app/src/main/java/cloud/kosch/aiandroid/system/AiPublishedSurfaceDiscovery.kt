package cloud.kosch.aiandroid.system

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.UserHandle

/**
 * Discovers only Android surfaces that the target app has deliberately published to launchers.
 *
 * Shortcuts are already loaded through AppCatalog/LauncherApps when an installed app is selected. This companion
 * discovery covers AppWidget providers so the AI Hub can offer an embedded surface only when Android reports one.
 */
data class AiPublishedWidgetSurface(
    val providerComponent: String,
    val label: String,
    val minWidthDp: Int,
    val minHeightDp: Int,
)

class AiPublishedSurfaceDiscovery(context: Context) {
    private val appContext = context.applicationContext
    private val appWidgetManager = AppWidgetManager.getInstance(appContext)

    fun widgets(packageName: String, user: UserHandle): List<AiPublishedWidgetSurface> {
        val normalized = packageName.trim().takeIf { it.isNotBlank() } ?: return emptyList()
        return runCatching {
            appWidgetManager.getInstalledProvidersForPackage(normalized, user)
                .filter { it.provider.packageName == normalized }
                .distinctBy { it.provider }
                .map { info ->
                    AiPublishedWidgetSurface(
                        providerComponent = info.provider.flattenToString(),
                        label = info.loadLabel(appContext.packageManager)?.toString()?.trim().orEmpty()
                            .ifBlank { info.provider.shortClassName },
                        minWidthDp = info.minWidth.coerceAtLeast(0),
                        minHeightDp = info.minHeight.coerceAtLeast(0),
                    )
                }
        }.getOrDefault(emptyList())
    }
}
