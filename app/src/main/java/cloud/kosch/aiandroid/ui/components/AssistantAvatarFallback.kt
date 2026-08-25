package cloud.kosch.aiandroid.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.model.AssistantVisualState
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.Mist
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Warm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Assistant avatar boundary.
 *
 * The historical name is retained to keep existing call sites stable. When a valid matrix-defined
 * body WebP exists in `src/main/assets/assistant/...`, it is rendered. Missing, malformed or
 * over-budget assets transparently fall back to the proven Canvas avatar.
 *
 * Eye and mouth files are already decoded by [AssistantAssetRuntime], but are intentionally not
 * composited in this stage: their 128 px face-anchor still needs calibration against the exported
 * 384 px body masters. Rendering a guessed anchor would be worse than the stable fallback.
 */
@Composable
fun AssistantAvatarFallback(
    state: AssistantVisualState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val runtime = remember(context.applicationContext) {
        AssistantAssetRuntime(context.applicationContext)
    }
    val sprite by produceState<AssistantSpriteFrame?>(
        initialValue = null,
        key1 = runtime,
        key2 = state,
    ) {
        value = withContext(Dispatchers.IO) { runtime.loadState(state) }
    }

    val body = sprite?.body
    if (body != null) {
        Image(
            bitmap = body.image,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    } else {
        AssistantCanvasFallback(state = state, modifier = modifier)
    }
}

@Composable
private fun AssistantCanvasFallback(
    state: AssistantVisualState,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "assistant-fallback")
    val blink by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4_600
                1f at 0
                1f at 3_650
                0.08f at 3_790
                1f at 3_940
                1f at 4_600
            },
        ),
        label = "blink",
    )
    val glance by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == AssistantVisualState.THINKING) 1_500 else 3_800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glance",
    )
    val mouthPulse by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == AssistantVisualState.SPEAKING) 240 else 1_200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mouth",
    )
    val accent = when (state) {
        AssistantVisualState.ERROR -> Warm
        AssistantVisualState.DISABLED -> Color(0xFF60717D)
        AssistantVisualState.OFFLINE -> Sky
        else -> Mint
    }

    Canvas(modifier = modifier) {
        val radius = 22.dp.toPx()
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(DeepSurface, Color(0xFF183344)),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
            cornerRadius = CornerRadius(radius, radius),
        )
        drawRoundRect(
            color = accent.copy(alpha = if (state == AssistantVisualState.DISABLED) 0.12f else 0.30f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
            cornerRadius = CornerRadius(radius, radius),
        )

        val effectiveBlink = if (state == AssistantVisualState.DISABLED) 0.16f else blink
        val eyeWidth = size.width * 0.25f
        val eyeHeight = size.height * 0.26f * effectiveBlink
        val eyeY = size.height * 0.24f + (size.height * 0.26f - eyeHeight) / 2f
        val leftX = size.width * 0.17f
        val rightX = size.width * 0.58f
        val eyeRadius = CornerRadius(eyeHeight / 2f, eyeHeight / 2f)
        drawRoundRect(
            Mist.copy(alpha = if (state == AssistantVisualState.DISABLED) 0.42f else 1f),
            Offset(leftX, eyeY),
            Size(eyeWidth, eyeHeight),
            eyeRadius,
        )
        drawRoundRect(
            Mist.copy(alpha = if (state == AssistantVisualState.DISABLED) 0.42f else 1f),
            Offset(rightX, eyeY),
            Size(eyeWidth, eyeHeight),
            eyeRadius,
        )

        if (effectiveBlink > 0.25f) {
            val pupilRadius = eyeHeight * 0.22f
            val pupilShift = glance * eyeWidth * 0.12f
            drawCircle(accent, pupilRadius, Offset(leftX + eyeWidth / 2f + pupilShift, eyeY + eyeHeight / 2f))
            drawCircle(accent, pupilRadius, Offset(rightX + eyeWidth / 2f + pupilShift, eyeY + eyeHeight / 2f))
        }

        val barWidth = size.width * 0.055f
        val gap = barWidth * 0.65f
        val centerX = size.width / 2f
        val baseY = size.height * 0.82f
        val talkScale = if (state == AssistantVisualState.SPEAKING) mouthPulse else 0.46f
        val heights = listOf(0.34f, 0.75f * talkScale, 1f * talkScale, 0.62f * talkScale, 0.30f)
        val totalWidth = heights.size * barWidth + (heights.size - 1) * gap
        heights.forEachIndexed { index, factor ->
            val height = size.height * 0.20f * factor.coerceAtLeast(0.12f)
            val x = centerX - totalWidth / 2f + index * (barWidth + gap)
            drawRoundRect(
                color = accent.copy(alpha = if (state == AssistantVisualState.DISABLED) 0.30f else 1f),
                topLeft = Offset(x, baseY - height),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}
