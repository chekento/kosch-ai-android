package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.InkPoint
import cloud.kosch.aiandroid.model.InkStroke
import cloud.kosch.aiandroid.model.InkTool
import kotlin.math.abs
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
     *
     * Selection includes strokes with a sampled point inside the polygon and strokes whose line segments cross the
     * polygon boundary. This avoids missing fast/long pen segments whose endpoints happen to lie outside the lasso.
     */
    fun summarizeLassoSelection(
        strokes: List<InkStroke>,
        lassoPoints: List<InkPoint>,
    ): PenAiLassoSummary {
        val finitePolygonPoints = lassoPoints.filter { it.x.isFinite() && it.y.isFinite() }
        if (finitePolygonPoints.size !in MIN_LASSO_POINTS..MAX_LASSO_POINTS) {
            return emptyLassoSummary(strokes.size)
        }
        val polygon = finitePolygonPoints.map {
            Point2(it.x.coerceIn(0f, 1f), it.y.coerceIn(0f, 1f))
        }

        val selectedStrokes = strokes.filter { strokeTouchesPolygon(it, polygon) }
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

    private fun emptyLassoSummary(totalStrokeCount: Int) = PenAiLassoSummary(
        selected = summarize(emptyList()),
        selectedStrokeCount = 0,
        totalStrokeCount = totalStrokeCount,
        selectionSharePercent = 0,
    )

    private fun strokeTouchesPolygon(stroke: InkStroke, polygon: List<Point2>): Boolean {
        val points = stroke.points
            .asSequence()
            .filter { it.x.isFinite() && it.y.isFinite() }
            .map { Point2(it.x.coerceIn(0f, 1f), it.y.coerceIn(0f, 1f)) }
            .toList()
        if (points.isEmpty()) return false
        if (points.any { pointInPolygon(it, polygon) }) return true
        if (points.size < 2) return false
        return points.zipWithNext().any { (start, end) ->
            segmentIntersectsPolygon(start, end, polygon)
        }
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

    private fun segmentIntersectsPolygon(start: Point2, end: Point2, polygon: List<Point2>): Boolean {
        var previous = polygon.last()
        polygon.forEach { current ->
            if (segmentsIntersect(start, end, previous, current)) return true
            previous = current
        }
        return false
    }

    private fun segmentsIntersect(a: Point2, b: Point2, c: Point2, d: Point2): Boolean {
        val o1 = cross(a, b, c)
        val o2 = cross(a, b, d)
        val o3 = cross(c, d, a)
        val o4 = cross(c, d, b)
        val properIntersection = oppositeSigns(o1, o2) && oppositeSigns(o3, o4)
        if (properIntersection) return true
        if (abs(o1) <= GEOMETRY_EPSILON && pointOnSegment(c, a, b)) return true
        if (abs(o2) <= GEOMETRY_EPSILON && pointOnSegment(d, a, b)) return true
        if (abs(o3) <= GEOMETRY_EPSILON && pointOnSegment(a, c, d)) return true
        if (abs(o4) <= GEOMETRY_EPSILON && pointOnSegment(b, c, d)) return true
        return false
    }

    private fun cross(a: Point2, b: Point2, c: Point2): Float =
        (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)

    private fun oppositeSigns(a: Float, b: Float): Boolean =
        (a > GEOMETRY_EPSILON && b < -GEOMETRY_EPSILON) ||
            (a < -GEOMETRY_EPSILON && b > GEOMETRY_EPSILON)

    private fun pointOnSegment(point: Point2, start: Point2, end: Point2): Boolean =
        point.x >= minOf(start.x, end.x) - GEOMETRY_EPSILON &&
            point.x <= maxOf(start.x, end.x) + GEOMETRY_EPSILON &&
            point.y >= minOf(start.y, end.y) - GEOMETRY_EPSILON &&
            point.y <= maxOf(start.y, end.y) + GEOMETRY_EPSILON

    private data class Point2(val x: Float, val y: Float)

    private const val MIN_LASSO_POINTS = 3
    private const val MAX_LASSO_POINTS = 2_048
    private const val GEOMETRY_EPSILON = 0.000_01f
}
