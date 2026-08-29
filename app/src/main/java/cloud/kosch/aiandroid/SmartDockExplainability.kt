package cloud.kosch.aiandroid

import cloud.kosch.aiandroid.ai.LocalSmartOrganizer
import cloud.kosch.aiandroid.ai.LocalUsageModel
import cloud.kosch.aiandroid.ai.SmartAppDescriptor
import cloud.kosch.aiandroid.ai.SmartDockSuggestion

/**
 * Read-only explanation bridge for the already active Smart Dock policy.
 *
 * This does not introduce another ranking algorithm. It feeds the same deterministic LocalSmartOrganizer with the
 * same launcher-local signals used by the dock: pins, package recency, bounded local usage rank and active scene.
 * Notification contents, contacts, browsing history, prompts and network activity are never inputs.
 */
fun LauncherController.smartDockExplanationFor(appKey: String): SmartDockSuggestion? {
    val visibleApps = apps.filterNot { it.key in hiddenAppKeys }
    if (visibleApps.none { it.key == appKey }) return null
    val descriptors = visibleApps.map { app ->
        SmartAppDescriptor(
            key = app.key,
            label = app.label,
            packageName = app.packageName,
        )
    }
    val usageKeys = LocalUsageModel.rankKeys(
        visibleApps.map { it.key },
        appUsageSignals,
        System.currentTimeMillis(),
    ).filter(appUsageSignals::containsKey)

    return LocalSmartOrganizer.smartDockSuggestions(
        apps = descriptors,
        pinnedKeys = pinnedAppKeys,
        recentPackages = recentPackages,
        usageKeys = usageKeys,
        scene = activeScene,
        limit = SMART_DOCK_VISIBLE_LIMIT,
    ).firstOrNull { it.app.key == appKey }
}

private const val SMART_DOCK_VISIBLE_LIMIT = 5
