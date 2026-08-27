package cloud.kosch.aiandroid.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
    fun saveLoadAndReset_areAtomicAtDocumentLevel() {
        val source = LauncherSettingsDocument(
            home = HomeSettings(gridColumns = 18, gridRows = 16, lockLayout = true),
            assistant = LauncherAssistantSettings(enabled = true, assistantId = "instrumented"),
        ).normalized()

        assertTrue(store.save(source))
        assertEquals(source, store.load())

        assertTrue(store.reset())
        assertEquals(LauncherSettingsDocument(), store.load())
    }

    @Test
    fun validatedImport_commitsNormalizedDocument() {
        val payload = LauncherSettingsCodec.encode(
            LauncherSettingsDocument(
                home = HomeSettings(gridColumns = 999, gridRows = 1),
                assistant = LauncherAssistantSettings(enabled = true),
            ),
        )

        val imported = store.applyImport(payload).getOrThrow()

        assertEquals(24, imported.home.gridColumns)
        assertEquals(4, imported.home.gridRows)
        assertTrue(store.load().assistant.enabled)
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
