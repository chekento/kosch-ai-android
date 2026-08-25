package cloud.kosch.aiandroid.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.WorkspaceHomeController
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
        val payload = store.createPortableSnapshot()
        var tamperedToWipe: ByteArray? = null
        try {
            val root = JSONObject(payload.toString(StandardCharsets.UTF_8))
            val workspace = root.getJSONObject("workspaceV7")
            val firstPage = workspace.getJSONArray("pages").getJSONObject(0)
            val firstItem = firstPage.getJSONArray("items").getJSONObject(0)
            firstItem.put("appWidgetId", 12345)
            val tampered = root.toString().toByteArray(StandardCharsets.UTF_8)
            tamperedToWipe = tampered

            try {
                store.previewPortableSnapshot(tampered)
                fail("Portable backup must reject Android appWidgetId")
            } catch (_: IllegalArgumentException) {
                // Expected: Android widget host IDs are deliberately device-bound.
            }
        } finally {
            payload.fill(0)
            tamperedToWipe?.fill(0)
        }
    }
}
