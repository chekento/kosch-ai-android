package cloud.kosch.aiandroid.system

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.WorkspaceHomeController
import cloud.kosch.aiandroid.data.LauncherSettingsStore
import cloud.kosch.aiandroid.data.WorkspaceStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceLayoutLockInstrumentationTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun lockedLayout_blocksMutationsAndEditEntryButKeepsWorkspaceIntact() {
        val settingsStore = LauncherSettingsStore(context)
        val workspaceStore = WorkspaceStore(context)
        val originalSettings = settingsStore.load()
        val originalWorkspace = workspaceStore.loadWorkspaceDocument()

        try {
            // Build one editable user page before enabling the policy so the test distinguishes lock from page type.
            check(settingsStore.save(originalSettings.copy(home = originalSettings.home.copy(lockLayout = false))))
            val home = WorkspaceHomeController(context, registerAsActive = false)
            home.createPage("Lock test")
            assertTrue(home.activePage.sceneAdapter == null)
            val unlockedDocument = home.document

            check(settingsStore.save(originalSettings.copy(home = originalSettings.home.copy(lockLayout = true))))
            assertTrue(home.layoutLocked)
            assertFalse("Home Studio edit entry must be gated while locked", home.isUserPage())

            home.createPage("Must not exist")
            assertEquals(unlockedDocument, home.document)
            assertTrue(home.statusMessage.orEmpty().contains("gesperrt", ignoreCase = true))

            home.addApp("app:blocked")
            assertEquals(unlockedDocument, home.document)

            home.undo()
            assertEquals(unlockedDocument, home.document)
        } finally {
            settingsStore.save(originalSettings)
            workspaceStore.saveWorkspaceDocument(originalWorkspace)
        }
    }
}
