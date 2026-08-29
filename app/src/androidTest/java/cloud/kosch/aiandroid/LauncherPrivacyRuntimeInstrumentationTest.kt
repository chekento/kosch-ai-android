package cloud.kosch.aiandroid

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.data.LauncherPrivacyRuntimePolicy
import cloud.kosch.aiandroid.data.LauncherSettingsStore
import cloud.kosch.aiandroid.model.LauncherSettingsDocument
import cloud.kosch.aiandroid.model.PrivacySettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherPrivacyRuntimeInstrumentationTest {
    private lateinit var context: Context
    private lateinit var store: LauncherSettingsStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        store = LauncherSettingsStore(context)
        store.reset()
        LauncherPrivacyRuntimePolicy.resetForTest()
    }

    @After
    fun tearDown() {
        store.reset()
        LauncherPrivacyRuntimePolicy.resetForTest()
    }

    @Test
    fun persistedOffGates_applyWhenSettingsControllerStarts() {
        assertTrue(
            store.save(
                LauncherSettingsDocument(
                    privacy = PrivacySettings(
                        localUsageLearningEnabled = false,
                        auditEnabled = false,
                        auditRetentionDays = 7,
                    ),
                ),
            ),
        )
        LauncherPrivacyRuntimePolicy.resetForTest()

        LauncherSettingsController(context)

        assertFalse(LauncherPrivacyRuntimePolicy.localUsageLearningEnabled)
        assertFalse(LauncherPrivacyRuntimePolicy.auditEnabled)
        assertEquals(7, LauncherPrivacyRuntimePolicy.auditRetentionDays)
    }

    @Test
    fun applyPrivacy_updatesRuntimeImmediately() {
        val controller = LauncherSettingsController(context)

        assertTrue(
            controller.applyPrivacy(
                PrivacySettings(
                    localUsageLearningEnabled = false,
                    auditEnabled = false,
                    auditRetentionDays = 14,
                ),
            ),
        )

        assertFalse(LauncherPrivacyRuntimePolicy.localUsageLearningEnabled)
        assertFalse(LauncherPrivacyRuntimePolicy.auditEnabled)
        assertEquals(14, LauncherPrivacyRuntimePolicy.auditRetentionDays)
    }

    @Test
    fun reload_rebindsRuntimeToPersistedPrivacyDocument() {
        val controller = LauncherSettingsController(context)
        controller.applyPrivacy(
            PrivacySettings(
                localUsageLearningEnabled = false,
                auditEnabled = false,
                auditRetentionDays = 5,
            ),
        )
        assertTrue(
            store.save(
                controller.document.copy(
                    privacy = PrivacySettings(
                        localUsageLearningEnabled = true,
                        auditEnabled = true,
                        auditRetentionDays = 30,
                    ),
                ),
            ),
        )

        controller.reload()

        assertTrue(LauncherPrivacyRuntimePolicy.localUsageLearningEnabled)
        assertTrue(LauncherPrivacyRuntimePolicy.auditEnabled)
        assertEquals(30, LauncherPrivacyRuntimePolicy.auditRetentionDays)
    }
}
