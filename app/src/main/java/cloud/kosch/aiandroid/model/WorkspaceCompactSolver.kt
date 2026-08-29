package cloud.kosch.aiandroid.model

/**
 * Deterministic bounded packer for Home Studio auto-arrange.
 *
 * Search is performed hardest-first so a valid layout is not rejected merely because the persisted item
 * list happens to put narrow/easy elements before a large one. The returned map is keyed by stable item id;
 * callers can therefore preserve the original item-list order when applying solved bounds.
 *
 * The solver is deliberately bounded. If the search budget is exhausted the caller must keep the existing
 * valid layout rather than throwing or dropping an item.
 */
internal object WorkspaceCompactSolver {
    private const val MAX_SEARCH_ATTEMPTS = 300_000

    fun solve(
        grid: WorkspaceGridSpec,
        items: List<WorkspaceItem>,
    ): Map<String, WorkspaceCellBounds>? {
        if (items.isEmpty()) return emptyMap()
        val totalArea = items.sumOf { it.bounds.columnSpan * it.bounds.rowSpan }
        if (totalArea > grid.columns * grid.rows) return null

        val ordered = items
            .mapIndexed { index, item -> IndexedItem(index, item) }
            .sortedWith(
                compareByDescending<IndexedItem> { it.area }
                    .thenByDescending { maxOf(it.item.bounds.columnSpan, it.item.bounds.rowSpan) }
                    .thenByDescending { minOf(it.item.bounds.columnSpan, it.item.bounds.rowSpan) }
                    .thenBy { it.originalIndex },
            )

        val occupied = BooleanArray(grid.columns * grid.rows)
        val solved = HashMap<String, WorkspaceCellBounds>(items.size)
        var attempts = 0
        var budgetExceeded = false

        fun fits(bounds: WorkspaceCellBounds): Boolean {
            for (row in bounds.row until bounds.row + bounds.rowSpan) {
                val rowOffset = row * grid.columns
                for (column in bounds.column until bounds.column + bounds.columnSpan) {
                    if (occupied[rowOffset + column]) return false
                }
            }
            return true
        }

        fun mark(bounds: WorkspaceCellBounds, value: Boolean) {
            for (row in bounds.row until bounds.row + bounds.rowSpan) {
                val rowOffset = row * grid.columns
                for (column in bounds.column until bounds.column + bounds.columnSpan) {
                    occupied[rowOffset + column] = value
                }
            }
        }

        fun search(index: Int): Boolean {
            if (index == ordered.size) return true
            if (attempts >= MAX_SEARCH_ATTEMPTS) {
                budgetExceeded = true
                return false
            }

            val item = ordered[index].item
            val columnSpan = item.bounds.columnSpan.coerceIn(1, grid.columns)
            val rowSpan = item.bounds.rowSpan.coerceIn(1, grid.rows)
            val lastRow = grid.rows - rowSpan
            val lastColumn = grid.columns - columnSpan

            for (row in 0..lastRow) {
                for (column in 0..lastColumn) {
                    attempts += 1
                    if (attempts > MAX_SEARCH_ATTEMPTS) {
                        budgetExceeded = true
                        return false
                    }
                    val candidate = WorkspaceCellBounds(column, row, columnSpan, rowSpan)
                    if (!fits(candidate)) continue

                    mark(candidate, true)
                    solved[item.id] = candidate
                    if (search(index + 1)) return true
                    solved.remove(item.id)
                    mark(candidate, false)
                    if (budgetExceeded) return false
                }
            }
            return false
        }

        return solved.takeIf { search(0) }?.toMap()
    }

    private data class IndexedItem(
        val originalIndex: Int,
        val item: WorkspaceItem,
    ) {
        val area: Int = item.bounds.columnSpan * item.bounds.rowSpan
    }
}
