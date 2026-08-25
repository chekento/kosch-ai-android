package cloud.kosch.aiandroid.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.model.AssistantSettings
import org.junit.Assert.assertEquals
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
}
