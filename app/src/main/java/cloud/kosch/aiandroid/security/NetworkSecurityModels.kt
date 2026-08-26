package cloud.kosch.aiandroid.security

/**
 * User-visible authorization state for KoSch's future local VPN boundary.
 *
 * N1 is deliberately consent-only: no VPN interface is established, no packet is inspected and no
 * foreground network service is started. Later stages may transition into active forwarding only through
 * a separate, explicitly reviewed engine state.
 */
enum class VpnAuthorizationState {
    UNKNOWN,
    CONSENT_REQUIRED,
    AUTHORIZED,
}

enum class NetworkEngineState {
    INACTIVE,
    READY_FOR_N2,
}

enum class TrafficPrivacyMode {
    OFF,
}

enum class FirewallMode {
    OFF,
}

enum class ProxyRoutingMode {
    DIRECT,
}

data class NetworkSecuritySnapshot(
    val vpnAuthorization: VpnAuthorizationState,
    val engineState: NetworkEngineState,
    val trafficPrivacyMode: TrafficPrivacyMode,
    val firewallMode: FirewallMode,
    val proxyRoutingMode: ProxyRoutingMode,
    val liveFlowCount: Int = 0,
    val allowedConnectionCount: Long = 0,
    val blockedConnectionCount: Long = 0,
    val uploadedBytes: Long = 0,
    val downloadedBytes: Long = 0,
) {
    init {
        require(liveFlowCount >= 0) { "Live flow count must not be negative" }
        require(allowedConnectionCount >= 0) { "Allowed connection count must not be negative" }
        require(blockedConnectionCount >= 0) { "Blocked connection count must not be negative" }
        require(uploadedBytes >= 0) { "Uploaded byte count must not be negative" }
        require(downloadedBytes >= 0) { "Downloaded byte count must not be negative" }

        // Strong N1 invariant: authorization may be prepared, but traffic handling cannot silently start.
        require(trafficPrivacyMode == TrafficPrivacyMode.OFF) { "N1 must not inspect traffic" }
        require(firewallMode == FirewallMode.OFF) { "N1 must not filter traffic" }
        require(proxyRoutingMode == ProxyRoutingMode.DIRECT) { "N1 must not reroute traffic" }
        require(liveFlowCount == 0) { "N1 must not retain live flows" }
        require(allowedConnectionCount == 0L && blockedConnectionCount == 0L) {
            "N1 must not count handled connections"
        }
        require(uploadedBytes == 0L && downloadedBytes == 0L) { "N1 must not count handled traffic" }
        if (engineState == NetworkEngineState.READY_FOR_N2) {
            require(vpnAuthorization == VpnAuthorizationState.AUTHORIZED) {
                "Only explicit Android VPN authorization may unlock the N2-ready state"
            }
        }
    }
}

object NetworkSecurityN1Policy {
    fun snapshot(authorization: VpnAuthorizationState): NetworkSecuritySnapshot = NetworkSecuritySnapshot(
        vpnAuthorization = authorization,
        engineState = if (authorization == VpnAuthorizationState.AUTHORIZED) {
            NetworkEngineState.READY_FOR_N2
        } else {
            NetworkEngineState.INACTIVE
        },
        trafficPrivacyMode = TrafficPrivacyMode.OFF,
        firewallMode = FirewallMode.OFF,
        proxyRoutingMode = ProxyRoutingMode.DIRECT,
    )

    fun afterConsentResult(
        resultAccepted: Boolean,
        systemReportsAuthorized: Boolean,
    ): NetworkSecuritySnapshot {
        val authorization = when {
            systemReportsAuthorized -> VpnAuthorizationState.AUTHORIZED
            resultAccepted -> VpnAuthorizationState.UNKNOWN
            else -> VpnAuthorizationState.CONSENT_REQUIRED
        }
        return snapshot(authorization)
    }
}
