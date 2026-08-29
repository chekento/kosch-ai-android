package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceCompactSolverTest {
    @Test
    fun compactUserPage_reconsidersEarlierPlacementsForValidMixedSizes() {
        val pageId = "page:user:packing"
        val items = listOf(
            WorkspaceItem(
                id = "narrow-1",
                bounds = WorkspaceCellBounds(0, 0, 1, 2),
                content = WorkspaceItemContent.App("app:narrow-1"),
            ),
            WorkspaceItem(
                id = "narrow-2",
                bounds = WorkspaceCellBounds(0, 2, 1, 2),
                content = WorkspaceItemContent.App("app:narrow-2"),
            ),
            WorkspaceItem(
                id = "large",
                bounds = WorkspaceCellBounds(1, 0, 3, 3),
                content = WorkspaceItemContent.App("app:large"),
            ),
        )
        val document = WorkspaceDocument(
            grid = WorkspaceGridSpec(columns = 4, rows = 4),
            activePageId = pageId,
            pages = listOf(WorkspacePage(pageId, "Packing", 0, items = items)),
        )

        val compacted = WorkspacePageEditor.compactUserPage(document, pageId)
        val after = compacted.pages.single().items

        assertEquals(items.map { it.id }, after.map { it.id })
        assertEquals(items.map { it.content }, after.map { it.content })
        assertEquals(items.map { it.bounds.columnSpan to it.bounds.rowSpan }, after.map { it.bounds.columnSpan to it.bounds.rowSpan })
        after.forEachIndexed { index, item ->
            after.drop(index + 1).forEach { other ->
                assertFalse("${item.id} overlaps ${other.id}", item.bounds.overlaps(other.bounds))
            }
        }
        assertEquals(WorkspaceCellBounds(0, 0, 3, 3), after.single { it.id == "large" }.bounds)
        assertTrue(after.single { it.id == "narrow-1" }.bounds.column >= 3 || after.single { it.id == "narrow-1" }.bounds.row >= 3)
    }

    @Test
    fun solver_isDeterministicForSameInput() {
        val grid = WorkspaceGridSpec(columns = 6, rows = 6)
        val items = listOf(
            WorkspaceItem("a", WorkspaceCellBounds(0, 0, 2, 4), WorkspaceItemContent.App("app:a")),
            WorkspaceItem("b", WorkspaceCellBounds(2, 0, 4, 2), WorkspaceItemContent.App("app:b")),
            WorkspaceItem("c", WorkspaceCellBounds(2, 2, 2, 2), WorkspaceItemContent.Folder("folder:c")),
        )

        assertEquals(
            WorkspaceCompactSolver.solve(grid, items),
            WorkspaceCompactSolver.solve(grid, items),
        )
    }
}
