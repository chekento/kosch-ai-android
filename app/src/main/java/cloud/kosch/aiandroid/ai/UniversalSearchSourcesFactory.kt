package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.CustomLauncherAction
import cloud.kosch.aiandroid.model.LaunchableApp
import cloud.kosch.aiandroid.model.LaunchableShortcut
import cloud.kosch.aiandroid.model.LauncherFolder
import cloud.kosch.aiandroid.model.SettingsFeatureRegistry
import cloud.kosch.aiandroid.model.WorkspacePage

/**
 * Snapshot adapter from current launcher state into the bounded Universal Search index.
 *
 * It copies only display/search metadata and stable target ids. No query history, prompt text, file context, contacts,
 * screen/camera data or secrets are added. Shortcut entries preserve the owning app/profile through appKey lookup.
 */
object UniversalSearchSourcesFactory {
    fun build(
        apps: List<LaunchableApp>,
        loadedShortcuts: List<LaunchableShortcut>,
        folders: List<LauncherFolder>,
        pages: List<WorkspacePage>,
        customActions: List<CustomLauncherAction>,
        appPriorityBoosts: Map<String, Int> = emptyMap(),
    ): UniversalSearchSources {
        val shortcuts = loadedShortcuts.mapNotNull { shortcut ->
            val app = apps.firstOrNull {
                it.packageName == shortcut.packageName && it.userSerialNumber == shortcut.userSerialNumber
            } ?: return@mapNotNull null
            UniversalShortcutSource(
                appKey = app.key,
                shortcutId = shortcut.id,
                label = shortcut.label,
                appLabel = app.label,
            )
        }
        return UniversalSearchSources(
            apps = apps,
            shortcuts = shortcuts,
            folders = folders,
            pages = pages,
            settings = SettingsFeatureRegistry.all,
            customActions = customActions,
            aiRoutes = AI_ROUTES,
            appPriorityBoosts = appPriorityBoosts,
        )
    }

    val AI_ROUTES: List<UniversalAiRouteSource> = listOf(
        UniversalAiRouteSource(
            routeId = "smart",
            title = "Smart AI",
            subtitle = "Beste verfügbare Route lokal bestimmen",
            keywords = listOf("ask", "assistant", "ki", "ai", "router"),
        ),
        UniversalAiRouteSource(
            routeId = "research",
            title = "KI-Recherche",
            subtitle = "Quellenorientierte Recherche starten",
            keywords = listOf("research", "recherche", "web", "quellen"),
        ),
        UniversalAiRouteSource(
            routeId = "summarize",
            title = "Zusammenfassen",
            subtitle = "Inhalt strukturiert zusammenfassen",
            keywords = listOf("summary", "zusammenfassung", "kurz"),
        ),
        UniversalAiRouteSource(
            routeId = "local_private",
            title = "Lokal & privat",
            subtitle = "Cloud-/Browser-Ziele ausdrücklich ausschließen",
            keywords = listOf("offline", "local", "lokal", "private", "privat"),
        ),
        UniversalAiRouteSource(
            routeId = "image",
            title = "Bild-KI",
            subtitle = "Visual-/Bildaufgabe routen",
            keywords = listOf("image", "bild", "visual", "grafik"),
        ),
        UniversalAiRouteSource(
            routeId = "voice",
            title = "Voice AI",
            subtitle = "Sprachfähige Route bevorzugen",
            keywords = listOf("voice", "sprache", "reden", "audio"),
        ),
        UniversalAiRouteSource(
            routeId = "sources",
            title = "Quellen & Notebook",
            subtitle = "Quellen-/Notebook-Ziel bevorzugen",
            keywords = listOf("sources", "quellen", "notebook", "belege"),
        ),
    )
}
