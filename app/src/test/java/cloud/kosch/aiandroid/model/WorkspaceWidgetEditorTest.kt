package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceWidgetEditorTest {
    @Test
    fun addWidget_storesProviderButNeverAndroidHostId() {
        val source = WorkspaceDocument(
            activePageId = "page:user:1",
            pages = listOf(WorkspacePage("page:user:1", "Home", 0)),
        )

        val updated = WorkspaceWidgetEditor.addWidget(
            document = source,
            pageId = "page:user:1",
            itemId = "item:user:widget-1",
            providerComponent = "com.example/.ClockWidget",
        )

        val item = updated.pages.single().items.single()
        val widget = item.content as WorkspaceItemContent.Widget
        assertEquals("com.example/.ClockWidget", widget.providerComponent)
        assertEquals(WorkspaceWidgetEditor.DEFAULT_COLUMN_SPAN, item.bounds.columnSpan)
        assertEquals(WorkspaceWidgetEditor.DEFAULT_ROW_SPAN, item.bounds.rowSpan)
        assertTrue(item.id.contains("widget-1"))
    }

    @Test
    fun remapProvider_preservesStableIdBoundsAndPage() {
        val source = WorkspaceDocument(
            activePageId = "page:user:1",
            pages = listOf(
                WorkspacePage(
                    id = "page:user:1",
                    title = "Home",
                    order = 0,
                    items = listOf(
                        WorkspaceItem(
                            id = "item:user:widget-1",
                            bounds = WorkspaceCellBounds(3, 4, 5, 2),
                            content = WorkspaceItemContent.Widget(null),
                        ),
                    ),
                ),
            ),
        )

        val remapped = WorkspaceWidgetEditor.remapProvider(
            document = source,
            itemId = "item:user:widget-1",
            providerComponent = "  com.example/.WeatherWidget  ",
        )

        val before = source.pages.single().items.single()
        val after = remapped.pages.single().items.single()
        assertEquals(before.id, after.id)
        assertEquals(before.bounds, after.bounds)
        assertEquals(source.activePageId, remapped.activePageId)
        assertEquals("com.example/.WeatherWidget", (after.content as WorkspaceItemContent.Widget).providerComponent)
    }

    @Test(expected = IllegalArgumentException::class)
    fun remapProvider_rejectsNonWidgetItems() {
        WorkspaceWidgetEditor.remapProvider(
            document = WorkspaceDocument(
                activePageId = "page:user:1",
                pages = listOf(
                    WorkspacePage(
                        id = "page:user:1",
                        title = "Home",
                        order = 0,
                        items = listOf(
                            WorkspaceItem(
                                id = "item:user:app-1",
                                bounds = WorkspaceCellBounds(0, 0, 2, 2),
                                content = WorkspaceItemContent.App("app:one"),
                            ),
                        ),
                    ),
                ),
            ),
            itemId = "item:user:app-1",
            providerComponent = "com.example/.ClockWidget",
        )
    }

    @Test
    fun duplicatePage_copiesPortableProviderButRequiresFreshBindingId() {
        val withWidget = WorkspaceWidgetEditor.addWidget(
            document = WorkspaceDocument(
                activePageId = "page:user:1",
                pages = listOf(WorkspacePage("page:user:1", "Home", 0)),
            ),
            pageId = "page:user:1",
            itemId = "item:user:widget-1",
            providerComponent = "com.example/.ClockWidget",
        )

        val duplicated = WorkspacePageEditor.duplicateUserPage(
            document = withWidget,
            sourcePageId = "page:user:1",
            pageId = "page:user:2",
            title = "",
            newItemIds = listOf("item:user:widget-2"),
        )

        val original = duplicated.pages.first { it.id == "page:user:1" }.items.single()
        val copy = duplicated.pages.first { it.id == "page:user:2" }.items.single()
        assertNotEquals(original.id, copy.id)
        assertEquals(original.content, copy.content)
        assertEquals(
            "com.example/.ClockWidget",
            (copy.content as WorkspaceItemContent.Widget).providerComponent,
        )
        // Android appWidgetId is structurally absent from WorkspaceItemContent.Widget and must be rebound device-locally.
    }

    @Test(expected = IllegalArgumentException::class)
    fun legacyScenePage_rejectsWidgetPlacement() {
        WorkspaceWidgetEditor.addWidget(
            document = WorkspaceDocument(
                activePageId = "page:scene",
                pages = listOf(WorkspacePage("page:scene", "AI", 0, sceneAdapter = SceneId.AI)),
            ),
            pageId = "page:scene",
            itemId = "item:widget",
            providerComponent = null,
        )
    }
}
