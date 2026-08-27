package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.AssistantAnchor
import cloud.kosch.aiandroid.model.AssistantWakeMode
import cloud.kosch.aiandroid.model.LauncherSettingsDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PortableLauncherSettingsPolicyTest {
    @Test
    fun project_keepsPresentationButNeutralizesAssistantRuntimeShadows() {
        val source = LauncherSettingsDocument().let { document ->
            document.copy(
                assistant = document.assistant.copy(
                    enabled = true,
                    assistantId = "anime_female",
                    anchor = AssistantAnchor.LEFT,
                    scale = 1.4f,
                    opacity = 0.72f,
                    wakeMode = AssistantWakeMode.CONTEXTUAL,
                    portalAnimationEnabled = false,
                    gazeTrackingEnabled = false,
                    liveChatEnabled = false,
                    voiceInputEnabled = false,
                    speechOutputEnabled = true,
                    hideOutsideAssistantPages = true,
                ),
            )
        }

        val projected = PortableLauncherSettingsPolicy.project(source)
        val defaults = LauncherSettingsDocument().assistant

        assertEquals(defaults.enabled, projected.assistant.enabled)
        assertEquals(defaults.assistantId, projected.assistant.assistantId)
        assertEquals(defaults.wakeMode, projected.assistant.wakeMode)
        assertEquals(defaults.liveChatEnabled, projected.assistant.liveChatEnabled)
        assertEquals(defaults.voiceInputEnabled, projected.assistant.voiceInputEnabled)
        assertEquals(defaults.speechOutputEnabled, projected.assistant.speechOutputEnabled)

        assertEquals(AssistantAnchor.LEFT, projected.assistant.anchor)
        assertEquals(1.4f, projected.assistant.scale)
        assertEquals(0.72f, projected.assistant.opacity)
        assertFalse(projected.assistant.portalAnimationEnabled)
        assertFalse(projected.assistant.gazeTrackingEnabled)
        assertTrue(projected.assistant.hideOutsideAssistantPages)
    }

    @Test
    fun encodedPortableRoundTrip_cannotRestoreShadowRuntimeState() {
        val source = LauncherSettingsDocument().let { document ->
            document.copy(
                assistant = document.assistant.copy(
                    enabled = true,
                    assistantId = "anime_male",
                    voiceInputEnabled = false,
                    speechOutputEnabled = true,
                    scale = 1.25f,
                ),
            )
        }

        val encoded = LauncherSettingsCodec.encode(PortableLauncherSettingsPolicy.project(source))
        val restored = PortableLauncherSettingsPolicy.project(LauncherSettingsCodec.decode(encoded))
        val defaults = LauncherSettingsDocument().assistant

        assertEquals(defaults.enabled, restored.assistant.enabled)
        assertEquals(defaults.assistantId, restored.assistant.assistantId)
        assertEquals(defaults.voiceInputEnabled, restored.assistant.voiceInputEnabled)
        assertEquals(defaults.speechOutputEnabled, restored.assistant.speechOutputEnabled)
        assertEquals(1.25f, restored.assistant.scale)
    }
}
