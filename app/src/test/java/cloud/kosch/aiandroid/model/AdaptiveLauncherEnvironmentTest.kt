package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLauncherEnvironmentTest {
    @Test
    fun windowBreakpoints_followAndroidV2Classes() {
        assertEquals(AdaptiveWidthClass.COMPACT, AdaptiveLauncherPlanner.widthClass(599))
        assertEquals(AdaptiveWidthClass.MEDIUM, AdaptiveLauncherPlanner.widthClass(600))
        assertEquals(AdaptiveWidthClass.EXPANDED, AdaptiveLauncherPlanner.widthClass(840))
        assertEquals(AdaptiveWidthClass.LARGE, AdaptiveLauncherPlanner.widthClass(1_200))
        assertEquals(AdaptiveWidthClass.EXTRA_LARGE, AdaptiveLauncherPlanner.widthClass(1_600))
        assertEquals(AdaptiveHeightClass.COMPACT, AdaptiveLauncherPlanner.heightClass(479))
        assertEquals(AdaptiveHeightClass.MEDIUM, AdaptiveLauncherPlanner.heightClass(480))
        assertEquals(AdaptiveHeightClass.EXPANDED, AdaptiveLauncherPlanner.heightClass(900))
    }

    @Test
    fun precisePointerOnWideDesktop_usesProductivityDensityAndPowerUi() {
        val plan = AdaptiveLauncherPlanner.plan(
            AdaptiveLauncherEnvironment(
                widthDp = 1_400,
                heightDp = 900,
                isDesktopWindowing = true,
                hasPrecisePointer = true,
                hasHardwareKeyboard = true,
            ),
        )
        assertEquals(AdaptiveSurfaceMode.DESKTOP_WINDOW, plan.surfaceMode)
        assertEquals(AdaptiveUiDensity.PRODUCTIVITY, plan.density)
        assertEquals(AdaptiveInteractionProfile.DESKTOP_PRO, plan.interactionProfile)
        assertEquals(AdaptiveWorkspaceProfileHint.WORK, plan.workspaceProfileHint)
        assertTrue(plan.enableHoverAffordances)
        assertTrue(plan.usePersistentCommandRail)
        assertTrue(plan.useTwoPaneSettings)
        assertTrue(plan.preferWindowedAssistantDock)
        assertTrue(plan.useEdgePowerRail)
        assertTrue(plan.preferDenseAppGrid)
        assertTrue(plan.showKeyboardShortcutHints)
        assertEquals(10, plan.maxVisibleQuickActions)
    }

    @Test
    fun mediumWidthButCompactHeight_avoidsTwoPaneAndPowerRail() {
        val plan = AdaptiveLauncherPlanner.plan(
            AdaptiveLauncherEnvironment(widthDp = 820, heightDp = 420),
        )
        assertEquals(AdaptiveWidthClass.MEDIUM, plan.widthClass)
        assertEquals(AdaptiveHeightClass.COMPACT, plan.heightClass)
        assertEquals(AdaptiveInteractionProfile.LARGE_TOUCH, plan.interactionProfile)
        assertFalse(plan.useTwoPaneSettings)
        assertFalse(plan.useEdgePowerRail)
        assertEquals(6, plan.maxVisibleQuickActions)
    }

    @Test
    fun connectedDisplay_canKeepIndependentWorkspaceWithoutChangingPhonePolicy() {
        val external = AdaptiveLauncherPlanner.plan(
            AdaptiveLauncherEnvironment(
                widthDp = 1_920,
                heightDp = 1_080,
                isExternalDisplay = true,
                hasPrecisePointer = true,
            ),
            externalWorkspacePreference = ExternalDisplayWorkspaceMode.INDEPENDENT,
        )
        assertEquals(AdaptiveSurfaceMode.EXTERNAL_DESKTOP, external.surfaceMode)
        assertEquals(ExternalDisplayWorkspaceMode.INDEPENDENT, external.externalWorkspaceMode)

        val phone = AdaptiveLauncherPlanner.plan(
            AdaptiveLauncherEnvironment(widthDp = 412, heightDp = 915),
        )
        assertEquals(AdaptiveSurfaceMode.PHONE, phone.surfaceMode)
        assertEquals(AdaptiveInteractionProfile.THUMB_FIRST, phone.interactionProfile)
        assertEquals(ExternalDisplayWorkspaceMode.MIRROR_CURRENT, phone.externalWorkspaceMode)
        assertEquals(4, phone.maxVisibleQuickActions)
    }

    @Test
    fun separatingFold_disablesSingleTwoPaneSurfaceEvenWhenWidthIsLarge() {
        val plan = AdaptiveLauncherPlanner.plan(
            AdaptiveLauncherEnvironment(
                widthDp = 1_000,
                heightDp = 900,
                hasSeparatingFold = true,
            ),
        )
        assertFalse(plan.useTwoPaneSettings)
        assertEquals(AdaptiveInteractionProfile.FOLD_DUAL_SURFACE, plan.interactionProfile)
    }

    @Test
    fun stylusOnLargeTouch_prioritizesCreativePenSpaceWithoutAutomaticMutation() {
        val plan = AdaptiveLauncherPlanner.plan(
            AdaptiveLauncherEnvironment(
                widthDp = 1_000,
                heightDp = 900,
                hasStylus = true,
            ),
        )
        assertEquals(AdaptiveInteractionProfile.PEN_CANVAS, plan.interactionProfile)
        assertEquals(AdaptiveWorkspaceProfileHint.CREATIVE, plan.workspaceProfileHint)
        assertTrue(plan.prioritizePenSpaceEntry)
        assertFalse(plan.preferDenseAppGrid)
    }

    @Test
    fun presentationDisplay_recommendsPresentationProfile() {
        val plan = AdaptiveLauncherPlanner.plan(
            AdaptiveLauncherEnvironment(
                widthDp = 1_920,
                heightDp = 1_080,
                isExternalDisplay = true,
                isPresentationDisplay = true,
            ),
        )
        assertEquals(AdaptiveInteractionProfile.PRESENTATION, plan.interactionProfile)
        assertEquals(AdaptiveWorkspaceProfileHint.PRESENTATION, plan.workspaceProfileHint)
    }
}
