package cloud.kosch.aiandroid

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.data.WorkspaceStore
import cloud.kosch.aiandroid.model.HomePage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceDragDropInstrumentationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun directDragAcrossRightEdge_movesStableItemToNextUserPageAndPersists() {
        composeTestRule.waitForIdle()
        dismissOnboardingIfVisible()

        val store = WorkspaceStore(composeTestRule.activity.applicationContext)
        val originalDocument = store.loadWorkspaceDocument()
        val viewModel = ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]
        val originalHomePage = viewModel.controller.homePage
        var sourcePageId = ""
        var targetPageId = ""
        var itemId = ""

        try {
            composeTestRule.runOnUiThread {
                viewModel.homeWorkspace.createPage("Drag Source")
                sourcePageId = viewModel.homeWorkspace.activePage.id
                viewModel.homeWorkspace.addApp("test:cloud.kosch.drag-missing")
                itemId = viewModel.homeWorkspace.activePage.items.single().id

                viewModel.homeWorkspace.createPage("Drag Target")
                targetPageId = viewModel.homeWorkspace.activePage.id
                viewModel.homeWorkspace.activatePage(sourcePageId)
                viewModel.controller.switchHomePage(HomePage.WORKSPACE)
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Home Studio", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Ziehen · skalieren · Seiten verwalten · automatisch anordnen", useUnmergedTree = true)
                .fetchSemanticsNode()

            composeTestRule
                .onNodeWithContentDescription(
                    "App. Ziehen zum Verschieben. Tippen für Größenoptionen",
                    useUnmergedTree = true,
                )
                .performTouchInput {
                    down(center)
                    // Touch injection distances are physical pixels. Move in several realistic pointer steps
                    // far enough to cross the full 12-column canvas on the API 36 Pixel 2 density.
                    repeat(8) { moveBy(Offset(160f, 0f)) }
                    up()
                }
            composeTestRule.waitForIdle()

            val moved = viewModel.homeWorkspace.document
            assertEquals(targetPageId, moved.activePageId)
            assertFalse(moved.pages.first { it.id == sourcePageId }.items.any { it.id == itemId })
            assertTrue(moved.pages.first { it.id == targetPageId }.items.any { it.id == itemId })
            assertEquals(1, moved.pages.flatMap { it.items }.count { it.id == itemId })

            composeTestRule.activityRule.scenario.recreate()
            composeTestRule.waitForIdle()

            val persisted = WorkspaceStore(composeTestRule.activity.applicationContext).loadWorkspaceDocument()
            assertEquals(targetPageId, persisted.activePageId)
            assertFalse(persisted.pages.first { it.id == sourcePageId }.items.any { it.id == itemId })
            assertTrue(persisted.pages.first { it.id == targetPageId }.items.any { it.id == itemId })
            assertEquals(1, persisted.pages.flatMap { it.items }.count { it.id == itemId })
        } finally {
            composeTestRule.runOnUiThread {
                val current = ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]
                store.saveWorkspaceDocument(originalDocument)
                current.homeWorkspace.reload()
                current.controller.switchHomePage(originalHomePage)
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
