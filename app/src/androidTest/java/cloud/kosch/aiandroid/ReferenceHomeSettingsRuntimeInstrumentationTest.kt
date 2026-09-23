package cloud.kosch.aiandroid

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.data.WorkspaceStore
import cloud.kosch.aiandroid.model.HomePage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReferenceHomeSettingsRuntimeInstrumentationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun communicationDockEnabled_isAnImmediateRuntimeGate() {
        composeTestRule.waitForIdle()
        dismissOnboardingIfVisible()
        val viewModel = ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]
        val original = viewModel.settings.document.dock

        try {
            composeTestRule.runOnUiThread {
                viewModel.controller.switchHomePage(HomePage.WORKSPACE)
                check(viewModel.settings.applyDock(original.copy(enabled = true)))
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithContentDescription("KAL Dock", useUnmergedTree = true).fetchSemanticsNode()
            listOf("Telefon", "SMS", "Alle Apps", "WhatsApp", "Kontakte").forEach { label ->
                composeTestRule.onNodeWithContentDescription(label, useUnmergedTree = true).fetchSemanticsNode()
            }

            composeTestRule.runOnUiThread {
                check(viewModel.settings.applyDock(viewModel.settings.document.dock.copy(enabled = false)))
            }
            composeTestRule.waitForIdle()
            check(
                composeTestRule
                    .onAllNodesWithContentDescription("KAL Dock", useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isEmpty(),
            ) { "Clean Home dock must disappear when DockSettings.enabled is false." }
        } finally {
            composeTestRule.runOnUiThread {
                viewModel.settings.applyDock(original)
            }
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun pageIndicatorSetting_isAnImmediateRuntimeGateForPersonalPages() {
        composeTestRule.waitForIdle()
        dismissOnboardingIfVisible()
        val viewModel = ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]
        val workspaceStore = WorkspaceStore(composeTestRule.activity.applicationContext)
        val originalWorkspace = workspaceStore.loadWorkspaceDocument()
        val originalHome = viewModel.settings.document.home

        try {
            composeTestRule.runOnUiThread {
                viewModel.controller.switchHomePage(HomePage.WORKSPACE)
                if (viewModel.homeWorkspace.personalPages().size < 2) {
                    viewModel.homeWorkspace.createPage("Indicator Test")
                }
                check(viewModel.homeWorkspace.personalPages().size > 1)
                check(viewModel.settings.applyHome(originalHome.copy(showPageIndicator = true), viewModel.homeWorkspace))
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
                workspaceStore.saveWorkspaceDocument(originalWorkspace)
                viewModel.homeWorkspace.reload()
                viewModel.settings.applyHome(originalHome, viewModel.homeWorkspace)
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
