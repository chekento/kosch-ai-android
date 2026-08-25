package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceEngineModelsTest {
    @Test
    fun grid_rejectsUnboundedDimensions() {
        var failed = false
        try {
            WorkspaceGridSpec(columns = 65, rows = 12)
        } catch (_: IllegalArgumentException) {
            failed = true
        }

        assertTrue(failed)
    }

    @Test
    fun boundsClamp_spansAndCoordinatesStayInsideGrid() {
        val grid = WorkspaceGridSpec(columns = 12, rows = 10)

        val clamped = WorkspaceCellBounds(
            column = -5,
            row = 99,
            columnSpan = 30,
            rowSpan = 0,
        ).clamped(grid)

        assertEquals(0, clamped.column)
        assertEquals(9, clamped.row)
        assertEquals(12, clamped.columnSpan)
        assertEquals(1, clamped.rowSpan)
    }

    @Test
    fun overlap_touchingEdgesAreNotCollisions() {
        val left = WorkspaceCellBounds(0, 0, 6, 6)
        val right = WorkspaceCellBounds(6, 0, 6, 6)
        val overlapping = WorkspaceCellBounds(5, 1, 6, 6)

        assertFalse(left.overlaps(right))
        assertTrue(left.overlaps(overlapping))
        assertTrue(overlapping.overlaps(left))
    }

    @Test
    fun portableContent_rejectsBlankReferencesButAllowsWidgetRemapPlaceholder() {
        var appRejected = false
        var folderRejected = false
        var widgetRejected = false
        try {
            WorkspaceItemContent.App("   ")
        } catch (_: IllegalArgumentException) {
            appRejected = true
        }
        try {
            WorkspaceItemContent.Folder("")
        } catch (_: IllegalArgumentException) {
            folderRejected = true
        }
        try {
            WorkspaceItemContent.Widget("  ")
        } catch (_: IllegalArgumentException) {
            widgetRejected = true
        }

        assertTrue(appRejected)
        assertTrue(folderRejected)
        assertTrue(widgetRejected)
        assertEquals(null, WorkspaceItemContent.Widget(null).providerComponent)
    }

    @Test
    fun normalizedDocument_recoversInvalidActivePageAndDeduplicatesLocalIds() {
        val page = WorkspacePage(
            id = "page:a",
            title = "  ",
            order = 8,
            items = listOf(
                WorkspaceItem(
                    id = "item:a",
                    bounds = WorkspaceCellBounds(-1, 50, 99, 2),
                    content = WorkspaceItemContent.App("profile:package"),
                ),
                WorkspaceItem(
                    id = "item:a",
                    bounds = WorkspaceCellBounds(2, 2, 1, 1),
                    content = WorkspaceItemContent.Folder("folder-a"),
                ),
            ),
        )

        val normalized = WorkspaceDocument(
            activePageId = "missing",
            pages = listOf(page, page.copy(title = "duplicate")),
        ).normalized()

        assertEquals(WORKSPACE_SCHEMA_VERSION, normalized.schemaVersion)
        assertEquals(1, normalized.pages.size)
        assertEquals("page:a", normalized.activePageId)
        assertEquals("Home 1", normalized.pages.single().title)
        assertEquals(1, normalized.pages.single().items.size)
        assertEquals(WorkspaceCellBounds(0, 10, 12, 2), normalized.pages.single().items.single().bounds)
    }

    @Test
    fun normalizedDocument_rejectsUnsupportedSchemaInsteadOfDowngradingIt() {
        var failed = false
        try {
            WorkspaceDocument(
                schemaVersion = WORKSPACE_SCHEMA_VERSION + 1,
                activePageId = "page:a",
                pages = listOf(WorkspacePage("page:a", "A", 0)),
            ).normalized()
        } catch (_: IllegalArgumentException) {
            failed = true
        }

        assertTrue(failed)
    }

    @Test
    fun normalizedDocument_deduplicatesItemIdsAcrossPages() {
        val duplicate = WorkspaceItem(
            id = "item:global",
            bounds = WorkspaceCellBounds(0, 0, 1, 1),
            content = WorkspaceItemContent.App("app:key"),
        )
        val normalized = WorkspaceDocument(
            activePageId = "page:a",
            pages = listOf(
                WorkspacePage("page:a", "A", 0, items = listOf(duplicate)),
                WorkspacePage("page:b", "B", 1, items = listOf(duplicate.copy(bounds = WorkspaceCellBounds(2, 2, 1, 1)))),
            ),
        ).normalized()

        assertEquals(1, normalized.pages.sumOf { it.items.size })
        assertEquals("item:global", normalized.pages.first().items.single().id)
        assertTrue(normalized.pages[1].items.isEmpty())
    }

    @Test
    fun normalizedDocument_neverProducesZeroPages() {
        val normalized = WorkspaceDocument(
            activePageId = "missing",
            pages = emptyList(),
        ).normalized()

        assertEquals(1, normalized.pages.size)
        assertEquals(WorkspaceDocument.DEFAULT_PAGE_ID, normalized.activePageId)
    }

    @Test
    fun legacyMigration_preservesEverySceneAndUsesDeterministicStableIds() {
        val positions = SceneId.entries.associateWith { scene ->
            DefaultWorkspace.tiles(scene).associate { tile -> tile.id to tile.defaultPosition }
        }

        val first = WorkspaceV7Migration.fromLegacyScenePositions(SceneId.WORK, positions)
        val second = WorkspaceV7Migration.fromLegacyScenePositions(SceneId.WORK, positions)

        assertEquals(first, second)
        assertEquals(SceneId.entries.size, first.pages.size)
        assertEquals(SceneId.entries.size * 4, first.pages.sumOf { it.items.size })
        assertEquals(WorkspaceStableIds.scenePage(SceneId.WORK), first.activePageId)
        assertEquals(first.pages.flatMap { it.items }.size, first.pages.flatMap { it.items }.map { it.id }.distinct().size)
    }

    @Test
    fun legacyMigration_mapsExistingQuadrantsToNonOverlappingGridCells() {
        val migrated = WorkspaceV7Migration.fromLegacyScenePositions(
            activeScene = SceneId.AI,
            positions = emptyMap(),
        )
        val ai = migrated.pages.single { it.sceneAdapter == SceneId.AI }
        val bounds = ai.items.map(WorkspaceItem::bounds)

        assertEquals(
            listOf(
                WorkspaceCellBounds(0, 0, 6, 6),
                WorkspaceCellBounds(6, 0, 6, 6),
                WorkspaceCellBounds(0, 6, 6, 6),
                WorkspaceCellBounds(6, 6, 6, 6),
            ),
            bounds,
        )
        bounds.forEachIndexed { index, candidate ->
            bounds.forEachIndexed { otherIndex, other ->
                if (index != otherIndex) assertFalse(candidate.overlaps(other))
            }
        }
    }

    @Test
    fun legacyMigration_preservesCustomTilePosition() {
        val custom = mapOf(
            SceneId.AI to mapOf("ask" to TilePosition(0.51f, 0.49f)),
        )

        val migrated = WorkspaceV7Migration.fromLegacyScenePositions(SceneId.AI, custom)
        val ask = migrated.pages
            .single { it.sceneAdapter == SceneId.AI }
            .items
            .single { (it.content as? WorkspaceItemContent.ActionTile)?.legacyTileId == "ask" }

        assertEquals(WorkspaceCellBounds(6, 6, 6, 6), ask.bounds)
    }

    @Test
    fun stableIds_separateScenesEvenWhenLegacyTileIdMatches() {
        val ai = WorkspaceStableIds.legacySceneTile(SceneId.AI, "same")
        val work = WorkspaceStableIds.legacySceneTile(SceneId.WORK, "same")

        assertNotEquals(ai, work)
        assertEquals(ai, WorkspaceStableIds.legacySceneTile(SceneId.AI, "same"))
    }

    @Test
    fun deviceWidgetBinding_requiresPositiveHostId() {
        var failed = false
        try {
            DeviceWidgetBinding("item:widget", 0)
        } catch (_: IllegalArgumentException) {
            failed = true
        }

        assertTrue(failed)
        assertEquals(42, DeviceWidgetBinding("item:widget", 42).appWidgetId)
    }
}
