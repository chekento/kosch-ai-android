package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherPresentationPlannerTest {
    @Test
    fun dock_keepsPinnedOrderThenFillsWithLocalUsageRanking() {
        val plan = LauncherPresentationPlanner.dock(
            settings = DockSettings(maxItems = 4, adaptiveSuggestions = true),
            pinnedAppKeys = listOf("mail", "browser", "mail", "missing"),
            usageSignals = listOf(
                AppUsageSignal("notes", launchCount = 8, lastUsedEpochMillis = 100L),
                AppUsageSignal("calendar", launchCount = 8, lastUsedEpochMillis = 200L),
                AppUsageSignal("browser", launchCount = 99, lastUsedEpochMillis = 999L),
                AppUsageSignal("music", launchCount = 3, lastUsedEpochMillis = 500L),
            ),
            availableAppKeys = setOf("mail", "browser", "notes", "calendar", "music"),
        )

        assertTrue(plan.enabled)
        assertEquals(listOf("mail", "browser", "calendar", "notes"), plan.appKeys)
    }

    @Test
    fun dock_disabled_neverShowsAppsOrAsk() {
        val plan = LauncherPresentationPlanner.dock(
            settings = DockSettings(enabled = false, showAskButton = true),
            pinnedAppKeys = listOf("mail"),
            usageSignals = emptyList(),
            availableAppKeys = setOf("mail"),
        )
        assertFalse(plan.enabled)
        assertTrue(plan.appKeys.isEmpty())
        assertFalse(plan.showAskButton)
    }

    @Test
    fun appItem_respectsLabelsAndWorkBadgeIndependently() {
        val hidden = LauncherPresentationPlanner.appItem(
            AppSpaceSettings(showLabels = false, showWorkProfileBadges = false),
            isPersonalProfile = false,
        )
        assertFalse(hidden.showLabel)
        assertFalse(hidden.showProfileBadge)

        val work = LauncherPresentationPlanner.appItem(
            AppSpaceSettings(showLabels = true, showWorkProfileBadges = true),
            isPersonalProfile = false,
        )
        assertTrue(work.showLabel)
        assertTrue(work.showProfileBadge)

        val personal = LauncherPresentationPlanner.appItem(
            AppSpaceSettings(showWorkProfileBadges = true),
            isPersonalProfile = true,
        )
        assertFalse(personal.showProfileBadge)
    }

    @Test
    fun pageNavigation_stopsAtEdgesUnlessLoopingIsEnabled() {
        val stopped = LauncherPresentationPlanner.adjacentPageIndex(
            settings = PageSettings(loopingEnabled = false),
            currentIndex = 2,
            pageCount = 3,
            direction = 1,
        )
        assertEquals(2, stopped)

        val wrappedForward = LauncherPresentationPlanner.adjacentPageIndex(
            settings = PageSettings(loopingEnabled = true),
            currentIndex = 2,
            pageCount = 3,
            direction = 1,
        )
        assertEquals(0, wrappedForward)

        val wrappedBackward = LauncherPresentationPlanner.adjacentPageIndex(
            settings = PageSettings(loopingEnabled = true),
            currentIndex = 0,
            pageCount = 3,
            direction = -1,
        )
        assertEquals(2, wrappedBackward)
    }
}
