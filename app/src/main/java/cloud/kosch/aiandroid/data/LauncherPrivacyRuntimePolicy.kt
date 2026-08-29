package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.PrivacySettings

/**
 * Process-local mirror of the persisted Privacy settings that gate launcher-internal data collection and use.
 *
 * Android permissions and provider Cloud Access remain separate boundaries. This object exists so low-level stores
 * and pure ranking code cannot accidentally keep collecting/using local telemetry after the visible Privacy switch
 * has been turned off. Defaults match a fresh [PrivacySettings] document and are replaced from persisted settings
 * before LauncherController starts.
 */
object LauncherPrivacyRuntimePolicy {
    @Volatile
    var localUsageLearningEnabled: Boolean = PrivacySettings().localUsageLearningEnabled
        private set

    @Volatile
    var auditEnabled: Boolean = PrivacySettings().auditEnabled
        private set

    @Volatile
    var auditRetentionDays: Int = PrivacySettings().auditRetentionDays
        private set

    fun configure(settings: PrivacySettings) {
        val normalized = settings.normalized()
        localUsageLearningEnabled = normalized.localUsageLearningEnabled
        auditEnabled = normalized.auditEnabled
        auditRetentionDays = normalized.auditRetentionDays
    }

    fun auditCutoffEpochMillis(nowEpochMillis: Long): Long {
        val retentionMillis = auditRetentionDays.toLong() * MILLIS_PER_DAY
        return (nowEpochMillis - retentionMillis).coerceAtLeast(0L)
    }

    internal fun resetForTest() {
        configure(PrivacySettings())
    }

    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1_000L
}
