package cloud.kosch.aiandroid.assistant

import cloud.kosch.aiandroid.model.AssistantObservationSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AssistantVisualContextRuntimeTest {
    @Before
    fun reset() {
        AssistantVisualContextRuntime.resetForTest()
    }

    @Test
    fun request_canBeClaimedExactlyOnceByMatchingSource() {
        val requestId = AssistantVisualContextRuntime.request(AssistantObservationSource.SCREEN)

        assertNull(AssistantVisualContextRuntime.claimCapture(AssistantObservationSource.CAMERA))
        assertEquals(requestId, AssistantVisualContextRuntime.claimCapture(AssistantObservationSource.SCREEN))
        assertNull(AssistantVisualContextRuntime.claimCapture(AssistantObservationSource.SCREEN))
    }

    @Test
    fun publishedFrame_isBoundedReadyAndConsumedOnce() {
        val requestId = AssistantVisualContextRuntime.request(AssistantObservationSource.CAMERA)
        assertEquals(requestId, AssistantVisualContextRuntime.claimCapture(AssistantObservationSource.CAMERA))
        val bytes = ByteArray(32 * 1024) { index -> (index % 251).toByte() }

        assertTrue(
            AssistantVisualContextRuntime.publishJpeg(
                requestId = requestId,
                source = AssistantObservationSource.CAMERA,
                width = 960,
                height = 540,
                rotationDegrees = 90,
                jpegBytes = bytes,
            ),
        )
        assertEquals(AssistantVisualContextRuntime.Status.READY, AssistantVisualContextRuntime.status)
        assertEquals(32 * 1024, AssistantVisualContextRuntime.metadata?.byteCount)

        val snapshot = AssistantVisualContextRuntime.consume()
        assertEquals(960, snapshot?.metadata?.width)
        assertEquals(540, snapshot?.metadata?.height)
        assertEquals(90, snapshot?.metadata?.rotationDegrees)
        assertEquals(bytes.size, snapshot?.jpegBytes?.size)
        assertEquals(AssistantVisualContextRuntime.Status.IDLE, AssistantVisualContextRuntime.status)
        assertNull(AssistantVisualContextRuntime.consume())
    }

    @Test
    fun oversizedFrame_isRejectedAndPayloadIsNotRetained() {
        val requestId = AssistantVisualContextRuntime.request(AssistantObservationSource.SCREEN)
        AssistantVisualContextRuntime.claimCapture(AssistantObservationSource.SCREEN)

        assertFalse(
            AssistantVisualContextRuntime.publishJpeg(
                requestId = requestId,
                source = AssistantObservationSource.SCREEN,
                width = 1280,
                height = 720,
                rotationDegrees = 0,
                jpegBytes = ByteArray(AssistantVisualContextRuntime.MAX_CONTEXT_BYTES + 1),
            ),
        )
        assertEquals(AssistantVisualContextRuntime.Status.FAILED, AssistantVisualContextRuntime.status)
        assertNull(AssistantVisualContextRuntime.metadata)
        assertNull(AssistantVisualContextRuntime.consume())
    }

    @Test
    fun endingMatchingSession_cancelsOnlyPendingRequest() {
        AssistantVisualContextRuntime.request(AssistantObservationSource.CAMERA)
        AssistantVisualContextRuntime.cancel(
            AssistantObservationSource.SCREEN,
            "wrong source",
        )
        assertEquals(AssistantVisualContextRuntime.Status.REQUESTED, AssistantVisualContextRuntime.status)

        AssistantVisualContextRuntime.cancel(
            AssistantObservationSource.CAMERA,
            "camera closed",
        )
        assertEquals(AssistantVisualContextRuntime.Status.FAILED, AssistantVisualContextRuntime.status)
        assertEquals("camera closed", AssistantVisualContextRuntime.failureMessage)
    }
}
