package cloud.kosch.aiandroid.ai

import android.content.ComponentName
import android.os.Process
import androidx.compose.ui.graphics.ImageBitmap
import cloud.kosch.aiandroid.model.AppProfile
import cloud.kosch.aiandroid.model.LaunchableApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserProviderRegistryTest {
    @Test
    fun missingKnownBrowser_routesToOfficialPlayStore() {
        val edge = BrowserProviderRegistry.browsers.first { it.id == "edge" }
        assertEquals(
            BrowserRouteTarget.PlayStore("com.microsoft.emmx"),
            BrowserProviderRegistry.routeTarget(edge, emptyList()),
        )
    }

    @Test
    fun installedBrowser_beatsStoreFallback() {
        val chrome = BrowserProviderRegistry.browsers.first { it.id == "chrome" }
        val installed = app("Google Chrome", "com.android.chrome")
        assertEquals(
            BrowserRouteTarget.InstalledApp(installed),
            BrowserProviderRegistry.routeTarget(chrome, listOf(installed)),
        )
    }

    @Test
    fun systemBrowser_isNeverSentToStore() {
        val system = BrowserProviderRegistry.browsers.first { it.id == "system-browser" }
        assertEquals(
            BrowserRouteTarget.SystemDefault,
            BrowserProviderRegistry.routeTarget(system, emptyList()),
        )
    }

    @Test
    fun browserAiClaims_remainConservative() {
        val chrome = BrowserProviderRegistry.browsers.first { it.id == "chrome" }
        val edge = BrowserProviderRegistry.browsers.first { it.id == "edge" }
        val opera = BrowserProviderRegistry.browsers.first { it.id == "opera" }
        val firefox = BrowserProviderRegistry.browsers.first { it.id == "firefox" }

        assertEquals(BrowserAiStatus.ROLLOUT_OR_REGION_DEPENDENT, chrome.aiStatus)
        assertEquals(BrowserAiStatus.BUILT_IN, edge.aiStatus)
        assertEquals(BrowserAiStatus.BUILT_IN, opera.aiStatus)
        assertEquals(BrowserAiStatus.NONE_CONFIRMED, firefox.aiStatus)
        assertFalse(firefox.aiCapabilities.contains(BrowserAiCapability.AGENTIC_BROWSING))
        assertTrue(edge.aiCapabilities.contains(BrowserAiCapability.SUMMARIZE_PAGE))
    }

    private fun app(label: String, packageName: String) = LaunchableApp(
        key = "$packageName/Main",
        label = label,
        packageName = packageName,
        componentName = ComponentName(packageName, "$packageName.Main"),
        user = Process.myUserHandle(),
        userSerialNumber = 0L,
        profile = AppProfile.PERSONAL,
        icon = ImageBitmap(1, 1),
    )
}
