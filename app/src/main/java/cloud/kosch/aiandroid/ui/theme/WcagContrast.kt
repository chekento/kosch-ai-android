package cloud.kosch.aiandroid.ui.theme

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Pure helper used to keep the static fallback palette above WCAG text contrast gates. */
object WcagContrast {
    fun ratio(foregroundRgb: Int, backgroundRgb: Int): Double {
        val foreground = luminance(foregroundRgb)
        val background = luminance(backgroundRgb)
        return (max(foreground, background) + 0.05) / (min(foreground, background) + 0.05)
    }

    private fun luminance(rgb: Int): Double {
        fun channel(shift: Int): Double {
            val normalized = ((rgb shr shift) and 0xFF) / 255.0
            return if (normalized <= 0.04045) normalized / 12.92
            else ((normalized + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
