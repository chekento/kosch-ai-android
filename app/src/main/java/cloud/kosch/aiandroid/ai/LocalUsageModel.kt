package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.AppUsageSignal
import kotlin.math.ln

/** A bounded, content-free and fully local adaptive ranking model. */
object LocalUsageModel {
    fun observe(
        current: Map<String, AppUsageSignal>,
        appKey: String,
        nowEpochMillis: Long,
        limit: Int = MAX_SIGNALS,
    ): Map<String, AppUsageSignal> {
        require(appKey.isNotBlank())
        require(nowEpochMillis > 0L)
        if (limit <= 0) return emptyMap()
        val previous = current[appKey]
        val updated = current + (
            appKey to AppUsageSignal(
                key = appKey,
                launchCount = (previous?.launchCount ?: 0).plus(1).coerceAtMost(MAX_LAUNCH_COUNT),
                lastUsedEpochMillis = nowEpochMillis,
            )
        )
        return updated.values
            .sortedWith(compareByDescending<AppUsageSignal> { it.lastUsedEpochMillis }.thenBy { it.key })
            .take(limit)
            .associateBy(AppUsageSignal::key)
    }

    fun rankKeys(
        keys: List<String>,
        signals: Map<String, AppUsageSignal>,
        nowEpochMillis: Long,
    ): List<String> = keys.withIndex()
        .sortedWith(
            compareByDescending<IndexedValue<String>> { score(signals[it.value], nowEpochMillis) }
                .thenBy(IndexedValue<String>::index),
        )
        .map(IndexedValue<String>::value)

    internal fun score(signal: AppUsageSignal?, nowEpochMillis: Long): Double {
        if (signal == null) return 0.0
        val ageMillis = (nowEpochMillis - signal.lastUsedEpochMillis).coerceAtLeast(0L)
        val recency = 6_000.0 / (1.0 + ageMillis.toDouble() / RECENCY_HALF_LIFE_MILLIS)
        val frequency = ln(signal.launchCount.coerceAtLeast(1).toDouble() + 1.0) * 1_200.0
        return recency + frequency
    }

    const val MAX_SIGNALS = 512
    const val MAX_LAUNCH_COUNT = 1_000_000
    private const val RECENCY_HALF_LIFE_MILLIS = 3L * 24L * 60L * 60L * 1_000L
}
