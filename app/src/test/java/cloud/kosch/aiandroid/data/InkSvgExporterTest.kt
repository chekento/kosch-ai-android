package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.InkPoint
import cloud.kosch.aiandroid.model.InkStroke
import cloud.kosch.aiandroid.model.InkTool
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InkSvgExporterTest {
    @Test
    fun `export is standalone svg and never serializes invalid numbers`() {
        val svg = InkSvgExporter.export(
            listOf(
                InkStroke(
                    InkTool.PEN,
                    listOf(InkPoint(0f, 0f, 0.2f, 0f), InkPoint(1f, 1f, 0.8f, 0f)),
                ),
            ),
            width = 100,
            height = 200,
        ).decodeToString()

        assertTrue(svg.startsWith("<?xml"))
        assertTrue(svg.contains("viewBox=\"0 0 100 200\""))
        assertTrue(svg.contains("M 0.000 0.000 L 100.000 200.000"))
        assertFalse(svg.contains("NaN"))
    }
}
