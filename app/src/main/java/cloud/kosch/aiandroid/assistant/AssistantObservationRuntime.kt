package cloud.kosch.aiandroid.assistant

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Process-only telemetry for explicit observation sessions.
 *
 * No frame pixels, projection tokens, camera handles or consent results are persisted here. The
 * runtime exists only so visible Assistant UI can truthfully mirror whether Android capture is
 * actually active and whether frames are flowing.
 */
object AssistantObservationRuntime {
    var screenActive by mutableStateOf(false)
        private set
    var screenFrameCount by mutableLongStateOf(0L)
        private set
    var screenWidth by mutableStateOf(0)
        private set
    var screenHeight by mutableStateOf(0)
        private set

    var cameraActive by mutableStateOf(false)
        private set
    var cameraFrameCount by mutableLongStateOf(0L)
        private set

    fun screenStarted(width: Int, height: Int) {
        screenWidth = width.coerceAtLeast(0)
        screenHeight = height.coerceAtLeast(0)
        screenFrameCount = 0L
        screenActive = true
    }

    fun screenFrameObserved() {
        if (screenActive) screenFrameCount += 1L
    }

    fun screenStopped() {
        screenActive = false
        screenWidth = 0
        screenHeight = 0
        screenFrameCount = 0L
    }

    fun cameraStarted() {
        cameraFrameCount = 0L
        cameraActive = true
    }

    fun cameraFrameObserved() {
        if (cameraActive) cameraFrameCount += 1L
    }

    fun cameraStopped() {
        cameraActive = false
        cameraFrameCount = 0L
    }
}
