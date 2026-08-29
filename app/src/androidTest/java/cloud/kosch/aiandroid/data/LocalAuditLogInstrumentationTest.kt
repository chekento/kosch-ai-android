package cloud.kosch.aiandroid.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.model.AuditAction
import cloud.kosch.aiandroid.model.AuditOutcome
import cloud.kosch.aiandroid.model.PrivacySettings
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalAuditLogInstrumentationTest {
    private lateinit var context: Context
    private lateinit var log: LocalAuditLog

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        auditPreferences().edit().clear().commit()
        LauncherPrivacyRuntimePolicy.resetForTest()
        log = LocalAuditLog(context)
    }

    @After
    fun tearDown() {
        auditPreferences().edit().clear().commit()
        LauncherPrivacyRuntimePolicy.resetForTest()
    }

    @Test
    fun disabledAudit_doesNotPersistNewEvents() {
        LauncherPrivacyRuntimePolicy.configure(PrivacySettings(auditEnabled = false))

        log.append(AuditAction.APP_LAUNCH, AuditOutcome.SUCCESS)

        assertTrue(log.events().isEmpty())
        assertEquals(null, auditPreferences().getString(KEY_EVENTS, null))
    }

    @Test
    fun enabledAudit_persistsMetadataOnlyEvent() {
        log.append(AuditAction.APP_LAUNCH, AuditOutcome.SUCCESS)

        val events = log.events()
        assertEquals(1, events.size)
        assertEquals(AuditAction.APP_LAUNCH, events.single().action)
        assertEquals(AuditOutcome.SUCCESS, events.single().outcome)
    }

    @Test
    fun configuredRetention_filtersOlderStoredEvents() {
        val now = 20L * MILLIS_PER_DAY
        val recent = now - 2L * MILLIS_PER_DAY
        val old = now - 10L * MILLIS_PER_DAY
        val payload = JSONArray()
            .put(eventJson(recent))
            .put(eventJson(old))
        auditPreferences().edit().putString(KEY_EVENTS, payload.toString()).commit()
        LauncherPrivacyRuntimePolicy.configure(PrivacySettings(auditRetentionDays = 3))

        val events = log.events(now)

        assertEquals(1, events.size)
        assertEquals(recent, events.single().timestampEpochMillis)
    }

    private fun eventJson(time: Long) = JSONObject()
        .put("time", time)
        .put("action", AuditAction.APP_LAUNCH.name)
        .put("outcome", AuditOutcome.SUCCESS.name)

    private fun auditPreferences() = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFERENCES_NAME = "kosch_local_audit_v1"
        private const val KEY_EVENTS = "events"
        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1_000L
    }
}
