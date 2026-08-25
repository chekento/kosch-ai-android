package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.AssistantVisualState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantLocalCoreTest {
    private val core = AssistantLocalCore()

    @Test
    fun launcherCommand_staysLocalAndExecutable() {
        val reply = core.reply("Öffne Kamera")

        assertEquals(LauncherCommand.OpenCamera, reply.command)
        assertNull(reply.handoffPrompt)
        assertEquals(AssistantVisualState.WORKING, reply.visualState)
    }

    @Test
    fun freePrompt_requiresExplicitProviderHandoff() {
        val input = "Erkläre mir Quantenphysik in drei Absätzen"
        val reply = core.reply(input)

        assertNull(reply.command)
        assertEquals(input, reply.handoffPrompt)
        assertEquals(AssistantVisualState.OFFLINE, reply.visualState)
        assertTrue(reply.text.contains("kein generatives Modell", ignoreCase = true))
    }

    @Test
    fun privacyAnswer_isLocalAndDoesNotCreateHandoff() {
        val reply = core.reply("Datenschutz")

        assertNull(reply.command)
        assertNull(reply.handoffPrompt)
        assertTrue(reply.text.contains("Sitzung"))
        assertTrue(reply.text.contains("ausdrücklich"))
    }

    @Test
    fun greeting_doesNotPretendToBeGenerativeModel() {
        val reply = core.reply("Hallo")

        assertNull(reply.command)
        assertNull(reply.handoffPrompt)
        assertTrue(reply.text.contains("Launcher-Befehle"))
        assertTrue(reply.text.contains("Auswahl"))
    }
}
