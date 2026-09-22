package cloud.kosch.aiandroid

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssistantUiInstrumentationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun askDockCompanion_opensAssistant_andSurvivesActivityRecreation() {
        composeTestRule.waitForIdle()
        dismissOnboardingIfVisible()
        openAssistant()

        assertTextPresent("KAL Assistant")

        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()

        assertTextPresent("KAL Assistant")
    }

    @Test
    fun assistantControlCenter_exposesPrivacyAndCharacterControls() {
        composeTestRule.waitForIdle()
        dismissOnboardingIfVisible()
        openAssistant()

        composeTestRule
            .onNodeWithText("Steuerung", useUnmergedTree = true)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Privacy Live", useUnmergedTree = true)
            .fetchSemanticsNode()
        composeTestRule
            .onNodeWithText("Charakter & Name", useUnmergedTree = true)
            .fetchSemanticsNode()
        composeTestRule
            .onNodeWithText("Name des Assistenten", useUnmergedTree = true)
            .fetchSemanticsNode()
    }

    private fun openAssistant() {
        val setupNodes = composeTestRule
            .onAllNodesWithContentDescription("KAL Assistant einrichten", useUnmergedTree = true)
            .fetchSemanticsNodes()
        if (setupNodes.isNotEmpty()) {
            composeTestRule
                .onNodeWithContentDescription("KAL Assistant einrichten", useUnmergedTree = true)
                .performClick()
        } else {
            composeTestRule
                .onNodeWithContentDescription("KAL Assistant öffnen", useUnmergedTree = true)
                .performClick()
        }
        composeTestRule.waitForIdle()
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

    private fun assertTextPresent(text: String) {
        val nodes = composeTestRule
            .onAllNodesWithText(text, useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("Expected at least one node containing '$text'", nodes.isNotEmpty())
    }
}
