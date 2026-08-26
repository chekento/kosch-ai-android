package cloud.kosch.aiandroid.security.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class N2EngineStateMachineTest {
    private val ready = N2ActivationGateDecision.Ready(
        implementationId = "forwarder-test",
        evidenceRevision = "evidence-1",
    )

    @Test
    fun blockedStart_remainsInactiveAndRecordsReason() {
        val result = N2EngineStateMachine.reduce(
            N2EngineSnapshot(),
            N2EngineEvent.RequestStart(
                N2ActivationGateDecision.Blocked(N2ActivationBlockReason.NO_BLACK_HOLE_UNVERIFIED),
            ),
        )

        assertEquals(N2EnginePhase.INACTIVE, result.phase)
        assertEquals(N2ActivationBlockReason.NO_BLACK_HOLE_UNVERIFIED, result.lastBlockReason)
    }

    @Test
    fun active_isReachableOnlyThroughMatchingStartingGeneration() {
        val starting = N2EngineStateMachine.reduce(
            N2EngineSnapshot(),
            N2EngineEvent.RequestStart(ready),
        )
        assertEquals(N2EnginePhase.STARTING, starting.phase)
        assertEquals(1L, starting.activationGeneration)

        val active = N2EngineStateMachine.reduce(
            starting,
            N2EngineEvent.ForwarderStarted(starting.activationGeneration),
        )

        assertEquals(N2EnginePhase.ACTIVE, active.phase)
        assertNull(active.stopReason)
    }

    @Test
    fun staleForwarderStarted_failsSafeInsteadOfActivating() {
        val starting = N2EngineStateMachine.reduce(
            N2EngineSnapshot(activationGeneration = 6),
            N2EngineEvent.RequestStart(ready),
        )

        val result = N2EngineStateMachine.reduce(
            starting,
            N2EngineEvent.ForwarderStarted(6),
        )

        assertEquals(N2EnginePhase.FAILED_SAFE, result.phase)
        assertEquals(N2StopReason.UNEXPECTED_EVENT, result.stopReason)
    }

    @Test
    fun vpnRevocation_fromActive_requiresCleanup() {
        val active = activeSnapshot()

        val stopping = N2EngineStateMachine.reduce(active, N2EngineEvent.VpnAuthorizationRevoked)
        assertEquals(N2EnginePhase.STOPPING, stopping.phase)
        assertEquals(N2StopReason.VPN_AUTHORIZATION_REVOKED, stopping.stopReason)

        val inactive = N2EngineStateMachine.reduce(stopping, N2EngineEvent.CleanupComplete)
        assertEquals(N2EnginePhase.INACTIVE, inactive.phase)
        assertEquals(active.activationGeneration, inactive.activationGeneration)
        assertNull(inactive.implementationId)
    }

    @Test
    fun networkChange_andForwarderFault_neverRemainActive() {
        val active = activeSnapshot()

        val networkChanged = N2EngineStateMachine.reduce(active, N2EngineEvent.NetworkChanged)
        val forwarderFault = N2EngineStateMachine.reduce(active, N2EngineEvent.ForwarderFault)

        assertEquals(N2EnginePhase.STOPPING, networkChanged.phase)
        assertEquals(N2StopReason.NETWORK_CHANGED, networkChanged.stopReason)
        assertEquals(N2EnginePhase.STOPPING, forwarderFault.phase)
        assertEquals(N2StopReason.FORWARDER_FAULT, forwarderFault.stopReason)
    }

    @Test
    fun startFailure_entersFailedSafeUntilCleanupCompletes() {
        val starting = N2EngineStateMachine.reduce(
            N2EngineSnapshot(),
            N2EngineEvent.RequestStart(ready),
        )

        val failed = N2EngineStateMachine.reduce(starting, N2EngineEvent.StartFailed)
        assertEquals(N2EnginePhase.FAILED_SAFE, failed.phase)
        assertEquals(N2StopReason.START_FAILED, failed.stopReason)

        val inactive = N2EngineStateMachine.reduce(failed, N2EngineEvent.CleanupComplete)
        assertEquals(N2EnginePhase.INACTIVE, inactive.phase)
        assertEquals(starting.activationGeneration, inactive.activationGeneration)
    }

    @Test
    fun duplicateStartWhileActive_failsSafe() {
        val result = N2EngineStateMachine.reduce(
            activeSnapshot(),
            N2EngineEvent.RequestStart(ready),
        )

        assertEquals(N2EnginePhase.FAILED_SAFE, result.phase)
        assertEquals(N2StopReason.UNEXPECTED_EVENT, result.stopReason)
    }

    @Test
    fun generationWrap_neverUsesZero() {
        val starting = N2EngineStateMachine.reduce(
            N2EngineSnapshot(activationGeneration = Long.MAX_VALUE),
            N2EngineEvent.RequestStart(ready),
        )

        assertEquals(1L, starting.activationGeneration)
    }

    private fun activeSnapshot(): N2EngineSnapshot {
        val starting = N2EngineStateMachine.reduce(
            N2EngineSnapshot(),
            N2EngineEvent.RequestStart(ready),
        )
        return N2EngineStateMachine.reduce(
            starting,
            N2EngineEvent.ForwarderStarted(starting.activationGeneration),
        )
    }
}
