package cloud.kosch.aiandroid.model

/**
 * Navigation policy for the unified KAL workspace.
 *
 * The first page is a protected personal Home. Additional sceneAdapter == null pages are user-owned personal pages.
 * Scene-adapter pages are KAL system/tool spaces. This classification intentionally derives from the existing stable
 * schema so old backups remain readable without a migration just to distinguish navigation roles.
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

    fun personalPages(document: WorkspaceDocument): List<WorkspacePage> = document.normalized().pages.filter(::isPersonal)

    fun userManagedPages(document: WorkspaceDocument): List<WorkspacePage> = document.normalized().pages.filter(::isUserManaged)

    fun systemPages(document: WorkspaceDocument): List<WorkspacePage> = document.normalized().pages.filter(::isSystem)

    /**
     * Keeps the navigation contract stable: primary Home first, user-created pages next, KAL system spaces last.
     * The active page is preserved and only order metadata changes.
     */
    fun organize(document: WorkspaceDocument): WorkspaceDocument {
        val normalized = document.normalized()
        val primary = normalized.pages.filter(::isPrimaryHome)
        val users = normalized.pages.filter(::isUserManaged)
        val systems = normalized.pages.filter(::isSystem)
        val ordered = (primary + users + systems).mapIndexed { index, page -> page.copy(order = index) }
        return normalized.copy(pages = ordered).normalized()
    }

    /** Moves only user-created personal pages. Home stays first and system spaces stay behind the personal pages. */
    fun moveUserPage(document: WorkspaceDocument, pageId: String, delta: Int): WorkspaceDocument {
        require(delta != 0) { "Workspace page move delta must not be zero" }
        val organized = organize(document)
        val page = organized.pages.firstOrNull { it.id == pageId }
            ?: throw IllegalArgumentException("Workspace page does not exist")
        require(canMove(page)) { "Only user-created pages can be reordered" }

        val users = organized.pages.filter(::isUserManaged).toMutableList()
        val index = users.indexOfFirst { it.id == pageId }
        val target = (index + delta).coerceIn(0, users.lastIndex)
        if (target == index) return organized
        val moved = users.removeAt(index)
        users.add(target, moved)

        val primary = organized.pages.filter(::isPrimaryHome)
        val systems = organized.pages.filter(::isSystem)
        val pages = (primary + users + systems).mapIndexed { order, candidate -> candidate.copy(order = order) }
        return organized.copy(pages = pages).normalized()
    }
}