package cloud.kosch.aiandroid.model

/** Pure portable placement/remap rules for V7 widget items; Android host ids never enter this layer. */
object WorkspaceWidgetEditor {
    const val DEFAULT_COLUMN_SPAN = 4
    const val DEFAULT_ROW_SPAN = 3

    fun addWidget(
        document: WorkspaceDocument,
        pageId: String,
        itemId: String,
        providerComponent: String?,
    ): WorkspaceDocument {
        val normalized = document.normalized()
        require(itemId.isNotBlank()) { "Workspace widget item id must not be blank" }
        require(itemId.length <= MAX_WORKSPACE_ID_LENGTH) { "Workspace widget item id is too long" }
        require(normalized.pages.flatMap(WorkspacePage::items).none { it.id == itemId }) {
            "Workspace widget item id already exists"
        }
        val page = normalized.pages.firstOrNull { it.id == pageId }
            ?: throw IllegalArgumentException("Workspace page does not exist")
        require(page.sceneAdapter == null) { "Widgets can only be placed on user Home pages" }

        val bounds = WorkspacePageEditor.firstFreeBounds(
            grid = normalized.grid,
            items = page.items,
            columnSpan = DEFAULT_COLUMN_SPAN,
            rowSpan = DEFAULT_ROW_SPAN,
        ) ?: throw IllegalStateException("Workspace page has no free cell for this widget")

        val content = WorkspaceItemContent.Widget(providerComponent)
        return normalized.copy(
            activePageId = pageId,
            pages = normalized.pages.map { candidate ->
                if (candidate.id != pageId) candidate else candidate.copy(
                    items = candidate.items + WorkspaceItem(
                        id = itemId,
                        bounds = bounds,
                        content = content,
                    ),
                )
            },
        ).normalized()
    }

    /**
     * Replaces only the portable provider identity of an existing V7 widget.
     *
     * Stable item id, page, order and grid bounds are preserved so restore/remap never jumps the widget or breaks
     * references from Home Studio. The Android appWidgetId is still bound separately by the device-local store.
     */
    fun remapProvider(
        document: WorkspaceDocument,
        itemId: String,
        providerComponent: String,
    ): WorkspaceDocument {
        val normalized = document.normalized()
        val safeProvider = providerComponent.trim()
        require(itemId.isNotBlank()) { "Workspace widget item id must not be blank" }
        require(itemId.length <= MAX_WORKSPACE_ID_LENGTH) { "Workspace widget item id is too long" }
        require(safeProvider.isNotBlank()) { "Widget provider must not be blank" }
        require(safeProvider.length <= MAX_WORKSPACE_REFERENCE_LENGTH) { "Widget provider reference is too long" }

        var found = false
        val pages = normalized.pages.map { page ->
            val updatedItems = page.items.map { item ->
                if (item.id != itemId) return@map item
                require(page.sceneAdapter == null) { "Legacy scene widgets cannot be remapped" }
                require(item.content is WorkspaceItemContent.Widget) { "Workspace item is not a widget" }
                found = true
                item.copy(content = WorkspaceItemContent.Widget(safeProvider))
            }
            if (updatedItems == page.items) page else page.copy(items = updatedItems)
        }
        require(found) { "Workspace widget item does not exist" }
        return normalized.copy(pages = pages).normalized()
    }
}
