package cloud.kosch.aiandroid.compliance

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseComplianceCatalogTest {
    @Test
    fun productionSensitiveCapabilitiesAreNotSilentDefaults() {
        val sensitive = ReleaseComplianceCatalog.productionCapabilities.filter {
            it.dataClass in setOf(
                ComplianceDataClass.CAMERA_CONTENT,
                ComplianceDataClass.SCREEN_CONTENT,
                ComplianceDataClass.NOTIFICATION_METADATA,
                ComplianceDataClass.USER_TEXT,
                ComplianceDataClass.AUTH_CREDENTIAL,
            )
        }

        assertTrue(sensitive.isNotEmpty())
        sensitive.forEach { capability ->
            assertFalse("${capability.id} must not silently default on", capability.defaultEnabled)
            assertTrue("${capability.id} must require an explicit user action", capability.requiresExplicitUserAction)
        }
    }

    @Test
    fun inertVpnPrototypeIsDevelopmentOnly() {
        val vpn = requireNotNull(ReleaseComplianceCatalog.byId("vpn_n1_prototype"))

        assertFalse(vpn.productionEnabled)
        assertFalse(vpn.defaultEnabled)
        assertFalse(vpn.kalNetworkTransfer)
    }

    @Test
    fun productionNetworkTransfersAreExplicitOptIn() {
        val transfers = ReleaseComplianceCatalog.productionCapabilities.filter { it.kalNetworkTransfer }

        assertTrue(transfers.isNotEmpty())
        assertTrue(ReleaseComplianceCatalog.productionNetworkTransfersAreExplicitOptIn())
        transfers.forEach { capability ->
            assertFalse(capability.defaultEnabled)
            assertTrue(capability.requiresExplicitUserAction)
        }
    }

    @Test
    fun directProviderRequestsAreDisclosedAsNetworkTransfer() {
        val direct = requireNotNull(ReleaseComplianceCatalog.byId("direct_ai_provider_request"))
        val authentication = requireNotNull(ReleaseComplianceCatalog.byId("provider_authentication"))

        assertTrue(direct.productionEnabled)
        assertTrue(direct.kalNetworkTransfer)
        assertTrue(authentication.productionEnabled)
        assertTrue(authentication.kalNetworkTransfer)
    }

    @Test
    fun complianceIdsRemainUnique() {
        val ids = ReleaseComplianceCatalog.capabilities.map { it.id }
        assertTrue(ids.size == ids.toSet().size)
    }
}
