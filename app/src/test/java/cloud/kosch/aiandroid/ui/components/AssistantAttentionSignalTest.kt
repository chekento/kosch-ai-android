package cloud.kosch.aiandroid.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantAttentionSignalTest {
    @Test
    fun pointerTarget_isFiniteNormalizedAndPressedCueHasFullWeight() {
        val signal = AssistantAttentionSignal.Idle.pointer(
            normalizedX = -4f,
            normalizedY = Float.NaN,
            isPressed = true,
            nowUptimeMillis = 100L,
        )

        assertEquals(-1f, signal.targetX, 0.0001f)
        assertEquals(0f, signal.targetY, 0.0001f)
        assertEquals(1f, signal.trackingWeight(50_000L), 0.0001f)
    }

    @Test
    fun releasedCue_lingersSmoothlyThenExpires() {
        val signal = AssistantAttentionSignal.Idle.pointer(
            normalizedX = 0.7f,
            normalizedY = -0.4f,
            isPressed = false,
            nowUptimeMillis = 1_000L,
        )

        assertEquals(1f, signal.trackingWeight(1_000L), 0.0001f)
        assertTrue(signal.trackingWeight(1_575L) in 0.45f..0.55f)
        assertEquals(0f, signal.trackingWeight(2_150L), 0.0001f)
    }

    @Test
    fun avatarCoordinates_mapCenterAndEdgesWithoutLeavingBounds() {
        val size = IntSize(width = 200, height = 100)

        assertEquals(Offset.Zero, Offset(100f, 50f).normalizedIn(size))
        assertEquals(Offset(-1f, -1f), Offset(-50f, -20f).normalizedIn(size))
        assertEquals(Offset(1f, 1f), Offset(300f, 180f).normalizedIn(size))
        assertEquals(Offset.Zero, Offset(10f, 10f).normalizedIn(IntSize.Zero))
    }
}
