package cloud.kosch.aiandroid.model

/**
 * Pure runtime presentation policy for settings that are shared by phone, foldable, desktop and external displays.
 *
 * Compose surfaces consume these plans instead of reimplementing ranking/looping rules locally. This keeps persisted
 * settings declarative and makes the eventual adaptive desktop wiring deterministic and independently testable.
 */
data class DockPresentationPlan(
    val enabled: Boolean,
    val appKeys: List<String>,
    val iconScale: Float,
    val backgroundOpacity: Float,
    val showAskButton: Boolean,
)

data class AppItemPresentationPlan(
    val showLabel: Boolean,
    val showProfileBadge: Boolean,
)

object LauncherPresentationPlanner {
    fun dock(
        settings: DockSettings,
        pinnedAppKeys: List<String>,
        usageSignals: List<AppUsageSignal>,
        availableAppKeys: Set<String>,
    ): DockPresentationPlan {
        val safe = settings.normalized()
        if (!safe.enabled || safe.maxItems == 0) {
            return DockPresentationPlan(
                enabled = false,
                appKeys = emptyList(),
                iconScale = safe.iconScale,
                backgroundOpacity = safe.backgroundOpacity,
                showAskButton = false,
            )
        }

        val pinned = pinnedAppKeys
            .asSequence()
            .filter(availableAppKeys::contains)
            .distinct()
            .toList()

        val adaptive = if (safe.adaptiveSuggestions) {
            usageSignals
                .asSequence()
                .filter { it.key in availableAppKeys && it.key !in pinned }
                .sortedWith(
                    compareByDescending<AppUsageSignal> { it.launchCount }
                        .thenByDescending { it.lastUsedEpochMillis }
                        .thenBy { it.key },
                )
                .map(AppUsageSignal::key)
                .distinct()
                .toList()
        } else {
            emptyList()
        }

        return DockPresentationPlan(
            enabled = true,
            appKeys = (pinned + adaptive).take(safe.maxItems),
            iconScale = safe.iconScale,
            backgroundOpacity = safe.backgroundOpacity,
            showAskButton = safe.showAskButton,
        )
    }

    fun appItem(settings: AppSpaceSettings, isPersonalProfile: Boolean): AppItemPresentationPlan =
        AppItemPresentationPlan(
            showLabel = settings.showLabels,
            showProfileBadge = !isPersonalProfile && settings.showWorkProfileBadges,
        )

    /** Returns the next valid page index, or the current index when navigation cannot move. */
    fun adjacentPageIndex(
        settings: PageSettings,
        currentIndex: Int,
        pageCount: Int,
        direction: Int,
    ): Int {
        if (pageCount <= 0 || currentIndex !in 0 until pageCount || direction == 0) return currentIndex
        val step = if (direction < 0) -1 else 1
        val requested = currentIndex + step
        if (requested in 0 until pageCount) return requested
        if (!settings.loopingEnabled || pageCount == 1) return currentIndex
        return if (step < 0) pageCount - 1 else 0
    }
}
