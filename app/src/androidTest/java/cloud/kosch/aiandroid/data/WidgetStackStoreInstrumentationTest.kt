package cloud.kosch.aiandroid.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.model.WidgetStack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.charset.StandardCharsets

@RunWith(AndroidJUnit4::class)
class WidgetStackStoreInstrumentationTest {
    @Test
    fun stackBindings_areDeviceLocal_andNeverEnterPortableWorkspaceBackup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val stackStore = WidgetStackStore(context)
        val workspaceStore = WorkspaceStore(context)
        val marker = "widget-stack-private-marker-9f27"

        try {
            assertTrue(
                stackStore.save(
                    listOf(
                        WidgetStack(
                            id = marker,
                            title = "Private stack",
                            appWidgetIds = listOf(101_991, 101_992),
                        ),
                    ),
                ),
            )
            val loaded = stackStore.load().single()
            assertEquals(marker, loaded.id)
            assertEquals(listOf(101_991, 101_992), loaded.appWidgetIds)

            val portable = workspaceStore.createPortableSnapshot()
            val encoded = portable.toString(StandardCharsets.UTF_8)
            portable.fill(0)

            assertFalse(encoded.contains(marker))
            assertFalse(encoded.contains("101991"))
            assertFalse(encoded.contains("101992"))
            assertTrue(encoded.contains("widgetHostIds"))
        } finally {
            stackStore.clear()
        }
    }
}
