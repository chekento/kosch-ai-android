package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.SceneId
import cloud.kosch.aiandroid.model.TilePosition
import cloud.kosch.aiandroid.model.WorkspaceCellBounds
import cloud.kosch.aiandroid.model.WorkspaceItem
import cloud.kosch.aiandroid.model.WorkspaceItemContent
import cloud.kosch.aiandroid.model.WorkspaceStableIds
import cloud.kosch.aiandroid.model.WorkspaceV7Migration
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceV7CompatibilityTest {
    @Test
    fun activateLegacyScene_switchesToMatchingScenePage() {
        val initial = WorkspaceV7Migration.fromLegacyScenePositions(SceneId.AI, emptyMap())

        val updated = WorkspaceV7Compatibility.activateLegacyScene(initial, SceneId.WORK)

        assertEquals(WorkspaceStableIds.scenePage(SceneId.WORK), updated.activePageId)
    }

    @Test
    fun applyLegacyScenePositions_updatesOnlyReferencedActionTiles() {
        val initial = WorkspaceV7Migration.fromLegacyScenePositions(SceneId.AI, emptyMap())
        val originalWorkPage = initial.pages.single { it.sceneAdapter == SceneId.WORK }

        val updated = WorkspaceV7Compatibility.applyLegacyScenePositions(
            document = initial,
            scene = SceneId.AI,
            positions = mapOf("ask" to TilePosition(0.51f, 0.49f)),
        )

        val aiPage = updated.pages.single { it.sceneAdapter == SceneId.AI }
        val ask = aiPage.items.single {
            (it.content as? WorkspaceItemContent.ActionTile)?.legacyTileId == "ask"
        }
        assertEquals(WorkspaceCellBounds(6, 6, 6, 6), ask.bounds)
        assertEquals(originalWorkPage, updated.pages.single { it.sceneAdapter == SceneId.WORK })
    }

    @Test
    fun applyLegacyScenePositions_preservesNewWorkspaceItems() {
        val initial = WorkspaceV7Migration.fromLegacyScenePositions(SceneId.AI, emptyMap())
        val customApp = WorkspaceItem(
            id = "item:custom-app",
            bounds = WorkspaceCellBounds(10, 10, 1, 1),
            content = WorkspaceItemContent.App("0:cloud.kosch.example"),
        )
        val withApp = initial.copy(
            pages = initial.pages.map { page ->
                if (page.sceneAdapter == SceneId.AI) page.copy(items = page.items + customApp) else page
            },
        ).normalized()

        val updated = WorkspaceV7Compatibility.applyLegacyScenePositions(
            document = withApp,
            scene = SceneId.AI,
            positions = mapOf("ask" to TilePosition(0.2f, 0.2f)),
        )

        val preserved = updated.pages
            .single { it.sceneAdapter == SceneId.AI }
            .items
            .single { it.id == customApp.id }
        assertEquals(customApp, preserved)
    }
}
