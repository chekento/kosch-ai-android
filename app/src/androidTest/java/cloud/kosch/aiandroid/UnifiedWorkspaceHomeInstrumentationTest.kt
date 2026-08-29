package cloud.kosch.aiandroid

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.data.WorkspaceStore
import cloud.kosch.aiandroid.model.HomePage
import cloud.kosch.aiandroid.model.WorkspaceDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UnifiedWorkspaceHomeInstrumentationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun userHomePage_keepsCoreEntryPointsAcrossDrawerAndSurvivesRecreation() {
        composeTestRule.waitForIdle()
        dismissOnboardingIfVisible()

        val initialViewModel = ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]
        val originalPage = initialViewModel.controller.homePage
        val store = WorkspaceStore(composeTestRule.activity.applicationContext)
        val originalDocument = store.loadWorkspaceDocument()

        try {
            composeTestRule.runOnUiThread {
                initialViewModel.homeWorkspace.createPage("API36 Home Test")
                initialViewModel.homeWorkspace.addApp("test:cloud.kosch.missing")
                initialViewModel.controller.switchHomePage(HomePage.WORKSPACE)
            }
            composeTestRule.waitForIdle()

            assertEquals(WorkspaceDocument.DEFAULT_PAGE_ID, initialViewModel.homeWorkspace.document.pages.first().id)
            assertEquals("API36 Home Test", initialViewModel.homeWorkspace.activePage.title)
            assertTrue(initialViewModel.homeWorkspace.isUserManagedPage())
            composeTestRule
                .onNodeWithText("App fehlt", useUnmergedTree = true)
                .fetchSemanticsNode()
            composeTestRule
                .onNodeWithContentDescription("Zum Homescreen hinzufügen", useUnmergedTree = true)
                .fetchSemanticsNode()
            composeTestRule
                .onNodeWithContentDescription("Alle Apps", useUnmergedTree = true)
                .fetchSemanticsNode()
            composeTestRule
                .onNodeWithContentDescription("KAL Menü", useUnmergedTree = true)
                .fetchSemanticsNode()

            // Exercise the real Home button. The product contract is that the tap opens the drawer state;
            // Material3 sheet composition/animation timing is deliberately not part of that contract.
            composeTestRule
                .onNodeWithContentDescription("Alle Apps", useUnmergedTree = true)
                .performClick()
            composeTestRule.waitUntil(timeoutMillis = 5_000L) {
                initialViewModel.controller.drawerVisible
            }

            val openDrawerViewModel = ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]
            assertEquals(HomePage.WORKSPACE, openDrawerViewModel.controller.homePage)
            assertTrue(openDrawerViewModel.controller.drawerVisible)

            composeTestRule.runOnUiThread {
                openDrawerViewModel.controller.closeDrawer()
            }
            composeTestRule.waitUntil(timeoutMillis = 5_000L) {
                !openDrawerViewModel.controller.drawerVisible
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("App fehlt", useUnmergedTree = true).fetchSemanticsNode()

            composeTestRule.activityRule.scenario.recreate()
            composeTestRule.waitForIdle()

            val recreated = ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]
            assertEquals(WorkspaceDocument.DEFAULT_PAGE_ID, recreated.homeWorkspace.document.pages.first().id)
            assertEquals("API36 Home Test", recreated.homeWorkspace.activePage.title)
            assertTrue(recreated.homeWorkspace.isUserManagedPage())
            composeTestRule
                .onNodeWithText("App fehlt", useUnmergedTree = true)
                .fetchSemanticsNode()
            composeTestRule
                .onNodeWithContentDescription("Alle Apps", useUnmergedTree = true)
                .fetchSemanticsNode()
            composeTestRule
                .onNodeWithContentDescription("Zum Homescreen hinzufügen", useUnmergedTree = true)
                .fetchSemanticsNode()
        } finally {
            composeTestRule.runOnUiThread {
                val currentViewModel = ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]
                store.saveWorkspaceDocument(originalDocument)
                currentViewModel.homeWorkspace.reload()
                currentViewModel.controller.switchHomePage(originalPage)
            }
            composeTestRule.waitForIdle()
        }
    }

    private fun dismissOnboardingIfVisible() {
        val skipNodes = composeTestRule
            .onAllNodesWithText("Tour überspringen", useUnmergedTree = true)
            .fetchSemanticsNodes()
        if (skipNodes.isNotEmpty()) {
            composeTestRule
                .onNodeWithText("Tour überspringen", useUnmergedTree = true)
                .performClick()
            composeTestRule.waitForIdle()
        }
    }
}
