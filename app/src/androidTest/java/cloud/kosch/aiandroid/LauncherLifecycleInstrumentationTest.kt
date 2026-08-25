package cloud.kosch.aiandroid

import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherLifecycleInstrumentationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun coldLaunch_rendersComposeShell_andReachesResumed() {
        assertEquals(Lifecycle.State.RESUMED, composeTestRule.activityRule.scenario.state)
        composeTestRule.waitForIdle()
        assertComposeShellPresent()
    }

    @Test
    fun activityRecreation_restoresComposeShell_andReturnsToResumed() {
        composeTestRule.waitForIdle()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()

        assertEquals(Lifecycle.State.RESUMED, composeTestRule.activityRule.scenario.state)
        assertComposeShellPresent()
    }

    private fun assertComposeShellPresent() {
        val roots = composeTestRule
            .onAllNodes(isRoot(), useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("Expected at least one Compose semantics root", roots.isNotEmpty())
    }
}
