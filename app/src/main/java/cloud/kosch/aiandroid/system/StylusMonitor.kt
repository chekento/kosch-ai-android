package cloud.kosch.aiandroid.system

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.view.InputDevice
import android.view.MotionEvent
import cloud.kosch.aiandroid.model.StylusCapabilities
import cloud.kosch.aiandroid.model.StylusTool
import kotlin.math.abs

class StylusMonitor(
    context: Context,
    private val callbackHandler: Handler,
    private val onChanged: (StylusCapabilities) -> Unit,
) : InputManager.InputDeviceListener {
    private val inputManager = context.getSystemService(InputManager::class.java)
    private var listening = false
    private var lastPublished = StylusCapabilities()
    private var lastSampleAt = 0L

    fun start() {
        if (listening) return
        inputManager.registerInputDeviceListener(this, callbackHandler)
        listening = true
        refreshDevices()
    }

    fun stop() {
        if (!listening) return
        inputManager.unregisterInputDeviceListener(this)
        listening = false
    }

    fun refreshDevices() {
        val devices = inputManager.inputDeviceIds
            .map(inputManager::getInputDevice)
            .filterNotNull()
        val styluses = devices.filter { device ->
            device.supportsSource(InputDevice.SOURCE_STYLUS) ||
                device.supportsSource(InputDevice.SOURCE_BLUETOOTH_STYLUS)
        }
        val detected = lastPublished.copy(
            present = styluses.isNotEmpty(),
            active = if (styluses.isEmpty()) false else lastPublished.active,
            deviceCount = styluses.size,
            supportsPressure = styluses.any { it.hasRange(MotionEvent.AXIS_PRESSURE) },
            supportsTilt = styluses.any { it.hasRange(MotionEvent.AXIS_TILT) },
            supportsHover = styluses.any {
                it.hasRange(MotionEvent.AXIS_DISTANCE) || it.supportsSource(InputDevice.SOURCE_STYLUS)
            },
            supportsBluetooth = styluses.any {
                it.supportsSource(InputDevice.SOURCE_BLUETOOTH_STYLUS)
            },
            lastTool = if (styluses.isEmpty()) StylusTool.NONE else lastPublished.lastTool,
        )
        publish(detected, force = true)
    }

    fun observe(event: MotionEvent) {
        val pointerIndex = event.actionIndex.coerceIn(0, event.pointerCount.coerceAtLeast(1) - 1)
        val tool = when (event.getToolType(pointerIndex)) {
            MotionEvent.TOOL_TYPE_STYLUS -> StylusTool.PEN
            MotionEvent.TOOL_TYPE_ERASER -> StylusTool.ERASER
            else -> return
        }
        val now = event.eventTime
        val isTerminal = event.actionMasked == MotionEvent.ACTION_UP ||
            event.actionMasked == MotionEvent.ACTION_CANCEL ||
            event.actionMasked == MotionEvent.ACTION_HOVER_EXIT
        val active = !isTerminal
        val pressure = event.getPressure(pointerIndex).coerceIn(0f, 1f)
        val tilt = event.getAxisValue(MotionEvent.AXIS_TILT, pointerIndex)
        val orientation = event.getAxisValue(MotionEvent.AXIS_ORIENTATION, pointerIndex)
        val button = event.buttonState and (
            MotionEvent.BUTTON_STYLUS_PRIMARY or MotionEvent.BUTTON_STYLUS_SECONDARY
            ) != 0

        val next = lastPublished.copy(
            present = true,
            active = active,
            supportsPressure = lastPublished.supportsPressure || pressure > 0f,
            supportsTilt = lastPublished.supportsTilt || abs(tilt) > 0.001f,
            supportsHover = lastPublished.supportsHover || event.actionMasked in HOVER_ACTIONS,
            lastTool = tool,
            pressure = pressure,
            tiltRadians = tilt,
            orientationRadians = orientation,
            barrelButtonPressed = button,
        )
        val materialChange = next.active != lastPublished.active ||
            next.lastTool != lastPublished.lastTool ||
            next.barrelButtonPressed != lastPublished.barrelButtonPressed ||
            abs(next.pressure - lastPublished.pressure) >= PRESSURE_STEP ||
            abs(next.tiltRadians - lastPublished.tiltRadians) >= TILT_STEP
        if (materialChange || now - lastSampleAt >= SAMPLE_INTERVAL_MS) {
            lastSampleAt = now
            publish(next)
        }
    }

    override fun onInputDeviceAdded(deviceId: Int) = refreshDevices()

    override fun onInputDeviceRemoved(deviceId: Int) = refreshDevices()

    override fun onInputDeviceChanged(deviceId: Int) = refreshDevices()

    private fun publish(value: StylusCapabilities, force: Boolean = false) {
        if (!force && value == lastPublished) return
        lastPublished = value
        onChanged(value)
    }

    private fun InputDevice.hasRange(axis: Int): Boolean =
        getMotionRange(axis, InputDevice.SOURCE_STYLUS) != null || getMotionRange(axis) != null

    private companion object {
        const val SAMPLE_INTERVAL_MS = 48L
        const val PRESSURE_STEP = 0.04f
        const val TILT_STEP = 0.04f
        val HOVER_ACTIONS = setOf(
            MotionEvent.ACTION_HOVER_ENTER,
            MotionEvent.ACTION_HOVER_MOVE,
            MotionEvent.ACTION_HOVER_EXIT,
        )
    }
}
