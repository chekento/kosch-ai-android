package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.SceneId
import cloud.kosch.aiandroid.model.TilePosition
import cloud.kosch.aiandroid.model.WorkspaceCellBounds
import cloud.kosch.aiandroid.model.WorkspaceDocument
import cloud.kosch.aiandroid.model.WorkspaceItem
import cloud.kosch.aiandroid.model.WorkspaceItemContent
import cloud.kosch.aiandroid.model.WorkspacePage
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

        assertEquals(WorkspaceDocument.DEFAULT_PAGE_ID, result.activePageId)
        assertTrue(result.pages.any { it.id == WorkspaceStableIds.scenePage(SceneId.WORK) })
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

        assertEquals(WorkspaceDocument.DEFAULT_PAGE_ID, updated.activePageId)
        assertTrue(updated.pages.flatMap { it.items }.any { it == custom })
        val ask = updated.pages
            .single { it.sceneAdapter == SceneId.AI }
            .items
            .single { (it.content as? WorkspaceItemContent.ActionTile)?.legacyTileId == "ask" }
        assertEquals(WorkspaceCellBounds(6, 6, 6, 6), ask.bounds)
    }

    @Test
    fun fullLegacyState_doesNotHijackActiveUserHomePage() {
        val initial = WorkspaceV7Migration.fromLegacyScenePositions(SceneId.AI, emptyMap())
        val customPage = WorkspacePage(
            id = "page:user:desk",
            title = "Desk",
            order = initial.pages.size,
            sceneAdapter = null,
            items = listOf(
                WorkspaceItem(
                    id = "item:user:app",
                    bounds = WorkspaceCellBounds(0, 0, 2, 2),
                    content = WorkspaceItemContent.App("0:cloud.kosch.custom"),
                ),
            ),
        )
        val enriched = initial.copy(
            activePageId = customPage.id,
            pages = initial.pages + customPage,
        ).normalized()

        val updated = WorkspaceV7LegacyMirror.fullLegacyState(
            document = enriched,
            activeScene = SceneId.WORK,
            positions = emptyMap(),
        )

        assertEquals(customPage.id, updated.activePageId)
        assertEquals(customPage, updated.pages.first { it.id == customPage.id })
    }
}
