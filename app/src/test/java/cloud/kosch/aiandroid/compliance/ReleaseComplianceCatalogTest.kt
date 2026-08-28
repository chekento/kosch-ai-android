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
        assertFalse(vpn.koSchNetworkTransfer)
    }

    @Test
    fun currentReleaseDeclaresNoKoSchContentUpload() {
        assertTrue(ReleaseComplianceCatalog.currentReleaseHasNoKoSchContentUpload())
    }

    @Test
    fun complianceIdsRemainUnique() {
        val ids = ReleaseComplianceCatalog.capabilities.map { it.id }
        assertTrue(ids.size == ids.toSet().size)
    }
}
