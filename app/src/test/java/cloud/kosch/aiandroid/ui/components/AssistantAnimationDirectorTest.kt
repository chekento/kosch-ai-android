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

    @Test
    fun directTouch_drivesAnalogGazeAndMatchingOverlayDirection() {
        val attention = AssistantAttentionSignal.Idle.pointer(
            normalizedX = 0.92f,
            normalizedY = 0.78f,
            isPressed = true,
            nowUptimeMillis = 1_000L,
        )
        val frame = AssistantAnimationDirector.frame(
            state = AssistantVisualState.IDLE,
            stateElapsedMillis = 1_000L,
            nowUptimeMillis = 1_000L,
            attentionSignal = attention,
        )

        assertEquals(AssistantEyeShape.DOWN_RIGHT, frame.eye)
        assertTrue(frame.gazeX > 0.9f)
        assertTrue(frame.gazeY > 0.75f)
        assertTrue(frame.headTiltDegrees > 1f)
    }

    @Test
    fun activationReaction_isBriefAndDoesNotReplaceStateErrors() {
        val attention = AssistantAttentionSignal.Idle
            .pointer(-0.7f, 0.1f, isPressed = false, nowUptimeMillis = 1_000L)
            .activate(nowUptimeMillis = 1_000L)
        val reacting = AssistantAnimationDirector.frame(
            state = AssistantVisualState.IDLE,
            stateElapsedMillis = 1_050L,
            nowUptimeMillis = 1_050L,
            attentionSignal = attention,
        )
        val expired = AssistantAnimationDirector.frame(
            state = AssistantVisualState.IDLE,
            stateElapsedMillis = 2_300L,
            nowUptimeMillis = 2_300L,
            attentionSignal = attention,
        )
        val error = AssistantAnimationDirector.frame(
            state = AssistantVisualState.ERROR,
            stateElapsedMillis = 1_050L,
            nowUptimeMillis = 1_050L,
            attentionSignal = attention,
        )

        assertEquals(AssistantEyeShape.WINK_LEFT, reacting.eye)
        assertEquals(AssistantMouthShape.Emotion(AssistantMouthEmotion.SMILE), reacting.mouth)
        assertTrue(expired.eye != AssistantEyeShape.WINK_LEFT)
        assertEquals(AssistantMouthShape.Emotion(AssistantMouthEmotion.NEUTRAL), expired.mouth)
        assertEquals(AssistantEyeShape.CONFUSED, error.eye)
        assertEquals(AssistantMouthShape.Emotion(AssistantMouthEmotion.FROWN), error.mouth)
    }

    @Test
    fun reducedMotion_keepsDirectAttentionButRemovesDecorativeTranslation() {
        val attention = AssistantAttentionSignal.Idle.pointer(
            normalizedX = -0.86f,
            normalizedY = 0f,
            isPressed = true,
            nowUptimeMillis = 4_000L,
        )
        val frame = AssistantAnimationDirector.frame(
            state = AssistantVisualState.IDLE,
            stateElapsedMillis = 4_000L,
            nowUptimeMillis = 4_000L,
            attentionSignal = attention,
            reducedMotion = true,
        )

        assertEquals(AssistantEyeShape.LEFT, frame.eye)
        assertTrue(frame.gazeX < -0.8f)
        assertEquals(0f, frame.bodyBob, 0.0001f)
        assertEquals(0f, frame.headTiltDegrees, 0.0001f)
        assertEquals(0, frame.portalFrame)
    }
}
