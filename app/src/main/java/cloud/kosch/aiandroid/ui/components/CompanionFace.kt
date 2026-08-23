package cloud.kosch.aiandroid.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.Mist
import cloud.kosch.aiandroid.ui.theme.Sky

@Composable
fun CompanionFace(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "companion-idle")
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
            animation = tween(durationMillis = 3_800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glance",
    )
    val mouthPulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mouth",
    )

    Box(
        modifier = modifier
            .semantics {
                role = Role.Button
                contentDescription = "KI-Begleiter, Spracheingabe starten"
            }
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
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
                color = Mint.copy(alpha = 0.24f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                cornerRadius = CornerRadius(radius, radius),
            )

            val eyeWidth = size.width * 0.25f
            val eyeHeight = size.height * 0.26f * blink
            val eyeY = size.height * 0.24f + (size.height * 0.26f - eyeHeight) / 2f
            val leftX = size.width * 0.17f
            val rightX = size.width * 0.58f
            val eyeRadius = CornerRadius(eyeHeight / 2f, eyeHeight / 2f)
            drawRoundRect(Mist, Offset(leftX, eyeY), Size(eyeWidth, eyeHeight), eyeRadius)
            drawRoundRect(Mist, Offset(rightX, eyeY), Size(eyeWidth, eyeHeight), eyeRadius)

            if (blink > 0.25f) {
                val pupilRadius = eyeHeight * 0.22f
                val pupilShift = glance * eyeWidth * 0.12f
                drawCircle(
                    color = Sky,
                    radius = pupilRadius,
                    center = Offset(leftX + eyeWidth / 2f + pupilShift, eyeY + eyeHeight / 2f),
                )
                drawCircle(
                    color = Sky,
                    radius = pupilRadius,
                    center = Offset(rightX + eyeWidth / 2f + pupilShift, eyeY + eyeHeight / 2f),
                )
            }

            val barWidth = size.width * 0.055f
            val gap = barWidth * 0.65f
            val centerX = size.width / 2f
            val baseY = size.height * 0.82f
            val heights = listOf(0.38f, 0.75f * mouthPulse, 1f, 0.62f * mouthPulse, 0.34f)
            val totalWidth = heights.size * barWidth + (heights.size - 1) * gap
            heights.forEachIndexed { index, factor ->
                val height = size.height * 0.20f * factor
                val x = centerX - totalWidth / 2f + index * (barWidth + gap)
                drawRoundRect(
                    color = Mint,
                    topLeft = Offset(x, baseY - height),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                )
            }
        }
    }
}

