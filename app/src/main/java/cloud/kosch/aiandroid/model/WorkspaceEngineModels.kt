package cloud.kosch.aiandroid.model

import kotlin.math.roundToInt

const val WORKSPACE_SCHEMA_VERSION = 7

/** Logical grid stored independently from physical pixels so layouts remain deterministic across windows. */
data class WorkspaceGridSpec(
    val columns: Int = 12,
    val rows: Int = 12,
) {
    init {
        require(columns > 0) { "Workspace grid needs at least one column" }
        require(rows > 0) { "Workspace grid needs at least one row" }
    }
}

data class WorkspaceCellBounds(
    val column: Int,
    val row: Int,
    val columnSpan: Int,
    val rowSpan: Int,
) {
    fun clamped(grid: WorkspaceGridSpec): WorkspaceCellBounds {
        val safeColumnSpan = columnSpan.coerceIn(1, grid.columns)
        val safeRowSpan = rowSpan.coerceIn(1, grid.rows)
        return WorkspaceCellBounds(
            column = column.coerceIn(0, grid.columns - safeColumnSpan),
            row = row.coerceIn(0, grid.rows - safeRowSpan),
            columnSpan = safeColumnSpan,
            rowSpan = safeRowSpan,
        )
    }

    fun overlaps(other: WorkspaceCellBounds): Boolean =
        column < other.column + other.columnSpan &&
            column + columnSpan > other.column &&
            row < other.row + other.rowSpan &&
            row + rowSpan > other.row

    companion object {
        fun fromNormalizedTopLeft(
            x: Float,
            y: Float,
            columnSpan: Int,
            rowSpan: Int,
            grid: WorkspaceGridSpec,
        ): WorkspaceCellBounds {
            val safeColumnSpan = columnSpan.coerceIn(1, grid.columns)
            val safeRowSpan = rowSpan.coerceIn(1, grid.rows)
            val column = (x.coerceIn(0f, 1f) * grid.columns)
                .roundToInt()
                .coerceIn(0, grid.columns - safeColumnSpan)
            val row = (y.coerceIn(0f, 1f) * grid.rows)
                .roundToInt()
                .coerceIn(0, grid.rows - safeRowSpan)
            return WorkspaceCellBounds(column, row, safeColumnSpan, safeRowSpan)
        }
    }
}

enum class WorkspaceItemKind {
    ACTION_TILE,
    APP,
    FOLDER,
    WIDGET,
}

sealed interface WorkspaceItemContent {
    val kind: WorkspaceItemKind

    data class ActionTile(
        val scene: SceneId,
        val legacyTileId: String,
        val action: TileAction,
    ) : WorkspaceItemContent {
        override val kind = WorkspaceItemKind.ACTION_TILE
    }

    data class App(val appKey: String) : WorkspaceItemContent {
        override val kind = WorkspaceItemKind.APP
    }

    data class Folder(val folderId: String) : WorkspaceItemContent {
        override val kind = WorkspaceItemKind.FOLDER
    }

    /** Portable provider identity only. Android appWidgetId is intentionally not part of this model. */
    data class Widget(val providerComponent: String?) : WorkspaceItemContent {
        override val kind = WorkspaceItemKind.WIDGET
    }
}

data class WorkspaceItem(
    val id: String,
    val bounds: WorkspaceCellBounds,
    val content: WorkspaceItemContent,
)

data class WorkspacePage(
    val id: String,
    val title: String,
    val order: Int,
    val sceneAdapter: SceneId? = null,
    val items: List<WorkspaceItem> = emptyList(),
)

data class WorkspaceDocument(
    val schemaVersion: Int = WORKSPACE_SCHEMA_VERSION,
    val grid: WorkspaceGridSpec = WorkspaceGridSpec(),
    val activePageId: String,
    val pages: List<WorkspacePage>,
) {
    fun normalized(): WorkspaceDocument {
        val uniquePages = pages
            .filter { it.id.isNotBlank() }
            .distinctBy(WorkspacePage::id)
            .sortedBy(WorkspacePage::order)
            .mapIndexed { index, page ->
                page.copy(
                    title = page.title.trim().ifBlank { "Home ${index + 1}" },
                    order = index,
                    items = page.items
                        .filter { it.id.isNotBlank() }
                        .distinctBy(WorkspaceItem::id)
                        .map { it.copy(bounds = it.bounds.clamped(grid)) },
                )
            }
            .ifEmpty { listOf(WorkspacePage(DEFAULT_PAGE_ID, "Home", 0)) }
        val validActive = activePageId.takeIf { candidate -> uniquePages.any { it.id == candidate } }
            ?: uniquePages.first().id
        return copy(
            schemaVersion = WORKSPACE_SCHEMA_VERSION,
            activePageId = validActive,
            pages = uniquePages,
        )
    }

    companion object {
        const val DEFAULT_PAGE_ID = "page:home"
    }
}

/** Device-local binding. Deliberately separate from WorkspaceItemContent.Widget and portable backups. */
data class DeviceWidgetBinding(
    val workspaceItemId: String,
    val appWidgetId: Int,
) {
    init {
        require(workspaceItemId.isNotBlank()) { "Widget binding needs a workspace item id" }
        require(appWidgetId > 0) { "Widget binding needs a positive Android host id" }
    }
}

object WorkspaceStableIds {
    fun scenePage(scene: SceneId): String = "page:scene:${scene.name.lowercase()}"

    fun legacySceneTile(scene: SceneId, legacyTileId: String): String =
        "item:v6:${scene.name.lowercase()}:${legacyTileId.trim()}"
}

/** Pure v6 → v7 adapter. Persistence writes are intentionally handled by WorkspaceStore later. */
object WorkspaceV7Migration {
    private const val LEGACY_TILE_COLUMN_SPAN = 6
    private const val LEGACY_TILE_ROW_SPAN = 6

    fun fromLegacyScenePositions(
        activeScene: SceneId,
        positions: Map<SceneId, Map<String, TilePosition>>,
        grid: WorkspaceGridSpec = WorkspaceGridSpec(),
    ): WorkspaceDocument {
        val pages = SceneId.entries.mapIndexed { pageIndex, scene ->
            val legacyPositions = positions[scene].orEmpty()
            WorkspacePage(
                id = WorkspaceStableIds.scenePage(scene),
                title = scene.title,
                order = pageIndex,
                sceneAdapter = scene,
                items = DefaultWorkspace.tiles(scene).map { tile ->
                    val position = legacyPositions[tile.id] ?: tile.defaultPosition
                    WorkspaceItem(
                        id = WorkspaceStableIds.legacySceneTile(scene, tile.id),
                        bounds = WorkspaceCellBounds.fromNormalizedTopLeft(
                            x = position.x,
                            y = position.y,
                            columnSpan = LEGACY_TILE_COLUMN_SPAN,
                            rowSpan = LEGACY_TILE_ROW_SPAN,
                            grid = grid,
                        ),
                        content = WorkspaceItemContent.ActionTile(
                            scene = scene,
                            legacyTileId = tile.id,
                            action = tile.action,
                        ),
                    )
                },
            )
        }
        return WorkspaceDocument(
            grid = grid,
            activePageId = WorkspaceStableIds.scenePage(activeScene),
            pages = pages,
        ).normalized()
    }
}
