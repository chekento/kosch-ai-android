package cloud.kosch.aiandroid.ui.components

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import cloud.kosch.aiandroid.model.AssistantVisualState
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Violet
import cloud.kosch.aiandroid.ui.theme.Warm
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.min

/**
 * Interim character-aware renderer used until the matrix-calibrated WebP packs are exported.
 *
 * The selected character must already be visually truthful in the APK: choosing Astra/Orion may not
 * silently render the Default robot. These fallbacks deliberately keep the same state, gaze and
 * speech signals as the final asset runtime so later WebP packs can replace only presentation.
 */
internal enum class AssistantProceduralCharacter {
    ANIME_FEMALE,
    ANIME_MALE,
}

internal object AssistantProceduralCharacterResolver {
    fun resolve(assistantId: String): AssistantProceduralCharacter? = when (assistantId) {
        "anime_female" -> AssistantProceduralCharacter.ANIME_FEMALE
        "anime_male" -> AssistantProceduralCharacter.ANIME_MALE
        else -> null
    }
}

@Composable
internal fun AssistantCharacterProceduralFallback(
    assistantId: String,
    state: AssistantVisualState,
    speechSignal: AssistantSpeechSignal,
    attentionSignal: AssistantAttentionSignal,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val character = remember(assistantId) { AssistantProceduralCharacterResolver.resolve(assistantId) }
        ?: return
    var nowUptimeMillis by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }

    LaunchedEffect(reducedMotion, speechSignal.active, attentionSignal.updatedAtUptimeMillis, attentionSignal.activatedAtUptimeMillis) {
        nowUptimeMillis = SystemClock.uptimeMillis()
        if (reducedMotion) return@LaunchedEffect
        while (isActive) {
            delay(FRAME_MILLIS)
            nowUptimeMillis = SystemClock.uptimeMillis()
        }
    }

    val gazeWeight = attentionSignal.trackingWeight(nowUptimeMillis)
    val reactionWeight = attentionSignal.reactionWeight(nowUptimeMillis)
    val viseme = speechSignal.currentViseme(nowUptimeMillis, reducedMotion)

    Canvas(modifier = modifier) {
        val unit = min(size.width, size.height)
        if (unit <= 0f) return@Canvas
        val centerX = size.width / 2f
        val portalY = size.height * 0.86f
        val accent = stateAccent(state)
        val glow = when (state) {
            AssistantVisualState.DISABLED -> 0.30f
            AssistantVisualState.ERROR -> 0.82f
            AssistantVisualState.THINKING, AssistantVisualState.SPEAKING -> 0.96f
            else -> 0.72f
        }

        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.30f * glow), Color.Transparent),
                center = Offset(centerX, portalY),
                radius = unit * 0.34f,
            ),
            topLeft = Offset(centerX - unit * 0.34f, portalY - unit * 0.085f),
            size = Size(unit * 0.68f, unit * 0.17f),
        )
        drawOval(
            color = accent.copy(alpha = 0.92f * glow),
            topLeft = Offset(centerX - unit * 0.25f, portalY - unit * 0.038f),
            size = Size(unit * 0.50f, unit * 0.076f),
            style = Stroke(width = unit * 0.010f),
        )
        if (state == AssistantVisualState.DISABLED) return@Canvas

        val headCenter = Offset(centerX, size.height * 0.38f)
        val headRadiusX = unit * if (character == AssistantProceduralCharacter.ANIME_FEMALE) 0.205f else 0.198f
        val headRadiusY = unit * 0.235f
        val hair = if (character == AssistantProceduralCharacter.ANIME_FEMALE) {
            Color(0xFF17233A)
        } else {
            Color(0xFF202739)
        }
        val skin = Color(0xFFFFDCCB)
        val shell = Color(0xFFE9F0F7)
        val darkShell = Color(0xFF192638)

        // Hair silhouette sits behind the face and makes the two interim characters unmistakably different.
        drawOval(
            color = hair,
            topLeft = Offset(headCenter.x - headRadiusX * 1.18f, headCenter.y - headRadiusY * 1.18f),
            size = Size(headRadiusX * 2.36f, headRadiusY * 2.48f),
        )
        if (character == AssistantProceduralCharacter.ANIME_FEMALE) {
            drawRoundRect(
                color = hair,
                topLeft = Offset(headCenter.x - headRadiusX * 1.12f, headCenter.y + headRadiusY * 0.05f),
                size = Size(headRadiusX * 0.45f, headRadiusY * 1.55f),
                cornerRadius = CornerRadius(unit * 0.06f),
            )
            drawRoundRect(
                color = hair,
                topLeft = Offset(headCenter.x + headRadiusX * 0.67f, headCenter.y + headRadiusY * 0.05f),
                size = Size(headRadiusX * 0.45f, headRadiusY * 1.55f),
                cornerRadius = CornerRadius(unit * 0.06f),
            )
        } else {
            val spikes = Path().apply {
                moveTo(headCenter.x - headRadiusX, headCenter.y - headRadiusY * 0.72f)
                lineTo(headCenter.x - headRadiusX * 0.55f, headCenter.y - headRadiusY * 1.34f)
                lineTo(headCenter.x - headRadiusX * 0.24f, headCenter.y - headRadiusY * 0.95f)
                lineTo(headCenter.x + headRadiusX * 0.08f, headCenter.y - headRadiusY * 1.42f)
                lineTo(headCenter.x + headRadiusX * 0.36f, headCenter.y - headRadiusY * 0.96f)
                lineTo(headCenter.x + headRadiusX * 0.82f, headCenter.y - headRadiusY * 1.25f)
                lineTo(headCenter.x + headRadiusX, headCenter.y - headRadiusY * 0.55f)
                close()
            }
            drawPath(spikes, hair)
        }

        drawOval(
            color = skin,
            topLeft = Offset(headCenter.x - headRadiusX, headCenter.y - headRadiusY),
            size = Size(headRadiusX * 2f, headRadiusY * 2f),
        )

        // Futuristic fringe / headset keeps Astra and Orion in the same visual universe as the default assistant.
        drawArc(
            color = accent.copy(alpha = 0.88f),
            startAngle = 205f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = Offset(headCenter.x - headRadiusX * 1.03f, headCenter.y - headRadiusY * 1.03f),
            size = Size(headRadiusX * 2.06f, headRadiusY * 2.06f),
            style = Stroke(width = unit * 0.014f),
        )
        drawCircle(accent.copy(alpha = 0.94f), radius = unit * 0.028f, center = Offset(headCenter.x - headRadiusX * 1.06f, headCenter.y))
        drawCircle(accent.copy(alpha = 0.94f), radius = unit * 0.028f, center = Offset(headCenter.x + headRadiusX * 1.06f, headCenter.y))

        val trackingX = attentionSignal.targetX * gazeWeight * unit * 0.016f
        val trackingY = attentionSignal.targetY * gazeWeight * unit * 0.012f
        val eyeY = headCenter.y - headRadiusY * 0.08f
        val eyeSpread = headRadiusX * 0.47f
        val eyeWhiteRadius = unit * 0.043f
        val irisRadius = unit * 0.022f
        val wink = reactionWeight > 0.36f

        drawOval(
            color = Color.White.copy(alpha = 0.94f),
            topLeft = Offset(headCenter.x - eyeSpread - eyeWhiteRadius, eyeY - eyeWhiteRadius * 0.70f),
            size = Size(eyeWhiteRadius * 2f, eyeWhiteRadius * 1.40f),
        )
        if (wink) {
            drawLine(
                color = hair,
                start = Offset(headCenter.x + eyeSpread - eyeWhiteRadius, eyeY),
                end = Offset(headCenter.x + eyeSpread + eyeWhiteRadius, eyeY),
                strokeWidth = unit * 0.012f,
            )
        } else {
            drawOval(
                color = Color.White.copy(alpha = 0.94f),
                topLeft = Offset(headCenter.x + eyeSpread - eyeWhiteRadius, eyeY - eyeWhiteRadius * 0.70f),
                size = Size(eyeWhiteRadius * 2f, eyeWhiteRadius * 1.40f),
            )
        }
        drawCircle(
            color = accent,
            radius = irisRadius,
            center = Offset(headCenter.x - eyeSpread + trackingX, eyeY + trackingY),
        )
        if (!wink) {
            drawCircle(
                color = accent,
                radius = irisRadius,
                center = Offset(headCenter.x + eyeSpread + trackingX, eyeY + trackingY),
            )
        }

        val mouthCenter = Offset(headCenter.x, headCenter.y + headRadiusY * 0.48f)
        val speaking = speechSignal.active && viseme != AssistantViseme.SIL
        val mouthWidth = unit * if (speaking) 0.085f else 0.070f
        val mouthHeight = unit * if (speaking) {
            (0.030f + speechSignal.amplitude * 0.040f).coerceAtLeast(0.040f)
        } else if (state == AssistantVisualState.ERROR) {
            0.010f
        } else {
            0.016f
        }
        drawRoundRect(
            color = if (speaking) accent else Color(0xFF884D58),
            topLeft = Offset(mouthCenter.x - mouthWidth / 2f, mouthCenter.y - mouthHeight / 2f),
            size = Size(mouthWidth, mouthHeight),
            cornerRadius = CornerRadius(mouthHeight),
        )

        val torsoTop = headCenter.y + headRadiusY * 0.92f
        val torsoWidth = unit * if (character == AssistantProceduralCharacter.ANIME_FEMALE) 0.36f else 0.40f
        val torsoHeight = unit * 0.29f
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(shell, Color(0xFFB9C8D6))),
            topLeft = Offset(centerX - torsoWidth / 2f, torsoTop),
            size = Size(torsoWidth, torsoHeight),
            cornerRadius = CornerRadius(unit * 0.08f),
        )
        drawRoundRect(
            color = darkShell,
            topLeft = Offset(centerX - torsoWidth * 0.31f, torsoTop + torsoHeight * 0.19f),
            size = Size(torsoWidth * 0.62f, torsoHeight * 0.36f),
            cornerRadius = CornerRadius(unit * 0.035f),
        )
        drawCircle(
            color = accent.copy(alpha = 0.96f),
            radius = unit * 0.032f,
            center = Offset(centerX, torsoTop + torsoHeight * 0.37f),
        )

        when (state) {
            AssistantVisualState.LISTENING -> drawCircle(
                color = accent.copy(alpha = 0.32f),
                radius = unit * 0.30f,
                center = headCenter,
                style = Stroke(width = unit * 0.010f),
            )
            AssistantVisualState.THINKING -> repeat(3) { index ->
                drawCircle(
                    color = accent.copy(alpha = 0.85f - index * 0.18f),
                    radius = unit * (0.020f - index * 0.003f),
                    center = Offset(
                        headCenter.x + headRadiusX * (0.82f + index * 0.30f),
                        headCenter.y - headRadiusY * (0.72f + index * 0.20f),
                    ),
                )
            }
            AssistantVisualState.ERROR -> drawLine(
                color = Warm,
                start = Offset(centerX - unit * 0.055f, torsoTop + torsoHeight * 0.70f),
                end = Offset(centerX + unit * 0.055f, torsoTop + torsoHeight * 0.70f),
                strokeWidth = unit * 0.014f,
            )
            else -> Unit
        }
    }
}

private fun stateAccent(state: AssistantVisualState): Color = when (state) {
    AssistantVisualState.ERROR -> Warm
    AssistantVisualState.OFFLINE -> Sky
    AssistantVisualState.THINKING -> Violet
    AssistantVisualState.DISABLED -> Color(0xFF60717D)
    else -> Mint
}

private const val FRAME_MILLIS = 33L
