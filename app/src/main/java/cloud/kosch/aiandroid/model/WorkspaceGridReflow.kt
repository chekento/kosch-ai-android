package cloud.kosch.aiandroid.model

/**
 * Pure grid migration used by live Home/Grid settings.
 *
 * Reflow never drops items. Each item keeps its span when possible, is clamped only when the new grid is smaller
 * than that span, and is first placed near its proportional previous position. A projected greedy placement may
 * fragment free space, so the whole page falls back to WorkspaceCompactSolver before a valid grid change is rejected.
 * If neither strategy can fit every item, the operation fails before persistence and the old workspace stays intact.
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
            val projected = projectedPlacement(
                page = page,
                sourceGrid = source.grid,
                targetGrid = targetGrid,
            )
            if (projected != null) {
                page.copy(items = projected)
            } else {
                val packed = WorkspaceCompactSolver.solve(
                    grid = targetGrid,
                    items = page.items,
                ) ?: throw IllegalStateException(
                    "${page.title}: Das Raster ${columns}×${rows} bietet nicht genug Platz für alle Elemente",
                )
                page.copy(
                    items = page.items.map { item ->
                        item.copy(bounds = packed.getValue(item.id))
                    },
                )
            }
        }

        return source.copy(grid = targetGrid, pages = pages).normalized()
    }

    private fun projectedPlacement(
        page: WorkspacePage,
        sourceGrid: WorkspaceGridSpec,
        targetGrid: WorkspaceGridSpec,
    ): List<WorkspaceItem>? {
        val placed = mutableListOf<WorkspaceItem>()
        for (item in page.items) {
            val safeSpanColumns = item.bounds.columnSpan.coerceAtMost(targetGrid.columns)
            val safeSpanRows = item.bounds.rowSpan.coerceAtMost(targetGrid.rows)
            val normalizedX = if (sourceGrid.columns <= 1) 0f
            else item.bounds.column.toFloat() / (sourceGrid.columns - 1).toFloat()
            val normalizedY = if (sourceGrid.rows <= 1) 0f
            else item.bounds.row.toFloat() / (sourceGrid.rows - 1).toFloat()
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
            ) ?: return null
            placed += item.copy(bounds = target)
        }
        return placed
    }
}
