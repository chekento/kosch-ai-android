package cloud.kosch.aiandroid

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.data.LauncherSettingsStore
import cloud.kosch.aiandroid.data.WorkspaceStore
import cloud.kosch.aiandroid.model.GestureSettings
import cloud.kosch.aiandroid.model.HomePage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceSwipeNavigationInstrumentationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun freeHomeSurface_swipesBetweenPersonalPagesInBothDirections() {
        composeTestRule.waitForIdle()
        dismissOnboardingIfVisible()

        val viewModel = ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]
        val workspaceStore = WorkspaceStore(composeTestRule.activity.applicationContext)
        val settingsStore = LauncherSettingsStore(composeTestRule.activity.applicationContext)
        val originalWorkspace = workspaceStore.loadWorkspaceDocument()
        val originalSettings = settingsStore.load()
        val originalHomePage = viewModel.controller.homePage

        try {
            lateinit var firstId: String
            lateinit var secondId: String
            composeTestRule.runOnUiThread {
                viewModel.settings.applyGestures(GestureSettings())
                viewModel.homeWorkspace.createPage("Swipe A")
                firstId = viewModel.homeWorkspace.activePage.id
                viewModel.homeWorkspace.createPage("Swipe B")
                secondId = viewModel.homeWorkspace.activePage.id
                viewModel.homeWorkspace.activatePage(firstId)
                viewModel.controller.switchHomePage(HomePage.WORKSPACE)
            }
            composeTestRule.waitForIdle()

            assertEquals(firstId, viewModel.homeWorkspace.document.activePageId)
            assertEquals("Swipe A", viewModel.homeWorkspace.activePage.title)

            composeTestRule.onRoot(useUnmergedTree = true).performTouchInput { swipeLeft() }
            composeTestRule.waitForIdle()

            assertEquals(secondId, viewModel.homeWorkspace.document.activePageId)
            assertEquals("Swipe B", viewModel.homeWorkspace.activePage.title)

            composeTestRule.onRoot(useUnmergedTree = true).performTouchInput { swipeRight() }
            composeTestRule.waitForIdle()

            assertEquals(firstId, viewModel.homeWorkspace.document.activePageId)
            assertEquals("Swipe A", viewModel.homeWorkspace.activePage.title)
        } finally {
            composeTestRule.runOnUiThread {
                workspaceStore.saveWorkspaceDocument(originalWorkspace)
                settingsStore.save(originalSettings)
                viewModel.homeWorkspace.reload()
                viewModel.settings.reload()
                viewModel.controller.switchHomePage(originalHomePage)
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
                .onAllNodesWithText("Tour überspringen", useUnmergedTree = true)[0]
                .performClick()
            composeTestRule.waitForIdle()
        }
    }
}
