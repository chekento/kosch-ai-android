package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceWidgetPlacementTest {
    private fun userHome(): WorkspaceDocument = WorkspacePageEditor.createUserPage(
        WorkspaceV7Migration.fromLegacyScenePositions(SceneId.AI, emptyMap()),
        pageId = "page:user:widgets",
        title = "Widgets",
    )

    @Test
    fun widgetPlacement_usesPortableProviderAndDefaultFourByFourGrid() {
        val document = WorkspacePageEditor.addWidget(
            document = userHome(),
            pageId = "page:user:widgets",
            itemId = "item:widget:clock",
            providerComponent = "com.example/.ClockProvider",
        )

        val item = document.pages.first { it.id == "page:user:widgets" }.items.single()
        assertEquals(WorkspaceCellBounds(0, 0, 4, 4), item.bounds)
        assertEquals(
            WorkspaceItemContent.Widget("com.example/.ClockProvider"),
            item.content,
        )
    }

    @Test
    fun widgetPlacement_isCollisionSafeAndSupportsRemapPlaceholder() {
        var document = WorkspacePageEditor.addWidget(
            userHome(),
            "page:user:widgets",
            "item:widget:one",
            "com.example/.OneProvider",
        )
        document = WorkspacePageEditor.addWidget(
            document,
            "page:user:widgets",
            "item:widget:remap",
            null,
        )

        val items = document.pages.first { it.id == "page:user:widgets" }.items
        assertEquals(WorkspaceCellBounds(0, 0, 4, 4), items[0].bounds)
        assertEquals(WorkspaceCellBounds(4, 0, 4, 4), items[1].bounds)
        assertFalse(items[0].bounds.overlaps(items[1].bounds))
        assertEquals(WorkspaceItemContent.Widget(null), items[1].content)
    }

    @Test
    fun widgetPlacement_rejectsLegacyPagesAndInvalidSpans() {
        val document = WorkspaceV7Migration.fromLegacyScenePositions(SceneId.AI, emptyMap())
        val legacyPage = WorkspaceStableIds.scenePage(SceneId.AI)

        assertThrows(IllegalArgumentException::class.java) {
            WorkspacePageEditor.addWidget(
                document,
                legacyPage,
                "item:widget:legacy",
                "com.example/.Widget",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorkspacePageEditor.addWidget(
                userHome(),
                "page:user:widgets",
                "item:widget:bad",
                "com.example/.Widget",
                columnSpan = 0,
            )
        }
    }

    @Test
    fun widgetIdentity_remainsPortableAndContainsNoAndroidHostId() {
        val document = WorkspacePageEditor.addWidget(
            userHome(),
            "page:user:widgets",
            "item:widget:stable",
            "com.example/.Widget",
        )
        val json = cloud.kosch.aiandroid.data.WorkspaceDocumentCodec.encode(document)

        assertTrue(json.contains("com.example/.Widget"))
        assertFalse(json.contains("appWidgetId"))
    }
}
