package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceGridReflowTest {
    @Test
    fun growGrid_preservesEveryItemIdContentAndSpan() {
        val source = documentWithItems(
            WorkspaceItem("one", WorkspaceCellBounds(0, 0, 2, 2), WorkspaceItemContent.App("app:one")),
            WorkspaceItem("two", WorkspaceCellBounds(4, 3, 3, 2), WorkspaceItemContent.Folder("folder:two")),
            WorkspaceItem("three", WorkspaceCellBounds(8, 8, 4, 4), WorkspaceItemContent.Widget("pkg/.Widget")),
        )

        val reflowed = WorkspaceGridReflow.reflow(source, columns = 18, rows = 16)
        val original = source.pages.single().items.associateBy(WorkspaceItem::id)
        val migrated = reflowed.pages.single().items.associateBy(WorkspaceItem::id)

        assertEquals(WorkspaceGridSpec(18, 16), reflowed.grid)
        assertEquals(original.keys, migrated.keys)
        original.forEach { (id, item) ->
            assertEquals(item.content, migrated.getValue(id).content)
            assertEquals(item.bounds.columnSpan, migrated.getValue(id).bounds.columnSpan)
            assertEquals(item.bounds.rowSpan, migrated.getValue(id).bounds.rowSpan)
        }
    }

    @Test
    fun shrinkGrid_reflowsWithoutOverlapWhenEverythingFits() {
        val source = documentWithItems(
            WorkspaceItem("one", WorkspaceCellBounds(0, 0, 2, 2), WorkspaceItemContent.App("app:one")),
            WorkspaceItem("two", WorkspaceCellBounds(4, 0, 2, 2), WorkspaceItemContent.App("app:two")),
            WorkspaceItem("three", WorkspaceCellBounds(0, 6, 2, 2), WorkspaceItemContent.App("app:three")),
            WorkspaceItem("four", WorkspaceCellBounds(8, 8, 2, 2), WorkspaceItemContent.App("app:four")),
        )

        val reflowed = WorkspaceGridReflow.reflow(source, columns = 4, rows = 4)
        val items = reflowed.pages.single().items

        assertEquals(4, items.size)
        assertEquals(setOf("one", "two", "three", "four"), items.map { it.id }.toSet())
        items.forEach { item ->
            assertTrue(item.bounds.column >= 0)
            assertTrue(item.bounds.row >= 0)
            assertTrue(item.bounds.column + item.bounds.columnSpan <= 4)
            assertTrue(item.bounds.row + item.bounds.rowSpan <= 4)
        }
        items.forEachIndexed { index, item ->
            items.drop(index + 1).forEach { other ->
                assertFalse("${item.id} overlaps ${other.id}", item.bounds.overlaps(other.bounds))
            }
        }
    }

    @Test
    fun shrinkGrid_throwsBeforeReturningPartialDocumentWhenCapacityIsInsufficient() {
        val source = documentWithItems(
            WorkspaceItem("one", WorkspaceCellBounds(0, 0, 2, 2), WorkspaceItemContent.App("app:one")),
            WorkspaceItem("two", WorkspaceCellBounds(2, 0, 2, 2), WorkspaceItemContent.App("app:two")),
            WorkspaceItem("three", WorkspaceCellBounds(4, 0, 2, 2), WorkspaceItemContent.App("app:three")),
            WorkspaceItem("four", WorkspaceCellBounds(6, 0, 2, 2), WorkspaceItemContent.App("app:four")),
            WorkspaceItem("five", WorkspaceCellBounds(8, 0, 2, 2), WorkspaceItemContent.App("app:five")),
        )

        val error = assertThrows(IllegalStateException::class.java) {
            WorkspaceGridReflow.reflow(source, columns = 4, rows = 4)
        }

        assertTrue(error.message.orEmpty().contains("nicht genug Platz"))
        assertEquals(WorkspaceGridSpec(12, 12), source.grid)
        assertEquals(5, source.pages.single().items.size)
    }

    private fun documentWithItems(vararg items: WorkspaceItem): WorkspaceDocument = WorkspaceDocument(
        grid = WorkspaceGridSpec(12, 12),
        activePageId = "page:user",
        pages = listOf(
            WorkspacePage(
                id = "page:user",
                title = "Studio",
                order = 0,
                sceneAdapter = null,
                items = items.toList(),
            ),
        ),
    ).normalized()
}
