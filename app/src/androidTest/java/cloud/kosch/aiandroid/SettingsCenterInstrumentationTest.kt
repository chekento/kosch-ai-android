package cloud.kosch.aiandroid

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
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
    fun launcherSettingsEntry_opensSearchableCalmSettingsCenterAndLiveHomeSection() {
        composeTestRule.waitForIdle()
        dismissOnboardingIfVisible()
        val viewModel = ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]

        composeTestRule.runOnUiThread {
            viewModel.controller.switchHomePage(HomePage.WORKSPACE)
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("KAL Menü", useUnmergedTree = true)
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("Einstellungen", useUnmergedTree = true)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Finde schnell, was du ändern möchtest.", useUnmergedTree = true).fetchSemanticsNode()
        val search = composeTestRule.onNodeWithText("Einstellungen durchsuchen", useUnmergedTree = true)
        search.fetchSemanticsNode()

        // Expert/diagnostic areas do not dominate the first screen, but stay one search away.
        check(
            composeTestRule
                .onAllNodesWithText("Erweitert & Diagnose", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isEmpty(),
        ) { "Advanced diagnostics must not dominate the initial Settings Center view." }
        search.performTextInput("Erweitert")
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("Erweitert & Diagnose", useUnmergedTree = true)
            .fetchSemanticsNode()
        search.performTextClearance()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Home & Raster", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Rasterspalten", useUnmergedTree = true).fetchSemanticsNode()
        composeTestRule.onNodeWithText("Rasterzeilen", useUnmergedTree = true).fetchSemanticsNode()

        // Phone navigation stays hierarchical: detail -> section list -> close.
        composeTestRule
            .onNodeWithContentDescription("Zurück zu allen Einstellungen", useUnmergedTree = true)
            .performClick()
        composeTestRule.waitForIdle()
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
