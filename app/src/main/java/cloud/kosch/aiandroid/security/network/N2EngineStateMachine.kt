package cloud.kosch.aiandroid.security.network

enum class N2EnginePhase {
    INACTIVE,
    STARTING,
    ACTIVE,
    STOPPING,
    FAILED_SAFE,
}

enum class N2StopReason {
    USER,
    VPN_AUTHORIZATION_REVOKED,
    NETWORK_CHANGED,
    FORWARDER_FAULT,
    START_FAILED,
    UNEXPECTED_EVENT,
}

data class N2EngineSnapshot(
    val phase: N2EnginePhase = N2EnginePhase.INACTIVE,
    val activationGeneration: Long = 0,
    val implementationId: String? = null,
    val evidenceRevision: String? = null,
    val stopReason: N2StopReason? = null,
    val lastBlockReason: N2ActivationBlockReason? = null,
) {
    init {
        require(activationGeneration >= 0) { "Activation generation must not be negative" }
        if (phase == N2EnginePhase.STARTING || phase == N2EnginePhase.ACTIVE || phase == N2EnginePhase.STOPPING) {
            require(!implementationId.isNullOrBlank()) { "Active-path phases require a forwarder id" }
            require(!evidenceRevision.isNullOrBlank()) { "Active-path phases require evidence revision" }
        }
        if (phase == N2EnginePhase.ACTIVE) {
            require(stopReason == null) { "Active state cannot already carry a stop reason" }
        }
    }
}

sealed interface N2EngineEvent {
    data class RequestStart(val gateDecision: N2ActivationGateDecision) : N2EngineEvent
    data class ForwarderStarted(val activationGeneration: Long) : N2EngineEvent
    data object StartFailed : N2EngineEvent
    data object UserStop : N2EngineEvent
    data object VpnAuthorizationRevoked : N2EngineEvent
    data object NetworkChanged : N2EngineEvent
    data object ForwarderFault : N2EngineEvent
    data object CleanupComplete : N2EngineEvent
}

/**
 * Pure fail-safe state machine for future N2 activation.
 *
 * It never starts a VPN or a forwarder itself. The only route into ACTIVE is:
 * INACTIVE -> RequestStart(Ready) -> STARTING -> ForwarderStarted(same generation) -> ACTIVE.
 * Any revocation, network change or forwarder fault leaves ACTIVE immediately and requires cleanup.
 */
object N2EngineStateMachine {
    fun reduce(current: N2EngineSnapshot, event: N2EngineEvent): N2EngineSnapshot = when (event) {
        is N2EngineEvent.RequestStart -> onRequestStart(current, event.gateDecision)
        is N2EngineEvent.ForwarderStarted -> onForwarderStarted(current, event.activationGeneration)
        N2EngineEvent.StartFailed -> onFailure(current, N2StopReason.START_FAILED)
        N2EngineEvent.UserStop -> requestStop(current, N2StopReason.USER)
        N2EngineEvent.VpnAuthorizationRevoked -> requestStop(current, N2StopReason.VPN_AUTHORIZATION_REVOKED)
        N2EngineEvent.NetworkChanged -> requestStop(current, N2StopReason.NETWORK_CHANGED)
        N2EngineEvent.ForwarderFault -> requestStop(current, N2StopReason.FORWARDER_FAULT)
        N2EngineEvent.CleanupComplete -> onCleanupComplete(current)
    }

    private fun onRequestStart(
        current: N2EngineSnapshot,
        decision: N2ActivationGateDecision,
    ): N2EngineSnapshot {
        if (current.phase != N2EnginePhase.INACTIVE) {
            return failSafe(current, N2StopReason.UNEXPECTED_EVENT)
        }
        return when (decision) {
            is N2ActivationGateDecision.Blocked -> current.copy(
                lastBlockReason = decision.reason,
                stopReason = null,
            )

            is N2ActivationGateDecision.Ready -> current.copy(
                phase = N2EnginePhase.STARTING,
                activationGeneration = nextGeneration(current.activationGeneration),
                implementationId = decision.implementationId,
                evidenceRevision = decision.evidenceRevision,
                stopReason = null,
                lastBlockReason = null,
            )
        }
    }

    private fun onForwarderStarted(current: N2EngineSnapshot, generation: Long): N2EngineSnapshot {
        if (current.phase != N2EnginePhase.STARTING || generation != current.activationGeneration) {
            return failSafe(current, N2StopReason.UNEXPECTED_EVENT)
        }
        return current.copy(phase = N2EnginePhase.ACTIVE, stopReason = null)
    }

    private fun onFailure(current: N2EngineSnapshot, reason: N2StopReason): N2EngineSnapshot = when (current.phase) {
        N2EnginePhase.STARTING,
        N2EnginePhase.ACTIVE,
        N2EnginePhase.STOPPING,
        -> failSafe(current, reason)

        N2EnginePhase.INACTIVE,
        N2EnginePhase.FAILED_SAFE,
        -> current.copy(phase = N2EnginePhase.FAILED_SAFE, stopReason = reason)
    }

    private fun requestStop(current: N2EngineSnapshot, reason: N2StopReason): N2EngineSnapshot = when (current.phase) {
        N2EnginePhase.STARTING,
        N2EnginePhase.ACTIVE,
        -> current.copy(phase = N2EnginePhase.STOPPING, stopReason = reason)

        N2EnginePhase.STOPPING -> current
        N2EnginePhase.FAILED_SAFE -> current
        N2EnginePhase.INACTIVE -> current.copy(stopReason = reason)
    }

    private fun onCleanupComplete(current: N2EngineSnapshot): N2EngineSnapshot = when (current.phase) {
        N2EnginePhase.STOPPING,
        N2EnginePhase.FAILED_SAFE,
        -> N2EngineSnapshot(activationGeneration = current.activationGeneration)

        N2EnginePhase.INACTIVE -> current
        N2EnginePhase.STARTING,
        N2EnginePhase.ACTIVE,
        -> failSafe(current, N2StopReason.UNEXPECTED_EVENT)
    }

    private fun failSafe(current: N2EngineSnapshot, reason: N2StopReason) = current.copy(
        phase = N2EnginePhase.FAILED_SAFE,
        stopReason = reason,
    )

    private fun nextGeneration(current: Long): Long =
        if (current == Long.MAX_VALUE) 1L else current + 1L
}
