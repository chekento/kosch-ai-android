package cloud.kosch.aiandroid

import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.model.SettingsSection
import cloud.kosch.aiandroid.ui.UNIVERSAL_SEARCH_RESULT_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UniversalSearchInstrumentationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun universalSearch_opensTypedSetting_andKeepsUtilityLocal() {
        composeTestRule.waitForIdle()
        dismissOnboardingIfVisible()
        val viewModel = ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]

        try {
            composeTestRule.runOnUiThread {
                viewModel.openUniversalSearch("Rasterspalten")
            }
            composeTestRule.waitForIdle()

            assertTextPresent("Universal Search")
            assertTextPresent("Rasterspalten")
            composeTestRule
                .onNode(
                    hasTestTag(UNIVERSAL_SEARCH_RESULT_TAG)
                        .and(hasAnyDescendant(hasText("Rasterspalten"))),
                    useUnmergedTree = true,
                )
                .performClick()
            composeTestRule.waitForIdle()

            assertFalse(viewModel.universalSearch.visible)
            assertTrue(viewModel.settings.visible)
            assertEquals(SettingsSection.HOME, viewModel.settings.requestedSection)

            composeTestRule.runOnUiThread {
                viewModel.settings.close()
                viewModel.openUniversalSearch("12*8")
            }
            composeTestRule.waitForIdle()

            assertTextPresent("96")
            assertTextPresent("Lokales Ergebnis · keine externe Aktion")
            assertTrue(viewModel.universalSearch.visible)
            assertFalse(viewModel.settings.visible)
        } finally {
            composeTestRule.runOnUiThread {
                viewModel.universalSearch.close()
                viewModel.settings.close()
                viewModel.aiHub.close()
            }
            composeTestRule.waitForIdle()
        }
    }

    private fun assertTextPresent(value: String) {
        assertTrue(
            "Expected text '$value' to exist",
            composeTestRule
                .onAllNodesWithText(value, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty(),
        )
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
