package cloud.kosch.aiandroid.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.model.DeviceWidgetBinding
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceWidgetBindingStoreInstrumentationTest {
    private lateinit var store: WorkspaceWidgetBindingStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = WorkspaceWidgetBindingStore(context)
        store.clear()
    }

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun bindLoadUnbind_roundTripsOnlyDeviceLocalPair() {
        val binding = DeviceWidgetBinding("item:user:widget-1", 42)

        assertTrue(store.bind(binding))
        assertTrue(store.bind(binding))
        assertEquals(42, store.bindingFor(binding.workspaceItemId))
        assertEquals(mapOf(binding.workspaceItemId to 42), store.load())
        assertEquals(42, store.unbind(binding.workspaceItemId))
        assertNull(store.bindingFor(binding.workspaceItemId))
    }

    @Test
    fun bind_rejectsHostAliasingAndImplicitReplacement() {
        assertTrue(store.bind(DeviceWidgetBinding("item:user:first", 41)))

        assertFalse(store.bind(DeviceWidgetBinding("item:user:second", 41)))
        assertFalse(store.bind(DeviceWidgetBinding("item:user:first", 42)))
        assertEquals(mapOf("item:user:first" to 41), store.load())
    }

    @Test
    fun prune_keepsOnlyExactValidatedPairs() {
        assertTrue(store.bind(DeviceWidgetBinding("item:user:keep", 41)))
        assertTrue(store.bind(DeviceWidgetBinding("item:user:crossed", 42)))
        assertTrue(store.bind(DeviceWidgetBinding("item:user:missing", 43)))

        val released = store.prune(
            validBindings = mapOf(
                "item:user:keep" to 41,
                // Same ids are individually known, but the crossed pair must still be rejected.
                "item:user:crossed" to 43,
            ),
        )

        assertEquals(setOf(42, 43), released)
        assertEquals(mapOf("item:user:keep" to 41), store.load())
    }
}
