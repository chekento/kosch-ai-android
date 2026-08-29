package cloud.kosch.aiandroid.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.model.AssistantAnchor
import cloud.kosch.aiandroid.model.HomeSettings
import cloud.kosch.aiandroid.model.LauncherAssistantSettings
import cloud.kosch.aiandroid.model.LauncherSettingsDocument
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherSettingsStoreInstrumentationTest {
    private lateinit var store: LauncherSettingsStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = LauncherSettingsStore(context)
        store.reset()
    }

    @After
    fun tearDown() {
        store.reset()
    }

    @Test
    fun saveLoadAndReset_areAtomicAtPortableDocumentLevel() {
        val source = LauncherSettingsDocument(
            home = HomeSettings(gridColumns = 18, gridRows = 16, lockLayout = true),
            assistant = LauncherAssistantSettings(
                enabled = true,
                assistantId = "instrumented",
                anchor = AssistantAnchor.LEFT,
                scale = 1.25f,
                opacity = 0.8f,
            ),
        ).normalized()
        val expectedPortable = PortableLauncherSettingsPolicy.project(source)

        assertTrue(store.save(source))
        assertEquals(expectedPortable, store.load())
        assertFalse(store.load().assistant.enabled)
        assertEquals("default", store.load().assistant.assistantId)
        assertEquals(AssistantAnchor.LEFT, store.load().assistant.anchor)
        assertEquals(1.25f, store.load().assistant.scale)
        assertEquals(0.8f, store.load().assistant.opacity)

        assertTrue(store.reset())
        assertEquals(LauncherSettingsDocument(), store.load())
    }

    @Test
    fun validatedImport_commitsNormalizedAndSanitizedDocument() {
        val payload = LauncherSettingsCodec.encode(
            LauncherSettingsDocument(
                home = HomeSettings(gridColumns = 999, gridRows = 1),
                assistant = LauncherAssistantSettings(
                    enabled = true,
                    assistantId = "imported-runtime-shadow",
                    anchor = AssistantAnchor.CENTER,
                    scale = 1.4f,
                ),
            ),
        )

        val imported = store.applyImport(payload).getOrThrow()

        assertEquals(24, imported.home.gridColumns)
        assertEquals(4, imported.home.gridRows)
        assertFalse(imported.assistant.enabled)
        assertEquals("default", imported.assistant.assistantId)
        assertEquals(AssistantAnchor.CENTER, imported.assistant.anchor)
        assertEquals(1.4f, imported.assistant.scale)
        assertEquals(imported, store.load())
    }

    @Test
    fun oversizedImport_doesNotOverwriteExistingSettings() {
        val existing = LauncherSettingsDocument(home = HomeSettings(gridColumns = 17))
        assertTrue(store.save(existing))

        val result = store.applyImport("x".repeat(600 * 1024))

        assertFalse(result.isSuccess)
        assertEquals(17, store.load().home.gridColumns)
    }
}
