package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.WorkspaceDocument
import cloud.kosch.aiandroid.model.WorkspacePage

/**
 * Lossless UI migration for installations that already persisted a scene-only v7 document before the reference-UI
 * redesign. The legacy pages and every item remain intact; KAL only prepends one empty portable Home page once.
 */
object WorkspaceHomeUpgrade {
    fun ensureCleanHome(document: WorkspaceDocument): WorkspaceDocument {
        val normalized = document.normalized()
        if (normalized.pages.any { it.sceneAdapter == null }) return normalized

        val cleanHome = WorkspacePage(
            id = WorkspaceDocument.DEFAULT_PAGE_ID,
            title = "Home",
            order = 0,
        )
        return normalized.copy(
            activePageId = cleanHome.id,
            pages = listOf(cleanHome) + normalized.pages.mapIndexed { index, page ->
                page.copy(order = index + 1)
            },
        ).normalized()
    }
}
