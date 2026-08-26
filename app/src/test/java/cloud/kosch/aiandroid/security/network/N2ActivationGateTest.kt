package cloud.kosch.aiandroid.security.network

import cloud.kosch.aiandroid.security.VpnAuthorizationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class N2ActivationGateTest {
    private val completeEvidence = ForwarderReadinessEvidence(
        implementationId = "forwarder-test",
        evidenceRevision = "api36-oem-gate-1",
        supportsIpv4 = true,
        supportsIpv6 = true,
        protectsUpstreamSockets = true,
        returnPathVerified = true,
        stopOnFaultVerified = true,
        noBlackHoleVerified = true,
    )

    @Test
    fun completeEvidence_allowsAttemptOnlyAfterVpnAuthorization() {
        val ready = N2ActivationGate.evaluate(
            vpnAuthorization = VpnAuthorizationState.AUTHORIZED,
            networkAvailable = true,
            forwarderEvidence = completeEvidence,
        )

        assertEquals(
            N2ActivationGateDecision.Ready("forwarder-test", "api36-oem-gate-1"),
            ready,
        )
    }

    @Test
    fun missingAuthorization_blocksBeforeForwarderEvidence() {
        val blocked = N2ActivationGate.evaluate(
            vpnAuthorization = VpnAuthorizationState.CONSENT_REQUIRED,
            networkAvailable = true,
            forwarderEvidence = completeEvidence,
        )

        assertEquals(
            N2ActivationGateDecision.Blocked(N2ActivationBlockReason.VPN_AUTHORIZATION_MISSING),
            blocked,
        )
    }

    @Test
    fun missingNetwork_blocksActivation() {
        val blocked = N2ActivationGate.evaluate(
            vpnAuthorization = VpnAuthorizationState.AUTHORIZED,
            networkAvailable = false,
            forwarderEvidence = completeEvidence,
        )

        assertEquals(
            N2ActivationGateDecision.Blocked(N2ActivationBlockReason.NETWORK_UNAVAILABLE),
            blocked,
        )
    }

    @Test
    fun missingForwarderEvidence_blocksActivation() {
        val blocked = N2ActivationGate.evaluate(
            vpnAuthorization = VpnAuthorizationState.AUTHORIZED,
            networkAvailable = true,
            forwarderEvidence = null,
        )

        assertEquals(
            N2ActivationGateDecision.Blocked(N2ActivationBlockReason.FORWARDER_EVIDENCE_MISSING),
            blocked,
        )
    }

    @Test
    fun everySafetyProperty_isIndividuallyRequired() {
        val cases = listOf(
            completeEvidence.copy(supportsIpv4 = false) to N2ActivationBlockReason.IPV4_UNVERIFIED,
            completeEvidence.copy(supportsIpv6 = false) to N2ActivationBlockReason.IPV6_UNVERIFIED,
            completeEvidence.copy(protectsUpstreamSockets = false) to N2ActivationBlockReason.UPSTREAM_SOCKET_PROTECTION_UNVERIFIED,
            completeEvidence.copy(returnPathVerified = false) to N2ActivationBlockReason.RETURN_PATH_UNVERIFIED,
            completeEvidence.copy(stopOnFaultVerified = false) to N2ActivationBlockReason.STOP_ON_FAULT_UNVERIFIED,
            completeEvidence.copy(noBlackHoleVerified = false) to N2ActivationBlockReason.NO_BLACK_HOLE_UNVERIFIED,
        )

        cases.forEach { (evidence, reason) ->
            val result = N2ActivationGate.evaluate(
                vpnAuthorization = VpnAuthorizationState.AUTHORIZED,
                networkAvailable = true,
                forwarderEvidence = evidence,
            )
            assertEquals(N2ActivationGateDecision.Blocked(reason), result)
        }
    }

    @Test
    fun evidenceIdentifiers_rejectBlankValues() {
        val implementationFailure = runCatching { completeEvidence.copy(implementationId = " ") }.exceptionOrNull()
        val revisionFailure = runCatching { completeEvidence.copy(evidenceRevision = "") }.exceptionOrNull()

        assertTrue(implementationFailure is IllegalArgumentException)
        assertTrue(revisionFailure is IllegalArgumentException)
    }
}
