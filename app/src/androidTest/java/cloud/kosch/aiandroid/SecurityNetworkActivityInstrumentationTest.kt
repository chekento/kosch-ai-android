package cloud.kosch.aiandroid

import android.content.ComponentName
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityNetworkActivityInstrumentationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SecurityNetworkActivity>()

    @Test
    fun securityActivity_rendersTrafficNeutralContract_andSurvivesRecreation() {
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Security & Network").fetchSemanticsNode()
        composeTestRule.onNodeWithText("Autorisierung ≠ aktives VPN").fetchSemanticsNode()
        composeTestRule.onNodeWithText("N1-Datenbilanz").fetchSemanticsNode()

        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Security & Network").fetchSemanticsNode()
        composeTestRule.onNodeWithText("Autorisierung ≠ aktives VPN").fetchSemanticsNode()
    }

    @Test
    fun help_disclosesN1PrivacyBoundary() {
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Datenschutz, VPN-Konflikte & nächste Stufen")
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Security & Network · N1").fetchSemanticsNode()
        composeTestRule.onNodeWithText("Verstanden").fetchSemanticsNode()
    }

    @Test
    fun backControl_isAlwaysDiscoverable() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Zurück zum Launcher").fetchSemanticsNode()
    }

    @Test
    fun securityActivity_isNotExported() {
        val activity = composeTestRule.activity
        val info = activity.packageManager.getActivityInfo(
            ComponentName(activity, SecurityNetworkActivity::class.java),
            0,
        )

        assertFalse("SecurityNetworkActivity must remain internal-only", info.exported)
    }
}
