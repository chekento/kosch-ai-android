package cloud.kosch.aiandroid.ui.components

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssistantAssetPackInstrumentationTest {
    @Test
    fun currentApk_withoutExportedSprites_staysExplicitlyUnready() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val audit = AssistantAssetPackInspector(context).auditDefault()

        assertEquals(150, audit.expectedPaths.size)
        assertEquals(0, audit.presentPaths.size)
        assertEquals(94, audit.bodyMissing.size)
        assertEquals(48, audit.overlayMissing.size)
        assertEquals(8, audit.portalMissing.size)
        assertTrue(audit.unexpectedPaths.isEmpty())
        assertFalse(audit.exportComplete)
        assertFalse(audit.faceCalibrated)
        assertFalse(audit.activationReady)
    }
}
