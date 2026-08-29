package cloud.kosch.aiandroid

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.model.HomePage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReferenceHomeSettingsRuntimeInstrumentationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun dockEnabledAndAskSwitch_areImmediateRuntimeGates() {
        composeTestRule.waitForIdle()
        dismissOnboardingIfVisible()
        val viewModel = ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]
        val original = viewModel.settings.document.dock

        try {
            composeTestRule.runOnUiThread {
                viewModel.controller.switchHomePage(HomePage.WORKSPACE)
                check(viewModel.settings.applyDock(original.copy(enabled = true, showAskButton = true)))
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithContentDescription("KAL Dock", useUnmergedTree = true).fetchSemanticsNode()
            composeTestRule.onNodeWithContentDescription("Ask / KI", useUnmergedTree = true).fetchSemanticsNode()

            composeTestRule.runOnUiThread {
                check(viewModel.settings.applyDock(viewModel.settings.document.dock.copy(showAskButton = false)))
            }
            composeTestRule.waitForIdle()
            check(
                composeTestRule
                    .onAllNodesWithContentDescription("Ask / KI", useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isEmpty(),
            ) { "Ask/AI dock entry must disappear when its setting is disabled." }

            composeTestRule.runOnUiThread {
                check(viewModel.settings.applyDock(viewModel.settings.document.dock.copy(enabled = false)))
            }
            composeTestRule.waitForIdle()
            check(
                composeTestRule
                    .onAllNodesWithContentDescription("KAL Dock", useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isEmpty(),
            ) { "Reference Home dock must disappear when DockSettings.enabled is false." }
        } finally {
            composeTestRule.runOnUiThread {
                viewModel.settings.applyDock(original)
            }
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun pageIndicatorSetting_isAnImmediateRuntimeGate() {
        composeTestRule.waitForIdle()
        dismissOnboardingIfVisible()
        val viewModel = ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]
        val original = viewModel.settings.document.home

        try {
            composeTestRule.runOnUiThread {
                viewModel.controller.switchHomePage(HomePage.WORKSPACE)
                check(viewModel.homeWorkspace.document.pages.size > 1) {
                    "Reference Home contract requires more than one workspace page."
                }
                check(viewModel.settings.applyHome(original.copy(showPageIndicator = true), viewModel.homeWorkspace))
            }
            composeTestRule.waitForIdle()
            composeTestRule
                .onNodeWithContentDescription("KAL Seitenindikator", useUnmergedTree = true)
                .fetchSemanticsNode()

            composeTestRule.runOnUiThread {
                check(
                    viewModel.settings.applyHome(
                        viewModel.settings.document.home.copy(showPageIndicator = false),
                        viewModel.homeWorkspace,
                    ),
                )
            }
            composeTestRule.waitForIdle()
            check(
                composeTestRule
                    .onAllNodesWithContentDescription("KAL Seitenindikator", useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isEmpty(),
            ) { "Page indicator must disappear when HomeSettings.showPageIndicator is false." }
        } finally {
            composeTestRule.runOnUiThread {
                viewModel.settings.applyHome(original, viewModel.homeWorkspace)
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
                .onAllNodesWithText("Tour überspringen", useUnmergedTree = true)[0]
                .performClick()
            composeTestRule.waitForIdle()
        }
    }
}
