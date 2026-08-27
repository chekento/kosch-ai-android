package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertEquals
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
    }
}
