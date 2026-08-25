package cloud.kosch.aiandroid

import androidx.compose.ui.test.fetchSemanticsNode
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
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

        composeTestRule
            .onNodeWithContentDescription("KoSch Assistant einrichten", useUnmergedTree = true)
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("KoSch Assistant", useUnmergedTree = true)
            .fetchSemanticsNode()

        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("KoSch Assistant", useUnmergedTree = true)
            .fetchSemanticsNode()
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
