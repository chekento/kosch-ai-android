package cloud.kosch.aiandroid.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

class WcagContrastTest {
    @Test
    fun staticFallbackTextPairsMeetNormalTextGate() {
        val pairs = listOf(
            0xD8F3F0 to 0x071018, // Mist on Ink
            0xD8F3F0 to 0x0E1A24, // Mist on DeepSurface
            0x9DB7B5 to 0x0E1A24, // MutedMist on DeepSurface
            0x69E6D7 to 0x162733, // Mint on RaisedSurface
            0x80BFFF to 0x162733, // Sky on RaisedSurface
            0xB7A7FF to 0x162733, // Violet on RaisedSurface
        )

        pairs.forEach { (foreground, background) ->
            assertTrue(
                "Contrast ${WcagContrast.ratio(foreground, background)} is below 4.5",
                WcagContrast.ratio(foreground, background) >= 4.5,
            )
        }
    }
}
