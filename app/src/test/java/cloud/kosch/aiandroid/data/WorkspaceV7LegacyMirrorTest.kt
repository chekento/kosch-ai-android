package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.SceneId
import cloud.kosch.aiandroid.model.TilePosition
import cloud.kosch.aiandroid.model.WorkspaceCellBounds
import cloud.kosch.aiandroid.model.WorkspaceItem
import cloud.kosch.aiandroid.model.WorkspaceItemContent
import cloud.kosch.aiandroid.model.WorkspaceStableIds
import cloud.kosch.aiandroid.model.WorkspaceV7Migration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceV7LegacyMirrorTest {
    @Test
    fun seedOrUpdate_absentV7_seedsFromCurrentLegacyState() {
        val positions = mapOf(
            SceneId.AI to mapOf("ask" to TilePosition(0.51f, 0.49f)),
        )

        val result = WorkspaceV7LegacyMirror.seedOrUpdate(
            storedDocument = null,
            hasStoredRawValue = false,
            activeScene = SceneId.WORK,
            positions = positions,
        )!!

        assertEquals(WorkspaceStableIds.scenePage(SceneId.WORK), result.activePageId)
        val ask = result.pages
            .single { it.sceneAdapter == SceneId.AI }
            .items
            .single { (it.content as? WorkspaceItemContent.ActionTile)?.legacyTileId == "ask" }
        assertEquals(WorkspaceCellBounds(6, 6, 6, 6), ask.bounds)
    }

    @Test
    fun seedOrUpdate_unknownRawV7_returnsNullSoCallerPreservesIt() {
        assertNull(
            WorkspaceV7LegacyMirror.seedOrUpdate(
                storedDocument = null,
                hasStoredRawValue = true,
                activeScene = SceneId.AI,
                positions = emptyMap(),
            ),
        )
    }

    @Test
    fun fullLegacyState_updatesLegacyAdapters_butPreservesV7OnlyItems() {
        val initial = WorkspaceV7Migration.fromLegacyScenePositions(SceneId.AI, emptyMap())
        val custom = WorkspaceItem(
            id = "item:custom-app",
            bounds = WorkspaceCellBounds(10, 10, 1, 1),
            content = WorkspaceItemContent.App("0:cloud.kosch.custom"),
        )
        val enriched = initial.copy(
            pages = initial.pages.map { page ->
                if (page.sceneAdapter == SceneId.AI) page.copy(items = page.items + custom) else page
            },
        ).normalized()

        val updated = WorkspaceV7LegacyMirror.fullLegacyState(
            document = enriched,
            activeScene = SceneId.STUDIO,
            positions = mapOf(
                SceneId.AI to mapOf("ask" to TilePosition(0.51f, 0.49f)),
            ),
        )

        assertEquals(WorkspaceStableIds.scenePage(SceneId.STUDIO), updated.activePageId)
        assertTrue(updated.pages.flatMap { it.items }.any { it == custom })
        val ask = updated.pages
            .single { it.sceneAdapter == SceneId.AI }
            .items
            .single { (it.content as? WorkspaceItemContent.ActionTile)?.legacyTileId == "ask" }
        assertEquals(WorkspaceCellBounds(6, 6, 6, 6), ask.bounds)
    }
}
