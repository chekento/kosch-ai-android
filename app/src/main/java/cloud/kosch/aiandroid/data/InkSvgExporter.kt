package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.InkStroke
import cloud.kosch.aiandroid.model.InkTool
import java.nio.charset.StandardCharsets
import java.util.Locale

/** Exports Pen Space as portable vector data without requesting storage permissions. */
object InkSvgExporter {
    fun export(
        strokes: List<InkStroke>,
        width: Int = 1_440,
        height: Int = 1_920,
    ): ByteArray {
        require(width in 1..8_192 && height in 1..8_192)
        val normalized = InkStrokeNormalizer.normalize(strokes)
        val body = buildString {
            normalized.forEach { stroke ->
                appendStroke(stroke, width, height)
            }
        }
        return buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 $width $height\" ")
            append("width=\"$width\" height=\"$height\" role=\"img\" aria-label=\"KoSch Pen Space export\">\n")
            append("<rect width=\"100%\" height=\"100%\" fill=\"#07141D\"/>\n")
            append(body)
            append("</svg>\n")
        }.toByteArray(StandardCharsets.UTF_8)
    }

    private fun StringBuilder.appendStroke(stroke: InkStroke, width: Int, height: Int) {
        if (stroke.points.isEmpty() || stroke.tool == InkTool.ERASER) return
        val color = when (stroke.tool) {
            InkTool.PEN -> "#D8F3F0"
            InkTool.HIGHLIGHTER -> "#B7A7FF"
            InkTool.ERASER -> return
        }
        val opacity = if (stroke.tool == InkTool.HIGHLIGHTER) "0.44" else "1"
        val averagePressure = stroke.points.map { if (it.pressure <= 0f) 0.45f else it.pressure }.average()
        val baseWidth = if (stroke.tool == InkTool.HIGHLIGHTER) 15.0 else 3.2
        val strokeWidth = baseWidth * (0.48 + averagePressure * 1.05)
        val path = stroke.points.mapIndexed { index, point ->
            val command = if (index == 0) "M" else "L"
            "$command ${format(point.x * width)} ${format(point.y * height)}"
        }.joinToString(" ")
        append("<path d=\"").append(path).append("\" fill=\"none\" stroke=\"")
            .append(color).append("\" stroke-opacity=\"").append(opacity)
            .append("\" stroke-width=\"").append(format(strokeWidth))
            .append("\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>\n")
    }

    private fun format(value: Number): String = String.format(Locale.ROOT, "%.3f", value.toDouble())
}
