package cloud.kosch.aiandroid

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.VpnService
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.security.FirewallMode
import cloud.kosch.aiandroid.security.KoSchConsentVpnService
import cloud.kosch.aiandroid.security.NetworkEngineState
import cloud.kosch.aiandroid.security.NetworkSecurityN1Policy
import cloud.kosch.aiandroid.security.ProxyRoutingMode
import cloud.kosch.aiandroid.security.TrafficPrivacyMode
import cloud.kosch.aiandroid.security.VpnAuthorizationState
import cloud.kosch.aiandroid.security.VpnConsentGateway
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VpnConsentContractInstrumentationTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun vpnService_isSystemProtectedAndAlwaysOnExplicitlyDisabled() {
        val serviceInfo = context.packageManager.getServiceInfo(
            ComponentName(context, KoSchConsentVpnService::class.java),
            PackageManager.ComponentInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
        )

        assertEquals(Manifest.permission.BIND_VPN_SERVICE, serviceInfo.permission)
        assertTrue(serviceInfo.exported)
        assertFalse(
            serviceInfo.metaData?.getBoolean(
                VpnService.SERVICE_META_DATA_SUPPORTS_ALWAYS_ON,
                true,
            ) ?: true,
        )
    }

    @Test
    fun packageNetworkPermissions_matchProviderAndN1Separation() {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
        )
        val requested = packageInfo.requestedPermissions.orEmpty().toSet()

        // INTERNET is a deliberate package capability for foreground, explicitly connected AI providers. It does not
        // activate the separate N1 VPN prototype; that boundary is asserted by the engine-state contract below.
        assertTrue(Manifest.permission.INTERNET in requested)
        assertTrue(Manifest.permission.ACCESS_NETWORK_STATE in requested)
    }

    @Test
    fun androidAuthorizationCheck_cannotProduceAnActiveN1TrafficEngine() {
        val authorization = VpnConsentGateway.authorizationState(context)
        val snapshot = NetworkSecurityN1Policy.snapshot(authorization)

        assertNotEquals(VpnAuthorizationState.UNKNOWN, authorization)
        assertEquals(TrafficPrivacyMode.OFF, snapshot.trafficPrivacyMode)
        assertEquals(FirewallMode.OFF, snapshot.firewallMode)
        assertEquals(ProxyRoutingMode.DIRECT, snapshot.proxyRoutingMode)
        assertEquals(0, snapshot.liveFlowCount)
        assertEquals(0L, snapshot.allowedConnectionCount)
        assertEquals(0L, snapshot.blockedConnectionCount)
        assertEquals(0L, snapshot.uploadedBytes)
        assertEquals(0L, snapshot.downloadedBytes)

        if (authorization == VpnAuthorizationState.AUTHORIZED) {
            assertEquals(NetworkEngineState.READY_FOR_N2, snapshot.engineState)
        } else {
            assertEquals(NetworkEngineState.INACTIVE, snapshot.engineState)
        }
    }
}
