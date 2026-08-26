package cloud.kosch.aiandroid.security

import org.junit.Assert.assertEquals
import org.junit.Test

class VpnConsentDecisionPolicyTest {
    @Test
    fun `authorized state never opens the system dialog`() {
        val action = VpnConsentDecisionPolicy.actionFor(
            VpnConsentInspection(
                authorization = VpnAuthorizationState.AUTHORIZED,
                conflict = VpnConflictState.NONE_DETECTED,
            ),
        )

        assertEquals(VpnConsentAction.NO_SYSTEM_DIALOG, action)
    }

    @Test
    fun `consent required without conflict may open system dialog`() {
        val action = VpnConsentDecisionPolicy.actionFor(
            VpnConsentInspection(
                authorization = VpnAuthorizationState.CONSENT_REQUIRED,
                conflict = VpnConflictState.NONE_DETECTED,
            ),
        )

        assertEquals(VpnConsentAction.OPEN_SYSTEM_DIALOG, action)
    }

    @Test
    fun `active vpn always requires a second acknowledgement`() {
        val action = VpnConsentDecisionPolicy.actionFor(
            VpnConsentInspection(
                authorization = VpnAuthorizationState.CONSENT_REQUIRED,
                conflict = VpnConflictState.ACTIVE_VPN_DETECTED,
            ),
        )

        assertEquals(VpnConsentAction.REQUIRE_CONFLICT_ACKNOWLEDGEMENT, action)
    }

    @Test
    fun `unknown vpn conflict status fails conservatively`() {
        val action = VpnConsentDecisionPolicy.actionFor(
            VpnConsentInspection(
                authorization = VpnAuthorizationState.UNKNOWN,
                conflict = VpnConflictState.UNKNOWN,
            ),
        )

        assertEquals(VpnConsentAction.REQUIRE_CONFLICT_ACKNOWLEDGEMENT, action)
    }
}
