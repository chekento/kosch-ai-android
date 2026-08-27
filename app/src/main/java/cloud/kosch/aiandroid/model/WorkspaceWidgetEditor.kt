package cloud.kosch.aiandroid.model

/** Pure portable placement rules for V7 widget items; Android host ids never enter this layer. */
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
}
