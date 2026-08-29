package cloud.kosch.aiandroid.ai

import java.util.Locale

enum class BrowserAiEntrypointKind(val title: String) {
    COPILOT("Copilot"),
    ARIA("Aria"),
    LEO("Leo"),
    DUCK_AI("Duck.ai"),
    GEMINI("Gemini"),
    GENERIC_AI("KI"),
}

data class BrowserAiShortcutMatch(
    val kind: BrowserAiEntrypointKind,
    val confidence: Int,
)

/**
 * Classifies only shortcuts that a browser actually publishes through LauncherApps.
 * Static browser marketing metadata never creates a launchable AI shortcut by itself.
 */
object BrowserAiShortcutClassifier {
    fun classify(browserId: String, shortcutId: String, label: String): BrowserAiShortcutMatch? {
        val haystack = "$shortcutId $label".lowercase(Locale.ROOT)
        val browser = browserId.lowercase(Locale.ROOT)
        return when {
            browser == "edge" && "copilot" in haystack -> match(BrowserAiEntrypointKind.COPILOT, 100)
            browser == "opera" && "aria" in haystack -> match(BrowserAiEntrypointKind.ARIA, 100)
            browser == "brave" && "leo" in haystack -> match(BrowserAiEntrypointKind.LEO, 100)
            browser == "duckduckgo" && ("duck.ai" in haystack || "duckai" in haystack || "duck ai" in haystack) ->
                match(BrowserAiEntrypointKind.DUCK_AI, 100)
            browser == "chrome" && "gemini" in haystack -> match(BrowserAiEntrypointKind.GEMINI, 100)
            listOf(" ai", "ai ", "assistant", "assistent", "chatbot").any(haystack::contains) ->
                match(BrowserAiEntrypointKind.GENERIC_AI, 60)
            else -> null
        }
    }

    private fun match(kind: BrowserAiEntrypointKind, confidence: Int) =
        BrowserAiShortcutMatch(kind = kind, confidence = confidence)
}
