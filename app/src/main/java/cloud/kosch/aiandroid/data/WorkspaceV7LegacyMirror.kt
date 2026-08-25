package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.SceneId
import cloud.kosch.aiandroid.model.TilePosition
import cloud.kosch.aiandroid.model.WorkspaceDocument
import cloud.kosch.aiandroid.model.WorkspaceV7Migration

/**
 * Pure planning layer for the temporary v6 -> v7 dual-write period.
 *
 * Legacy scene writes keep their action-tile pages synchronized without hijacking a user-created v7 Home page.
 * If a raw v7 value exists but cannot be decoded, callers get null so the unknown/future value is preserved.
 */
object WorkspaceV7LegacyMirror {
    fun sceneUpdate(document: WorkspaceDocument, scene: SceneId): WorkspaceDocument =
        if (activePageIsLegacy(document)) WorkspaceV7Compatibility.activateLegacyScene(document, scene)
        else document.normalized()

    fun positionUpdate(
        document: WorkspaceDocument,
        scene: SceneId,
        positions: Map<String, TilePosition>,
    ): WorkspaceDocument = WorkspaceV7Compatibility.applyLegacyScenePositions(document, scene, positions)

    fun fullLegacyState(
        document: WorkspaceDocument,
        activeScene: SceneId,
        positions: Map<SceneId, Map<String, TilePosition>>,
    ): WorkspaceDocument {
        var updated = document.normalized()
        if (activePageIsLegacy(updated)) {
            updated = WorkspaceV7Compatibility.activateLegacyScene(updated, activeScene)
        }
        SceneId.entries.forEach { scene ->
            updated = WorkspaceV7Compatibility.applyLegacyScenePositions(
                document = updated,
                scene = scene,
                positions = positions[scene].orEmpty(),
            )
        }
        return updated.normalized()
    }

    /**
     * Returns the document that may safely be written next to legacy state.
     * null means a raw but undecodable v7 value exists and must be left untouched.
     */
    fun seedOrUpdate(
        storedDocument: WorkspaceDocument?,
        hasStoredRawValue: Boolean,
        activeScene: SceneId,
        positions: Map<SceneId, Map<String, TilePosition>>,
    ): WorkspaceDocument? = when {
        storedDocument != null -> fullLegacyState(storedDocument, activeScene, positions)
        hasStoredRawValue -> null
        else -> WorkspaceV7Migration.fromLegacyScenePositions(activeScene, positions)
    }

    private fun activePageIsLegacy(document: WorkspaceDocument): Boolean {
        val normalized = document.normalized()
        return normalized.pages.firstOrNull { it.id == normalized.activePageId }?.sceneAdapter != null
    }
}
