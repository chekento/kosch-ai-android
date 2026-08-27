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
}
