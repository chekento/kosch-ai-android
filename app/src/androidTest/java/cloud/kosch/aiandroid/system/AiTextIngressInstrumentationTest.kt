package cloud.kosch.aiandroid.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiTextIngressInstrumentationTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun processTextAndPlainTextShare_resolveToDedicatedConsentIngress() {
        val packageManager = context.packageManager

        val processText = Intent(Intent.ACTION_PROCESS_TEXT)
            .setType("text/plain")
            .setPackage(context.packageName)
        val processHandler = packageManager.resolveActivity(processText, PackageManager.MATCH_DEFAULT_ONLY)
        assertNotNull(processHandler)
        assertEquals(
            "cloud.kosch.aiandroid.AiTextIngressActivity",
            processHandler?.activityInfo?.name,
        )
        assertTrue(processHandler?.activityInfo?.exported == true)

        val shareText = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .setPackage(context.packageName)
        val shareHandler = packageManager.resolveActivity(shareText, PackageManager.MATCH_DEFAULT_ONLY)
        assertNotNull(shareHandler)
        assertEquals(
            "cloud.kosch.aiandroid.AiTextIngressActivity",
            shareHandler?.activityInfo?.name,
        )
    }

    @Test
    fun homeRole_stillResolvesOnlyToMainLauncherActivity() {
        val homeIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addCategory(Intent.CATEGORY_DEFAULT)
            .setPackage(context.packageName)
        val handler = context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        assertNotNull(handler)
        assertEquals(
            "cloud.kosch.aiandroid.MainActivity",
            handler?.activityInfo?.name,
        )
    }
}
