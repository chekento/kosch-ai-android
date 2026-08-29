package cloud.kosch.aiandroid

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
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
    fun freeHomeSurface_swipesBetweenAdjacentPersonalPagesInBothDirections() {
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
            composeTestRule.runOnUiThread {
                viewModel.settings.applyGestures(GestureSettings())
                viewModel.homeWorkspace.createPage("Swipe A")
                firstId = viewModel.homeWorkspace.activePage.id
                viewModel.homeWorkspace.createPage("Swipe B")
                viewModel.homeWorkspace.activatePage(firstId)
                viewModel.controller.switchHomePage(HomePage.WORKSPACE)
            }
            composeTestRule.waitForIdle()

            assertEquals(firstId, viewModel.homeWorkspace.document.activePageId)
            assertEquals("Swipe A", viewModel.homeWorkspace.activePage.title)

            val adjacentId = checkNotNull(viewModel.homeWorkspace.adjacentUserPageId(+1)) {
                "Swipe A must have a personal page to its right"
            }
            val adjacentTitle = viewModel.homeWorkspace.document.pages.first { it.id == adjacentId }.title

            // Keep the gesture well away from edge-trigger regions. API 36's generic swipeLeft()/swipeRight()
            // helpers can begin close enough to an edge on some managed-device densities to exercise EDGE_* instead.
            composeTestRule.onRoot(useUnmergedTree = true).performTouchInput {
                val width = visibleSize.width.toFloat()
                val height = visibleSize.height.toFloat()
                swipe(
                    start = Offset(width * 0.75f, height * 0.50f),
                    end = Offset(width * 0.25f, height * 0.50f),
                    durationMillis = 300L,
                )
            }
            composeTestRule.waitUntil(timeoutMillis = 5_000L) {
                viewModel.homeWorkspace.document.activePageId == adjacentId
            }
            composeTestRule.waitForIdle()

            assertEquals(adjacentId, viewModel.homeWorkspace.document.activePageId)
            assertEquals(adjacentTitle, viewModel.homeWorkspace.activePage.title)

            composeTestRule.onRoot(useUnmergedTree = true).performTouchInput {
                val width = visibleSize.width.toFloat()
                val height = visibleSize.height.toFloat()
                swipe(
                    start = Offset(width * 0.25f, height * 0.50f),
                    end = Offset(width * 0.75f, height * 0.50f),
                    durationMillis = 300L,
                )
            }
            composeTestRule.waitUntil(timeoutMillis = 5_000L) {
                viewModel.homeWorkspace.document.activePageId == firstId
            }
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
