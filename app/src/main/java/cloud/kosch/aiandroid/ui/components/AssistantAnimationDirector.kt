package cloud.kosch.aiandroid.ui.components

import cloud.kosch.aiandroid.model.AssistantVisualState
import kotlin.math.PI
import kotlin.math.sin

enum class AssistantEyeShape(val assetSuffix: String) {
    CENTER("center"),
    LEFT("left"),
    RIGHT("right"),
    UP("up"),
    DOWN("down"),
    UP_LEFT("up_left"),
    UP_RIGHT("up_right"),
    DOWN_LEFT("down_left"),
    DOWN_RIGHT("down_right"),
    BLINK_OPEN("blink_open"),
    BLINK_HALF_1("blink_half_1"),
    BLINK_CLOSED("blink_closed"),
    BLINK_HALF_2("blink_half_2"),
    HAPPY("happy"),
    SAD("sad"),
    SURPRISED("surprised"),
    ANGRY("angry"),
    CONFUSED("confused"),
    LOVE("love"),
    SLEEPY("sleepy"),
    EXCITED("excited"),
    WORRIED("worried"),
    FOCUS("focus"),
    WINK_LEFT("wink_left"),
    WINK_RIGHT("wink_right"),
}

enum class AssistantMouthEmotion(val assetSuffix: String) {
    NEUTRAL("neutral"),
    SMILE("smile"),
    GRIN("grin"),
    LAUGH("laugh"),
    SAD("sad"),
    FROWN("frown"),
    SURPRISED("surprised"),
    YAWN("yawn"),
}

sealed interface AssistantMouthShape {
    data class Viseme(val value: AssistantViseme) : AssistantMouthShape
    data class Emotion(val value: AssistantMouthEmotion) : AssistantMouthShape
}

/** One deterministic frame shared by the WebP compositor and the safe Canvas fallback. */
data class AssistantAnimationFrame(
    val eye: AssistantEyeShape,
    val mouth: AssistantMouthShape,
    val gazeX: Float,
    val gazeY: Float,
    val eyeOpen: Float,
    val mouthOpen: Float,
    val bodyScale: Float,
    val bodyBob: Float,
    val headTiltDegrees: Float,
    val glow: Float,
    val portalFrame: Int,
)

/**
 * Small deterministic behavior director. It avoids random jitter, keeps eye/mouth layers independent
 * of the body pose and can be unit-tested without a device or an animation clock.
 */
object AssistantAnimationDirector {
    fun frame(
        state: AssistantVisualState,
        stateElapsedMillis: Long,
        nowUptimeMillis: Long,
        speechSignal: AssistantSpeechSignal = AssistantSpeechSignal.Idle,
        attentionSignal: AssistantAttentionSignal = AssistantAttentionSignal.Idle,
        reducedMotion: Boolean = false,
    ): AssistantAnimationFrame {
        val elapsed = stateElapsedMillis.coerceAtLeast(0L)
        val baseEye = baseEye(state, elapsed, reducedMotion)
        val attentionAllowed = state != AssistantVisualState.DISABLED && state != AssistantVisualState.ERROR
        val trackingWeight = if (attentionAllowed) attentionSignal.trackingWeight(nowUptimeMillis) else 0f
        val reactionWeight = if (state == AssistantVisualState.IDLE) {
            attentionSignal.reactionWeight(nowUptimeMillis)
        } else {
            0f
        }
        val attentionWeight = maxOf(trackingWeight, reactionWeight * 0.82f)
        val eye = when {
            state == AssistantVisualState.DISABLED || state == AssistantVisualState.ERROR -> baseEye
            reactionWeight > 0f -> if (attentionSignal.targetX <= 0f) {
                AssistantEyeShape.WINK_LEFT
            } else {
                AssistantEyeShape.WINK_RIGHT
            }
            state == AssistantVisualState.IDLE && trackingWeight > 0.08f -> gazeShape(
                attentionSignal.targetX,
                attentionSignal.targetY,
            )
            reducedMotion -> baseEye
            else -> blinkFrame(elapsed, blinkPeriod(state)) ?: baseEye
        }
        val mouth = when (state) {
            AssistantVisualState.SPEAKING -> AssistantMouthShape.Viseme(
                speechSignal.currentViseme(nowUptimeMillis, reducedMotion),
            )
            AssistantVisualState.IDLE -> {
                val quietSmile = !reducedMotion && elapsed % 14_000L in 11_900L..13_100L
                AssistantMouthShape.Emotion(
                    if (reactionWeight > 0f || quietSmile) {
                        AssistantMouthEmotion.SMILE
                    } else {
                        AssistantMouthEmotion.NEUTRAL
                    },
                )
            }
            AssistantVisualState.OFFLINE -> AssistantMouthShape.Emotion(AssistantMouthEmotion.SAD)
            AssistantVisualState.ERROR -> AssistantMouthShape.Emotion(AssistantMouthEmotion.FROWN)
            else -> AssistantMouthShape.Emotion(AssistantMouthEmotion.NEUTRAL)
        }

        val phase = elapsed / 1_000.0 * 2.0 * PI
        val bodyScale = if (reducedMotion || state == AssistantVisualState.DISABLED) {
            1f
        } else {
            (1.0 + sin(phase / 2.7) * 0.009).toFloat()
        }
        val bodyBob = if (reducedMotion || state == AssistantVisualState.DISABLED) {
            0f
        } else {
            (sin(phase / 3.1) * when (state) {
                AssistantVisualState.LISTENING -> 0.006
                AssistantVisualState.THINKING -> 0.010
                AssistantVisualState.SPEAKING -> 0.013
                AssistantVisualState.WORKING -> 0.008
                else -> 0.007
            }).toFloat()
        }
        val baseHeadTilt = if (reducedMotion) {
            when (state) {
                AssistantVisualState.THINKING -> -2.2f
                AssistantVisualState.ERROR -> 2.2f
                else -> 0f
            }
        } else {
            when (state) {
                AssistantVisualState.THINKING -> (-2.4 + sin(phase / 2.2) * 1.0).toFloat()
                AssistantVisualState.SPEAKING -> (sin(phase / 2.8) * 1.4).toFloat()
                AssistantVisualState.ERROR -> 2.2f
                else -> (sin(phase / 4.6) * 0.55).toFloat()
            }
        }
        val headTilt = if (reducedMotion || !attentionAllowed) {
            baseHeadTilt
        } else {
            (baseHeadTilt + attentionSignal.targetX * attentionWeight * 2.4f).coerceIn(-4.5f, 4.5f)
        }
        val baseGlow = if (reducedMotion) {
            stateGlow(state)
        } else {
            (stateGlow(state) + sin(phase / 1.9).toFloat() * 0.08f).coerceIn(0.18f, 1f)
        }
        val glow = (baseGlow + attentionWeight * 0.08f).coerceIn(0.18f, 1f)
        val ambientGaze = ambientGaze(state, elapsed, reducedMotion, baseEye)
        val gazeX = lerp(ambientGaze.x, attentionSignal.targetX, attentionWeight)
        val gazeY = lerp(ambientGaze.y, attentionSignal.targetY, attentionWeight)

        return AssistantAnimationFrame(
            eye = eye,
            mouth = mouth,
            gazeX = gazeX,
            gazeY = gazeY,
            eyeOpen = eye.openness(),
            mouthOpen = mouth.openness(speechSignal.amplitude),
            bodyScale = bodyScale,
            bodyBob = bodyBob,
            headTiltDegrees = headTilt,
            glow = glow,
            portalFrame = if (reducedMotion) 0 else ((elapsed / 95L) % 8L).toInt(),
        )
    }

    private fun baseEye(
        state: AssistantVisualState,
        elapsedMillis: Long,
        reducedMotion: Boolean,
    ): AssistantEyeShape = when (state) {
        AssistantVisualState.DISABLED -> AssistantEyeShape.BLINK_CLOSED
        AssistantVisualState.LISTENING -> AssistantEyeShape.FOCUS
        AssistantVisualState.THINKING -> if (reducedMotion || (elapsedMillis / 1_250L) % 2L == 0L) {
            AssistantEyeShape.UP_RIGHT
        } else {
            AssistantEyeShape.UP_LEFT
        }
        AssistantVisualState.SPEAKING -> AssistantEyeShape.HAPPY
        AssistantVisualState.WORKING -> AssistantEyeShape.FOCUS
        AssistantVisualState.OFFLINE -> AssistantEyeShape.WORRIED
        AssistantVisualState.ERROR -> AssistantEyeShape.CONFUSED
        AssistantVisualState.IDLE -> when {
            reducedMotion -> AssistantEyeShape.CENTER
            else -> when ((elapsedMillis / 1_180L) % 8L) {
                2L -> AssistantEyeShape.LEFT
                5L -> AssistantEyeShape.RIGHT
                7L -> AssistantEyeShape.UP_RIGHT
                else -> AssistantEyeShape.CENTER
            }
        }
    }

    private fun blinkPeriod(state: AssistantVisualState): Long = when (state) {
        AssistantVisualState.LISTENING -> 4_300L
        AssistantVisualState.SPEAKING -> 4_900L
        AssistantVisualState.THINKING -> 6_100L
        AssistantVisualState.WORKING -> 5_300L
        else -> 5_700L
    }

    private fun blinkFrame(elapsedMillis: Long, periodMillis: Long): AssistantEyeShape? {
        val local = elapsedMillis % periodMillis
        val cycle = elapsedMillis / periodMillis
        val primary = blinkAt(local - (periodMillis - 230L))
        if (primary != null) return primary
        return if (cycle % 4L == 3L) blinkAt(local - (periodMillis - 510L)) else null
    }

    private fun blinkAt(positionMillis: Long): AssistantEyeShape? = when (positionMillis) {
        in 0L..49L -> AssistantEyeShape.BLINK_HALF_1
        in 50L..119L -> AssistantEyeShape.BLINK_CLOSED
        in 120L..229L -> AssistantEyeShape.BLINK_HALF_2
        else -> null
    }

    private fun stateGlow(state: AssistantVisualState): Float = when (state) {
        AssistantVisualState.DISABLED -> 0.24f
        AssistantVisualState.IDLE -> 0.58f
        AssistantVisualState.LISTENING -> 0.92f
        AssistantVisualState.THINKING -> 0.74f
        AssistantVisualState.SPEAKING -> 0.88f
        AssistantVisualState.WORKING -> 0.78f
        AssistantVisualState.OFFLINE -> 0.48f
        AssistantVisualState.ERROR -> 0.82f
    }

    private fun ambientGaze(
        state: AssistantVisualState,
        elapsedMillis: Long,
        reducedMotion: Boolean,
        baseEye: AssistantEyeShape,
    ): AssistantGaze {
        if (reducedMotion) return AssistantGaze(baseEye.gazeX(), baseEye.gazeY())
        val targets = when (state) {
            AssistantVisualState.IDLE -> idleGazeTargets
            AssistantVisualState.THINKING -> thinkingGazeTargets
            else -> return AssistantGaze(baseEye.gazeX(), baseEye.gazeY())
        }
        val slotMillis = if (state == AssistantVisualState.IDLE) 1_180L else 1_250L
        val index = ((elapsedMillis / slotMillis) % targets.size).toInt()
        val previousIndex = if (elapsedMillis < slotMillis) 0 else (index - 1 + targets.size) % targets.size
        val localProgress = ((elapsedMillis % slotMillis).toFloat() / 230f).coerceIn(0f, 1f)
        val eased = localProgress * localProgress * (3f - 2f * localProgress)
        return AssistantGaze(
            x = lerp(targets[previousIndex].x, targets[index].x, eased),
            y = lerp(targets[previousIndex].y, targets[index].y, eased),
        )
    }

    private val idleGazeTargets = listOf(
        AssistantGaze(0f, 0f),
        AssistantGaze(0f, 0f),
        AssistantGaze(-1f, 0f),
        AssistantGaze(0f, 0f),
        AssistantGaze(0f, 0f),
        AssistantGaze(1f, 0f),
        AssistantGaze(0f, 0f),
        AssistantGaze(0.75f, -0.72f),
    )

    private val thinkingGazeTargets = listOf(
        AssistantGaze(0.78f, -0.82f),
        AssistantGaze(-0.78f, -0.82f),
    )
}

private data class AssistantGaze(val x: Float, val y: Float)

private fun gazeShape(x: Float, y: Float): AssistantEyeShape {
    val horizontal = when {
        x < -0.34f -> -1
        x > 0.34f -> 1
        else -> 0
    }
    val vertical = when {
        y < -0.34f -> -1
        y > 0.34f -> 1
        else -> 0
    }
    return when (horizontal to vertical) {
        -1 to -1 -> AssistantEyeShape.UP_LEFT
        0 to -1 -> AssistantEyeShape.UP
        1 to -1 -> AssistantEyeShape.UP_RIGHT
        -1 to 0 -> AssistantEyeShape.LEFT
        1 to 0 -> AssistantEyeShape.RIGHT
        -1 to 1 -> AssistantEyeShape.DOWN_LEFT
        0 to 1 -> AssistantEyeShape.DOWN
        1 to 1 -> AssistantEyeShape.DOWN_RIGHT
        else -> AssistantEyeShape.CENTER
    }
}

private fun lerp(start: Float, end: Float, amount: Float): Float =
    start + (end - start) * amount.coerceIn(0f, 1f)

private fun AssistantEyeShape.gazeX(): Float = when (this) {
    AssistantEyeShape.LEFT,
    AssistantEyeShape.UP_LEFT,
    AssistantEyeShape.DOWN_LEFT,
    -> -1f
    AssistantEyeShape.RIGHT,
    AssistantEyeShape.UP_RIGHT,
    AssistantEyeShape.DOWN_RIGHT,
    -> 1f
    else -> 0f
}

private fun AssistantEyeShape.gazeY(): Float = when (this) {
    AssistantEyeShape.UP,
    AssistantEyeShape.UP_LEFT,
    AssistantEyeShape.UP_RIGHT,
    -> -1f
    AssistantEyeShape.DOWN,
    AssistantEyeShape.DOWN_LEFT,
    AssistantEyeShape.DOWN_RIGHT,
    -> 1f
    else -> 0f
}

private fun AssistantEyeShape.openness(): Float = when (this) {
    AssistantEyeShape.BLINK_CLOSED -> 0.06f
    AssistantEyeShape.BLINK_HALF_1,
    AssistantEyeShape.BLINK_HALF_2,
    AssistantEyeShape.SLEEPY,
    -> 0.48f
    AssistantEyeShape.HAPPY -> 0.66f
    else -> 1f
}

private fun AssistantMouthShape.openness(amplitude: Float): Float = when (this) {
    is AssistantMouthShape.Emotion -> when (value) {
        AssistantMouthEmotion.NEUTRAL -> 0.08f
        AssistantMouthEmotion.SMILE -> 0.20f
        AssistantMouthEmotion.GRIN -> 0.36f
        AssistantMouthEmotion.LAUGH -> 0.84f
        AssistantMouthEmotion.SAD -> 0.16f
        AssistantMouthEmotion.FROWN -> 0.18f
        AssistantMouthEmotion.SURPRISED -> 0.72f
        AssistantMouthEmotion.YAWN -> 0.92f
    }
    is AssistantMouthShape.Viseme -> {
        val base = when (value) {
            AssistantViseme.SIL -> 0.04f
            AssistantViseme.PP -> 0.10f
            AssistantViseme.FF -> 0.20f
            AssistantViseme.TH -> 0.26f
            AssistantViseme.DD -> 0.32f
            AssistantViseme.KK -> 0.44f
            AssistantViseme.CH -> 0.48f
            AssistantViseme.SS -> 0.25f
            AssistantViseme.NN -> 0.18f
            AssistantViseme.RR -> 0.40f
            AssistantViseme.AA -> 1f
            AssistantViseme.E -> 0.72f
            AssistantViseme.IH -> 0.46f
            AssistantViseme.OH -> 0.78f
            AssistantViseme.OU -> 0.64f
        }
        (base * (0.68f + amplitude * 0.42f)).coerceIn(0.04f, 1f)
    }
}
