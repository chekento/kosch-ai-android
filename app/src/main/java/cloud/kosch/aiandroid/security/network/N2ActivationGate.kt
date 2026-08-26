package cloud.kosch.aiandroid.security.network

import cloud.kosch.aiandroid.security.VpnAuthorizationState

/**
 * Evidence required before N2 may even attempt to create an active packet path.
 *
 * This is deliberately stricter than "a forwarder library exists". The evidence records the
 * properties that protect users from a partially established local VPN that silently black-holes
 * traffic or loops its own upstream sockets back into the tunnel.
 */
data class ForwarderReadinessEvidence(
    val implementationId: String,
    val evidenceRevision: String,
    val supportsIpv4: Boolean,
    val supportsIpv6: Boolean,
    val protectsUpstreamSockets: Boolean,
    val returnPathVerified: Boolean,
    val stopOnFaultVerified: Boolean,
    val noBlackHoleVerified: Boolean,
) {
    init {
        require(implementationId.isNotBlank() && implementationId.length <= 128) {
            "Forwarder implementation id is invalid"
        }
        require(evidenceRevision.isNotBlank() && evidenceRevision.length <= 128) {
            "Forwarder evidence revision is invalid"
        }
    }
}

enum class N2ActivationBlockReason {
    VPN_AUTHORIZATION_MISSING,
    NETWORK_UNAVAILABLE,
    FORWARDER_EVIDENCE_MISSING,
    IPV4_UNVERIFIED,
    IPV6_UNVERIFIED,
    UPSTREAM_SOCKET_PROTECTION_UNVERIFIED,
    RETURN_PATH_UNVERIFIED,
    STOP_ON_FAULT_UNVERIFIED,
    NO_BLACK_HOLE_UNVERIFIED,
}

sealed interface N2ActivationGateDecision {
    data class Ready(
        val implementationId: String,
        val evidenceRevision: String,
    ) : N2ActivationGateDecision

    data class Blocked(val reason: N2ActivationBlockReason) : N2ActivationGateDecision
}

/** Pure policy. It has no Android or networking side effects. */
object N2ActivationGate {
    fun evaluate(
        vpnAuthorization: VpnAuthorizationState,
        networkAvailable: Boolean,
        forwarderEvidence: ForwarderReadinessEvidence?,
    ): N2ActivationGateDecision {
        if (vpnAuthorization != VpnAuthorizationState.AUTHORIZED) {
            return N2ActivationGateDecision.Blocked(N2ActivationBlockReason.VPN_AUTHORIZATION_MISSING)
        }
        if (!networkAvailable) {
            return N2ActivationGateDecision.Blocked(N2ActivationBlockReason.NETWORK_UNAVAILABLE)
        }
        val evidence = forwarderEvidence
            ?: return N2ActivationGateDecision.Blocked(N2ActivationBlockReason.FORWARDER_EVIDENCE_MISSING)

        return when {
            !evidence.supportsIpv4 -> N2ActivationGateDecision.Blocked(N2ActivationBlockReason.IPV4_UNVERIFIED)
            !evidence.supportsIpv6 -> N2ActivationGateDecision.Blocked(N2ActivationBlockReason.IPV6_UNVERIFIED)
            !evidence.protectsUpstreamSockets -> N2ActivationGateDecision.Blocked(
                N2ActivationBlockReason.UPSTREAM_SOCKET_PROTECTION_UNVERIFIED,
            )
            !evidence.returnPathVerified -> N2ActivationGateDecision.Blocked(
                N2ActivationBlockReason.RETURN_PATH_UNVERIFIED,
            )
            !evidence.stopOnFaultVerified -> N2ActivationGateDecision.Blocked(
                N2ActivationBlockReason.STOP_ON_FAULT_UNVERIFIED,
            )
            !evidence.noBlackHoleVerified -> N2ActivationGateDecision.Blocked(
                N2ActivationBlockReason.NO_BLACK_HOLE_UNVERIFIED,
            )
            else -> N2ActivationGateDecision.Ready(
                implementationId = evidence.implementationId,
                evidenceRevision = evidence.evidenceRevision,
            )
        }
    }
}
