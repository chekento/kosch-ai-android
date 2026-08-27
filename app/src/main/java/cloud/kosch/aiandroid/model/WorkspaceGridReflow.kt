package cloud.kosch.aiandroid.model

/**
 * Pure grid migration used by live Home/Grid settings.
 *
 * Reflow never drops items. Each item keeps its span when possible, is clamped only when the new grid is smaller
 * than that span, and is placed near its proportional previous position. If any page cannot fit all items the
 * operation fails before persistence, allowing Settings Center to keep the previous workspace intact.
 */
object WorkspaceGridReflow {
    fun reflow(
        document: WorkspaceDocument,
        columns: Int,
        rows: Int,
    ): WorkspaceDocument {
        val source = document.normalized()
        val targetGrid = WorkspaceGridSpec(columns = columns, rows = rows)
        if (source.grid == targetGrid) return source

        val pages = source.pages.map { page ->
            val placed = mutableListOf<WorkspaceItem>()
            page.items.forEach { item ->
                val safeSpanColumns = item.bounds.columnSpan.coerceAtMost(targetGrid.columns)
                val safeSpanRows = item.bounds.rowSpan.coerceAtMost(targetGrid.rows)
                val normalizedX = if (source.grid.columns <= 1) 0f
                else item.bounds.column.toFloat() / (source.grid.columns - 1).toFloat()
                val normalizedY = if (source.grid.rows <= 1) 0f
                else item.bounds.row.toFloat() / (source.grid.rows - 1).toFloat()
                val requested = WorkspaceCellBounds.fromNormalizedTopLeft(
                    x = normalizedX,
                    y = normalizedY,
                    columnSpan = safeSpanColumns,
                    rowSpan = safeSpanRows,
                    grid = targetGrid,
                )
                val target = WorkspacePageEditor.nearestAvailableBounds(
                    grid = targetGrid,
                    items = placed,
                    requested = requested,
                ) ?: throw IllegalStateException(
                    "${page.title}: Das Raster ${columns}×${rows} bietet nicht genug Platz für alle Elemente",
                )
                placed += item.copy(bounds = target)
            }
            page.copy(items = placed)
        }

        return source.copy(grid = targetGrid, pages = pages).normalized()
    }
}
