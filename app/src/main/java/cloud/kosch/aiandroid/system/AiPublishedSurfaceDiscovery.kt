package cloud.kosch.aiandroid.system

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.UserHandle
import cloud.kosch.aiandroid.ai.AiPublishedShortcutClassifier
import cloud.kosch.aiandroid.ai.AiPublishedShortcutKind
import cloud.kosch.aiandroid.model.LaunchableApp

/**
 * Discovers only Android surfaces that the target app has deliberately published to launchers.
 *
 * No undocumented intents are reconstructed here. Shortcuts are queried through LauncherApps and widgets through
 * AppWidgetManager. The original user/profile handle is retained so Work Profile routes stay correctly isolated.
 */
data class AiPublishedShortcutSurface(
    val packageName: String,
    val shortcutId: String,
    val label: String,
    val kind: AiPublishedShortcutKind,
    val user: UserHandle,
)

data class AiPublishedWidgetSurface(
    val providerComponent: String,
    val label: String,
    val minWidthDp: Int,
    val minHeightDp: Int,
)

data class AiPublishedSurfaceSnapshot(
    val shortcuts: List<AiPublishedShortcutSurface> = emptyList(),
    val widgets: List<AiPublishedWidgetSurface> = emptyList(),
)

class AiPublishedSurfaceDiscovery(context: Context) {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val appWidgetManager = AppWidgetManager.getInstance(appContext)

    fun shortcuts(app: LaunchableApp): List<AiPublishedShortcutSurface> {
        val query = LauncherApps.ShortcutQuery()
            .setPackage(app.packageName)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
            )
        return runCatching { launcherApps.getShortcuts(query, app.user).orEmpty() }
            .getOrDefault(emptyList())
            .asSequence()
            .filter(ShortcutInfo::isEnabled)
            .mapNotNull { shortcut ->
                val label = shortcut.shortLabel?.toString()?.trim().orEmpty()
                    .ifBlank { shortcut.longLabel?.toString()?.trim().orEmpty() }
                    .ifBlank { shortcut.id }
                val kind = AiPublishedShortcutClassifier.classify(label) ?: return@mapNotNull null
                AiPublishedShortcutSurface(
                    packageName = shortcut.`package`,
                    shortcutId = shortcut.id,
                    label = label,
                    kind = kind,
                    user = app.user,
                )
            }
            .distinctBy { it.shortcutId }
            .take(MAX_SHORTCUTS_PER_APP)
            .toList()
    }

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
                .take(MAX_WIDGETS_PER_APP)
        }.getOrDefault(emptyList())
    }

    fun snapshot(app: LaunchableApp): AiPublishedSurfaceSnapshot = AiPublishedSurfaceSnapshot(
        shortcuts = shortcuts(app),
        widgets = widgets(app.packageName, app.user),
    )

    private companion object {
        const val MAX_SHORTCUTS_PER_APP = 12
        const val MAX_WIDGETS_PER_APP = 8
    }
}
