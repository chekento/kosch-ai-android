package cloud.kosch.aiandroid.model

/** Which page edge a drag crossed before collision resolution. */
enum class WorkspaceDragEdge {
    NONE,
    PREVIOUS,
    NEXT,
}

data class WorkspaceDragIntent(
    val edge: WorkspaceDragEdge,
    val targetPageId: String?,
    val requestedBounds: WorkspaceCellBounds,
)

/**
 * Pure pointer-to-workspace resolution shared by live preview and the final drop commit.
 *
 * Keeping this outside Compose is important: a drop must be derived from the final pointer delta even when
 * the last pointer event and ACTION_UP arrive before another recomposition can publish visual preview state.
 */
object WorkspaceDragResolver {
    fun resolve(
        itemBounds: WorkspaceCellBounds,
        deltaColumns: Int,
        deltaRows: Int,
        grid: WorkspaceGridSpec,
        previousPageId: String?,
        nextPageId: String?,
    ): WorkspaceDragIntent {
        val rawColumn = itemBounds.column + deltaColumns
        val rawRow = itemBounds.row + deltaRows

        val edge = when {
            rawColumn < 0 && previousPageId != null -> WorkspaceDragEdge.PREVIOUS
            rawColumn + itemBounds.columnSpan > grid.columns && nextPageId != null -> WorkspaceDragEdge.NEXT
            else -> WorkspaceDragEdge.NONE
        }
        val targetPageId = when (edge) {
            WorkspaceDragEdge.PREVIOUS -> previousPageId
            WorkspaceDragEdge.NEXT -> nextPageId
            WorkspaceDragEdge.NONE -> null
        }
        val requested = when (edge) {
            WorkspaceDragEdge.PREVIOUS -> itemBounds.copy(
                column = grid.columns - itemBounds.columnSpan,
                row = rawRow,
            )
            WorkspaceDragEdge.NEXT -> itemBounds.copy(
                column = 0,
                row = rawRow,
            )
            WorkspaceDragEdge.NONE -> itemBounds.copy(
                column = rawColumn,
                row = rawRow,
            )
        }

        return WorkspaceDragIntent(
            edge = edge,
            targetPageId = targetPageId,
            requestedBounds = requested,
        )
    }
}
