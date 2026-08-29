package cloud.kosch.aiandroid.system

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.view.InputDevice
import cloud.kosch.aiandroid.model.AdaptiveInputCapabilities
import cloud.kosch.aiandroid.model.AdaptiveInputCapabilityPolicy
import cloud.kosch.aiandroid.model.AdaptiveInputDeviceSignal

/**
 * Permission-free hot-plug monitor for desktop-class launcher input.
 *
 * Only aggregate capability booleans/counts leave this class. Device ids, names, vendor/product ids and descriptors
 * are never retained, logged or exposed to routing.
 */
class AdaptiveInputDeviceMonitor(
    context: Context,
    private val callbackHandler: Handler,
    private val onChanged: (AdaptiveInputCapabilities) -> Unit,
) : InputManager.InputDeviceListener {
    private val inputManager = context.getSystemService(InputManager::class.java)
    private var listening = false
    private var lastPublished = AdaptiveInputCapabilities()

    fun start() {
        if (listening) return
        inputManager.registerInputDeviceListener(this, callbackHandler)
        listening = true
        refreshDevices(force = true)
    }

    fun stop() {
        if (!listening) return
        inputManager.unregisterInputDeviceListener(this)
        listening = false
    }

    fun refreshDevices(force: Boolean = false) {
        val signals = inputManager.inputDeviceIds
            .map(inputManager::getInputDevice)
            .filterNotNull()
            .filterNot(InputDevice::isVirtual)
            .map { device ->
                AdaptiveInputDeviceSignal(
                    precisePointer = device.supportsSource(InputDevice.SOURCE_MOUSE) ||
                        device.supportsSource(InputDevice.SOURCE_MOUSE_RELATIVE) ||
                        device.supportsSource(InputDevice.SOURCE_TOUCHPAD),
                    hardwareKeyboard = device.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC,
                )
            }
        val next = AdaptiveInputCapabilityPolicy.summarize(signals)
        if (force || next != lastPublished) {
            lastPublished = next
            onChanged(next)
        }
    }

    override fun onInputDeviceAdded(deviceId: Int) = refreshDevices()

    override fun onInputDeviceRemoved(deviceId: Int) = refreshDevices()

    override fun onInputDeviceChanged(deviceId: Int) = refreshDevices()
}
