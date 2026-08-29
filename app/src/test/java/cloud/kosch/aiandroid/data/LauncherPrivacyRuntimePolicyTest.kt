package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.PrivacySettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherPrivacyRuntimePolicyTest {
    @After
    fun tearDown() {
        LauncherPrivacyRuntimePolicy.resetForTest()
    }

    @Test
    fun `configure mirrors persisted privacy gates`() {
        LauncherPrivacyRuntimePolicy.configure(
            PrivacySettings(
                localUsageLearningEnabled = false,
                auditEnabled = false,
                auditRetentionDays = 7,
            ),
        )

        assertFalse(LauncherPrivacyRuntimePolicy.localUsageLearningEnabled)
        assertFalse(LauncherPrivacyRuntimePolicy.auditEnabled)
        assertEquals(7, LauncherPrivacyRuntimePolicy.auditRetentionDays)
    }

    @Test
    fun `retention is normalized and cutoff never underflows`() {
        LauncherPrivacyRuntimePolicy.configure(PrivacySettings(auditRetentionDays = 999))
        assertEquals(365, LauncherPrivacyRuntimePolicy.auditRetentionDays)

        LauncherPrivacyRuntimePolicy.configure(PrivacySettings(auditRetentionDays = 1))
        assertEquals(0L, LauncherPrivacyRuntimePolicy.auditCutoffEpochMillis(1_000L))
        assertEquals(86_400_000L, LauncherPrivacyRuntimePolicy.auditCutoffEpochMillis(172_800_000L))
    }

    @Test
    fun `fresh defaults keep local bounded features enabled`() {
        LauncherPrivacyRuntimePolicy.resetForTest()

        assertTrue(LauncherPrivacyRuntimePolicy.localUsageLearningEnabled)
        assertTrue(LauncherPrivacyRuntimePolicy.auditEnabled)
        assertEquals(90, LauncherPrivacyRuntimePolicy.auditRetentionDays)
    }
}
