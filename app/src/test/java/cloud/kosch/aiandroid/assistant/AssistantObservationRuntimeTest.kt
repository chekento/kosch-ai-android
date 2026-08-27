package cloud.kosch.aiandroid.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AssistantObservationRuntimeTest {
    @Before
    fun reset() {
        AssistantObservationRuntime.screenStopped()
        AssistantObservationRuntime.cameraStopped()
        AssistantObservationRuntime.clearScreenFailure()
    }

    @Test
    fun screenSession_tracksOnlyEphemeralTelemetryAndResetsOnStop() {
        AssistantObservationRuntime.screenStarted(width = 720, height = 1280)
        AssistantObservationRuntime.screenFrameObserved()
        AssistantObservationRuntime.screenFrameObserved()

        assertTrue(AssistantObservationRuntime.screenActive)
        assertEquals(720, AssistantObservationRuntime.screenWidth)
        assertEquals(1280, AssistantObservationRuntime.screenHeight)
        assertEquals(2L, AssistantObservationRuntime.screenFrameCount)

        AssistantObservationRuntime.screenStopped()

        assertFalse(AssistantObservationRuntime.screenActive)
        assertEquals(0, AssistantObservationRuntime.screenWidth)
        assertEquals(0, AssistantObservationRuntime.screenHeight)
        assertEquals(0L, AssistantObservationRuntime.screenFrameCount)
    }

    @Test
    fun cameraSession_tracksFramesOnlyWhileActive() {
        AssistantObservationRuntime.cameraFrameObserved()
        assertEquals(0L, AssistantObservationRuntime.cameraFrameCount)

        AssistantObservationRuntime.cameraStarted()
        AssistantObservationRuntime.cameraFrameObserved()
        assertTrue(AssistantObservationRuntime.cameraActive)
        assertEquals(1L, AssistantObservationRuntime.cameraFrameCount)

        AssistantObservationRuntime.cameraStopped()
        assertFalse(AssistantObservationRuntime.cameraActive)
        assertEquals(0L, AssistantObservationRuntime.cameraFrameCount)
    }

    @Test
    fun screenFailure_isGenerationBasedAndMessageCanBeConsumed() {
        val before = AssistantObservationRuntime.screenFailureGeneration
        AssistantObservationRuntime.screenStarted(600, 900)
        AssistantObservationRuntime.screenFailed("projection failed")

        assertFalse(AssistantObservationRuntime.screenActive)
        assertEquals(before + 1L, AssistantObservationRuntime.screenFailureGeneration)
        assertEquals("projection failed", AssistantObservationRuntime.screenFailureMessage)

        AssistantObservationRuntime.clearScreenFailure()
        assertNull(AssistantObservationRuntime.screenFailureMessage)
    }
}
