package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.AppUsageSignal

/** Migrates the pre-profile `UserHandle.hashCode()` app keys to stable user-serial keys. */
object AppKeyMigration {
    fun keys(values: List<String>, aliases: Map<String, String>): List<String> = values
        .map { aliases[it] ?: it }
        .distinct()

    fun usage(
        values: Map<String, AppUsageSignal>,
        aliases: Map<String, String>,
    ): Map<String, AppUsageSignal> = buildMap {
        values.forEach { (key, signal) ->
            val migratedKey = aliases[key] ?: key
            val migrated = signal.copy(key = migratedKey)
            val existing = get(migratedKey)
            put(
                migratedKey,
                if (existing == null) {
                    migrated
                } else {
                    migrated.copy(
                        launchCount = (existing.launchCount.toLong() + migrated.launchCount)
                            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                        lastUsedEpochMillis = maxOf(existing.lastUsedEpochMillis, migrated.lastUsedEpochMillis),
                    )
                },
            )
        }
    }
}
