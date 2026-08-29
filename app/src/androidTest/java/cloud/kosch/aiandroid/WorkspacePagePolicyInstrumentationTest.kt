package cloud.kosch.aiandroid

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.data.LauncherSettingsStore
import cloud.kosch.aiandroid.data.WorkspaceStore
import cloud.kosch.aiandroid.model.SceneId
import cloud.kosch.aiandroid.model.WorkspaceDocument
import cloud.kosch.aiandroid.model.WorkspaceItemContent
import cloud.kosch.aiandroid.model.WorkspacePagePolicy
import cloud.kosch.aiandroid.model.WorkspaceV7Migration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspacePagePolicyInstrumentationTest {
    @Test
    fun homeAndUserPages_staySeparateFromKalSystemSpaces() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val workspaceStore = WorkspaceStore(context)
        val settingsStore = LauncherSettingsStore(context)
        val originalWorkspace = workspaceStore.loadWorkspaceDocument()
        val originalSettings = settingsStore.load()

        try {
            check(settingsStore.save(originalSettings.copy(home = originalSettings.home.copy(lockLayout = false))))
            val fixture = WorkspaceV7Migration.fromLegacyScenePositions(SceneId.AI, emptyMap())
            check(workspaceStore.saveWorkspaceDocument(fixture))
            val controller = WorkspaceHomeController(context, registerAsActive = false)

            assertEquals(WorkspaceDocument.DEFAULT_PAGE_ID, controller.activePage.id)
            assertTrue(controller.isPrimaryHomePage())
            val initialPageCount = controller.document.pages.size
            val initialSystemCount = controller.systemPages().size

            controller.deleteActiveUserPage()
            assertEquals(WorkspaceDocument.DEFAULT_PAGE_ID, controller.activePage.id)
            assertEquals(initialPageCount, controller.document.pages.size)

            controller.createPage("Games")
            val gamesId = controller.activePage.id
            assertTrue(controller.isUserManagedPage())
            assertEquals("Games", controller.activePage.title)
            assertEquals(WorkspaceDocument.DEFAULT_PAGE_ID, controller.document.pages.first().id)
            assertEquals(gamesId, controller.document.pages[1].id)
            assertEquals(initialSystemCount, controller.systemPages().size)

            controller.createPage("Media")
            val mediaId = controller.activePage.id
            controller.moveActivePage(-1)
            assertEquals(
                listOf(WorkspaceDocument.DEFAULT_PAGE_ID, mediaId, gamesId),
                controller.document.pages.take(3).map { it.id },
            )

            controller.deleteActiveUserPage()
            assertFalse(controller.document.pages.any { it.id == mediaId })
            assertTrue(controller.document.pages.any { it.id == WorkspaceDocument.DEFAULT_PAGE_ID })
            assertEquals(initialSystemCount, controller.systemPages().size)

            val aiPage = controller.document.pages.first { it.sceneAdapter == SceneId.AI }
            val originalAiItems = aiPage.items
            controller.activatePage(aiPage.id)
            assertTrue(controller.isSystemPage())

            controller.addApp("test:cloud.kosch.personal-only")

            assertTrue(controller.isUserManagedPage())
            assertTrue(
                controller.activePage.items.any {
                    (it.content as? WorkspaceItemContent.App)?.appKey == "test:cloud.kosch.personal-only"
                },
            )
            val unchangedAi = controller.document.pages.first { it.id == aiPage.id }
            assertEquals(originalAiItems, unchangedAi.items)
            assertTrue(WorkspacePagePolicy.isSystem(unchangedAi))
        } finally {
            workspaceStore.saveWorkspaceDocument(originalWorkspace)
            settingsStore.save(originalSettings)
        }
    }
}
