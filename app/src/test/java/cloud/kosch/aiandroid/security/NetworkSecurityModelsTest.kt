package cloud.kosch.aiandroid.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NetworkSecurityModelsTest {
    @Test
    fun consentRequired_isCompletelyInactive() {
        val snapshot = NetworkSecurityN1Policy.snapshot(VpnAuthorizationState.CONSENT_REQUIRED)

        assertEquals(NetworkEngineState.INACTIVE, snapshot.engineState)
        assertEquals(TrafficPrivacyMode.OFF, snapshot.trafficPrivacyMode)
        assertEquals(FirewallMode.OFF, snapshot.firewallMode)
        assertEquals(ProxyRoutingMode.DIRECT, snapshot.proxyRoutingMode)
        assertEquals(0, snapshot.liveFlowCount)
        assertEquals(0L, snapshot.uploadedBytes)
        assertEquals(0L, snapshot.downloadedBytes)
    }

    @Test
    fun authorized_onlyMeansReadyForNextStage_notActiveTrafficHandling() {
        val snapshot = NetworkSecurityN1Policy.snapshot(VpnAuthorizationState.AUTHORIZED)

        assertEquals(NetworkEngineState.READY_FOR_N2, snapshot.engineState)
        assertEquals(TrafficPrivacyMode.OFF, snapshot.trafficPrivacyMode)
        assertEquals(FirewallMode.OFF, snapshot.firewallMode)
        assertEquals(ProxyRoutingMode.DIRECT, snapshot.proxyRoutingMode)
        assertEquals(0L, snapshot.allowedConnectionCount)
        assertEquals(0L, snapshot.blockedConnectionCount)
    }

    @Test
    fun acceptedActivityResult_withoutSystemAuthorization_doesNotAssumeConsent() {
        val snapshot = NetworkSecurityN1Policy.afterConsentResult(
            resultAccepted = true,
            systemReportsAuthorized = false,
        )

        assertEquals(VpnAuthorizationState.UNKNOWN, snapshot.vpnAuthorization)
        assertEquals(NetworkEngineState.INACTIVE, snapshot.engineState)
    }

    @Test
    fun systemAuthorization_isSourceOfTruth_evenIfActivityResultWasNotAccepted() {
        val snapshot = NetworkSecurityN1Policy.afterConsentResult(
            resultAccepted = false,
            systemReportsAuthorized = true,
        )

        assertEquals(VpnAuthorizationState.AUTHORIZED, snapshot.vpnAuthorization)
        assertEquals(NetworkEngineState.READY_FOR_N2, snapshot.engineState)
    }

    @Test
    fun n1RejectsSyntheticTrafficCounters() {
        assertThrows(IllegalArgumentException::class.java) {
            NetworkSecuritySnapshot(
                vpnAuthorization = VpnAuthorizationState.AUTHORIZED,
                engineState = NetworkEngineState.READY_FOR_N2,
                trafficPrivacyMode = TrafficPrivacyMode.OFF,
                firewallMode = FirewallMode.OFF,
                proxyRoutingMode = ProxyRoutingMode.DIRECT,
                liveFlowCount = 1,
            )
        }
    }

    @Test
    fun readyStateRequiresExplicitAuthorization() {
        assertThrows(IllegalArgumentException::class.java) {
            NetworkSecuritySnapshot(
                vpnAuthorization = VpnAuthorizationState.CONSENT_REQUIRED,
                engineState = NetworkEngineState.READY_FOR_N2,
                trafficPrivacyMode = TrafficPrivacyMode.OFF,
                firewallMode = FirewallMode.OFF,
                proxyRoutingMode = ProxyRoutingMode.DIRECT,
            )
        }
    }
}
