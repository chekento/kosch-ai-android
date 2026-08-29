package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspacePagePolicyTest {
    @Test
    fun primaryHome_isPersonalButProtected() {
        val home = WorkspacePage(WorkspaceDocument.DEFAULT_PAGE_ID, "Home", 0)

        assertEquals(WorkspacePageKind.PRIMARY_HOME, WorkspacePagePolicy.kind(home))
        assertTrue(WorkspacePagePolicy.canEditItems(home))
        assertTrue(WorkspacePagePolicy.canDuplicate(home))
        assertFalse(WorkspacePagePolicy.canRename(home))
        assertFalse(WorkspacePagePolicy.canDelete(home))
        assertFalse(WorkspacePagePolicy.canMove(home))
    }

    @Test
    fun userPage_isFullyManagedWhileSystemPageIsProtected() {
        val user = WorkspacePage("page:user:work", "Mein Work", 1)
        val system = WorkspacePage("page:scene:work", "Work", 2, sceneAdapter = SceneId.WORK)

        assertEquals(WorkspacePageKind.USER, WorkspacePagePolicy.kind(user))
        assertTrue(WorkspacePagePolicy.canEditItems(user))
        assertTrue(WorkspacePagePolicy.canRename(user))
        assertTrue(WorkspacePagePolicy.canDelete(user))
        assertTrue(WorkspacePagePolicy.canMove(user))

        assertEquals(WorkspacePageKind.SYSTEM, WorkspacePagePolicy.kind(system))
        assertFalse(WorkspacePagePolicy.canEditItems(system))
        assertFalse(WorkspacePagePolicy.canDelete(system))
        assertFalse(WorkspacePagePolicy.canDuplicate(system))
    }

    @Test
    fun organize_keepsHomeThenUsersThenSystemSpaces() {
        val document = WorkspaceDocument(
            activePageId = "page:user:games",
            pages = listOf(
                WorkspacePage("page:scene:ai", "AI", 0, sceneAdapter = SceneId.AI),
                WorkspacePage("page:user:games", "Games", 1),
                WorkspacePage(WorkspaceDocument.DEFAULT_PAGE_ID, "Home", 2),
                WorkspacePage("page:scene:work", "Work", 3, sceneAdapter = SceneId.WORK),
                WorkspacePage("page:user:media", "Media", 4),
            ),
        )

        val organized = WorkspacePagePolicy.organize(document)

        assertEquals(
            listOf(
                WorkspaceDocument.DEFAULT_PAGE_ID,
                "page:user:games",
                "page:user:media",
                "page:scene:ai",
                "page:scene:work",
            ),
            organized.pages.map { it.id },
        )
        assertEquals("page:user:games", organized.activePageId)
        assertEquals(organized.pages.indices.toList(), organized.pages.map { it.order })
    }

    @Test
    fun moveUserPage_neverMovesAcrossHomeOrSystemBoundary() {
        val document = WorkspaceDocument(
            activePageId = "page:user:media",
            pages = listOf(
                WorkspacePage(WorkspaceDocument.DEFAULT_PAGE_ID, "Home", 0),
                WorkspacePage("page:user:work", "Work", 1),
                WorkspacePage("page:user:media", "Media", 2),
                WorkspacePage("page:scene:ai", "AI", 3, sceneAdapter = SceneId.AI),
            ),
        )

        val moved = WorkspacePagePolicy.moveUserPage(document, "page:user:media", -1)

        assertEquals(
            listOf(
                WorkspaceDocument.DEFAULT_PAGE_ID,
                "page:user:media",
                "page:user:work",
                "page:scene:ai",
            ),
            moved.pages.map { it.id },
        )
    }
}