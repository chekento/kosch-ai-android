package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.InkPoint
import cloud.kosch.aiandroid.model.InkStroke
import cloud.kosch.aiandroid.model.InkTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InkStrokeNormalizerTest {
    @Test
    fun `long stroke is resampled and keeps both visible endpoints`() {
        val points = List(5_000) { index ->
            InkPoint(index / 4_999f, index / 4_999f, 0.5f, 0f)
        }

        val result = InkStrokeNormalizer.normalizeStroke(InkStroke(InkTool.PEN, points))!!

        assertEquals(InkStrokeNormalizer.MAX_POINTS_PER_STROKE, result.points.size)
        assertEquals(points.first(), result.points.first())
        assertEquals(points.last(), result.points.last())
    }

    @Test
    fun `invalid points are removed and valid values are clamped`() {
        val result = InkStrokeNormalizer.normalizeStroke(
            InkStroke(
                InkTool.HIGHLIGHTER,
                listOf(
                    InkPoint(Float.NaN, 0.2f, 0.5f, 0f),
                    InkPoint(2f, -1f, 4f, 9f),
                ),
            ),
        )!!

        assertEquals(1, result.points.size)
        assertEquals(1f, result.points.single().x)
        assertEquals(0f, result.points.single().y)
        assertEquals(1f, result.points.single().pressure)
        assertTrue(result.points.single().tiltRadians.isFinite())
    }
}
