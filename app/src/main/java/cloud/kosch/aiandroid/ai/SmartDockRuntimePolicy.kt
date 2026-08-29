package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.AppUsageSignal
import cloud.kosch.aiandroid.model.DockSettings
import cloud.kosch.aiandroid.model.SceneId

/**
 * Runtime contract between portable DockSettings and the local Smart Dock ranker.
 *
 * Disabling adaptive suggestions never falls back to hidden learning: only explicitly pinned apps remain.
 * The policy only returns stable app keys and never inspects app content, notification text or network state.
 */
object SmartDockRuntimePolicy {
    fun selectKeys(
        apps: List<SmartAppDescriptor>,
        pinnedKeys: List<String>,
        recentPackages: List<String>,
        usageSignals: Map<String, AppUsageSignal>,
        scene: SceneId,
        settings: DockSettings,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): List<String> {
        val normalized = settings.normalized()
        if (!normalized.enabled || normalized.maxItems <= 0) return emptyList()

        val availableKeys = apps.mapTo(linkedSetOf(), SmartAppDescriptor::key)
        if (!normalized.adaptiveSuggestions) {
            return pinnedKeys
                .asSequence()
                .filter(availableKeys::contains)
                .distinct()
                .take(normalized.maxItems)
                .toList()
        }

        val usageKeys = LocalUsageModel.rankKeys(
            apps.map(SmartAppDescriptor::key),
            usageSignals,
            nowEpochMillis,
        ).filter(usageSignals::containsKey)

        return LocalSmartOrganizer.smartDockKeys(
            apps = apps,
            pinnedKeys = pinnedKeys,
            recentPackages = recentPackages,
            usageKeys = usageKeys,
            scene = scene,
            limit = normalized.maxItems,
        )
    }

    fun visibleBadgeCount(rawCount: Int, showBadgesOnDock: Boolean): Int =
        if (showBadgesOnDock) rawCount.coerceAtLeast(0) else 0
}
