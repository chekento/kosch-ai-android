package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.AssistantAgentPreferences
import cloud.kosch.aiandroid.model.AssistantPresenceMode
import cloud.kosch.aiandroid.model.AssistantWakeWordMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantPortablePreferencesCodecTest {
    @Test
    fun portableRoundTrip_keepsPersonaButDropsObservationAndActions() {
        val source = AssistantAgentPreferences(
            characterId = "anime_female",
            assistantName = "Aira",
            presenceMode = AssistantPresenceMode.FULL_COMPANION,
            wakeWordMode = AssistantWakeWordMode.ASSISTANT_NAME,
            customWakeWord = "Aira",
            localWakeWordOnly = true,
            screenObservationEnabled = true,
            cameraObservationEnabled = true,
            actionExecutionEnabled = true,
            confirmationRequiredForExternalActions = false,
        )

        val decoded = AssistantPortablePreferencesCodec.decode(
            AssistantPortablePreferencesCodec.encode(source),
        )

        assertEquals("anime_female", decoded.characterId)
        assertEquals("Aira", decoded.assistantName)
        assertEquals(AssistantPresenceMode.FULL_COMPANION, decoded.presenceMode)
        assertEquals(AssistantWakeWordMode.ASSISTANT_NAME, decoded.wakeWordMode)
        assertTrue(decoded.localWakeWordOnly)
        assertFalse(decoded.screenObservationEnabled)
        assertFalse(decoded.cameraObservationEnabled)
        assertFalse(decoded.actionExecutionEnabled)
        assertTrue(decoded.confirmationRequiredForExternalActions)
    }

    @Test
    fun wireFormat_cannotRepresentSensitiveRuntimeOptIns() {
        val payload = AssistantPortablePreferencesCodec.encode(
            AssistantAgentPreferences(
                screenObservationEnabled = true,
                cameraObservationEnabled = true,
                actionExecutionEnabled = true,
            ),
        )
        assertFalse(payload.contains("screen", ignoreCase = true))
        assertFalse(payload.contains("camera", ignoreCase = true))
        assertFalse(payload.contains("actionExecution", ignoreCase = true))
    }
}
