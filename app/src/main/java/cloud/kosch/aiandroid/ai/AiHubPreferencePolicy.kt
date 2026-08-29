package cloud.kosch.aiandroid.ai

/**
 * User preference is a ranking hint, never a capability grant or privacy override.
 * A preferred target must be ready now and semantically fit the current task family.
 */
object AiHubPreferencePolicy {
    fun canPrefer(intent: AiHubTaskIntent, entry: AiHubEntry): Boolean {
        if (entry.installState != AiHubInstallState.INSTALLED &&
            entry.installState != AiHubInstallState.SYSTEM_AVAILABLE
        ) {
            return false
        }

        return when (intent) {
            AiHubTaskIntent.GENERAL_CHAT -> entry.kind != AiHubEntryKind.SYSTEM_BROWSER
            AiHubTaskIntent.RESEARCH ->
                "Recherche" in entry.aiCapabilities || entry.kind == AiHubEntryKind.BROWSER
            AiHubTaskIntent.BROWSER_PAGE ->
                entry.kind == AiHubEntryKind.BROWSER || entry.kind == AiHubEntryKind.SYSTEM_BROWSER ||
                    "Seite fragen" in entry.aiCapabilities || "Seite zusammenfassen" in entry.aiCapabilities
            AiHubTaskIntent.LOCAL_PRIVATE -> entry.kind == AiHubEntryKind.LOCAL_LLM_APP
            AiHubTaskIntent.VOICE -> "Voice" in entry.aiCapabilities
            AiHubTaskIntent.IMAGE ->
                "Bild" in entry.aiCapabilities || "Bildgenerierung" in entry.aiCapabilities
            AiHubTaskIntent.SOURCE_NOTEBOOK ->
                "Quellen-Notebook" in entry.aiCapabilities || "Dateien" in entry.aiCapabilities
        }
    }

    fun apply(
        intent: AiHubTaskIntent,
        preferredStableId: String?,
        recommendations: List<AiHubRecommendation>,
    ): List<AiHubRecommendation> {
        if (preferredStableId.isNullOrBlank()) return recommendations
        return recommendations
            .map { recommendation ->
                if (
                    recommendation.entry.stableId == preferredStableId &&
                    canPrefer(intent, recommendation.entry)
                ) {
                    recommendation.copy(
                        score = recommendation.score + PREFERENCE_BOOST,
                        reason = "Bevorzugt · ${recommendation.reason}",
                    )
                } else {
                    recommendation
                }
            }
            .sortedWith(
                compareByDescending<AiHubRecommendation> { it.score }
                    .thenBy { it.entry.title },
            )
    }

    private const val PREFERENCE_BOOST = 1_000
}
