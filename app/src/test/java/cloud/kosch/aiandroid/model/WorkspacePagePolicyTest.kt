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
        val system = WorkspacePage("page:scene:work", "Legacy Work", 2, sceneAdapter = SceneId.WORK)

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
    fun organize_preservesPersonalLeftRightOrder_andMovesSystemSpacesBehindIt() {
        val document = WorkspaceDocument(
            activePageId = "page:user:left",
            pages = listOf(
                WorkspacePage("page:user:left", "Links", 0),
                WorkspacePage("page:scene:ai", "Old AI", 1, sceneAdapter = SceneId.AI),
                WorkspacePage(WorkspaceDocument.DEFAULT_PAGE_ID, "Home", 2),
                WorkspacePage("page:user:right", "Rechts", 3),
                WorkspacePage("page:scene:work", "Old Work", 4, sceneAdapter = SceneId.WORK),
            ),
        )

        val organized = WorkspacePagePolicy.organize(document)

        assertEquals(
            listOf(
                "page:user:left",
                WorkspaceDocument.DEFAULT_PAGE_ID,
                "page:user:right",
                "page:scene:ai",
                "page:scene:work",
            ),
            organized.pages.map { it.id },
        )
        assertEquals("page:user:left", organized.activePageId)
        assertEquals(organized.pages.indices.toList(), organized.pages.map { it.order })
        assertEquals("Links", organized.pages.first { it.id == "page:user:left" }.title)
        assertEquals("Rechts", organized.pages.first { it.id == "page:user:right" }.title)
        assertEquals(SceneId.AI.title, organized.pages.first { it.id == "page:scene:ai" }.title)
        assertEquals(SceneId.WORK.title, organized.pages.first { it.id == "page:scene:work" }.title)
    }

    @Test
    fun userPage_canMoveAcrossHome_butSystemPagesStayOutsidePersonalStrip() {
        val document = WorkspaceDocument(
            activePageId = "page:user:media",
            pages = listOf(
                WorkspacePage(WorkspaceDocument.DEFAULT_PAGE_ID, "Home", 0),
                WorkspacePage("page:user:work", "Work", 1),
                WorkspacePage("page:user:media", "Media", 2),
                WorkspacePage("page:scene:ai", "AI", 3, sceneAdapter = SceneId.AI),
            ),
        )

        val once = WorkspacePagePolicy.moveUserPage(document, "page:user:media", -1)
        val twice = WorkspacePagePolicy.moveUserPage(once, "page:user:media", -1)

        assertEquals(
            listOf(
                "page:user:media",
                WorkspaceDocument.DEFAULT_PAGE_ID,
                "page:user:work",
                "page:scene:ai",
            ),
            twice.pages.map { it.id },
        )
    }

    @Test
    fun placeUserPageAdjacent_supportsLeftAndRightOfProtectedHome() {
        val document = WorkspaceDocument(
            activePageId = WorkspaceDocument.DEFAULT_PAGE_ID,
            pages = listOf(
                WorkspacePage(WorkspaceDocument.DEFAULT_PAGE_ID, "Home", 0),
                WorkspacePage("page:user:left", "Links", 1),
                WorkspacePage("page:user:right", "Rechts", 2),
                WorkspacePage("page:scene:ai", "AI", 3, sceneAdapter = SceneId.AI),
            ),
        )

        val left = WorkspacePagePolicy.placeUserPageAdjacent(
            document = document,
            pageId = "page:user:left",
            anchorPageId = WorkspaceDocument.DEFAULT_PAGE_ID,
            direction = -1,
        )
        assertEquals(
            listOf(
                "page:user:left",
                WorkspaceDocument.DEFAULT_PAGE_ID,
                "page:user:right",
                "page:scene:ai",
            ),
            left.pages.map { it.id },
        )

        val right = WorkspacePagePolicy.placeUserPageAdjacent(
            document = left,
            pageId = "page:user:right",
            anchorPageId = WorkspaceDocument.DEFAULT_PAGE_ID,
            direction = 1,
        )
        assertEquals(
            listOf(
                "page:user:left",
                WorkspaceDocument.DEFAULT_PAGE_ID,
                "page:user:right",
                "page:scene:ai",
            ),
            right.pages.map { it.id },
        )
    }
}
