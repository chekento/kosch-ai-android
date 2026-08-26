package cloud.kosch.aiandroid.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.security.NetworkSecurityN1Policy
import cloud.kosch.aiandroid.security.VpnAuthorizationState
import cloud.kosch.aiandroid.ui.theme.KoSchLauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityNetworkCenterSurfaceInstrumentationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun consentRequired_rendersTrafficNeutralContract_andOnlyRequestsAndroidConsent() {
        var consentRequests = 0

        composeTestRule.setContent {
            KoSchLauncherTheme {
                Column {
                    SecurityNetworkCenterSurface(
                        snapshot = NetworkSecurityN1Policy.snapshot(VpnAuthorizationState.CONSENT_REQUIRED),
                        onRequestVpnConsent = { consentRequests += 1 },
                        onOpenFaq = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Autorisierung ≠ aktives VPN").assertExists()
        composeTestRule.onNodeWithText("Android-Freigabe erforderlich").assertExists()
        composeTestRule.onAllNodesWithText("AUS").assertCountEquals(2)
        composeTestRule.onNodeWithText("DIREKT").assertExists()
        composeTestRule.onNodeWithText("Live-Flows 0 · erlaubt 0 · blockiert 0").assertExists()
        composeTestRule.onNodeWithText("Upload 0 B · Download 0 B").assertExists()

        composeTestRule.onNodeWithText("Android-VPN-Freigabe öffnen").performClick()
        composeTestRule.runOnIdle { assertEquals(1, consentRequests) }
    }

    @Test
    fun authorized_isPresentedAsN2Ready_butNeverAsActiveVpn() {
        composeTestRule.setContent {
            KoSchLauncherTheme {
                Column {
                    SecurityNetworkCenterSurface(
                        snapshot = NetworkSecurityN1Policy.snapshot(VpnAuthorizationState.AUTHORIZED),
                        onRequestVpnConsent = {},
                        onOpenFaq = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Von Android autorisiert").assertExists()
        composeTestRule.onNodeWithText("N2 BEREIT").assertExists()
        composeTestRule.onNodeWithText("Autorisierung ≠ aktives VPN").assertExists()
        composeTestRule.onNodeWithText("Android-Freigabe erneut prüfen").assertExists()
        composeTestRule.onAllNodesWithText("AUS").assertCountEquals(2)
        composeTestRule.onNodeWithText("DIREKT").assertExists()
    }
}
