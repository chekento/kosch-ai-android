package cloud.kosch.aiandroid

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.data.WorkspaceStore
import cloud.kosch.aiandroid.model.HomePage
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
    fun userHomePage_rendersCompanionKeepsHomeAcrossDrawerAndSurvivesRecreation() {
        composeTestRule.waitForIdle()
        dismissOnboardingIfVisible()

        val originalPage = ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]
            .controller.homePage
        val store = WorkspaceStore(composeTestRule.activity.applicationContext)
        val originalDocument = store.loadWorkspaceDocument()

        try {
            composeTestRule.runOnUiThread {
                val viewModel = ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]
                viewModel.homeWorkspace.createPage("API36 Home Test")
                viewModel.homeWorkspace.addApp("test:cloud.kosch.missing")
                viewModel.controller.switchHomePage(HomePage.WORKSPACE)
            }
            composeTestRule.waitForIdle()

            assertTextPresent("API36 Home Test")
            composeTestRule
                .onNodeWithText("V7 HOME · frei platzierbar", useUnmergedTree = true)
                .fetchSemanticsNode()
            composeTestRule
                .onNodeWithText("App fehlt", useUnmergedTree = true)
                .fetchSemanticsNode()
            composeTestRule
                .onNodeWithContentDescription("App, Ordner oder Seite hinzufügen", useUnmergedTree = true)
                .fetchSemanticsNode()

            val assistantNodes = composeTestRule
                .onAllNodesWithContentDescription("KoSch Assistant einrichten", useUnmergedTree = true)
                .fetchSemanticsNodes() + composeTestRule
                .onAllNodesWithContentDescription("KoSch Assistant öffnen", useUnmergedTree = true)
                .fetchSemanticsNodes()
            assertTrue("Expected Assistant companion on unified Home", assistantNodes.isNotEmpty())

            composeTestRule
                .onNodeWithContentDescription("Alle Apps", useUnmergedTree = true)
                .performClick()
            composeTestRule.waitForIdle()

            val openDrawerViewModel = ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]
            assertEquals(HomePage.WORKSPACE, openDrawerViewModel.controller.homePage)
            composeTestRule
                .onNodeWithText("App-Raum", useUnmergedTree = true)
                .fetchSemanticsNode()

            composeTestRule.runOnUiThread {
                ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]
                    .controller.closeDrawer()
            }
            composeTestRule.waitForIdle()
            assertTextPresent("API36 Home Test")

            composeTestRule.activityRule.scenario.recreate()
            composeTestRule.waitForIdle()

            assertTextPresent("API36 Home Test")
            composeTestRule
                .onNodeWithText("App fehlt", useUnmergedTree = true)
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

    private fun assertTextPresent(text: String) {
        val nodes = composeTestRule
            .onAllNodesWithText(text, useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("Expected at least one node containing '$text'", nodes.isNotEmpty())
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
