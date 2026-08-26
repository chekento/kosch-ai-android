package cloud.kosch.aiandroid

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.model.HomePage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsCenterInstrumentationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launcherSettingsEntry_opensSearchableSettingsCenterAndLiveHomeSection() {
        composeTestRule.waitForIdle()
        dismissOnboardingIfVisible()
        val viewModel = ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]

        composeTestRule.runOnUiThread {
            viewModel.controller.switchHomePage(HomePage.WORKSPACE)
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("Launcher-Einstellungen", useUnmergedTree = true)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Settings Center", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Einstellungen durchsuchen", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Home & Raster", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Rasterspalten", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Rasterzeilen", useUnmergedTree = true).assertExists()

        composeTestRule
            .onNodeWithContentDescription("Settings Center schließen", useUnmergedTree = true)
            .performClick()
        composeTestRule.waitForIdle()
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
