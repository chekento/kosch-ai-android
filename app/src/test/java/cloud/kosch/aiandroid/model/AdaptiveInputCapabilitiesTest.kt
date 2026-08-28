package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveInputCapabilitiesTest {
    @Test
    fun summarize_keepsOnlyAggregateCapabilities() {
        val result = AdaptiveInputCapabilityPolicy.summarize(
            listOf(
                AdaptiveInputDeviceSignal(precisePointer = true, hardwareKeyboard = false),
                AdaptiveInputDeviceSignal(precisePointer = true, hardwareKeyboard = true),
                AdaptiveInputDeviceSignal(precisePointer = false, hardwareKeyboard = true),
            ),
        )

        assertEquals(2, result.precisePointerDeviceCount)
        assertEquals(2, result.hardwareKeyboardDeviceCount)
        assertTrue(result.hasPrecisePointer)
        assertTrue(result.hasHardwareKeyboard)
    }

    @Test
    fun summarize_emptyInput_isTouchOnly() {
        val result = AdaptiveInputCapabilityPolicy.summarize(emptyList())

        assertEquals(0, result.precisePointerDeviceCount)
        assertEquals(0, result.hardwareKeyboardDeviceCount)
        assertFalse(result.hasPrecisePointer)
        assertFalse(result.hasHardwareKeyboard)
    }
}
