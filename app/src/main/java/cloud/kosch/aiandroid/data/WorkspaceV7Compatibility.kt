package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.SceneId
import cloud.kosch.aiandroid.model.TilePosition
import cloud.kosch.aiandroid.model.WorkspaceCellBounds
import cloud.kosch.aiandroid.model.WorkspaceDocument
import cloud.kosch.aiandroid.model.WorkspaceItemContent
import cloud.kosch.aiandroid.model.WorkspaceStableIds

/**
 * Temporary compatibility bridge while the M2.5 scene UI and the unified v7 workspace coexist.
 * Legacy writes are mirrored into v7 without making the new model depend on the old controller API.
 */
object WorkspaceV7Compatibility {
    fun activateLegacyScene(document: WorkspaceDocument, scene: SceneId): WorkspaceDocument {
        val normalized = document.normalized()
        val pageId = WorkspaceStableIds.scenePage(scene)
        return if (normalized.pages.any { it.id == pageId }) {
            normalized.copy(activePageId = pageId).normalized()
        } else {
            normalized
        }
    }

    fun applyLegacyScenePositions(
        document: WorkspaceDocument,
        scene: SceneId,
        positions: Map<String, TilePosition>,
    ): WorkspaceDocument {
        val normalized = document.normalized()
        if (positions.isEmpty()) return normalized
        val pageId = WorkspaceStableIds.scenePage(scene)
        val updatedPages = normalized.pages.map pageLoop@{ page ->
            if (page.id != pageId) return@pageLoop page
            page.copy(
                items = page.items.map itemLoop@{ item ->
                    val actionTile = item.content as? WorkspaceItemContent.ActionTile ?: return@itemLoop item
                    if (actionTile.scene != scene) return@itemLoop item
                    val position = positions[actionTile.legacyTileId] ?: return@itemLoop item
                    item.copy(
                        bounds = WorkspaceCellBounds.fromNormalizedTopLeft(
                            x = position.x,
                            y = position.y,
                            columnSpan = item.bounds.columnSpan,
                            rowSpan = item.bounds.rowSpan,
                            grid = normalized.grid,
                        ),
                    )
                },
            )
        }
        return normalized.copy(pages = updatedPages).normalized()
    }
}
