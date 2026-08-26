package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceDragResolverTest {
    private val grid = WorkspaceGridSpec(columns = 12, rows = 12)
    private val item = WorkspaceCellBounds(column = 2, row = 3, columnSpan = 2, rowSpan = 2)

    @Test
    fun inPageDrag_keepsCurrentPageAndRawRequestedBounds() {
        val intent = WorkspaceDragResolver.resolve(
            itemBounds = item,
            deltaColumns = 3,
            deltaRows = 2,
            grid = grid,
            previousPageId = "previous",
            nextPageId = "next",
        )

        assertEquals(WorkspaceDragEdge.NONE, intent.edge)
        assertEquals(null, intent.targetPageId)
        assertEquals(WorkspaceCellBounds(5, 5, 2, 2), intent.requestedBounds)
    }

    @Test
    fun rightEdgeDrag_targetsNextPageAtItsLeftEdge() {
        val intent = WorkspaceDragResolver.resolve(
            itemBounds = item,
            deltaColumns = 9,
            deltaRows = 1,
            grid = grid,
            previousPageId = "previous",
            nextPageId = "next",
        )

        assertEquals(WorkspaceDragEdge.NEXT, intent.edge)
        assertEquals("next", intent.targetPageId)
        assertEquals(WorkspaceCellBounds(0, 4, 2, 2), intent.requestedBounds)
    }

    @Test
    fun leftEdgeDrag_targetsPreviousPageAtItsRightEdge() {
        val intent = WorkspaceDragResolver.resolve(
            itemBounds = item,
            deltaColumns = -3,
            deltaRows = -1,
            grid = grid,
            previousPageId = "previous",
            nextPageId = "next",
        )

        assertEquals(WorkspaceDragEdge.PREVIOUS, intent.edge)
        assertEquals("previous", intent.targetPageId)
        assertEquals(WorkspaceCellBounds(10, 2, 2, 2), intent.requestedBounds)
    }

    @Test
    fun missingAdjacentPage_neverInventsCrossPageTarget() {
        val intent = WorkspaceDragResolver.resolve(
            itemBounds = item,
            deltaColumns = 20,
            deltaRows = 0,
            grid = grid,
            previousPageId = null,
            nextPageId = null,
        )

        assertEquals(WorkspaceDragEdge.NONE, intent.edge)
        assertEquals(null, intent.targetPageId)
        assertEquals(WorkspaceCellBounds(22, 3, 2, 2), intent.requestedBounds)
    }
}
