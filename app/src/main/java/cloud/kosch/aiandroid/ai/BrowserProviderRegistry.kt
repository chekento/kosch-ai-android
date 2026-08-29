package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.LaunchableApp
import java.util.Locale

enum class BrowserAiStatus(val title: String) {
    NONE_CONFIRMED("Kein integriertes Browser-LLM bestätigt"),
    BUILT_IN("Browser-KI integriert"),
    ROLLOUT_OR_REGION_DEPENDENT("Browser-KI abhängig von Region/Rollout"),
}

enum class BrowserAiCapability(val title: String) {
    ASK_PAGE("Seite fragen"),
    SUMMARIZE_PAGE("Seite zusammenfassen"),
    RESEARCH("Recherche"),
    IMAGE_GENERATION("Bildgenerierung"),
    AGENTIC_BROWSING("Agentisches Browsen"),
    PRIVATE_AI_CHAT("Privater KI-Chat"),
}

data class BrowserProviderProfile(
    val id: String,
    val name: String,
    val packageHints: Set<String>,
    val labelHints: Set<String>,
    val playStorePackageName: String?,
    val webUrl: String?,
    val aiStatus: BrowserAiStatus,
    val aiCapabilities: Set<BrowserAiCapability> = emptySet(),
    val notes: String = "",
    val isSystemDefaultRoute: Boolean = false,
)

sealed interface BrowserRouteTarget {
    data object SystemDefault : BrowserRouteTarget
    data class InstalledApp(val app: LaunchableApp) : BrowserRouteTarget
    data class PlayStore(val packageName: String) : BrowserRouteTarget
    data class Web(val url: String) : BrowserRouteTarget
    data object Unavailable : BrowserRouteTarget
}

/**
 * Browser registry for KoSch's AI Hub.
 *
 * Package/store entries are explicit. AI flags are intentionally conservative: runtime-discovered shortcuts/widgets
 * remain more authoritative than static marketing capabilities, and rollout-dependent functions must never be shown
 * as guaranteed on every device/account/region.
 */
object BrowserProviderRegistry {
    val browsers = listOf(
        BrowserProviderProfile(
            id = "system-browser",
            name = "Android Systembrowser",
            packageHints = emptySet(),
            labelHints = emptySet(),
            playStorePackageName = null,
            webUrl = null,
            aiStatus = BrowserAiStatus.NONE_CONFIRMED,
            notes = "Öffnet die aktuell von Android gewählte Standard-Browser-App.",
            isSystemDefaultRoute = true,
        ),
        BrowserProviderProfile(
            id = "chrome",
            name = "Google Chrome",
            packageHints = setOf("com.android.chrome"),
            labelHints = setOf("chrome"),
            playStorePackageName = "com.android.chrome",
            webUrl = "https://www.google.com/chrome/",
            aiStatus = BrowserAiStatus.ROLLOUT_OR_REGION_DEPENDENT,
            aiCapabilities = setOf(
                BrowserAiCapability.ASK_PAGE,
                BrowserAiCapability.SUMMARIZE_PAGE,
                BrowserAiCapability.RESEARCH,
                BrowserAiCapability.IMAGE_GENERATION,
                BrowserAiCapability.AGENTIC_BROWSING,
            ),
            notes = "Gemini in Chrome auf Android wird regional und geräteabhängig ausgerollt.",
        ),
        BrowserProviderProfile(
            id = "edge",
            name = "Microsoft Edge",
            packageHints = setOf("com.microsoft.emmx"),
            labelHints = setOf("edge"),
            playStorePackageName = "com.microsoft.emmx",
            webUrl = "https://www.microsoft.com/edge",
            aiStatus = BrowserAiStatus.BUILT_IN,
            aiCapabilities = setOf(
                BrowserAiCapability.ASK_PAGE,
                BrowserAiCapability.SUMMARIZE_PAGE,
                BrowserAiCapability.RESEARCH,
                BrowserAiCapability.IMAGE_GENERATION,
            ),
            notes = "Copilot ist in der aktuellen Android-App integriert.",
        ),
        BrowserProviderProfile(
            id = "opera",
            name = "Opera Browser",
            packageHints = setOf("com.opera.browser"),
            labelHints = setOf("opera"),
            playStorePackageName = "com.opera.browser",
            webUrl = "https://www.opera.com/opera/android",
            aiStatus = BrowserAiStatus.BUILT_IN,
            aiCapabilities = setOf(
                BrowserAiCapability.ASK_PAGE,
                BrowserAiCapability.SUMMARIZE_PAGE,
                BrowserAiCapability.RESEARCH,
                BrowserAiCapability.IMAGE_GENERATION,
            ),
            notes = "Opera AI (ehemals Aria) ist im Android-Browser integriert und kann ohne Konto genutzt werden.",
        ),
        BrowserProviderProfile(
            id = "opera-gx",
            name = "Opera GX",
            packageHints = setOf("com.opera.gx"),
            labelHints = setOf("opera gx"),
            playStorePackageName = "com.opera.gx",
            webUrl = "https://www.opera.com/gx/mobile",
            aiStatus = BrowserAiStatus.NONE_CONFIRMED,
            notes = "Die aktuelle Android-Storebeschreibung bestätigt die Desktop-GX-Browser-KI nicht für Mobile; KoSch behauptet sie daher nicht.",
        ),
        BrowserProviderProfile(
            id = "brave",
            name = "Brave Browser",
            packageHints = setOf("com.brave.browser"),
            labelHints = setOf("brave"),
            playStorePackageName = "com.brave.browser",
            webUrl = "https://brave.com/download/",
            aiStatus = BrowserAiStatus.BUILT_IN,
            aiCapabilities = setOf(
                BrowserAiCapability.ASK_PAGE,
                BrowserAiCapability.SUMMARIZE_PAGE,
                BrowserAiCapability.RESEARCH,
            ),
            notes = "Brave Leo ist als KI-Assistent im Browser integriert.",
        ),
        BrowserProviderProfile(
            id = "duckduckgo",
            name = "DuckDuckGo Browser",
            packageHints = setOf("com.duckduckgo.mobile.android"),
            labelHints = setOf("duckduckgo", "duck duck go"),
            playStorePackageName = "com.duckduckgo.mobile.android",
            webUrl = "https://duckduckgo.com/app",
            aiStatus = BrowserAiStatus.BUILT_IN,
            aiCapabilities = setOf(
                BrowserAiCapability.PRIVATE_AI_CHAT,
                BrowserAiCapability.RESEARCH,
            ),
            notes = "Duck.ai ist optional im Browser integriert; Verfügbarkeit kann kontobasiert variieren.",
        ),
        BrowserProviderProfile(
            id = "firefox",
            name = "Mozilla Firefox",
            packageHints = setOf("org.mozilla.firefox"),
            labelHints = setOf("firefox"),
            playStorePackageName = "org.mozilla.firefox",
            webUrl = "https://www.mozilla.org/firefox/browsers/mobile/android/",
            aiStatus = BrowserAiStatus.NONE_CONFIRMED,
        ),
        BrowserProviderProfile(
            id = "samsung-internet",
            name = "Samsung Internet",
            packageHints = setOf("com.sec.android.app.sbrowser"),
            labelHints = setOf("samsung internet", "samsung browser"),
            playStorePackageName = "com.sec.android.app.sbrowser",
            webUrl = "https://www.samsung.com/internet/",
            aiStatus = BrowserAiStatus.NONE_CONFIRMED,
        ),
        BrowserProviderProfile(
            id = "vivaldi",
            name = "Vivaldi Browser",
            packageHints = setOf("com.vivaldi.browser"),
            labelHints = setOf("vivaldi"),
            playStorePackageName = "com.vivaldi.browser",
            webUrl = "https://vivaldi.com/android/",
            aiStatus = BrowserAiStatus.NONE_CONFIRMED,
        ),
    )

    fun installedApp(
        browser: BrowserProviderProfile,
        apps: List<LaunchableApp>,
    ): LaunchableApp? {
        if (browser.isSystemDefaultRoute) return null
        return apps.firstOrNull { app ->
            app.packageName in browser.packageHints || browser.labelHints.any { hint ->
                app.label.lowercase(Locale.ROOT).contains(hint)
            }
        }
    }

    fun routeTarget(
        browser: BrowserProviderProfile,
        apps: List<LaunchableApp>,
    ): BrowserRouteTarget {
        if (browser.isSystemDefaultRoute) return BrowserRouteTarget.SystemDefault
        installedApp(browser, apps)?.let { return BrowserRouteTarget.InstalledApp(it) }
        browser.playStorePackageName?.let { return BrowserRouteTarget.PlayStore(it) }
        browser.webUrl?.let { return BrowserRouteTarget.Web(it) }
        return BrowserRouteTarget.Unavailable
    }
}
