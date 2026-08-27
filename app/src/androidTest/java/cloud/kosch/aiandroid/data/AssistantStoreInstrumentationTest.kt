package cloud.kosch.aiandroid.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.AssistantSessionController
import cloud.kosch.aiandroid.LauncherController
import cloud.kosch.aiandroid.model.AssistantMessageRole
import cloud.kosch.aiandroid.model.AssistantSettings
import cloud.kosch.aiandroid.model.AssistantVisualState
import cloud.kosch.aiandroid.ui.components.AssistantAttentionSignal
import cloud.kosch.aiandroid.ui.components.AssistantViseme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssistantStoreInstrumentationTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun assistantOptInSettings_roundTripWithoutTranscriptStorage() {
        val store = AssistantStore(context)
        val original = store.load()
        try {
            val expected = AssistantSettings(
                enabled = true,
                voiceInputEnabled = false,
                speechOutputEnabled = true,
                reducedMotion = true,
                assistantId = "default",
            )
            store.save(expected)

            assertEquals(expected, AssistantStore(context).load())
        } finally {
            store.save(original)
        }
    }

    @Test
    fun chatTranscript_isSessionOnlyAndNotRestoredFromPreferences() {
        val store = AssistantStore(context)
        val original = store.load()
        try {
            store.save(
                AssistantSettings(
                    enabled = true,
                    voiceInputEnabled = true,
                    speechOutputEnabled = false,
                    assistantId = "default",
                ),
            )
            val launcher = LauncherController(context)
            val session = AssistantSessionController(context)
            session.submit(
                text = "Erkläre mir Quantenphysik in drei Absätzen",
                launcherController = launcher,
                requestVoiceInput = {},
                requestDocument = {},
                requestContact = {},
                requestSpeech = { false },
            )

            assertTrue(session.messages.any { it.role == AssistantMessageRole.USER })
            assertTrue(session.messages.any { it.role == AssistantMessageRole.ASSISTANT })
            assertEquals("Erkläre mir Quantenphysik in drei Absätzen", session.handoffPrompt)

            val freshSession = AssistantSessionController(context)
            assertTrue(freshSession.messages.isEmpty())
            assertEquals(null, freshSession.handoffPrompt)
            assertTrue(freshSession.settings.enabled)
        } finally {
            store.save(original)
        }
    }

    @Test
    fun ttsVisualSignals_areEphemeralBoundedAndIgnoreStaleCallbacks() {
        val store = AssistantStore(context)
        val original = store.load()
        try {
            store.save(original.copy(enabled = true, speechOutputEnabled = true))
            val session = AssistantSessionController(context)

            session.speechQueued("active", "Hallo Welt")
            assertEquals(AssistantVisualState.IDLE, session.visualState)
            session.speechStarted("active")
            assertEquals(AssistantVisualState.SPEAKING, session.visualState)
            assertTrue(session.speechSignal.active)

            session.speechRange("active", start = 0, end = 5)
            assertTrue(session.speechSignal.rangeTimed)
            assertTrue(session.speechSignal.rangeVisemes.any { it != AssistantViseme.SIL })

            session.speechAudioLevel("stale", normalizedRms = 1f)
            assertEquals(0f, session.speechSignal.amplitude, 0.0001f)
            session.speechAudioLevel("active", normalizedRms = 1f)
            assertTrue(session.speechSignal.amplitude > 0f)

            session.speechInterrupted("active")
            assertFalse(session.speechSignal.active)
            assertEquals(AssistantVisualState.IDLE, session.visualState)
            assertFalse(AssistantSessionController(context).speechSignal.active)
        } finally {
            store.save(original)
        }
    }

    @Test
    fun touchAttention_isBoundedSessionOnlyAndClearedWhenDisabled() {
        val store = AssistantStore(context)
        val original = store.load()
        try {
            store.save(original.copy(enabled = true))
            val session = AssistantSessionController(context)

            session.pointerAttention(normalizedX = 4f, normalizedY = -4f, pressed = true)
            assertEquals(1f, session.attentionSignal.targetX, 0.0001f)
            assertEquals(-1f, session.attentionSignal.targetY, 0.0001f)
            assertTrue(session.attentionSignal.pressed)

            val freshSession = AssistantSessionController(context)
            assertEquals(AssistantAttentionSignal.Idle, freshSession.attentionSignal)

            session.setEnabled(false)
            assertEquals(AssistantAttentionSignal.Idle, session.attentionSignal)
        } finally {
            store.save(original)
        }
    }
}
