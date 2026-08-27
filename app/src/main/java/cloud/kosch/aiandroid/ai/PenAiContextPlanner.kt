package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.InkStroke
import cloud.kosch.aiandroid.model.InkTool
import kotlin.math.roundToInt

/**
 * Privacy-minimal local description of Pen Space content.
 *
 * Raw coordinates are never emitted. The planner only exposes aggregate geometry/tool statistics so a later AI
 * handoff can remain useful even without sending SVG/vector data.
 */
data class PenAiContextSummary(
    val strokeCount: Int,
    val pointCount: Int,
    val tools: Set<InkTool>,
    val averagePressure: Float?,
    val horizontalCoveragePercent: Int?,
    val verticalCoveragePercent: Int?,
) {
    val text: String
        get() = buildString {
            append(strokeCount).append(if (strokeCount == 1) " Strich" else " Striche")
            append(" · ").append(pointCount).append(if (pointCount == 1) " Punkt" else " Punkte")
            if (tools.isNotEmpty()) {
                append(" · Werkzeuge: ").append(tools.joinToString { it.title })
            }
            averagePressure?.let {
                append(" · mittlerer Druck ").append((it * 100f).roundToInt()).append('%')
            }
            if (horizontalCoveragePercent != null && verticalCoveragePercent != null) {
                append(" · grobe Flächennutzung ")
                    .append(horizontalCoveragePercent)
                    .append("% × ")
                    .append(verticalCoveragePercent)
                    .append('%')
            }
        }
}

object PenAiContextPlanner {
    fun summarize(strokes: List<InkStroke>): PenAiContextSummary {
        val points = strokes.flatMap(InkStroke::points)
        val pressureValues = points.map { it.pressure }.filter { it.isFinite() && it >= 0f }
        val finiteX = points.map { it.x }.filter(Float::isFinite)
        val finiteY = points.map { it.y }.filter(Float::isFinite)

        fun coverage(values: List<Float>): Int? {
            if (values.size < 2) return null
            val min = values.minOrNull() ?: return null
            val max = values.maxOrNull() ?: return null
            // Pen Space coordinates are normalized by the drawing surface today. Clamp derived coverage defensively.
            return ((max - min).coerceIn(0f, 1f) * 100f).roundToInt()
        }

        return PenAiContextSummary(
            strokeCount = strokes.size,
            pointCount = points.size,
            tools = strokes.map(InkStroke::tool).toSet(),
            averagePressure = pressureValues.takeIf(List<Float>::isNotEmpty)?.average()?.toFloat()?.coerceIn(0f, 1f),
            horizontalCoveragePercent = coverage(finiteX),
            verticalCoveragePercent = coverage(finiteY),
        )
    }
}
