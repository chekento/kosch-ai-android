package cloud.kosch.aiandroid.ui.components

import cloud.kosch.aiandroid.model.AssistantVisualState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantAnimationDirectorTest {
    @Test
    fun reducedMotion_removesDecorativeLoopsButPreservesStateCues() {
        val early = AssistantAnimationDirector.frame(
            state = AssistantVisualState.THINKING,
            stateElapsedMillis = 0L,
            nowUptimeMillis = 1_000L,
            reducedMotion = true,
        )
        val late = AssistantAnimationDirector.frame(
            state = AssistantVisualState.THINKING,
            stateElapsedMillis = 9_000L,
            nowUptimeMillis = 10_000L,
            reducedMotion = true,
        )

        assertEquals(AssistantEyeShape.UP_RIGHT, early.eye)
        assertEquals(early.eye, late.eye)
        assertEquals(1f, early.bodyScale, 0.0001f)
        assertEquals(0f, early.bodyBob, 0.0001f)
        assertEquals(0, early.portalFrame)
        assertTrue(early.headTiltDegrees < 0f)
    }

    @Test
    fun idleBlink_usesFastFourStageOverlayCycle() {
        val closing = AssistantAnimationDirector.frame(
            state = AssistantVisualState.IDLE,
            stateElapsedMillis = 5_480L,
            nowUptimeMillis = 5_480L,
        )
        val closed = AssistantAnimationDirector.frame(
            state = AssistantVisualState.IDLE,
            stateElapsedMillis = 5_530L,
            nowUptimeMillis = 5_530L,
        )
        val opening = AssistantAnimationDirector.frame(
            state = AssistantVisualState.IDLE,
            stateElapsedMillis = 5_620L,
            nowUptimeMillis = 5_620L,
        )

        assertEquals(AssistantEyeShape.BLINK_HALF_1, closing.eye)
        assertEquals(AssistantEyeShape.BLINK_CLOSED, closed.eye)
        assertEquals(AssistantEyeShape.BLINK_HALF_2, opening.eye)
    }

    @Test
    fun speakingFrame_usesTimedVisemeAndKeepsHappyEyeState() {
        val signal = AssistantSpeechSignal(
            utteranceId = "speech",
            rangeVisemes = listOf(AssistantViseme.PP, AssistantViseme.AA),
            rangeStartedAtUptimeMillis = 1_000L,
            amplitude = 0.8f,
            rangeTimed = true,
        )
        val frame = AssistantAnimationDirector.frame(
            state = AssistantVisualState.SPEAKING,
            stateElapsedMillis = 500L,
            nowUptimeMillis = 1_080L,
            speechSignal = signal,
        )

        assertEquals(AssistantEyeShape.HAPPY, frame.eye)
        assertEquals(AssistantMouthShape.Viseme(AssistantViseme.AA), frame.mouth)
        assertTrue(frame.mouthOpen > 0.8f)
    }

    @Test
    fun errorFrame_isStableAndClearlyNonNeutral() {
        val frame = AssistantAnimationDirector.frame(
            state = AssistantVisualState.ERROR,
            stateElapsedMillis = 4_000L,
            nowUptimeMillis = 4_000L,
        )

        assertEquals(AssistantEyeShape.CONFUSED, frame.eye)
        assertEquals(AssistantMouthShape.Emotion(AssistantMouthEmotion.FROWN), frame.mouth)
        assertTrue(frame.headTiltDegrees > 0f)
    }
}
