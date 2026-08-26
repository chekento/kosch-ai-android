package cloud.kosch.aiandroid

import android.content.ComponentName
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.data.WorkspaceStore
import cloud.kosch.aiandroid.data.WorkspaceWidgetBindingStore
import cloud.kosch.aiandroid.model.DeviceWidgetBinding
import java.nio.charset.StandardCharsets
import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceWidgetPlacementContractInstrumentationTest {
    @Test
    fun placementActivity_isInternalOnly() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val info = context.packageManager.getActivityInfo(
            ComponentName(context, WorkspaceWidgetPlacementActivity::class.java),
            0,
        )

        assertFalse("Widget placement transaction must remain internal", info.exported)
    }

    @Test
    fun deviceBinding_neverLeaksIntoPortableWorkspaceBackup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val itemId = "item:test:binding:${UUID.randomUUID()}"
        val bindingStore = WorkspaceWidgetBindingStore(context)

        try {
            assertTrue(bindingStore.bind(DeviceWidgetBinding(itemId, 7373)))
            val backup = WorkspaceStore(context).createPortableSnapshot().toString(StandardCharsets.UTF_8)

            assertFalse(backup.contains(itemId))
            assertFalse(backup.contains("appWidgetId"))
        } finally {
            bindingStore.unbind(itemId)
        }
    }
}
