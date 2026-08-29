package cloud.kosch.aiandroid.model

/** Runtime input summary used only for presentation decisions and discoverability. */
data class AdaptiveInputCapabilities(
    val precisePointerDeviceCount: Int = 0,
    val hardwareKeyboardDeviceCount: Int = 0,
) {
    val hasPrecisePointer: Boolean get() = precisePointerDeviceCount > 0
    val hasHardwareKeyboard: Boolean get() = hardwareKeyboardDeviceCount > 0
}

data class AdaptiveInputDeviceSignal(
    val precisePointer: Boolean,
    val hardwareKeyboard: Boolean,
)

/**
 * Pure aggregation policy kept outside Android's InputDevice API so the classification contract is unit-testable.
 * Counts are useful for hot-plug transitions without retaining any device id, name, vendor id or descriptor.
 */
object AdaptiveInputCapabilityPolicy {
    fun summarize(signals: List<AdaptiveInputDeviceSignal>): AdaptiveInputCapabilities =
        AdaptiveInputCapabilities(
            precisePointerDeviceCount = signals.count(AdaptiveInputDeviceSignal::precisePointer),
            hardwareKeyboardDeviceCount = signals.count(AdaptiveInputDeviceSignal::hardwareKeyboard),
        )
}
