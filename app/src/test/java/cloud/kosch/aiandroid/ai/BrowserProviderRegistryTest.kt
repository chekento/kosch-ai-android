package cloud.kosch.aiandroid.ai

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
    fun officialPackageHints_areExplicitAndStable() {
        val expected = mapOf(
            "chrome" to "com.android.chrome",
            "edge" to "com.microsoft.emmx",
            "opera" to "com.opera.browser",
            "brave" to "com.brave.browser",
            "duckduckgo" to "com.duckduckgo.mobile.android",
            "firefox" to "org.mozilla.firefox",
            "samsung-internet" to "com.sec.android.app.sbrowser",
            "vivaldi" to "com.vivaldi.browser",
        )
        expected.forEach { (id, packageName) ->
            val browser = BrowserProviderRegistry.browsers.first { it.id == id }
            assertTrue(packageName in browser.packageHints)
            assertEquals(packageName, browser.playStorePackageName)
        }
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
}
