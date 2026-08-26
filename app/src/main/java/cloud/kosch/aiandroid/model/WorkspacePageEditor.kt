package cloud.kosch.aiandroid.model

import kotlin.math.abs

/**
 * Pure editing rules for user-controlled v7 Home pages.
 *
 * Legacy scene pages remain valid compatibility pages while Stage B lands. User pages hold apps, folders
 * and portable widget identities; Android appWidgetId bindings remain device-local outside this model.
 */
object WorkspacePageEditor {
    const val MAX_USER_PAGES = 20
    const val APP_COLUMN_SPAN = 2
    const val APP_ROW_SPAN = 2
    const val FOLDER_COLUMN_SPAN = 2
    const val FOLDER_ROW_SPAN = 2
    const val WIDGET_DEFAULT_COLUMN_SPAN = 4
    const val WIDGET_DEFAULT_ROW_SPAN = 4

    fun createUserPage(
        document: WorkspaceDocument,
        pageId: String,
        title: String,
    ): WorkspaceDocument {
        val normalized = document.normalized()
        require(pageId.isNotBlank()) { "Workspace page id must not be blank" }
        require(pageId.length <= MAX_WORKSPACE_ID_LENGTH) { "Workspace page id is too long" }
        require(normalized.pages.none { it.id == pageId }) { "Workspace page id already exists" }
        require(normalized.pages.count { it.sceneAdapter == null } < MAX_USER_PAGES) {
            "Maximum number of user Home pages reached"
        }
        val safeTitle = title.trim().ifBlank { nextDefaultTitle(normalized) }
        require(safeTitle.length <= MAX_WORKSPACE_TITLE_LENGTH) { "Workspace page title is too long" }
        return normalized.copy(
            activePageId = pageId,
            pages = normalized.pages + WorkspacePage(
                id = pageId,
                title = safeTitle,
                order = normalized.pages.size,
                sceneAdapter = null,
            ),
        ).normalized()
    }

    fun activatePage(document: WorkspaceDocument, pageId: String): WorkspaceDocument {
        val normalized = document.normalized()
        require(normalized.pages.any { it.id == pageId }) { "Workspace page does not exist" }
        return normalized.copy(activePageId = pageId)
    }

    fun renameUserPage(
        document: WorkspaceDocument,
        pageId: String,
        title: String,
    ): WorkspaceDocument {
        val normalized = document.normalized()
        val safeTitle = title.trim()
        require(safeTitle.isNotBlank()) { "Workspace page title must not be blank" }
        require(safeTitle.length <= MAX_WORKSPACE_TITLE_LENGTH) { "Workspace page title is too long" }
        var changed = false
        val pages = normalized.pages.map { page ->
            if (page.id != pageId) return@map page
            require(page.sceneAdapter == null) { "Legacy scene pages cannot be renamed" }
            changed = true
            page.copy(title = safeTitle)
        }
        require(changed) { "Workspace page does not exist" }
        return normalized.copy(pages = pages).normalized()
    }

    fun deleteUserPage(document: WorkspaceDocument, pageId: String): WorkspaceDocument {
        val normalized = document.normalized()
        val page = normalized.pages.firstOrNull { it.id == pageId }
            ?: throw IllegalArgumentException("Workspace page does not exist")
        require(page.sceneAdapter == null) { "Legacy scene pages cannot be deleted" }
        val remaining = normalized.pages.filterNot { it.id == pageId }
        val safePages = remaining.ifEmpty { listOf(WorkspacePage(WorkspaceDocument.DEFAULT_PAGE_ID, "Home", 0)) }
        val nextActive = when {
            normalized.activePageId != pageId -> normalized.activePageId
            else -> safePages.getOrNull(page.order.coerceAtMost(safePages.lastIndex))?.id ?: safePages.first().id
        }
        return normalized.copy(activePageId = nextActive, pages = safePages).normalized()
    }

    fun movePage(document: WorkspaceDocument, pageId: String, delta: Int): WorkspaceDocument {
        require(delta != 0) { "Workspace page move delta must not be zero" }
        val normalized = document.normalized()
        val index = normalized.pages.indexOfFirst { it.id == pageId }
        require(index >= 0) { "Workspace page does not exist" }
        val target = (index + delta).coerceIn(0, normalized.pages.lastIndex)
        if (target == index) return normalized
        val mutable = normalized.pages.toMutableList()
        val page = mutable.removeAt(index)
        mutable.add(target, page)
        return normalized.copy(pages = mutable.mapIndexed { order, item -> item.copy(order = order) }).normalized()
    }

    fun addApp(
        document: WorkspaceDocument,
        pageId: String,
        itemId: String,
        appKey: String,
    ): WorkspaceDocument = addItem(
        document = document,
        pageId = pageId,
        itemId = itemId,
        content = WorkspaceItemContent.App(appKey),
        columnSpan = APP_COLUMN_SPAN,
        rowSpan = APP_ROW_SPAN,
    )

    fun addFolder(
        document: WorkspaceDocument,
        pageId: String,
        itemId: String,
        folderId: String,
    ): WorkspaceDocument = addItem(
        document = document,
        pageId = pageId,
        itemId = itemId,
        content = WorkspaceItemContent.Folder(folderId),
        columnSpan = FOLDER_COLUMN_SPAN,
        rowSpan = FOLDER_ROW_SPAN,
    )

    /** Adds only the portable provider identity. Android appWidgetId is deliberately stored elsewhere. */
    fun addWidget(
        document: WorkspaceDocument,
        pageId: String,
        itemId: String,
        providerComponent: String?,
        columnSpan: Int = WIDGET_DEFAULT_COLUMN_SPAN,
        rowSpan: Int = WIDGET_DEFAULT_ROW_SPAN,
    ): WorkspaceDocument {
        require(columnSpan > 0) { "Widget column span must be positive" }
        require(rowSpan > 0) { "Widget row span must be positive" }
        return addItem(
            document = document,
            pageId = pageId,
            itemId = itemId,
            content = WorkspaceItemContent.Widget(providerComponent),
            columnSpan = columnSpan,
            rowSpan = rowSpan,
        )
    }

    fun moveItem(
        document: WorkspaceDocument,
        pageId: String,
        itemId: String,
        requestedBounds: WorkspaceCellBounds,
    ): WorkspaceDocument {
        val normalized = document.normalized()
        val page = normalized.pages.firstOrNull { it.id == pageId }
            ?: throw IllegalArgumentException("Workspace page does not exist")
        val item = page.items.firstOrNull { it.id == itemId }
            ?: throw IllegalArgumentException("Workspace item does not exist")
        val target = nearestAvailableBounds(
            grid = normalized.grid,
            items = page.items,
            requested = requestedBounds.copy(
                columnSpan = item.bounds.columnSpan,
                rowSpan = item.bounds.rowSpan,
            ),
            excludingItemId = itemId,
        ) ?: return normalized
        val pages = normalized.pages.map { candidate ->
            if (candidate.id != pageId) candidate else candidate.copy(
                items = candidate.items.map { existing ->
                    if (existing.id == itemId) existing.copy(bounds = target) else existing
                },
            )
        }
        return normalized.copy(pages = pages).normalized()
    }

    fun moveItemToPage(
        document: WorkspaceDocument,
        sourcePageId: String,
        targetPageId: String,
        itemId: String,
        requestedBounds: WorkspaceCellBounds? = null,
    ): WorkspaceDocument {
        val normalized = document.normalized()
        if (sourcePageId == targetPageId) {
            val sourceItem = normalized.pages
                .firstOrNull { it.id == sourcePageId }
                ?.items
                ?.firstOrNull { it.id == itemId }
                ?: throw IllegalArgumentException("Workspace item does not exist")
            return moveItem(
                normalized,
                sourcePageId,
                itemId,
                requestedBounds ?: sourceItem.bounds,
            )
        }

        val sourcePage = normalized.pages.firstOrNull { it.id == sourcePageId }
            ?: throw IllegalArgumentException("Source workspace page does not exist")
        val targetPage = normalized.pages.firstOrNull { it.id == targetPageId }
            ?: throw IllegalArgumentException("Target workspace page does not exist")
        require(sourcePage.sceneAdapter == null) { "Items cannot be dragged out of legacy scene pages" }
        require(targetPage.sceneAdapter == null) { "Items cannot be dropped on legacy scene pages" }
        val item = sourcePage.items.firstOrNull { it.id == itemId }
            ?: throw IllegalArgumentException("Workspace item does not exist")
        val targetBounds = nearestAvailableBounds(
            grid = normalized.grid,
            items = targetPage.items,
            requested = (requestedBounds ?: item.bounds).copy(
                columnSpan = item.bounds.columnSpan,
                rowSpan = item.bounds.rowSpan,
            ),
        ) ?: return normalized

        val pages = normalized.pages.map { page ->
            when (page.id) {
                sourcePageId -> page.copy(items = page.items.filterNot { it.id == itemId })
                targetPageId -> page.copy(items = page.items + item.copy(bounds = targetBounds))
                else -> page
            }
        }
        return normalized.copy(activePageId = targetPageId, pages = pages).normalized()
    }

    fun removeItem(document: WorkspaceDocument, pageId: String, itemId: String): WorkspaceDocument {
        val normalized = document.normalized()
        require(normalized.pages.any { it.id == pageId }) { "Workspace page does not exist" }
        val pages = normalized.pages.map { page ->
            if (page.id != pageId) page else page.copy(items = page.items.filterNot { it.id == itemId })
        }
        return normalized.copy(pages = pages).normalized()
    }

    fun firstFreeBounds(
        grid: WorkspaceGridSpec,
        items: List<WorkspaceItem>,
        columnSpan: Int,
        rowSpan: Int,
        excludingItemId: String? = null,
    ): WorkspaceCellBounds? {
        val safeColumnSpan = columnSpan.coerceIn(1, grid.columns)
        val safeRowSpan = rowSpan.coerceIn(1, grid.rows)
        for (row in 0..(grid.rows - safeRowSpan)) {
            for (column in 0..(grid.columns - safeColumnSpan)) {
                val candidate = WorkspaceCellBounds(column, row, safeColumnSpan, safeRowSpan)
                if (isFree(candidate, items, excludingItemId)) return candidate
            }
        }
        return null
    }

    fun nearestAvailableBounds(
        grid: WorkspaceGridSpec,
        items: List<WorkspaceItem>,
        requested: WorkspaceCellBounds,
        excludingItemId: String? = null,
    ): WorkspaceCellBounds? {
        val clamped = requested.clamped(grid)
        if (isFree(clamped, items, excludingItemId)) return clamped
        val candidates = buildList {
            for (row in 0..(grid.rows - clamped.rowSpan)) {
                for (column in 0..(grid.columns - clamped.columnSpan)) {
                    add(WorkspaceCellBounds(column, row, clamped.columnSpan, clamped.rowSpan))
                }
            }
        }.sortedWith(
            compareBy<WorkspaceCellBounds> {
                abs(it.column - clamped.column) + abs(it.row - clamped.row)
            }.thenBy { it.row }.thenBy { it.column },
        )
        return candidates.firstOrNull { isFree(it, items, excludingItemId) }
    }

    private fun addItem(
        document: WorkspaceDocument,
        pageId: String,
        itemId: String,
        content: WorkspaceItemContent,
        columnSpan: Int,
        rowSpan: Int,
    ): WorkspaceDocument {
        val normalized = document.normalized()
        require(itemId.isNotBlank()) { "Workspace item id must not be blank" }
        require(itemId.length <= MAX_WORKSPACE_ID_LENGTH) { "Workspace item id is too long" }
        require(normalized.pages.flatMap(WorkspacePage::items).none { it.id == itemId }) {
            "Workspace item id already exists"
        }
        val page = normalized.pages.firstOrNull { it.id == pageId }
            ?: throw IllegalArgumentException("Workspace page does not exist")
        require(page.sceneAdapter == null) { "Items can only be placed on user Home pages" }
        val bounds = firstFreeBounds(normalized.grid, page.items, columnSpan, rowSpan)
            ?: throw IllegalStateException("Workspace page has no free cell for this item")
        val pages = normalized.pages.map { candidate ->
            if (candidate.id != pageId) candidate else candidate.copy(
                items = candidate.items + WorkspaceItem(itemId, bounds, content),
            )
        }
        return normalized.copy(activePageId = pageId, pages = pages).normalized()
    }

    private fun isFree(
        candidate: WorkspaceCellBounds,
        items: List<WorkspaceItem>,
        excludingItemId: String?,
    ): Boolean = items.none { item ->
        item.id != excludingItemId && candidate.overlaps(item.bounds)
    }

    private fun nextDefaultTitle(document: WorkspaceDocument): String {
        val used = document.pages.map { it.title }.toSet()
        var index = 1
        while ("Home $index" in used) index += 1
        return "Home $index"
    }
}
