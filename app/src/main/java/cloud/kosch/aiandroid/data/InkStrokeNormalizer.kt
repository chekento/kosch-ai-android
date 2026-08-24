package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.InkPoint
import cloud.kosch.aiandroid.model.InkStroke
import kotlin.math.PI
import kotlin.math.roundToInt

/** Keeps persisted ink bounded while preserving the visible beginning and end of every stroke. */
object InkStrokeNormalizer {
    fun normalize(strokes: List<InkStroke>): List<InkStroke> = strokes
        .mapNotNull(::normalizeStroke)
        .takeLast(MAX_STROKES)

    fun normalizeStroke(stroke: InkStroke): InkStroke? {
        val sanitized = stroke.points.mapNotNull { point ->
            if (!point.x.isFinite() || !point.y.isFinite() || !point.pressure.isFinite() ||
                !point.tiltRadians.isFinite()
            ) {
                null
            } else {
                InkPoint(
                    x = point.x.coerceIn(0f, 1f),
                    y = point.y.coerceIn(0f, 1f),
                    pressure = point.pressure.coerceIn(0f, 1f),
                    tiltRadians = point.tiltRadians.coerceIn((-PI / 2).toFloat(), (PI / 2).toFloat()),
                )
            }
        }
        if (sanitized.isEmpty()) return null
        if (sanitized.size <= MAX_POINTS_PER_STROKE) return stroke.copy(points = sanitized)

        val lastIndex = sanitized.lastIndex
        val points = List(MAX_POINTS_PER_STROKE) { targetIndex ->
            val sourceIndex = (
                targetIndex.toDouble() * lastIndex.toDouble() / (MAX_POINTS_PER_STROKE - 1).toDouble()
            ).roundToInt().coerceIn(0, lastIndex)
            sanitized[sourceIndex]
        }
        return stroke.copy(points = points)
    }

    const val MAX_STROKES = 100
    const val MAX_POINTS_PER_STROKE = 2_048
}
