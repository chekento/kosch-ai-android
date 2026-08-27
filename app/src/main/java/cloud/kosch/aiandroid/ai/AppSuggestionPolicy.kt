package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.LaunchableApp
import java.util.Locale

enum class AppSuggestionCategory(val title: String) {
    AI("KI"),
    BROWSER("Browser"),
    PRODUCTIVITY("Produktivität"),
    COMMUNICATION("Kommunikation"),
    MEDIA("Medien"),
    SECURITY("Sicherheit"),
    CREATIVE("Kreativ"),
    OTHER("Weitere"),
}

data class AppSuggestion(
    val stableId: String,
    val title: String,
    val description: String,
    val category: AppSuggestionCategory,
    val packageName: String?,
    val webFallbackUrl: String? = null,
    val labelHints: Set<String> = emptySet(),
) {
    init {
        require(STABLE_ID.matches(stableId)) { "Invalid suggestion id" }
        require(title.isNotBlank() && title.length <= 100) { "Invalid suggestion title" }
        require(description.length <= 500) { "Suggestion description is too long" }
        require(packageName != null || webFallbackUrl != null) { "Suggestion needs a store package or web fallback" }
    }

    private companion object {
        val STABLE_ID = Regex("[a-z0-9][a-z0-9._:-]{1,127}")
    }
}

sealed interface AppSuggestionRoute {
    data class Installed(val app: LaunchableApp) : AppSuggestionRoute
    data class PlayStore(val packageName: String) : AppSuggestionRoute
    data class Web(val url: String) : AppSuggestionRoute
    data object Unavailable : AppSuggestionRoute
}

/** Shared install/store rule for every current and future KoSch app recommendation. */
object AppSuggestionPolicy {
    fun route(suggestion: AppSuggestion, apps: List<LaunchableApp>): AppSuggestionRoute {
        val installed = apps.firstOrNull { app ->
            app.packageName == suggestion.packageName || suggestion.labelHints.any { hint ->
                app.label.lowercase(Locale.ROOT).contains(hint.lowercase(Locale.ROOT))
            }
        }
        if (installed != null) return AppSuggestionRoute.Installed(installed)
        suggestion.packageName?.let { return AppSuggestionRoute.PlayStore(it) }
        suggestion.webFallbackUrl?.let { return AppSuggestionRoute.Web(it) }
        return AppSuggestionRoute.Unavailable
    }
}
