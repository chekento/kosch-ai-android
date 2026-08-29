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

enum class AppSuggestionRouteReason(val title: String) {
    EXACT_PACKAGE("Installierte App · Paket exakt erkannt"),
    LABEL_HINT("Installierte App · lokaler Namenshinweis erkannt"),
    PLAY_STORE("Nicht installiert · bestätigter Play-Store-Pfad"),
    WEB_FALLBACK("Nicht installiert · Web-Fallback"),
    UNAVAILABLE("Kein sicherer Startpfad"),
}

data class ResolvedAppSuggestion(
    val suggestion: AppSuggestion,
    val route: AppSuggestionRoute,
    val reason: AppSuggestionRouteReason,
    val score: Int,
)

/**
 * Shared install/store rule for every current and future KoSch app recommendation.
 *
 * Installed matches always beat acquisition routes. Ranking is deterministic and local; it uses only the suggestion
 * category, caller-provided category preference and whether an app is already present. No ads, payments, telemetry,
 * contacts, notification contents or browsing data participate in the score.
 */
object AppSuggestionPolicy {
    fun route(suggestion: AppSuggestion, apps: List<LaunchableApp>): AppSuggestionRoute =
        resolve(suggestion, apps).route

    fun resolve(
        suggestion: AppSuggestion,
        apps: List<LaunchableApp>,
    ): ResolvedAppSuggestion {
        val exact = suggestion.packageName?.let { packageName ->
            apps.firstOrNull { it.packageName == packageName }
        }
        if (exact != null) {
            return ResolvedAppSuggestion(
                suggestion = suggestion,
                route = AppSuggestionRoute.Installed(exact),
                reason = AppSuggestionRouteReason.EXACT_PACKAGE,
                score = INSTALLED_EXACT_SCORE,
            )
        }

        val hinted = apps.firstOrNull { app ->
            suggestion.labelHints.any { hint ->
                normalized(app.label).contains(normalized(hint))
            }
        }
        if (hinted != null) {
            return ResolvedAppSuggestion(
                suggestion = suggestion,
                route = AppSuggestionRoute.Installed(hinted),
                reason = AppSuggestionRouteReason.LABEL_HINT,
                score = INSTALLED_HINT_SCORE,
            )
        }

        suggestion.packageName?.let {
            return ResolvedAppSuggestion(
                suggestion = suggestion,
                route = AppSuggestionRoute.PlayStore(it),
                reason = AppSuggestionRouteReason.PLAY_STORE,
                score = STORE_SCORE,
            )
        }
        suggestion.webFallbackUrl?.let {
            return ResolvedAppSuggestion(
                suggestion = suggestion,
                route = AppSuggestionRoute.Web(it),
                reason = AppSuggestionRouteReason.WEB_FALLBACK,
                score = WEB_SCORE,
            )
        }
        return ResolvedAppSuggestion(
            suggestion = suggestion,
            route = AppSuggestionRoute.Unavailable,
            reason = AppSuggestionRouteReason.UNAVAILABLE,
            score = 0,
        )
    }

    fun rank(
        suggestions: List<AppSuggestion>,
        apps: List<LaunchableApp>,
        preferredCategories: List<AppSuggestionCategory> = emptyList(),
    ): List<ResolvedAppSuggestion> {
        val categoryIndex = preferredCategories.distinct().withIndex().associate { it.value to it.index }
        return suggestions
            .distinctBy(AppSuggestion::stableId)
            .map { suggestion ->
                val resolved = resolve(suggestion, apps)
                val preference = categoryIndex[suggestion.category]
                if (preference == null) resolved else resolved.copy(
                    score = resolved.score + CATEGORY_SCORE - preference.coerceAtMost(9) * CATEGORY_STEP,
                )
            }
            .sortedWith(
                compareByDescending<ResolvedAppSuggestion> { it.score }
                    .thenBy { it.suggestion.title.lowercase(Locale.ROOT) },
            )
    }

    private fun normalized(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace("[^a-z0-9äöüß ]".toRegex(), " ")
        .replace("\\s+".toRegex(), " ")
        .trim()

    private const val INSTALLED_EXACT_SCORE = 10_000
    private const val INSTALLED_HINT_SCORE = 9_000
    private const val STORE_SCORE = 2_000
    private const val WEB_SCORE = 1_000
    private const val CATEGORY_SCORE = 800
    private const val CATEGORY_STEP = 75
}
