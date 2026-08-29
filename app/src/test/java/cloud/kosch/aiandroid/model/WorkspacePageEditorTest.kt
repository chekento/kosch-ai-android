package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspacePageEditorTest {
    private fun base(): WorkspaceDocument = WorkspaceV7Migration.fromLegacyScenePositions(
        activeScene = SceneId.AI,
        positions = emptyMap(),
    )

    @Test
    fun createRenameMoveDeleteUserPage_preservesAtLeastOnePage() {
        val created = WorkspacePageEditor.createUserPage(base(), "page:user:one", "")
        assertEquals("page:user:one", created.activePageId)
        assertEquals("Home 1", created.pages.last().title)
        assertEquals(null, created.pages.last().sceneAdapter)

        val renamed = WorkspacePageEditor.renameUserPage(created, "page:user:one", "Studio")
        assertEquals("Studio", renamed.pages.first { it.id == "page:user:one" }.title)

        val moved = WorkspacePageEditor.movePage(renamed, "page:user:one", -1)
        assertEquals(
            moved.pages.size - 2,
            moved.pages.indexOfFirst { it.id == "page:user:one" },
        )

        val deleted = WorkspacePageEditor.deleteUserPage(moved, "page:user:one")
        assertFalse(deleted.pages.any { it.id == "page:user:one" })
        assertTrue(deleted.pages.isNotEmpty())
        assertTrue(deleted.pages.any { it.id == deleted.activePageId })
    }

    @Test
    fun duplicateUserPage_copiesPortableLayoutWithFreshIdsImmediatelyAfterSource() {
        var document = WorkspacePageEditor.createUserPage(base(), "page:user:one", "Studio")
        document = WorkspacePageEditor.addApp(document, "page:user:one", "item:app:1", "app:one")
        document = WorkspacePageEditor.addFolder(document, "page:user:one", "item:folder:1", "folder:one")

        val duplicate = WorkspacePageEditor.duplicateUserPage(
            document = document,
            sourcePageId = "page:user:one",
            pageId = "page:user:copy",
            title = "",
            newItemIds = listOf("item:copy:1", "item:copy:2"),
        )

        val sourceIndex = duplicate.pages.indexOfFirst { it.id == "page:user:one" }
        val copied = duplicate.pages.first { it.id == "page:user:copy" }
        val source = duplicate.pages[sourceIndex]
        assertEquals(sourceIndex + 1, duplicate.pages.indexOf(copied))
        assertEquals("Studio Kopie", copied.title)
        assertEquals("page:user:copy", duplicate.activePageId)
        assertEquals(source.items.map { it.bounds }, copied.items.map { it.bounds })
        assertEquals(source.items.map { it.content }, copied.items.map { it.content })
        assertEquals(listOf("item:copy:1", "item:copy:2"), copied.items.map { it.id })
        assertTrue(source.items.map { it.id }.intersect(copied.items.map { it.id }.toSet()).isEmpty())
    }

    @Test
    fun duplicateUserPage_rejectsLegacyPagesAndReusedItemIds() {
        val legacyId = WorkspaceStableIds.scenePage(SceneId.AI)
        assertThrows(IllegalArgumentException::class.java) {
            WorkspacePageEditor.duplicateUserPage(
                document = base(),
                sourcePageId = legacyId,
                pageId = "page:user:copy",
                title = "Copy",
                newItemIds = base().pages.first { it.id == legacyId }.items.mapIndexed { index, _ -> "copy:$index" },
            )
        }

        var document = WorkspacePageEditor.createUserPage(base(), "page:user:one", "Home")
        document = WorkspacePageEditor.addApp(document, "page:user:one", "item:app:1", "app:one")
        assertThrows(IllegalArgumentException::class.java) {
            WorkspacePageEditor.duplicateUserPage(
                document = document,
                sourcePageId = "page:user:one",
                pageId = "page:user:copy",
                title = "Copy",
                newItemIds = listOf("item:app:1"),
            )
        }
    }

    @Test
    fun legacyScenePages_areProtectedFromRenameDeleteAndAppPlacement() {
        val document = base()
        val legacyId = WorkspaceStableIds.scenePage(SceneId.AI)

        assertThrows(IllegalArgumentException::class.java) {
            WorkspacePageEditor.renameUserPage(document, legacyId, "Nope")
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorkspacePageEditor.deleteUserPage(document, legacyId)
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorkspacePageEditor.addApp(document, legacyId, "item:app:1", "app-key")
        }
    }

    @Test
    fun appsAndFolders_useDeterministicFirstFreeTwoByTwoCells() {
        var document = WorkspacePageEditor.createUserPage(base(), "page:user:one", "Home")
        document = WorkspacePageEditor.addApp(document, "page:user:one", "item:app:1", "app:one")
        document = WorkspacePageEditor.addFolder(document, "page:user:one", "item:folder:1", "folder:one")

        val page = document.pages.first { it.id == "page:user:one" }
        assertEquals(WorkspaceCellBounds(0, 0, 2, 2), page.items[0].bounds)
        assertEquals(WorkspaceCellBounds(2, 0, 2, 2), page.items[1].bounds)
        assertFalse(page.items[0].bounds.overlaps(page.items[1].bounds))
    }

    @Test
    fun moveItem_reflowsToNearestFreeCellDeterministically() {
        var document = WorkspacePageEditor.createUserPage(base(), "page:user:one", "Home")
        document = WorkspacePageEditor.addApp(document, "page:user:one", "item:app:1", "app:one")
        document = WorkspacePageEditor.addApp(document, "page:user:one", "item:app:2", "app:two")

        document = WorkspacePageEditor.moveItem(
            document = document,
            pageId = "page:user:one",
            itemId = "item:app:2",
            requestedBounds = WorkspaceCellBounds(0, 0, 2, 2),
        )

        val page = document.pages.first { it.id == "page:user:one" }
        val first = page.items.first { it.id == "item:app:1" }
        val second = page.items.first { it.id == "item:app:2" }
        assertEquals(WorkspaceCellBounds(0, 0, 2, 2), first.bounds)
        assertEquals(WorkspaceCellBounds(2, 0, 2, 2), second.bounds)
        assertFalse(first.bounds.overlaps(second.bounds))
    }

    @Test
    fun resizeItem_preservesRequestedSizeAndAvoidsCollisionsDeterministically() {
        var document = WorkspacePageEditor.createUserPage(base(), "page:user:one", "Home")
        document = WorkspacePageEditor.addApp(document, "page:user:one", "item:app:1", "app:one")
        document = WorkspacePageEditor.addApp(document, "page:user:one", "item:app:2", "app:two")

        document = WorkspacePageEditor.resizeItem(
            document = document,
            pageId = "page:user:one",
            itemId = "item:app:2",
            columnSpan = 4,
            rowSpan = 2,
        )

        val page = document.pages.first { it.id == "page:user:one" }
        val first = page.items.first { it.id == "item:app:1" }
        val second = page.items.first { it.id == "item:app:2" }
        assertEquals(4, second.bounds.columnSpan)
        assertEquals(2, second.bounds.rowSpan)
        assertFalse(first.bounds.overlaps(second.bounds))
        assertEquals(WorkspaceCellBounds(2, 0, 4, 2), second.bounds)
    }

    @Test
    fun compactUserPage_packsItemsTopLeftWithoutChangingOrderSizeOrContent() {
        var document = WorkspacePageEditor.createUserPage(base(), "page:user:one", "Home")
        document = WorkspacePageEditor.addApp(document, "page:user:one", "item:app:1", "app:one")
        document = WorkspacePageEditor.addFolder(document, "page:user:one", "item:folder:1", "folder:one")
        document = WorkspacePageEditor.moveItem(
            document,
            "page:user:one",
            "item:app:1",
            WorkspaceCellBounds(8, 8, 2, 2),
        )
        val before = document.pages.first { it.id == "page:user:one" }.items

        val compacted = WorkspacePageEditor.compactUserPage(document, "page:user:one")
        val after = compacted.pages.first { it.id == "page:user:one" }.items

        assertEquals(before.map { it.id }, after.map { it.id })
        assertEquals(before.map { it.content }, after.map { it.content })
        assertEquals(before.map { it.bounds.columnSpan to it.bounds.rowSpan }, after.map { it.bounds.columnSpan to it.bounds.rowSpan })
        assertEquals(WorkspaceCellBounds(0, 0, 2, 2), after[0].bounds)
        assertEquals(WorkspaceCellBounds(2, 0, 2, 2), after[1].bounds)
    }

    @Test
    fun moveItemToPage_preservesStableIdAndUsesNearestFreeTarget() {
        var document = WorkspacePageEditor.createUserPage(base(), "page:user:one", "One")
        document = WorkspacePageEditor.addApp(document, "page:user:one", "item:app:moving", "app:moving")
        document = WorkspacePageEditor.createUserPage(document, "page:user:two", "Two")
        document = WorkspacePageEditor.addFolder(document, "page:user:two", "item:folder:blocker", "folder:blocker")

        val moved = WorkspacePageEditor.moveItemToPage(
            document = document,
            sourcePageId = "page:user:one",
            targetPageId = "page:user:two",
            itemId = "item:app:moving",
            requestedBounds = WorkspaceCellBounds(0, 0, 2, 2),
        )

        val source = moved.pages.first { it.id == "page:user:one" }
        val target = moved.pages.first { it.id == "page:user:two" }
        assertTrue(source.items.none { it.id == "item:app:moving" })
        val movedItem = target.items.single { it.id == "item:app:moving" }
        assertEquals(WorkspaceCellBounds(2, 0, 2, 2), movedItem.bounds)
        assertEquals("page:user:two", moved.activePageId)
        assertEquals(1, moved.pages.flatMap(WorkspacePage::items).count { it.id == "item:app:moving" })
    }

    @Test
    fun moveItemToPage_rejectsLegacySceneTargetsAndSources() {
        var document = WorkspacePageEditor.createUserPage(base(), "page:user:one", "One")
        document = WorkspacePageEditor.addApp(document, "page:user:one", "item:app:1", "app:one")
        val legacyId = WorkspaceStableIds.scenePage(SceneId.AI)

        assertThrows(IllegalArgumentException::class.java) {
            WorkspacePageEditor.moveItemToPage(
                document,
                sourcePageId = "page:user:one",
                targetPageId = legacyId,
                itemId = "item:app:1",
            )
        }

        val legacyItemId = document.pages
            .first { it.id == legacyId }
            .items
            .first()
            .id
        assertThrows(IllegalArgumentException::class.java) {
            WorkspacePageEditor.moveItemToPage(
                document,
                sourcePageId = legacyId,
                targetPageId = "page:user:one",
                itemId = legacyItemId,
            )
        }
    }

    @Test
    fun normalizedAnchorsAndDuplicateIds_remainBoundedByV7Contract() {
        var document = WorkspacePageEditor.createUserPage(base(), "page:user:one", "Home")
        document = WorkspacePageEditor.addApp(document, "page:user:one", "item:app:1", "app:one")

        assertThrows(IllegalArgumentException::class.java) {
            WorkspacePageEditor.addFolder(document, "page:user:one", "item:app:1", "folder:one")
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorkspacePageEditor.createUserPage(document, "page:user:one", "Duplicate")
        }
    }

    @Test
    fun userPageLimit_isExplicitlyBounded() {
        var document = base()
        repeat(WorkspacePageEditor.MAX_USER_PAGES) { index ->
            document = WorkspacePageEditor.createUserPage(document, "page:user:$index", "Home ${index + 1}")
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorkspacePageEditor.createUserPage(document, "page:user:overflow", "Overflow")
        }
    }
}
