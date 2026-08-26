package cloud.kosch.aiandroid.security.network

/**
 * Pinned source-review facts only. A true flag means the pinned upstream source exposes that
 * capability; it does not mean KoSch has integrated or device-validated it.
 *
 * Runtime activation is governed separately by [ForwarderCandidateEvaluation] and
 * [N2ActivationGate].
 */
object ForwarderCandidateCatalog {
    /**
     * xjasonlyu/tun2socks v2.7.0.
     *
     * Source review: MIT tag, gVisor userspace stack, FD-based TUN device code and a direct TCP/UDP
     * dialer exist in the pinned source. No KoSch Android embedding/protect/lifecycle evidence exists
     * yet, so those fields stay deliberately false/unverified.
     */
    val TUN2SOCKS_2_7_0 = ForwarderCandidateProfile(
        id = "xjasonlyu/tun2socks",
        pinnedVersion = "v2.7.0",
        licenseSpdx = "MIT",
        intendedUseCase = ForwarderUseCase.N2_DIRECT,
        acceptsTunFileDescriptor = true,
        supportsDirectEgress = true,
        supportsIpv4 = true,
        supportsIpv6 = true,
        supportsTcp = true,
        supportsUdp = true,
        androidEmbeddingPathVerified = false,
        socketEscapeStrategy = SocketEscapeStrategy.UNVERIFIED,
        deterministicStopApiVerified = false,
        noBlackHoleEvidenceVerified = false,
        physicalDeviceEvidenceVerified = false,
    )

    /**
     * heiher/hev-socks5-tunnel 2.17.1.
     *
     * Source review: MIT tag, Android builds, TUN-FD C/JNI API, IPv4/IPv6 and TCP/UDP are documented.
     * The library forwards through SOCKS5 rather than direct egress, so it is catalogued for N4.
     * The quit API is explicit, but KoSch still has no protect/no-black-hole/OEM evidence.
     */
    val HEV_SOCKS5_2_17_1 = ForwarderCandidateProfile(
        id = "heiher/hev-socks5-tunnel",
        pinnedVersion = "2.17.1",
        licenseSpdx = "MIT",
        intendedUseCase = ForwarderUseCase.N4_PROXY,
        acceptsTunFileDescriptor = true,
        supportsDirectEgress = false,
        supportsIpv4 = true,
        supportsIpv6 = true,
        supportsTcp = true,
        supportsUdp = true,
        androidEmbeddingPathVerified = true,
        socketEscapeStrategy = SocketEscapeStrategy.UNVERIFIED,
        deterministicStopApiVerified = true,
        noBlackHoleEvidenceVerified = false,
        physicalDeviceEvidenceVerified = false,
    )

    val all: List<ForwarderCandidateProfile> = listOf(TUN2SOCKS_2_7_0, HEV_SOCKS5_2_17_1)
}
