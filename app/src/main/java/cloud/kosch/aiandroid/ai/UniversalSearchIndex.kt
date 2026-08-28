package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.CustomLauncherAction
import cloud.kosch.aiandroid.model.LaunchableApp
import cloud.kosch.aiandroid.model.LauncherFolder
import cloud.kosch.aiandroid.model.SettingsFeatureDefinition
import cloud.kosch.aiandroid.model.WorkspacePage

enum class UniversalSearchKind {
    APP,
    APP_SHORTCUT,
    FOLDER,
    PAGE,
    SETTING,
    CUSTOM_ACTION,
    AI_ROUTE,
}

sealed interface UniversalSearchTarget {
    data class App(val appKey: String) : UniversalSearchTarget
    data class AppShortcut(val appKey: String, val shortcutId: String) : UniversalSearchTarget
    data class Folder(val folderId: String) : UniversalSearchTarget
    data class Page(val pageId: String) : UniversalSearchTarget
    data class Setting(val featureId: String) : UniversalSearchTarget
    data class CustomAction(val actionId: String) : UniversalSearchTarget
    data class AiRoute(val routeId: String) : UniversalSearchTarget
}

data class UniversalSearchEntry(
    val id: String,
    val kind: UniversalSearchKind,
    val title: String,
    val subtitle: String = "",
    val keywords: List<String> = emptyList(),
    val target: UniversalSearchTarget,
    /** Optional bounded local ranking hint; never contains query/prompt/history text. */
    val localPriorityBoost: Int = 0,
)

data class UniversalShortcutSource(
    val appKey: String,
    val shortcutId: String,
    val label: String,
    val appLabel: String,
)

data class UniversalAiRouteSource(
    val routeId: String,
    val title: String,
    val subtitle: String = "",
    val keywords: List<String> = emptyList(),
)

data class UniversalSearchSources(
    val apps: List<LaunchableApp> = emptyList(),
    val shortcuts: List<UniversalShortcutSource> = emptyList(),
    val folders: List<LauncherFolder> = emptyList(),
    val pages: List<WorkspacePage> = emptyList(),
    val settings: List<SettingsFeatureDefinition> = emptyList(),
    val customActions: List<CustomLauncherAction> = emptyList(),
    val aiRoutes: List<UniversalAiRouteSource> = emptyList(),
    val appPriorityBoosts: Map<String, Int> = emptyMap(),
)

data class RankedUniversalSearchEntry(
    val entry: UniversalSearchEntry,
    val score: Int,
    val reason: SearchMatchReason,
)

/**
 * Bounded, fully local launcher-wide search index. Search and execution remain separated: results contain typed
 * targets only; the caller still applies the existing confirmation/capability policy before any external action.
 */
object UniversalSearchIndex {
    fun build(sources: UniversalSearchSources): List<UniversalSearchEntry> = buildList {
        sources.apps.take(MAX_APPS).forEach { app ->
            add(
                UniversalSearchEntry(
                    id = "app:${app.key}",
                    kind = UniversalSearchKind.APP,
                    title = app.label,
                    subtitle = app.packageName,
                    keywords = listOf(app.packageName, app.profile.title),
                    target = UniversalSearchTarget.App(app.key),
                    localPriorityBoost = sources.appPriorityBoosts[app.key].orZero().coerceIn(0, MAX_LOCAL_BOOST),
                ),
            )
        }
        sources.shortcuts.take(MAX_SHORTCUTS).forEach { shortcut ->
            add(
                UniversalSearchEntry(
                    id = "shortcut:${shortcut.appKey}:${shortcut.shortcutId}",
                    kind = UniversalSearchKind.APP_SHORTCUT,
                    title = shortcut.label,
                    subtitle = shortcut.appLabel,
                    keywords = listOf(shortcut.appLabel),
                    target = UniversalSearchTarget.AppShortcut(shortcut.appKey, shortcut.shortcutId),
                ),
            )
        }
        sources.folders.take(MAX_FOLDERS).forEach { folder ->
            add(
                UniversalSearchEntry(
                    id = "folder:${folder.id}",
                    kind = UniversalSearchKind.FOLDER,
                    title = folder.title,
                    subtitle = folder.kind.title,
                    keywords = listOf(folder.kind.title, "Ordner", "Folder"),
                    target = UniversalSearchTarget.Folder(folder.id),
                ),
            )
        }
        sources.pages.take(MAX_PAGES).forEach { page ->
            add(
                UniversalSearchEntry(
                    id = "page:${page.id}",
                    kind = UniversalSearchKind.PAGE,
                    title = page.title,
                    subtitle = page.sceneAdapter?.title ?: "Home",
                    keywords = listOfNotNull("Seite", "Page", page.sceneAdapter?.title),
                    target = UniversalSearchTarget.Page(page.id),
                ),
            )
        }
        sources.settings.take(MAX_SETTINGS).forEach { feature ->
            add(
                UniversalSearchEntry(
                    id = "setting:${feature.id}",
                    kind = UniversalSearchKind.SETTING,
                    title = feature.title,
                    subtitle = feature.section.title,
                    keywords = feature.keywords.toList() + feature.id + feature.section.title,
                    target = UniversalSearchTarget.Setting(feature.id),
                ),
            )
        }
        sources.customActions.take(MAX_CUSTOM_ACTIONS).forEach { action ->
            add(
                UniversalSearchEntry(
                    id = "action:${action.id}",
                    kind = UniversalSearchKind.CUSTOM_ACTION,
                    title = action.name,
                    subtitle = "Eigene Aktion",
                    keywords = listOf("Action", "Aktion", action.id),
                    target = UniversalSearchTarget.CustomAction(action.id),
                ),
            )
        }
        sources.aiRoutes.take(MAX_AI_ROUTES).forEach { route ->
            add(
                UniversalSearchEntry(
                    id = "ai:${route.routeId}",
                    kind = UniversalSearchKind.AI_ROUTE,
                    title = route.title,
                    subtitle = route.subtitle,
                    keywords = route.keywords + listOf("AI", "KI"),
                    target = UniversalSearchTarget.AiRoute(route.routeId),
                ),
            )
        }
    }
        .distinctBy(UniversalSearchEntry::id)
        .take(MAX_TOTAL_ENTRIES)

    fun rank(query: String, entries: List<UniversalSearchEntry>): List<RankedUniversalSearchEntry> {
        val documents = entries.associateBy(
            keySelector = UniversalSearchEntry::id,
            valueTransform = { entry ->
                SearchDocument(
                    id = entry.id,
                    title = entry.title,
                    keywords = entry.keywords + entry.subtitle,
                )
            },
        )
        val ranked = SearchRanker.rankDetailed(query, documents.values.toList())
        return ranked.mapNotNull { result ->
            val entry = entries.firstOrNull { it.id == result.document.id } ?: return@mapNotNull null
            RankedUniversalSearchEntry(
                entry = entry,
                score = result.score + entry.localPriorityBoost.coerceIn(0, MAX_LOCAL_BOOST),
                reason = result.reason,
            )
        }.sortedWith(
            compareByDescending<RankedUniversalSearchEntry> { it.score }
                .thenBy { kindOrder(it.entry.kind) }
                .thenBy { it.entry.title.lowercase() },
        )
    }

    private fun kindOrder(kind: UniversalSearchKind): Int = when (kind) {
        UniversalSearchKind.APP -> 0
        UniversalSearchKind.APP_SHORTCUT -> 1
        UniversalSearchKind.CUSTOM_ACTION -> 2
        UniversalSearchKind.FOLDER -> 3
        UniversalSearchKind.PAGE -> 4
        UniversalSearchKind.SETTING -> 5
        UniversalSearchKind.AI_ROUTE -> 6
    }

    private fun Int?.orZero(): Int = this ?: 0

    private const val MAX_APPS = 4_096
    private const val MAX_SHORTCUTS = 2_048
    private const val MAX_FOLDERS = 512
    private const val MAX_PAGES = 256
    private const val MAX_SETTINGS = 512
    private const val MAX_CUSTOM_ACTIONS = 512
    private const val MAX_AI_ROUTES = 256
    private const val MAX_TOTAL_ENTRIES = 8_192
    private const val MAX_LOCAL_BOOST = 80
}
