package cloud.kosch.aiandroid.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AssistantEnableCoordinatorTest {
    @Test
    fun disable_stopsEveryLiveModalityBeforeBothRuntimes() {
        val calls = mutableListOf<String>()

        AssistantEnableCoordinator.apply(
            enabled = false,
            stopSpeech = { calls += "speech:stop" },
            stopScreenSession = { calls += "screen:stop" },
            stopCameraSession = { calls += "camera:stop" },
            setSessionEnabled = { calls += "session:$it" },
            setAgentEnabled = { calls += "agent:$it" },
        )

        assertEquals(
            listOf(
                "speech:stop",
                "screen:stop",
                "camera:stop",
                "session:false",
                "agent:false",
            ),
            calls,
        )
    }

    @Test
    fun enable_updatesBothRuntimesWithoutTouchingCaptureStops() {
        val calls = mutableListOf<String>()

        AssistantEnableCoordinator.apply(
            enabled = true,
            stopSpeech = { calls += "speech:stop" },
            stopScreenSession = { calls += "screen:stop" },
            stopCameraSession = { calls += "camera:stop" },
            setSessionEnabled = { calls += "session:$it" },
            setAgentEnabled = { calls += "agent:$it" },
        )

        assertEquals(listOf("session:true", "agent:true"), calls)
    }
}
