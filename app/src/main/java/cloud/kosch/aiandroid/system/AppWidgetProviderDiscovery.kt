package cloud.kosch.aiandroid.system

import android.appwidget.AppWidgetManager
import android.content.Context
import cloud.kosch.aiandroid.model.LaunchableApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Read-only discovery of Android-published widget providers for one exact launcher app/profile.
 *
 * This never allocates or binds an AppWidget id. Binding remains an explicit Android picker/configuration flow owned
 * by WidgetHostController. Failure to enumerate providers is treated as zero visible providers.
 */
class AppWidgetProviderDiscovery(context: Context) {
    private val manager = AppWidgetManager.getInstance(context.applicationContext)

    suspend fun countFor(app: LaunchableApp): Int = withContext(Dispatchers.IO) {
        runCatching {
            manager.getInstalledProvidersForProfile(app.user)
                .asSequence()
                .filter { it.provider.packageName == app.packageName }
                .distinctBy { it.provider.flattenToString() }
                .take(MAX_PROVIDER_COUNT)
                .count()
        }.getOrDefault(0)
    }

    private companion object {
        const val MAX_PROVIDER_COUNT = 64
    }
}
