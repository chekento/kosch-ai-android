package cloud.kosch.aiandroid.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.AssistantSessionController
import cloud.kosch.aiandroid.LauncherController
import cloud.kosch.aiandroid.model.AssistantMessageRole
import cloud.kosch.aiandroid.model.AssistantSettings
import org.junit.Assert.assertEquals
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
}
