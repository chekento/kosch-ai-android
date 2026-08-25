package cloud.kosch.aiandroid

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
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
        composeTestRule.onRoot(useUnmergedTree = true).fetchSemanticsNode()
    }

    @Test
    fun activityRecreation_restoresComposeShell_andReturnsToResumed() {
        composeTestRule.waitForIdle()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()

        assertEquals(Lifecycle.State.RESUMED, composeTestRule.activityRule.scenario.state)
        composeTestRule.onRoot(useUnmergedTree = true).fetchSemanticsNode()
    }
}
