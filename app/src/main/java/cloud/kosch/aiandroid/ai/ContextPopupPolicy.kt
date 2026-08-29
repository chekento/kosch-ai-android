package cloud.kosch.aiandroid.ai

/**
 * Pure, bounded ordering policy for the app context popup.
 *
 * Inputs deliberately contain only launcher-owned ids/labels plus package-level badge count. Notification titles,
 * message bodies, people, Android Intent payloads and widget binding ids are outside this model. Execution remains in
 * the existing LauncherController/Android capability routes.
 */
data class ContextPopupShortcut(
    val id: String,
    val label: String,
)

data class ContextPopupInput(
    val appKey: String,
    val appLabel: String,
    val isPinned: Boolean,
    val isHidden: Boolean,
    val badgeCount: Int = 0,
    val publishedShortcuts: List<ContextPopupShortcut> = emptyList(),
    val publishedWidgetCount: Int = 0,
)

enum class ContextPopupItemKind {
    OPEN_APP,
    PUBLISHED_SHORTCUT,
    BADGE_SUMMARY,
    WIDGET_ENTRY,
    PIN_TOGGLE,
    HIDE_TOGGLE,
    APP_INFO,
    STORE,
    FOLDER_ASSIGN,
}

sealed interface ContextPopupItem {
    val stableId: String
    val kind: ContextPopupItemKind
    val title: String

    data class Action(
        override val stableId: String,
        override val kind: ContextPopupItemKind,
        override val title: String,
    ) : ContextPopupItem

    data class Shortcut(
        val shortcutId: String,
        override val title: String,
    ) : ContextPopupItem {
        override val stableId: String = "shortcut:$shortcutId"
        override val kind: ContextPopupItemKind = ContextPopupItemKind.PUBLISHED_SHORTCUT
    }

    data class BadgeSummary(
        val count: Int,
    ) : ContextPopupItem {
        override val stableId: String = "badge"
        override val kind: ContextPopupItemKind = ContextPopupItemKind.BADGE_SUMMARY
        override val title: String = "$count Benachrichtigungen"
    }

    data class WidgetEntry(
        val providerCount: Int,
    ) : ContextPopupItem {
        override val stableId: String = "widgets"
        override val kind: ContextPopupItemKind = ContextPopupItemKind.WIDGET_ENTRY
        override val title: String = if (providerCount == 1) "1 Widget verfügbar" else "$providerCount Widgets verfügbar"
    }
}

object ContextPopupPolicy {
    const val MAX_SHORTCUTS = 6
    const val MAX_ITEMS = 16
    const val MAX_BADGE_COUNT = 999
    const val MAX_LABEL_CHARS = 120

    fun build(input: ContextPopupInput): List<ContextPopupItem> {
        if (input.appKey.isBlank() || input.appLabel.isBlank()) return emptyList()

        return buildList {
            add(
                ContextPopupItem.Action(
                    stableId = "open",
                    kind = ContextPopupItemKind.OPEN_APP,
                    title = "${input.appLabel.take(MAX_LABEL_CHARS)} öffnen",
                ),
            )

            input.publishedShortcuts
                .asSequence()
                .filter { it.id.isNotBlank() && it.label.isNotBlank() }
                .distinctBy(ContextPopupShortcut::id)
                .sortedWith(
                    compareBy<ContextPopupShortcut> { it.label.lowercase() }
                        .thenBy(ContextPopupShortcut::id),
                )
                .take(MAX_SHORTCUTS)
                .forEach { shortcut ->
                    add(
                        ContextPopupItem.Shortcut(
                            shortcutId = shortcut.id.take(512),
                            title = shortcut.label.trim().take(MAX_LABEL_CHARS),
                        ),
                    )
                }

            input.badgeCount.coerceIn(0, MAX_BADGE_COUNT)
                .takeIf { it > 0 }
                ?.let { add(ContextPopupItem.BadgeSummary(it)) }

            input.publishedWidgetCount.coerceIn(0, 64)
                .takeIf { it > 0 }
                ?.let { add(ContextPopupItem.WidgetEntry(it)) }

            add(
                ContextPopupItem.Action(
                    stableId = "pin",
                    kind = ContextPopupItemKind.PIN_TOGGLE,
                    title = if (input.isPinned) "Aus Dock lösen" else "Ins Dock",
                ),
            )
            add(
                ContextPopupItem.Action(
                    stableId = "hide",
                    kind = ContextPopupItemKind.HIDE_TOGGLE,
                    title = if (input.isHidden) "Einblenden" else "Verbergen",
                ),
            )
            add(ContextPopupItem.Action("info", ContextPopupItemKind.APP_INFO, "App-Info"))
            add(ContextPopupItem.Action("store", ContextPopupItemKind.STORE, "Store"))
            add(ContextPopupItem.Action("folder", ContextPopupItemKind.FOLDER_ASSIGN, "In Ordner"))
        }.take(MAX_ITEMS)
    }
}
