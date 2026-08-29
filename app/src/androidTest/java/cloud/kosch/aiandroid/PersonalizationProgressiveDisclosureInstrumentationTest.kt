package cloud.kosch.aiandroid

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
class PersonalizationProgressiveDisclosureInstrumentationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun quickPersonalization_keepsDetailedGesturesCollapsedUntilRequested() {
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
        composeTestRule
            .onNodeWithText("Anpassen", useUnmergedTree = true)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Home Studio öffnen", useUnmergedTree = true)
            .fetchSemanticsNode()
        composeTestRule
            .onNodeWithText("Gesten bearbeiten", useUnmergedTree = true)
            .fetchSemanticsNode()
        check(
            composeTestRule
                .onAllNodesWithText("Nach oben wischen", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isEmpty(),
        ) { "Detailed gesture rows must stay collapsed until the user requests them." }

        composeTestRule
            .onNodeWithText("Gesten bearbeiten", useUnmergedTree = true)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Nach oben wischen", useUnmergedTree = true)
            .fetchSemanticsNode()
        composeTestRule
            .onNodeWithText("Weniger anzeigen", useUnmergedTree = true)
            .fetchSemanticsNode()
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
