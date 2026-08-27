package cloud.kosch.aiandroid

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.model.HomePage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiHubInstrumentationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun unifiedHome_opensAiAndBrowserHubWithSystemAndStoreRoutes() {
        composeTestRule.waitForIdle()
        dismissOnboardingIfVisible()
        val viewModel = ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]
        val originalPage = viewModel.controller.homePage

        try {
            composeTestRule.runOnUiThread {
                viewModel.controller.switchHomePage(HomePage.WORKSPACE)
                viewModel.aiHub.restoreAll()
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("AI Hub", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()

            assertTextPresent("AI & Browser Hub")
            assertTextPresent("Android Systembrowser")
            assertTextPresent("Google Chrome")
            assertTextPresent("Microsoft Edge")
            assertTextPresent("Opera Browser")
            assertTextPresent("PLAY STORE")

            composeTestRule.onNodeWithText("Browser", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()
            assertTextPresent("Brave Browser")
            assertTextPresent("DuckDuckGo Browser")
            assertTextPresent("Mozilla Firefox")
        } finally {
            composeTestRule.runOnUiThread {
                viewModel.aiHub.close()
                viewModel.aiHub.restoreAll()
                viewModel.controller.switchHomePage(originalPage)
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
            composeTestRule.onNodeWithText("Tour überspringen", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()
        }
    }
}
