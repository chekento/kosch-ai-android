package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WorkspaceGridReflowFallbackTest {
    @Test
    fun projectedFragmentation_fallsBackToLosslessPacking() {
        val pageId = "page:test"
        val document = WorkspaceDocument(
            grid = WorkspaceGridSpec(12, 12),
            activePageId = pageId,
            pages = listOf(
                WorkspacePage(
                    id = pageId,
                    title = "Test",
                    order = 0,
                    items = listOf(
                        app("a", column = 2),
                        app("b", column = 4),
                        app("c", column = 6),
                    ),
                ),
            ),
        )

        val reflowed = WorkspaceGridReflow.reflow(document, columns = 4, rows = 4)
        val items = reflowed.pages.single().items

        assertEquals(WorkspaceGridSpec(4, 4), reflowed.grid)
        assertEquals(listOf("a", "b", "c"), items.map(WorkspaceItem::id))
        items.forEach { item ->
            assertEquals(2, item.bounds.columnSpan)
            assertEquals(2, item.bounds.rowSpan)
        }
        items.forEachIndexed { index, item ->
            items.drop(index + 1).forEach { other ->
                assertFalse("${item.id} overlaps ${other.id}", item.bounds.overlaps(other.bounds))
            }
        }
    }

    private fun app(id: String, column: Int) = WorkspaceItem(
        id = id,
        bounds = WorkspaceCellBounds(column = column, row = 0, columnSpan = 2, rowSpan = 2),
        content = WorkspaceItemContent.App("app:$id"),
    )
}
