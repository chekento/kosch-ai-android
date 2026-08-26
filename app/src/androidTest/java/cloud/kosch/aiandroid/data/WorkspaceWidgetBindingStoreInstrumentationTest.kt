package cloud.kosch.aiandroid.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.model.DeviceWidgetBinding
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceWidgetBindingStoreInstrumentationTest {
    @Test
    fun binding_isDeviceLocalAndSurvivesFreshStoreInstance() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val itemId = "item:test:widget:${UUID.randomUUID()}"
        val store = WorkspaceWidgetBindingStore(context)

        try {
            assertTrue(store.bind(DeviceWidgetBinding(itemId, 4242)))
            assertEquals(4242, WorkspaceWidgetBindingStore(context).appWidgetIdFor(itemId))
        } finally {
            store.unbind(itemId)
        }
    }

    @Test
    fun prune_removesOnlyBindingsWhoseStableWorkspaceItemDisappeared() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val keptId = "item:test:widget:${UUID.randomUUID()}"
        val staleId = "item:test:widget:${UUID.randomUUID()}"
        val store = WorkspaceWidgetBindingStore(context)

        try {
            assertTrue(store.bind(DeviceWidgetBinding(keptId, 5151)))
            assertTrue(store.bind(DeviceWidgetBinding(staleId, 6161)))

            val released = store.prune(setOf(keptId))

            assertEquals(listOf(6161), released)
            assertEquals(5151, store.appWidgetIdFor(keptId))
            assertNull(store.appWidgetIdFor(staleId))
        } finally {
            store.unbind(keptId)
            store.unbind(staleId)
        }
    }
}
