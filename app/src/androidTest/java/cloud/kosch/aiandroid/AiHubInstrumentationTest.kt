package cloud.kosch.aiandroid

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.model.HomePage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiHubInstrumentationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun unifiedHome_opensSmartHubAndBrowserInventoryWithSystemAndStoreRoutes() {
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
            assertTextPresent("Smart")
            assertTextPresent("KoSch empfiehlt")

            composeTestRule.onNodeWithText("Browser", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()

            assertTextPresent("Android Systembrowser")
            assertTextPresent("Google Chrome")
            assertTextPresent("Microsoft Edge")
            assertTextPresent("Opera Browser")
            assertTextPresent("Brave Browser")
            assertTextPresent("DuckDuckGo Browser")
            assertTextPresent("Mozilla Firefox")
            assertTextPresent("PLAY STORE")
        } finally {
            composeTestRule.runOnUiThread {
                viewModel.aiHub.close()
                viewModel.aiHub.restoreAll()
                viewModel.controller.switchHomePage(originalPage)
            }
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun legacyProviderEntry_isMigratedIntoTaskAwareSmartHub() {
        composeTestRule.waitForIdle()
        dismissOnboardingIfVisible()
        val viewModel = ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]
        val originalPage = viewModel.controller.homePage
        val prompt = "Recherchiere aktuelle Quellen"

        try {
            composeTestRule.runOnUiThread {
                viewModel.controller.switchHomePage(HomePage.WORKSPACE)
                viewModel.aiHub.close()
                viewModel.controller.openProviderChooser(prompt)
            }
            composeTestRule.waitForIdle()

            assertFalse(viewModel.controller.providerChooserVisible)
            assertTrue(viewModel.aiHub.visible)
            assertEquals(prompt, viewModel.aiHub.prompt)
            assertTextPresent("AI & Browser Hub")
            assertTextPresent("Recherche")
        } finally {
            composeTestRule.runOnUiThread {
                viewModel.controller.closeProviderChooser()
                viewModel.aiHub.close()
                viewModel.controller.switchHomePage(originalPage)
            }
            composeTestRule.waitForIdle()
        }
    }

    private fun assertTextPresent(text: String) {
        val nodes = composeTestRule
            .onAllNodesWithText(text, substring = true, useUnmergedTree = true)
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
