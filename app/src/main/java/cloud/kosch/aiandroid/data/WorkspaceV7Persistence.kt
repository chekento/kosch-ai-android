package cloud.kosch.aiandroid.data

import android.content.SharedPreferences
import cloud.kosch.aiandroid.model.SceneId
import cloud.kosch.aiandroid.model.TilePosition
import cloud.kosch.aiandroid.model.WorkspaceDocument
import cloud.kosch.aiandroid.model.WorkspaceV7Migration

/**
 * Device-local persistence boundary for the portable v7 workspace document.
 *
 * A corrupt or future-format document is never silently overwritten during load. The caller receives a safe
 * legacy-derived fallback while the original value remains available for diagnostics or a newer app version.
 */
class WorkspaceV7Persistence(private val preferences: SharedPreferences) {
    fun loadOrLegacyFallback(
        activeScene: SceneId,
        legacyPositions: Map<SceneId, Map<String, TilePosition>>,
    ): WorkspaceDocument {
        loadStoredOrNull()?.let { return it }
        return WorkspaceV7Migration.fromLegacyScenePositions(activeScene, legacyPositions)
    }

    fun loadStoredOrNull(): WorkspaceDocument? {
        val raw = preferences.getString(KEY_WORKSPACE_DOCUMENT, null) ?: return null
        return runCatching { WorkspaceDocumentCodec.decode(raw) }.getOrNull()
    }

    /** Writes the deterministic v6 → v7 seed only when no v7 value exists yet. */
    fun migrateIfAbsent(
        activeScene: SceneId,
        legacyPositions: Map<SceneId, Map<String, TilePosition>>,
    ): Boolean {
        if (preferences.contains(KEY_WORKSPACE_DOCUMENT)) return false
        val migrated = WorkspaceV7Migration.fromLegacyScenePositions(activeScene, legacyPositions)
        return preferences.edit()
            .putString(KEY_WORKSPACE_DOCUMENT, WorkspaceDocumentCodec.encode(migrated))
            .commit()
    }

    fun save(document: WorkspaceDocument): Boolean = preferences.edit()
        .putString(KEY_WORKSPACE_DOCUMENT, WorkspaceDocumentCodec.encode(document))
        .commit()

    fun hasStoredValue(): Boolean = preferences.contains(KEY_WORKSPACE_DOCUMENT)

    companion object {
        const val KEY_WORKSPACE_DOCUMENT = "workspace_document_v7"
    }
}
