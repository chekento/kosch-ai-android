package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.InkPoint
import cloud.kosch.aiandroid.model.InkStroke
import cloud.kosch.aiandroid.model.InkTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PenAiContextPlannerTest {
    @Test
    fun summarize_emitsAggregateDescriptionWithoutCoordinates() {
        val strokes = listOf(
            InkStroke(
                tool = InkTool.PEN,
                points = listOf(
                    InkPoint(0.10f, 0.20f, 0.4f, 0.1f),
                    InkPoint(0.50f, 0.70f, 0.8f, 0.2f),
                ),
            ),
            InkStroke(
                tool = InkTool.HIGHLIGHTER,
                points = listOf(InkPoint(0.20f, 0.40f, 0.6f, 0.0f)),
            ),
        )

        val summary = PenAiContextPlanner.summarize(strokes)
        assertEquals(2, summary.strokeCount)
        assertEquals(3, summary.pointCount)
        assertTrue(InkTool.PEN in summary.tools)
        assertTrue(InkTool.HIGHLIGHTER in summary.tools)
        assertEquals(40, summary.horizontalCoveragePercent)
        assertEquals(50, summary.verticalCoveragePercent)
        assertTrue(summary.text.contains("2 Striche"))
        assertFalse(summary.text.contains("0.10"))
        assertFalse(summary.text.contains("0.70"))
    }

    @Test
    fun emptySketch_isSafeAndDescriptive() {
        val summary = PenAiContextPlanner.summarize(emptyList())
        assertEquals(0, summary.strokeCount)
        assertEquals(0, summary.pointCount)
        assertTrue(summary.text.contains("0 Striche"))
    }

    @Test
    fun lassoSelection_returnsOnlyAggregateSelectedContent() {
        val inside = InkStroke(
            tool = InkTool.PEN,
            points = listOf(
                InkPoint(0.25f, 0.25f, 0.6f, 0f),
                InkPoint(0.30f, 0.30f, 0.7f, 0f),
            ),
        )
        val outside = InkStroke(
            tool = InkTool.HIGHLIGHTER,
            points = listOf(
                InkPoint(0.80f, 0.80f, 0.5f, 0f),
                InkPoint(0.90f, 0.90f, 0.5f, 0f),
            ),
        )
        val lasso = rectangleLasso()

        val result = PenAiContextPlanner.summarizeLassoSelection(listOf(inside, outside), lasso)
        assertEquals(1, result.selectedStrokeCount)
        assertEquals(2, result.totalStrokeCount)
        assertEquals(50, result.selectionSharePercent)
        assertEquals(1, result.selected.strokeCount)
        assertTrue(InkTool.PEN in result.selected.tools)
        assertFalse(InkTool.HIGHLIGHTER in result.selected.tools)
        assertTrue(result.text.contains("1 von 2"))
        assertFalse(result.text.contains("0.25"))
        assertFalse(result.text.contains("0.50"))
    }

    @Test
    fun lassoSelection_detectsCrossingSegmentWithoutInteriorSamplePoint() {
        val crossing = InkStroke(
            tool = InkTool.PEN,
            points = listOf(
                InkPoint(0.0f, 0.30f, 0.5f, 0f),
                InkPoint(0.80f, 0.30f, 0.5f, 0f),
            ),
        )
        val outside = InkStroke(
            tool = InkTool.HIGHLIGHTER,
            points = listOf(
                InkPoint(0.70f, 0.80f, 0.5f, 0f),
                InkPoint(0.90f, 0.80f, 0.5f, 0f),
            ),
        )

        val result = PenAiContextPlanner.summarizeLassoSelection(
            listOf(crossing, outside),
            rectangleLasso(),
        )

        assertEquals(1, result.selectedStrokeCount)
        assertTrue(InkTool.PEN in result.selected.tools)
        assertFalse(InkTool.HIGHLIGHTER in result.selected.tools)
    }

    @Test
    fun invalidLasso_failsClosedWithEmptySelection() {
        val strokes = listOf(
            InkStroke(
                tool = InkTool.PEN,
                points = listOf(InkPoint(0.25f, 0.25f, 0.5f, 0f)),
            ),
        )
        val result = PenAiContextPlanner.summarizeLassoSelection(
            strokes,
            listOf(
                InkPoint(0.1f, 0.1f, 0f, 0f),
                InkPoint(0.2f, 0.2f, 0f, 0f),
            ),
        )
        assertEquals(0, result.selectedStrokeCount)
        assertEquals(0, result.selectionSharePercent)
    }

    @Test
    fun oversizedLasso_failsClosedInsteadOfDoingUnboundedGeometryWork() {
        val stroke = InkStroke(
            tool = InkTool.PEN,
            points = listOf(InkPoint(0.25f, 0.25f, 0.5f, 0f)),
        )
        val oversized = List(2_049) { index ->
            val x = if (index % 2 == 0) 0.1f else 0.5f
            InkPoint(x, 0.2f, 0f, 0f)
        }

        val result = PenAiContextPlanner.summarizeLassoSelection(listOf(stroke), oversized)

        assertEquals(0, result.selectedStrokeCount)
        assertEquals(0, result.selectionSharePercent)
    }

    private fun rectangleLasso() = listOf(
        InkPoint(0.10f, 0.10f, 0f, 0f),
        InkPoint(0.50f, 0.10f, 0f, 0f),
        InkPoint(0.50f, 0.50f, 0f, 0f),
        InkPoint(0.10f, 0.50f, 0f, 0f),
    )
}
