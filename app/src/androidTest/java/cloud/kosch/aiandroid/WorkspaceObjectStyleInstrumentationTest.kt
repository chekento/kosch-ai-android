package cloud.kosch.aiandroid

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.data.WorkspaceStore
import cloud.kosch.aiandroid.model.HomePage
import cloud.kosch.aiandroid.model.PortableSettingValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceObjectStyleInstrumentationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun objectVisibility_isLiveOnHome_persistsAcrossRecreation_andInheritRestoresIt() {
        composeTestRule.waitForIdle()
        dismissOnboardingIfVisible()

        val workspaceStore = WorkspaceStore(composeTestRule.activity.applicationContext)
        val originalWorkspace = workspaceStore.loadWorkspaceDocument()
        val initialViewModel = ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]
        val originalScopedPayload = initialViewModel.scopedSettings.exportPortable()
        val originalHomePage = initialViewModel.controller.homePage
        var itemId = ""

        try {
            composeTestRule.runOnUiThread {
                initialViewModel.homeWorkspace.createPage("Style Runtime Test")
                initialViewModel.homeWorkspace.addApp("test:cloud.kosch.style-missing")
                itemId = initialViewModel.homeWorkspace.activePage.items.single().id
                initialViewModel.controller.switchHomePage(HomePage.WORKSPACE)
            }
            composeTestRule.waitForIdle()

            assertContentDescriptionPresent("Nicht verfügbare App")

            composeTestRule.runOnUiThread {
                val saved = initialViewModel.scopedSettings.setObjectOverride(
                    itemId = itemId,
                    featureId = VISIBLE_FEATURE,
                    value = PortableSettingValue.Bool(false),
                )
                check(saved)
            }
            composeTestRule.waitForIdle()

            assertContentDescriptionAbsent("Nicht verfügbare App")

            composeTestRule.activityRule.scenario.recreate()
            composeTestRule.waitForIdle()

            val recreated = ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]
            assertEquals(
                PortableSettingValue.Bool(false),
                recreated.scopedSettings.document.objectOverride(itemId, VISIBLE_FEATURE),
            )
            assertContentDescriptionAbsent("Nicht verfügbare App")

            composeTestRule.runOnUiThread {
                val inherited = recreated.scopedSettings.setObjectOverride(
                    itemId = itemId,
                    featureId = VISIBLE_FEATURE,
                    value = null,
                )
                check(inherited)
            }
            composeTestRule.waitForIdle()

            assertNull(recreated.scopedSettings.document.objectOverride(itemId, VISIBLE_FEATURE))
            assertContentDescriptionPresent("Nicht verfügbare App")
        } finally {
            composeTestRule.runOnUiThread {
                val current = ViewModelProvider(composeTestRule.activity)[LauncherViewModel::class.java]
                workspaceStore.saveWorkspaceDocument(originalWorkspace)
                current.homeWorkspace.reload()
                current.scopedSettings.importPortable(originalScopedPayload, originalWorkspace).getOrThrow()
                current.controller.switchHomePage(originalHomePage)
            }
            composeTestRule.waitForIdle()
        }
    }

    private fun assertContentDescriptionPresent(value: String) {
        assertTrue(
            "Expected content description '$value' to exist",
            composeTestRule
                .onAllNodesWithContentDescription(value, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty(),
        )
    }

    private fun assertContentDescriptionAbsent(value: String) {
        assertTrue(
            "Expected content description '$value' to be absent",
            composeTestRule
                .onAllNodesWithContentDescription(value, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isEmpty(),
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

    private companion object {
        const val VISIBLE_FEATURE = "workspace.style.visible"
    }
}
