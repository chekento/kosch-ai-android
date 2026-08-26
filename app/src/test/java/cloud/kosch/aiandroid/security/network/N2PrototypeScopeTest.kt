package cloud.kosch.aiandroid.security.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class N2PrototypeScopeTest {
    private val tun2socks = ForwarderCandidateEvaluation.evaluate(
        ForwarderCandidateCatalog.TUN2SOCKS_2_7_0,
        ForwarderUseCase.N2_DIRECT,
    )

    @Test
    fun pinnedOfflinePackagingPrototype_isPermitted() {
        val decision = N2PrototypeScope.evaluate(
            safePlan(nativePackagingExperiment = true),
        )

        assertTrue(decision.permitted)
        assertTrue(decision.blockers.isEmpty())
    }

    @Test
    fun prototypeCannotUseActivationEligibilityAsShortcut() {
        assertTrue(tun2socks.prototypeEligible)
        assertFalse(tun2socks.activationEligible)

        val decision = N2PrototypeScope.evaluate(safePlan())
        assertTrue(decision.permitted)
    }

    @Test
    fun manifestVpnSocketConsentAndPayloadChanges_areIndividuallyForbidden() {
        val unsafePlans = listOf(
            safePlan().copy(changesManifestPermissions = true) to N2PrototypeBlocker.MANIFEST_PERMISSION_CHANGE_FORBIDDEN,
            safePlan().copy(callsVpnEstablish = true) to N2PrototypeBlocker.VPN_ESTABLISH_FORBIDDEN,
            safePlan().copy(opensNetworkSockets = true) to N2PrototypeBlocker.NETWORK_SOCKET_FORBIDDEN,
            safePlan().copy(touchesConsentVpnService = true) to N2PrototypeBlocker.CONSENT_SERVICE_MUTATION_FORBIDDEN,
            safePlan().copy(capturesPayload = true) to N2PrototypeBlocker.PAYLOAD_CAPTURE_FORBIDDEN,
        )

        unsafePlans.forEach { (plan, expected) ->
            val decision = N2PrototypeScope.evaluate(plan)
            assertFalse(decision.permitted)
            assertTrue(decision.blockers.contains(expected))
        }
    }

    @Test
    fun unpinnedOrNonReproducibleBuild_isRejected() {
        val unpinned = N2PrototypeScope.evaluate(safePlan().copy(sourceRevisionPinned = false))
        val nonReproducible = N2PrototypeScope.evaluate(safePlan().copy(reproducibleBuildPlan = false))

        assertTrue(unpinned.blockers.contains(N2PrototypeBlocker.SOURCE_REVISION_NOT_PINNED))
        assertTrue(nonReproducible.blockers.contains(N2PrototypeBlocker.BUILD_NOT_REPRODUCIBLE))
    }

    @Test
    fun wrongUseCaseCandidate_cannotEnterN2Prototype() {
        val hevForN2 = ForwarderCandidateEvaluation.evaluate(
            ForwarderCandidateCatalog.HEV_SOCKS5_2_17_1,
            ForwarderUseCase.N2_DIRECT,
        )
        val decision = N2PrototypeScope.evaluate(safePlan().copy(candidate = hevForN2))

        assertFalse(decision.permitted)
        assertTrue(decision.blockers.contains(N2PrototypeBlocker.CANDIDATE_NOT_PROTOTYPE_ELIGIBLE))
    }

    private fun safePlan(nativePackagingExperiment: Boolean = false) = N2PrototypePlan(
        candidate = tun2socks,
        sourceRevisionPinned = true,
        reproducibleBuildPlan = true,
        nativePackagingExperiment = nativePackagingExperiment,
        changesManifestPermissions = false,
        callsVpnEstablish = false,
        opensNetworkSockets = false,
        touchesConsentVpnService = false,
        capturesPayload = false,
    )
}
