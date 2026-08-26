package cloud.kosch.aiandroid.security.network

data class N2PrototypePlan(
    val candidate: ForwarderPreflightResult,
    val sourceRevisionPinned: Boolean,
    val reproducibleBuildPlan: Boolean,
    val nativePackagingExperiment: Boolean,
    val changesManifestPermissions: Boolean,
    val callsVpnEstablish: Boolean,
    val opensNetworkSockets: Boolean,
    val touchesConsentVpnService: Boolean,
    val capturesPayload: Boolean,
)

enum class N2PrototypeBlocker {
    CANDIDATE_NOT_PROTOTYPE_ELIGIBLE,
    SOURCE_REVISION_NOT_PINNED,
    BUILD_NOT_REPRODUCIBLE,
    MANIFEST_PERMISSION_CHANGE_FORBIDDEN,
    VPN_ESTABLISH_FORBIDDEN,
    NETWORK_SOCKET_FORBIDDEN,
    CONSENT_SERVICE_MUTATION_FORBIDDEN,
    PAYLOAD_CAPTURE_FORBIDDEN,
}

data class N2PrototypeDecision(val blockers: List<N2PrototypeBlocker>) {
    val permitted: Boolean get() = blockers.isEmpty()
}

/**
 * Keeps the first native/embedding experiment physically separated from active VPN behavior.
 *
 * A permitted prototype may compile/package a pinned native artifact and exercise its local API,
 * but it may not change permissions, establish a VPN, open network sockets, modify the N1 consent
 * service or inspect packet payloads.
 */
object N2PrototypeScope {
    fun evaluate(plan: N2PrototypePlan): N2PrototypeDecision = N2PrototypeDecision(
        blockers = buildList {
            if (!plan.candidate.prototypeEligible) add(N2PrototypeBlocker.CANDIDATE_NOT_PROTOTYPE_ELIGIBLE)
            if (!plan.sourceRevisionPinned) add(N2PrototypeBlocker.SOURCE_REVISION_NOT_PINNED)
            if (!plan.reproducibleBuildPlan) add(N2PrototypeBlocker.BUILD_NOT_REPRODUCIBLE)
            if (plan.changesManifestPermissions) add(N2PrototypeBlocker.MANIFEST_PERMISSION_CHANGE_FORBIDDEN)
            if (plan.callsVpnEstablish) add(N2PrototypeBlocker.VPN_ESTABLISH_FORBIDDEN)
            if (plan.opensNetworkSockets) add(N2PrototypeBlocker.NETWORK_SOCKET_FORBIDDEN)
            if (plan.touchesConsentVpnService) add(N2PrototypeBlocker.CONSENT_SERVICE_MUTATION_FORBIDDEN)
            if (plan.capturesPayload) add(N2PrototypeBlocker.PAYLOAD_CAPTURE_FORBIDDEN)
        },
    )
}
