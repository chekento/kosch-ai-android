package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.MotionProfile
import cloud.kosch.aiandroid.model.ThemeMode
import cloud.kosch.aiandroid.model.WallpaperMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalThemeIntentPlannerTest {
    @Test
    fun expressiveDarkGlassPromptBuildsUsefulLocalProposal() {
        val proposal = LocalThemeIntentPlanner.propose("Dunkel, kinoreif und Neural Glass mit Theme-Hintergrund")

        assertTrue(proposal.actionable)
        assertEquals(ThemeMode.DARK, proposal.appearance.mode)
        assertEquals(MotionProfile.EXPRESSIVE, proposal.appearance.motionProfile)
        assertEquals(WallpaperMode.THEME, proposal.appearance.wallpaperMode)
        assertEquals("neural-glass", proposal.theme.activeThemeId)
        assertTrue(proposal.appearance.blurStrength >= 0.58f)
    }

    @Test
    fun compactCalmPromptAdjustsHomeWithoutCloudModel() {
        val proposal = LocalThemeIntentPlanner.propose("Bitte kompakt und ruhig")

        assertEquals(MotionProfile.REDUCED, proposal.appearance.motionProfile)
        assertEquals(3, proposal.home.horizontalGapDp)
        assertEquals(3, proposal.home.verticalGapDp)
        assertTrue(proposal.home.iconScale < 1f)
    }

    @Test
    fun unknownPromptDoesNotPretendToUnderstandIt() {
        val proposal = LocalThemeIntentPlanner.propose("Quantenmarmelade")

        assertFalse(proposal.actionable)
        assertEquals(0, proposal.matchedSignals)
        assertTrue(proposal.rationale.isEmpty())
    }
}
