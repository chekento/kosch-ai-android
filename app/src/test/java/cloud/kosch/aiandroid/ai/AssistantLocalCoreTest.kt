package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.AssistantActionRisk
import cloud.kosch.aiandroid.model.AssistantVisualState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantLocalCoreTest {
    private val core = AssistantLocalCore()

    @Test
    fun launcherCommand_staysLocalAndCarriesRiskMetadata() {
        val reply = core.reply("Öffne Kamera")

        assertEquals(LauncherCommand.OpenCamera, reply.command)
        assertNull(reply.handoffPrompt)
        assertEquals(AssistantVisualState.WORKING, reply.visualState)
        assertEquals(AssistantActionRisk.EXTERNAL_SIDE_EFFECT, reply.actionRisk)
        assertTrue(reply.text.contains("Camera-Awareness", ignoreCase = true))
    }

    @Test
    fun voiceCommand_isMarkedSensitiveBeforeRuntimeCapture() {
        val reply = core.reply("Computer, starte Voice bitte")
        assertEquals(LauncherCommand.StartVoice, reply.command)
        assertEquals(AssistantActionRisk.SENSITIVE_SIDE_EFFECT, reply.actionRisk)
        assertTrue(reply.text.contains("Bestätigungsschritt", ignoreCase = true))
    }

    @Test
    fun freePrompt_requiresExplicitProviderHandoffAndCarriesExternalRisk() {
        val input = "Erkläre mir Quantenphysik in drei Absätzen"
        val reply = core.reply(input)

        assertNull(reply.command)
        assertEquals(input, reply.handoffPrompt)
        assertEquals(AssistantVisualState.OFFLINE, reply.visualState)
        assertEquals(AssistantActionRisk.EXTERNAL_SIDE_EFFECT, reply.actionRisk)
        assertTrue(reply.text.contains("kein direkt verwendbarer Provider", ignoreCase = true))
        assertTrue(reply.text.contains("AI Hub", ignoreCase = true))
    }

    @Test
    fun privacyAnswer_isLocalAndDoesNotCreateHandoff() {
        val reply = core.reply("Datenschutz")

        assertNull(reply.command)
        assertNull(reply.handoffPrompt)
        assertNull(reply.actionRisk)
        assertTrue(reply.text.contains("Sitzung"))
        assertTrue(reply.text.contains("ausdrücklich"))
    }

    @Test
    fun greeting_doesNotPretendToBeGenerativeModel() {
        val reply = core.reply("Hallo")

        assertNull(reply.command)
        assertNull(reply.handoffPrompt)
        assertTrue(reply.text.contains("Launcher-Befehle"))
        assertTrue(reply.text.contains("freigegebenen Provider", ignoreCase = true))
        assertTrue(reply.text.contains("AI Hub", ignoreCase = true))
    }
}
