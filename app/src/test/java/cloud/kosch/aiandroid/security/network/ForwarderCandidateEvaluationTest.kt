package cloud.kosch.aiandroid.security.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForwarderCandidateEvaluationTest {
    @Test
    fun tun2socks_isN2SourceCandidate_butCannotActivateWithoutAndroidEvidence() {
        val result = ForwarderCandidateEvaluation.evaluate(
            ForwarderCandidateCatalog.TUN2SOCKS_2_7_0,
            ForwarderUseCase.N2_DIRECT,
        )

        assertFalse(result.activationEligible)
        assertFalse(result.blockers.contains(ForwarderPreflightBlocker.USE_CASE_MISMATCH))
        assertFalse(result.blockers.contains(ForwarderPreflightBlocker.LICENSE_NOT_APPROVED))
        assertFalse(result.blockers.contains(ForwarderPreflightBlocker.TUN_FD_INTAKE_UNVERIFIED))
        assertFalse(result.blockers.contains(ForwarderPreflightBlocker.DIRECT_EGRESS_UNAVAILABLE))
        assertTrue(result.blockers.contains(ForwarderPreflightBlocker.ANDROID_EMBEDDING_UNVERIFIED))
        assertTrue(result.blockers.contains(ForwarderPreflightBlocker.VPN_PROTECT_UNVERIFIED))
        assertTrue(result.blockers.contains(ForwarderPreflightBlocker.STOP_API_UNVERIFIED))
        assertTrue(result.blockers.contains(ForwarderPreflightBlocker.NO_BLACK_HOLE_EVIDENCE_MISSING))
        assertTrue(result.blockers.contains(ForwarderPreflightBlocker.PHYSICAL_DEVICE_EVIDENCE_MISSING))
    }

    @Test
    fun hev_isNotAcceptedAsN2DirectForwarder() {
        val result = ForwarderCandidateEvaluation.evaluate(
            ForwarderCandidateCatalog.HEV_SOCKS5_2_17_1,
            ForwarderUseCase.N2_DIRECT,
        )

        assertFalse(result.activationEligible)
        assertTrue(result.blockers.contains(ForwarderPreflightBlocker.USE_CASE_MISMATCH))
        assertTrue(result.blockers.contains(ForwarderPreflightBlocker.DIRECT_EGRESS_UNAVAILABLE))
    }

    @Test
    fun hev_matchesN4Role_butStillNeedsProtectAndDeviceEvidence() {
        val result = ForwarderCandidateEvaluation.evaluate(
            ForwarderCandidateCatalog.HEV_SOCKS5_2_17_1,
            ForwarderUseCase.N4_PROXY,
        )

        assertFalse(result.activationEligible)
        assertFalse(result.blockers.contains(ForwarderPreflightBlocker.USE_CASE_MISMATCH))
        assertFalse(result.blockers.contains(ForwarderPreflightBlocker.ANDROID_EMBEDDING_UNVERIFIED))
        assertFalse(result.blockers.contains(ForwarderPreflightBlocker.STOP_API_UNVERIFIED))
        assertTrue(result.blockers.contains(ForwarderPreflightBlocker.VPN_PROTECT_UNVERIFIED))
        assertTrue(result.blockers.contains(ForwarderPreflightBlocker.NO_BLACK_HOLE_EVIDENCE_MISSING))
        assertTrue(result.blockers.contains(ForwarderPreflightBlocker.PHYSICAL_DEVICE_EVIDENCE_MISSING))
    }

    @Test
    fun completeProfile_isEligibleOnlyWhenEverySafetyInvariantIsVerified() {
        val candidate = ForwarderCandidateProfile(
            id = "test/direct-forwarder",
            pinnedVersion = "1.0.0",
            licenseSpdx = "MIT",
            intendedUseCase = ForwarderUseCase.N2_DIRECT,
            acceptsTunFileDescriptor = true,
            supportsDirectEgress = true,
            supportsIpv4 = true,
            supportsIpv6 = true,
            supportsTcp = true,
            supportsUdp = true,
            androidEmbeddingPathVerified = true,
            socketEscapeStrategy = SocketEscapeStrategy.VPN_PROTECT,
            deterministicStopApiVerified = true,
            noBlackHoleEvidenceVerified = true,
            physicalDeviceEvidenceVerified = true,
        )

        val result = ForwarderCandidateEvaluation.evaluate(candidate, ForwarderUseCase.N2_DIRECT)

        assertTrue(result.activationEligible)
        assertTrue(result.blockers.isEmpty())
    }

    @Test
    fun permissiveLicenseDoesNotOverrideMissingNetworkEvidence() {
        val result = ForwarderCandidateEvaluation.evaluate(
            ForwarderCandidateCatalog.TUN2SOCKS_2_7_0.copy(
                androidEmbeddingPathVerified = true,
                socketEscapeStrategy = SocketEscapeStrategy.VPN_PROTECT,
                deterministicStopApiVerified = true,
            ),
            ForwarderUseCase.N2_DIRECT,
        )

        assertFalse(result.activationEligible)
        assertTrue(result.blockers.contains(ForwarderPreflightBlocker.NO_BLACK_HOLE_EVIDENCE_MISSING))
        assertTrue(result.blockers.contains(ForwarderPreflightBlocker.PHYSICAL_DEVICE_EVIDENCE_MISSING))
    }
}
