package cloud.kosch.aiandroid.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.WorkspaceHomeController
import cloud.kosch.aiandroid.model.WorkspaceCellBounds
import cloud.kosch.aiandroid.model.WorkspaceItem
import cloud.kosch.aiandroid.model.WorkspaceItemContent
import cloud.kosch.aiandroid.model.WorkspacePageEditor
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.charset.StandardCharsets

@RunWith(AndroidJUnit4::class)
class WorkspaceHomeBackupInstrumentationTest {
    @Test
    fun userHomePage_appsAndFolders_surviveFreshControllerAndPortableBackupV3() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = WorkspaceStore(context)
        val original = store.loadWorkspaceDocument()
        var payloadToWipe: ByteArray? = null
        try {
            var custom = WorkspacePageEditor.createUserPage(
                original,
                pageId = "page:test:portable-home",
                title = "Portable Home",
            )
            custom = WorkspacePageEditor.addApp(
                custom,
                pageId = "page:test:portable-home",
                itemId = "item:test:portable-app",
                appKey = "test:cloud.kosch.portable",
            )
            custom = WorkspacePageEditor.addFolder(
                custom,
                pageId = "page:test:portable-home",
                itemId = "item:test:portable-folder",
                folderId = "folder:test:portable",
            )
            assertTrue(store.saveWorkspaceDocument(custom))

            val freshController = WorkspaceHomeController(context)
            assertEquals("page:test:portable-home", freshController.document.activePageId)
            assertEquals(
                2,
                freshController.document.pages.first { it.id == "page:test:portable-home" }.items.size,
            )

            val payload = store.createPortableSnapshot()
            payloadToWipe = payload
            val root = JSONObject(payload.toString(StandardCharsets.UTF_8))
            assertEquals(3, root.getInt("version"))
            assertTrue(root.has("workspaceV7"))

            assertTrue(store.saveWorkspaceDocument(original))
            store.restorePortableSnapshot(payload)

            val restored = WorkspaceStore(context).loadWorkspaceDocument()
            val restoredPage = restored.pages.first { it.id == "page:test:portable-home" }
            assertEquals("Portable Home", restoredPage.title)
            assertTrue(restoredPage.items.any { it.content is WorkspaceItemContent.App })
            assertTrue(restoredPage.items.any { it.content is WorkspaceItemContent.Folder })
            assertFalse(root.optJSONArray("excluded")?.toString().orEmpty().contains("workspaceV7"))
        } finally {
            payloadToWipe?.fill(0)
            store.saveWorkspaceDocument(original)
        }
    }

    @Test
    fun portableBackupV3_rejectsEmbeddedAndroidWidgetHostId() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = WorkspaceStore(context)
        val original = store.loadWorkspaceDocument()
        var payloadToWipe: ByteArray? = null
        var tamperedToWipe: ByteArray? = null
        try {
            val homeId = original.activePageId
            val fixtureItem = WorkspaceItem(
                id = "item:test:tamper-widget",
                bounds = WorkspaceCellBounds(0, 0, 2, 2),
                content = WorkspaceItemContent.Widget("cloud.kosch.fixture/.Widget"),
            )
            val fixture = original.copy(
                pages = original.pages.map { page ->
                    if (page.id == homeId) page.copy(items = page.items + fixtureItem) else page
                },
            ).normalized()
            assertTrue(store.saveWorkspaceDocument(fixture))

            val payload = store.createPortableSnapshot()
            payloadToWipe = payload
            val root = JSONObject(payload.toString(StandardCharsets.UTF_8))
            val pages = root.getJSONObject("workspaceV7").getJSONArray("pages")
            var targetItem: JSONObject? = null
            for (pageIndex in 0 until pages.length()) {
                val page = pages.getJSONObject(pageIndex)
                val items = page.getJSONArray("items")
                for (itemIndex in 0 until items.length()) {
                    val item = items.getJSONObject(itemIndex)
                    if (item.optString("id") == fixtureItem.id) {
                        targetItem = item
                        break
                    }
                }
                if (targetItem != null) break
            }
            requireNotNull(targetItem) { "Portable fixture item missing from backup" }
                .put("appWidgetId", 12345)
            val tampered = root.toString().toByteArray(StandardCharsets.UTF_8)
            tamperedToWipe = tampered

            try {
                store.previewPortableSnapshot(tampered)
                fail("Portable backup must reject Android appWidgetId")
            } catch (_: IllegalArgumentException) {
                // Expected: Android widget host IDs are deliberately device-bound.
            }
        } finally {
            payloadToWipe?.fill(0)
            tamperedToWipe?.fill(0)
            store.saveWorkspaceDocument(original)
        }
    }
}
