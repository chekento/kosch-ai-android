package cloud.kosch.aiandroid.model

import kotlin.math.roundToInt

const val WORKSPACE_SCHEMA_VERSION = 7
const val MAX_WORKSPACE_ID_LENGTH = 240
const val MAX_WORKSPACE_TITLE_LENGTH = 160
const val MAX_WORKSPACE_REFERENCE_LENGTH = 512
private const val MAX_WORKSPACE_GRID_EXTENT = 64

/** Logical grid stored independently from physical pixels so layouts remain deterministic across windows. */
data class WorkspaceGridSpec(
    val columns: Int = 12,
    val rows: Int = 12,
) {
    init {
        require(columns in 1..MAX_WORKSPACE_GRID_EXTENT) { "Workspace grid column count is out of range" }
        require(rows in 1..MAX_WORKSPACE_GRID_EXTENT) { "Workspace grid row count is out of range" }
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
        init {
            require(legacyTileId.isNotBlank()) { "Action tile reference must not be blank" }
            require(legacyTileId.length <= MAX_WORKSPACE_REFERENCE_LENGTH) { "Action tile reference is too long" }
        }

        override val kind = WorkspaceItemKind.ACTION_TILE
    }

    data class App(val appKey: String) : WorkspaceItemContent {
        init {
            require(appKey.isNotBlank()) { "App key must not be blank" }
            require(appKey.length <= MAX_WORKSPACE_REFERENCE_LENGTH) { "App key is too long" }
        }

        override val kind = WorkspaceItemKind.APP
    }

    data class Folder(val folderId: String) : WorkspaceItemContent {
        init {
            require(folderId.isNotBlank()) { "Folder id must not be blank" }
            require(folderId.length <= MAX_WORKSPACE_REFERENCE_LENGTH) { "Folder id is too long" }
        }

        override val kind = WorkspaceItemKind.FOLDER
    }

    /** Portable provider identity only. Android appWidgetId is intentionally not part of this model. */
    data class Widget(val providerComponent: String?) : WorkspaceItemContent {
        init {
            require(providerComponent == null || providerComponent.isNotBlank()) {
                "Widget provider must be null for remap or a non-blank component"
            }
            require(providerComponent == null || providerComponent.length <= MAX_WORKSPACE_REFERENCE_LENGTH) {
                "Widget provider reference is too long"
            }
        }

        override val kind = WorkspaceItemKind.WIDGET
    }
}

data class WorkspaceItem(
    val id: String,
    val bounds: WorkspaceCellBounds,
    val content: WorkspaceItemContent,
) {
    init {
        require(id.length <= MAX_WORKSPACE_ID_LENGTH) { "Workspace item id is too long" }
    }
}

data class WorkspacePage(
    val id: String,
    val title: String,
    val order: Int,
    val sceneAdapter: SceneId? = null,
    val items: List<WorkspaceItem> = emptyList(),
) {
    init {
        require(id.length <= MAX_WORKSPACE_ID_LENGTH) { "Workspace page id is too long" }
        require(title.length <= MAX_WORKSPACE_TITLE_LENGTH) { "Workspace page title is too long" }
    }
}

data class WorkspaceDocument(
    val schemaVersion: Int = WORKSPACE_SCHEMA_VERSION,
    val grid: WorkspaceGridSpec = WorkspaceGridSpec(),
    val activePageId: String,
    val pages: List<WorkspacePage>,
) {
    fun normalized(): WorkspaceDocument {
        require(schemaVersion == WORKSPACE_SCHEMA_VERSION) {
            "Unsupported workspace schema $schemaVersion"
        }
        require(activePageId.length <= MAX_WORKSPACE_ID_LENGTH) { "Workspace active page id is too long" }
        val seenItemIds = mutableSetOf<String>()
        val uniquePages = pages
            .filter { it.id.isNotBlank() }
            .distinctBy(WorkspacePage::id)
            .sortedBy(WorkspacePage::order)
            .mapIndexed { index, page ->
                page.copy(
                    title = page.title.trim().ifBlank { "Home ${index + 1}" },
                    order = index,
                    items = page.items
                        .filter { it.id.isNotBlank() && seenItemIds.add(it.id) }
                        .map { it.copy(bounds = it.bounds.clamped(grid)) },
                )
            }
            .ifEmpty { listOf(WorkspacePage(DEFAULT_PAGE_ID, "Home", 0)) }
        val validActive = activePageId.takeIf { candidate -> uniquePages.any { it.id == candidate } }
            ?: uniquePages.first().id
        return copy(
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
        require(workspaceItemId.length <= MAX_WORKSPACE_ID_LENGTH) { "Widget binding workspace item id is too long" }
        require(appWidgetId > 0) { "Widget binding needs a positive Android host id" }
    }
}

object WorkspaceStableIds {
    fun scenePage(scene: SceneId): String = "page:scene:${scene.name.lowercase()}"

    fun legacySceneTile(scene: SceneId, legacyTileId: String): String {
        require(legacyTileId.isNotBlank()) { "Legacy tile id must not be blank" }
        require(legacyTileId.length <= MAX_WORKSPACE_REFERENCE_LENGTH) { "Legacy tile id is too long" }
        val stableId = "item:v6:${scene.name.lowercase()}:${legacyTileId.trim()}"
        require(stableId.length <= MAX_WORKSPACE_ID_LENGTH) { "Legacy tile stable id is too long" }
        return stableId
    }
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
        // A launcher should land on the user's desktop, not on a migration/debug dashboard. Legacy scene pages remain
        // available and untouched after the first clean portable Home page, so this is additive and lossless.
        val cleanHome = WorkspacePage(
            id = WorkspaceDocument.DEFAULT_PAGE_ID,
            title = "Home",
            order = 0,
        )
        val scenePages = SceneId.entries.mapIndexed { pageIndex, scene ->
            val legacyPositions = positions[scene].orEmpty()
            WorkspacePage(
                id = WorkspaceStableIds.scenePage(scene),
                title = scene.title,
                order = pageIndex + 1,
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
            activePageId = cleanHome.id,
            pages = listOf(cleanHome) + scenePages,
        ).normalized()
    }
}
