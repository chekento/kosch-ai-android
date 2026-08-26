package cloud.kosch.aiandroid.security.network

enum class ForwarderUseCase {
    N2_DIRECT,
    N4_PROXY,
}

enum class SocketEscapeStrategy {
    VPN_PROTECT,
    SELF_APP_EXCLUSION,
    UNVERIFIED,
}

data class ForwarderCandidateProfile(
    val id: String,
    val pinnedVersion: String,
    val licenseSpdx: String,
    val intendedUseCase: ForwarderUseCase,
    val acceptsTunFileDescriptor: Boolean,
    val supportsDirectEgress: Boolean,
    val supportsIpv4: Boolean,
    val supportsIpv6: Boolean,
    val supportsTcp: Boolean,
    val supportsUdp: Boolean,
    val androidEmbeddingPathVerified: Boolean,
    val socketEscapeStrategy: SocketEscapeStrategy,
    val deterministicStopApiVerified: Boolean,
    val noBlackHoleEvidenceVerified: Boolean,
    val physicalDeviceEvidenceVerified: Boolean,
) {
    init {
        require(id.isNotBlank() && id.length <= 128) { "Forwarder candidate id is invalid" }
        require(pinnedVersion.isNotBlank() && pinnedVersion.length <= 64) { "Forwarder version is invalid" }
        require(licenseSpdx.matches(Regex("[A-Za-z0-9.+-]{2,32}"))) { "Forwarder SPDX id is invalid" }
    }
}

enum class ForwarderPreflightBlocker {
    USE_CASE_MISMATCH,
    LICENSE_NOT_APPROVED,
    TUN_FD_INTAKE_UNVERIFIED,
    DIRECT_EGRESS_UNAVAILABLE,
    IPV4_UNVERIFIED,
    IPV6_UNVERIFIED,
    TCP_UNVERIFIED,
    UDP_UNVERIFIED,
    ANDROID_EMBEDDING_UNVERIFIED,
    VPN_PROTECT_UNVERIFIED,
    STOP_API_UNVERIFIED,
    NO_BLACK_HOLE_EVIDENCE_MISSING,
    PHYSICAL_DEVICE_EVIDENCE_MISSING,
}

data class ForwarderPreflightResult(
    val candidateId: String,
    val pinnedVersion: String,
    val targetUseCase: ForwarderUseCase,
    val blockers: List<ForwarderPreflightBlocker>,
) {
    /** Source-level fit only. It authorizes an isolated engineering POC, never VPN activation. */
    val prototypeEligible: Boolean
        get() = blockers.none { it in SOURCE_FIT_BLOCKERS }

    /** Production-path gate. This is intentionally much stricter than prototype eligibility. */
    val activationEligible: Boolean get() = blockers.isEmpty()

    companion object {
        private val SOURCE_FIT_BLOCKERS = setOf(
            ForwarderPreflightBlocker.USE_CASE_MISMATCH,
            ForwarderPreflightBlocker.LICENSE_NOT_APPROVED,
            ForwarderPreflightBlocker.TUN_FD_INTAKE_UNVERIFIED,
            ForwarderPreflightBlocker.DIRECT_EGRESS_UNAVAILABLE,
            ForwarderPreflightBlocker.IPV4_UNVERIFIED,
            ForwarderPreflightBlocker.IPV6_UNVERIFIED,
            ForwarderPreflightBlocker.TCP_UNVERIFIED,
            ForwarderPreflightBlocker.UDP_UNVERIFIED,
        )
    }
}

/**
 * Conservative source-review policy for selecting a future packet forwarder.
 *
 * A candidate can be useful for an isolated proof of concept while still being activation-ineligible.
 * Prototype eligibility only means the pinned source is worth evaluating. It never bypasses the
 * Android embedding, VpnService.protect, lifecycle, no-black-hole or physical-device gates.
 */
object ForwarderCandidateEvaluation {
    private val approvedLicenses = setOf("MIT", "Apache-2.0", "BSD-2-Clause", "BSD-3-Clause")

    fun evaluate(
        candidate: ForwarderCandidateProfile,
        targetUseCase: ForwarderUseCase,
    ): ForwarderPreflightResult {
        val blockers = buildList {
            if (candidate.intendedUseCase != targetUseCase) add(ForwarderPreflightBlocker.USE_CASE_MISMATCH)
            if (candidate.licenseSpdx !in approvedLicenses) add(ForwarderPreflightBlocker.LICENSE_NOT_APPROVED)
            if (!candidate.acceptsTunFileDescriptor) add(ForwarderPreflightBlocker.TUN_FD_INTAKE_UNVERIFIED)
            if (targetUseCase == ForwarderUseCase.N2_DIRECT && !candidate.supportsDirectEgress) {
                add(ForwarderPreflightBlocker.DIRECT_EGRESS_UNAVAILABLE)
            }
            if (!candidate.supportsIpv4) add(ForwarderPreflightBlocker.IPV4_UNVERIFIED)
            if (!candidate.supportsIpv6) add(ForwarderPreflightBlocker.IPV6_UNVERIFIED)
            if (!candidate.supportsTcp) add(ForwarderPreflightBlocker.TCP_UNVERIFIED)
            if (!candidate.supportsUdp) add(ForwarderPreflightBlocker.UDP_UNVERIFIED)
            if (!candidate.androidEmbeddingPathVerified) add(ForwarderPreflightBlocker.ANDROID_EMBEDDING_UNVERIFIED)
            if (candidate.socketEscapeStrategy != SocketEscapeStrategy.VPN_PROTECT) {
                add(ForwarderPreflightBlocker.VPN_PROTECT_UNVERIFIED)
            }
            if (!candidate.deterministicStopApiVerified) add(ForwarderPreflightBlocker.STOP_API_UNVERIFIED)
            if (!candidate.noBlackHoleEvidenceVerified) add(ForwarderPreflightBlocker.NO_BLACK_HOLE_EVIDENCE_MISSING)
            if (!candidate.physicalDeviceEvidenceVerified) add(ForwarderPreflightBlocker.PHYSICAL_DEVICE_EVIDENCE_MISSING)
        }
        return ForwarderPreflightResult(
            candidateId = candidate.id,
            pinnedVersion = candidate.pinnedVersion,
            targetUseCase = targetUseCase,
            blockers = blockers,
        )
    }
}
