package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveFoldPostureTest {
    @Test
    fun `vertical half-open fold becomes book posture`() {
        val posture = AdaptiveFoldPosturePolicy.summarize(
            listOf(
                AdaptiveFoldFeatureSignal(
                    separating = true,
                    orientation = AdaptiveFoldOrientation.VERTICAL,
                    state = AdaptiveFoldState.HALF_OPENED,
                    occluding = false,
                ),
            ),
        )

        assertTrue(posture.present)
        assertTrue(posture.separating)
        assertTrue(posture.isBookPosture)
        assertFalse(posture.isTabletopPosture)

        val plan = AdaptiveFoldAwarePlanner.plan(
            environment = AdaptiveLauncherEnvironment(widthDp = 1_000, heightDp = 900),
            posture = posture,
        )
        assertEquals(AdaptiveFoldLayoutMode.BOOK_DUAL_PANE, plan.foldLayoutMode)
        assertTrue(plan.suppressGenericTwoPaneAcrossHinge)
        assertFalse(plan.base.useTwoPaneSettings)
    }

    @Test
    fun `horizontal half-open fold becomes tabletop posture`() {
        val posture = AdaptiveFoldPosturePolicy.summarize(
            listOf(
                AdaptiveFoldFeatureSignal(
                    separating = false,
                    orientation = AdaptiveFoldOrientation.HORIZONTAL,
                    state = AdaptiveFoldState.HALF_OPENED,
                    occluding = false,
                ),
            ),
        )

        assertTrue(posture.isTabletopPosture)
        val plan = AdaptiveFoldAwarePlanner.plan(
            environment = AdaptiveLauncherEnvironment(widthDp = 900, heightDp = 900),
            posture = posture,
        )
        assertEquals(AdaptiveFoldLayoutMode.TABLETOP_SPLIT, plan.foldLayoutMode)
    }

    @Test
    fun `occluding feature requests hinge-safe placement`() {
        val posture = AdaptiveFoldPosturePolicy.summarize(
            listOf(
                AdaptiveFoldFeatureSignal(
                    separating = false,
                    orientation = AdaptiveFoldOrientation.VERTICAL,
                    state = AdaptiveFoldState.FLAT,
                    occluding = true,
                ),
            ),
        )
        val plan = AdaptiveFoldAwarePlanner.plan(
            environment = AdaptiveLauncherEnvironment(widthDp = 800, heightDp = 900),
            posture = posture,
        )

        assertTrue(plan.avoidFoldOcclusion)
        assertEquals(AdaptiveFoldLayoutMode.NONE, plan.foldLayoutMode)
    }

    @Test
    fun `empty feature list stays ordinary adaptive layout`() {
        val posture = AdaptiveFoldPosturePolicy.summarize(emptyList())
        val plan = AdaptiveFoldAwarePlanner.plan(
            environment = AdaptiveLauncherEnvironment(widthDp = 500, heightDp = 800),
            posture = posture,
        )

        assertFalse(posture.present)
        assertEquals(AdaptiveFoldLayoutMode.NONE, plan.foldLayoutMode)
        assertFalse(plan.avoidFoldOcclusion)
    }
}
