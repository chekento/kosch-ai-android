package cloud.kosch.aiandroid.system

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherPinCapabilityInstrumentationTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun appWidgetPinning_isAdvertisedButShortcutPinningWaitsForFirstClassStorage() {
        val packageManager = context.packageManager
        val widgetIntent = Intent(LauncherApps.ACTION_CONFIRM_PIN_APPWIDGET)
            .setPackage(context.packageName)
        val widgetHandler = packageManager.resolveActivity(widgetIntent, PackageManager.MATCH_DEFAULT_ONLY)
        assertNotNull(widgetHandler)
        assertEquals(
            "cloud.kosch.aiandroid.LauncherPinWidgetActivity",
            widgetHandler?.activityInfo?.name,
        )

        val shortcutIntent = Intent(LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT)
            .setPackage(context.packageName)
        assertNull(packageManager.resolveActivity(shortcutIntent, PackageManager.MATCH_DEFAULT_ONLY))
    }
}
