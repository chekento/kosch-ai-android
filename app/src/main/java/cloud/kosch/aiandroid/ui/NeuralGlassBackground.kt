package cloud.kosch.aiandroid.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import cloud.kosch.aiandroid.ui.theme.Ink
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Violet
import kotlin.math.cos
import kotlin.math.sin

/** Code-native living background. It is intentionally a neutral visual layer, not an LCARS theme. */
@Composable
fun NeuralGlassBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "neural-glass")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(18_000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ambient-phase",
    )

    Canvas(modifier) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF183E4D), Ink),
                center = Offset(size.width * (0.18f + phase * 0.12f), size.height * 0.12f),
                radius = size.maxDimension * 0.88f,
            ),
        )

        val center = Offset(size.width * 0.72f, size.height * 0.34f)
        val radius = size.minDimension * 0.28f
        val nodes = List(7) { index ->
            val angle = index * (Math.PI * 2 / 7) + phase * 0.7
            Offset(
                x = center.x + cos(angle).toFloat() * radius * (0.72f + (index % 3) * 0.12f),
                y = center.y + sin(angle).toFloat() * radius,
            )
        }
        nodes.forEachIndexed { index, node ->
            val next = nodes[(index + 2) % nodes.size]
            drawLine(
                color = Sky.copy(alpha = 0.08f),
                start = node,
                end = next,
                strokeWidth = 1.2f,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = if (index % 2 == 0) Mint.copy(alpha = 0.14f) else Violet.copy(alpha = 0.10f),
                radius = 5f + phase * 3f,
                center = node,
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Mint.copy(alpha = 0.08f), Color.Transparent),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }
}
