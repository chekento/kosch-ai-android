package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSuggestionPolicyTest {
    @Test
    fun missingPackagedSuggestion_routesToPlayStoreBeforeWeb() {
        val suggestion = AppSuggestion(
            stableId = "browser.edge",
            title = "Microsoft Edge",
            description = "Browser mit optionalen KI-Funktionen",
            category = AppSuggestionCategory.BROWSER,
            packageName = "com.microsoft.emmx",
            webFallbackUrl = "https://www.microsoft.com/edge",
        )
        assertEquals(
            AppSuggestionRoute.PlayStore("com.microsoft.emmx"),
            AppSuggestionPolicy.route(suggestion, emptyList()),
        )
        assertEquals(
            AppSuggestionRouteReason.PLAY_STORE,
            AppSuggestionPolicy.resolve(suggestion, emptyList()).reason,
        )
    }

    @Test
    fun webOnlyTip_isAllowedButNeverPretendsToBePlayStoreApp() {
        val suggestion = AppSuggestion(
            stableId = "tool.web-only",
            title = "Web Tool",
            description = "Nur Web",
            category = AppSuggestionCategory.OTHER,
            packageName = null,
            webFallbackUrl = "https://example.com/",
        )
        assertEquals(
            AppSuggestionRoute.Web("https://example.com/"),
            AppSuggestionPolicy.route(suggestion, emptyList()),
        )
        assertEquals(AppSuggestionRouteReason.WEB_FALLBACK, AppSuggestionPolicy.resolve(suggestion, emptyList()).reason)
    }

    @Test
    fun packagedAcquisitionRoute_outranksWebOnlySuggestionLocally() {
        val store = AppSuggestion(
            stableId = "productivity.store",
            title = "Store Productivity",
            description = "Bestätigter Paketpfad",
            category = AppSuggestionCategory.PRODUCTIVITY,
            packageName = "com.example.productivity",
        )
        val web = AppSuggestion(
            stableId = "productivity.web",
            title = "Web Productivity",
            description = "Web Fallback",
            category = AppSuggestionCategory.PRODUCTIVITY,
            packageName = null,
            webFallbackUrl = "https://example.com/productivity",
        )
        val ranked = AppSuggestionPolicy.rank(listOf(web, store), emptyList())
        assertEquals("productivity.store", ranked.first().suggestion.stableId)
        assertTrue(ranked.first().score > ranked.last().score)
    }

    @Test
    fun categoryPreference_isBoundedAndExplainable() {
        val browser = AppSuggestion(
            stableId = "browser.store",
            title = "Browser",
            description = "Browser",
            category = AppSuggestionCategory.BROWSER,
            packageName = "com.example.browser",
        )
        val creative = AppSuggestion(
            stableId = "creative.store",
            title = "Creative",
            description = "Creative",
            category = AppSuggestionCategory.CREATIVE,
            packageName = "com.example.creative",
        )
        val ranked = AppSuggestionPolicy.rank(
            listOf(browser, creative),
            apps = emptyList(),
            preferredCategories = listOf(AppSuggestionCategory.CREATIVE, AppSuggestionCategory.BROWSER),
        )
        assertEquals("creative.store", ranked.first().suggestion.stableId)
        assertEquals(AppSuggestionRouteReason.PLAY_STORE, ranked.first().reason)
    }
}
