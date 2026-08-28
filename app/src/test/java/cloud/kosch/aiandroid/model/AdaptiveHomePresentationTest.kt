package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveHomePresentationTest {
    @Test
    fun compactPhone_keepsComfortableChromeAndBoundedDock() {
        val plan = AdaptiveLauncherPlanner.plan(
            AdaptiveLauncherEnvironment(widthDp = 412, heightDp = 915),
        )

        val presentation = AdaptiveHomePresentationPolicy.from(plan, hasStylus = false)

        assertEquals(14, presentation.horizontalPaddingDp)
        assertEquals(10, presentation.verticalPaddingDp)
        assertEquals(10, presentation.verticalGapDp)
        assertEquals(4, presentation.dockPinnedAppLimit)
        assertFalse(presentation.showEdgePowerRail)
        assertFalse(presentation.showPenShortcut)
        assertFalse(presentation.emphasizePenShortcut)
    }

    @Test
    fun compactHeight_reducesVerticalChromeWithoutChangingWorkspace() {
        val plan = AdaptiveLauncherPlanner.plan(
            AdaptiveLauncherEnvironment(widthDp = 820, heightDp = 420),
        )

        val presentation = AdaptiveHomePresentationPolicy.from(plan, hasStylus = false)

        assertEquals(12, presentation.horizontalPaddingDp)
        assertEquals(6, presentation.verticalPaddingDp)
        assertEquals(6, presentation.verticalGapDp)
        assertEquals(6, presentation.dockPinnedAppLimit)
        assertFalse(presentation.showEdgePowerRail)
    }

    @Test
    fun largeStylusSurface_exposesPenShortcutAndEdgeRail() {
        val plan = AdaptiveLauncherPlanner.plan(
            AdaptiveLauncherEnvironment(
                widthDp = 1_000,
                heightDp = 900,
                hasStylus = true,
            ),
        )

        val presentation = AdaptiveHomePresentationPolicy.from(plan, hasStylus = true)

        assertEquals(12, presentation.horizontalPaddingDp)
        assertEquals(10, presentation.verticalPaddingDp)
        assertEquals(8, presentation.verticalGapDp)
        assertEquals(8, presentation.dockPinnedAppLimit)
        assertTrue(presentation.showEdgePowerRail)
        assertTrue(presentation.showPenShortcut)
        assertTrue(presentation.emphasizePenShortcut)
    }

    @Test
    fun desktopPlan_usesProductivityDensityAndLargestDockBudget() {
        val plan = AdaptiveLauncherPlanner.plan(
            AdaptiveLauncherEnvironment(
                widthDp = 1_400,
                heightDp = 900,
                isDesktopWindowing = true,
                hasPrecisePointer = true,
                hasHardwareKeyboard = true,
            ),
        )

        val presentation = AdaptiveHomePresentationPolicy.from(plan, hasStylus = false)

        assertEquals(10, presentation.horizontalPaddingDp)
        assertEquals(10, presentation.verticalPaddingDp)
        assertEquals(7, presentation.verticalGapDp)
        assertEquals(10, presentation.dockPinnedAppLimit)
        assertTrue(presentation.showEdgePowerRail)
        assertFalse(presentation.showPenShortcut)
    }
}
