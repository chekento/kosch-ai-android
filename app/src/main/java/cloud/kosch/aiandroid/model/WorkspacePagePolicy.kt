package cloud.kosch.aiandroid.model

/**
 * Navigation policy for the unified KAL workspace.
 *
 * The protected primary Home is the user's anchor desktop. Additional sceneAdapter == null pages are user-owned
 * personal pages and may sit on either side of Home. Scene-adapter pages are KAL system/tool spaces and stay outside
 * the personal horizontal page strip. The stable persisted schema already carries page order, so no migration field is
 * required for left/right personal pages.
 */
enum class WorkspacePageKind {
    PRIMARY_HOME,
    USER,
    SYSTEM,
}

object WorkspacePagePolicy {
    fun kind(page: WorkspacePage): WorkspacePageKind = when {
        page.id == WorkspaceDocument.DEFAULT_PAGE_ID -> WorkspacePageKind.PRIMARY_HOME
        page.sceneAdapter != null || page.id.startsWith("page:scene:") -> WorkspacePageKind.SYSTEM
        else -> WorkspacePageKind.USER
    }

    fun isPrimaryHome(page: WorkspacePage): Boolean = kind(page) == WorkspacePageKind.PRIMARY_HOME

    fun isPersonal(page: WorkspacePage): Boolean = when (kind(page)) {
        WorkspacePageKind.PRIMARY_HOME,
        WorkspacePageKind.USER -> true
        WorkspacePageKind.SYSTEM -> false
    }

    fun isUserManaged(page: WorkspacePage): Boolean = kind(page) == WorkspacePageKind.USER

    fun isSystem(page: WorkspacePage): Boolean = kind(page) == WorkspacePageKind.SYSTEM

    fun canEditItems(page: WorkspacePage): Boolean = isPersonal(page)

    /** The protected primary Home may be duplicated into a normal user page but never renamed, moved or deleted. */
    fun canDuplicate(page: WorkspacePage): Boolean = isPersonal(page)

    fun canRename(page: WorkspacePage): Boolean = isUserManaged(page)

    fun canDelete(page: WorkspacePage): Boolean = isUserManaged(page)

    fun canMove(page: WorkspacePage): Boolean = isUserManaged(page)

    fun personalPages(document: WorkspaceDocument): List<WorkspacePage> =
        document.normalized().pages.filter(::isPersonal)

    fun userManagedPages(document: WorkspaceDocument): List<WorkspacePage> =
        document.normalized().pages.filter(::isUserManaged)

    fun systemPages(document: WorkspaceDocument): List<WorkspacePage> =
        document.normalized().pages.filter(::isSystem)

    /**
     * Preserve the user's personal left/right order, including pages placed before Home. KAL system spaces stay after
     * that personal strip and receive current KAL-facing titles. User-created titles are never rewritten.
     */
    fun organize(document: WorkspaceDocument): WorkspaceDocument {
        val normalized = document.normalized()
        val personal = normalized.pages.filter(::isPersonal)
        val systems = normalized.pages.filter(::isSystem).map { page ->
            page.sceneAdapter?.let { scene -> page.copy(title = scene.title) } ?: page
        }
        val ordered = (personal + systems).mapIndexed { index, page -> page.copy(order = index) }
        return normalized.copy(pages = ordered).normalized()
    }

    /**
     * Reorders a user-created personal page inside the complete personal strip. Home itself cannot be moved, but a
     * user page may cross it, which is how a user intentionally places free pages to the left or right of Home.
     */
    fun moveUserPage(document: WorkspaceDocument, pageId: String, delta: Int): WorkspaceDocument {
        require(delta != 0) { "Workspace page move delta must not be zero" }
        val organized = organize(document)
        val page = organized.pages.firstOrNull { it.id == pageId }
            ?: throw IllegalArgumentException("Workspace page does not exist")
        require(canMove(page)) { "Only user-created pages can be reordered" }

        val personal = organized.pages.filter(::isPersonal).toMutableList()
        val index = personal.indexOfFirst { it.id == pageId }
        val target = (index + delta).coerceIn(0, personal.lastIndex)
        if (target == index) return organized
        val moved = personal.removeAt(index)
        personal.add(target, moved)

        return withPersonalOrder(organized, personal)
    }

    /**
     * Places a user page directly to the left or right of a personal anchor. Used by Home Studio so page creation has
     * an obvious spatial result rather than silently appending everything to one side.
     */
    fun placeUserPageAdjacent(
        document: WorkspaceDocument,
        pageId: String,
        anchorPageId: String,
        direction: Int,
    ): WorkspaceDocument {
        require(direction == -1 || direction == 1) { "Direction must be -1 or +1" }
        val organized = organize(document)
        val page = organized.pages.firstOrNull { it.id == pageId }
            ?: throw IllegalArgumentException("Workspace page does not exist")
        val anchor = organized.pages.firstOrNull { it.id == anchorPageId }
            ?: throw IllegalArgumentException("Workspace anchor page does not exist")
        require(isUserManaged(page)) { "Only user-created pages can be positioned" }
        require(isPersonal(anchor)) { "Page anchor must be personal" }

        val personal = organized.pages.filter(::isPersonal).toMutableList()
        val movedIndex = personal.indexOfFirst { it.id == pageId }
        personal.removeAt(movedIndex)
        val anchorIndex = personal.indexOfFirst { it.id == anchorPageId }
        require(anchorIndex >= 0) { "Workspace anchor page is not in the personal strip" }
        val insertIndex = if (direction < 0) anchorIndex else anchorIndex + 1
        personal.add(insertIndex.coerceIn(0, personal.size), page)

        return withPersonalOrder(organized, personal).copy(activePageId = pageId).normalized()
    }

    private fun withPersonalOrder(
        document: WorkspaceDocument,
        personal: List<WorkspacePage>,
    ): WorkspaceDocument {
        val systems = document.pages.filter(::isSystem)
        val pages = (personal + systems).mapIndexed { order, candidate -> candidate.copy(order = order) }
        return document.copy(pages = pages).normalized()
    }
}
