package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.InkPoint
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

data class PenAiLassoSummary(
    val selected: PenAiContextSummary,
    val selectedStrokeCount: Int,
    val totalStrokeCount: Int,
    val selectionSharePercent: Int,
) {
    val text: String
        get() = buildString {
            append("Lasso-Auswahl: ")
                .append(selectedStrokeCount)
                .append(" von ")
                .append(totalStrokeCount)
                .append(" Strichen · ca. ")
                .append(selectionSharePercent)
                .append("% der Striche ausgewählt")
            if (selectedStrokeCount > 0) append(" · ").append(selected.text)
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

    /**
     * Privacy-first Circle/Lasso-to-Ask core.
     *
     * The polygon exists only as an input to this local calculation. The returned object contains no lasso vertices,
     * no selected point coordinates and no SVG. A caller may turn the textual aggregate into an explicit
     * AiContextHandoff draft, which still requires the normal user confirmation before any AI route sees it.
     */
    fun summarizeLassoSelection(
        strokes: List<InkStroke>,
        lassoPoints: List<InkPoint>,
    ): PenAiLassoSummary {
        val validPolygon = lassoPoints
            .filter { it.x.isFinite() && it.y.isFinite() }
            .map { Point2(it.x.coerceIn(0f, 1f), it.y.coerceIn(0f, 1f)) }
        if (validPolygon.size < MIN_LASSO_POINTS) {
            return PenAiLassoSummary(
                selected = summarize(emptyList()),
                selectedStrokeCount = 0,
                totalStrokeCount = strokes.size,
                selectionSharePercent = 0,
            )
        }

        val selectedStrokes = strokes.filter { stroke ->
            stroke.points.any { point ->
                point.x.isFinite() && point.y.isFinite() &&
                    pointInPolygon(Point2(point.x, point.y), validPolygon)
            }
        }
        val share = if (strokes.isEmpty()) 0 else {
            ((selectedStrokes.size.toFloat() / strokes.size.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        }
        return PenAiLassoSummary(
            selected = summarize(selectedStrokes),
            selectedStrokeCount = selectedStrokes.size,
            totalStrokeCount = strokes.size,
            selectionSharePercent = share,
        )
    }

    private fun pointInPolygon(point: Point2, polygon: List<Point2>): Boolean {
        var inside = false
        var previous = polygon.last()
        polygon.forEach { current ->
            val crosses = (current.y > point.y) != (previous.y > point.y)
            if (crosses) {
                val denominator = previous.y - current.y
                if (denominator != 0f) {
                    val intersectionX = (previous.x - current.x) * (point.y - current.y) / denominator + current.x
                    if (point.x < intersectionX) inside = !inside
                }
            }
            previous = current
        }
        return inside
    }

    private data class Point2(val x: Float, val y: Float)

    private const val MIN_LASSO_POINTS = 3
}
