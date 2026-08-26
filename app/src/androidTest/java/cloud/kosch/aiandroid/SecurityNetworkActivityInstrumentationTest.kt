package cloud.kosch.aiandroid

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
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

        composeTestRule.onNodeWithText("Security & Network").assertExists()
        composeTestRule.onNodeWithText("Autorisierung ≠ aktives VPN").assertExists()
        composeTestRule.onNodeWithText("N1-Datenbilanz").assertExists()

        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Security & Network").assertExists()
        composeTestRule.onNodeWithText("Autorisierung ≠ aktives VPN").assertExists()
    }

    @Test
    fun help_disclosesN1PrivacyBoundary() {
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Datenschutz, VPN-Konflikte & nächste Stufen")
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Security & Network · N1").assertExists()
        composeTestRule.onNodeWithText("Verstanden").assertExists()
    }

    @Test
    fun backControl_isAlwaysDiscoverable() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Zurück zum Launcher").assertExists()
    }
}
