package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.ai.AiHubInstallState.INSTALLED
import cloud.kosch.aiandroid.ai.AiHubInstallState.SYSTEM_AVAILABLE

enum class AiHubDecisionConfidence(val title: String) {
    HIGH("Eindeutige Route"),
    MEDIUM("Starke Empfehlung"),
    LOW("Mehrere gute Wege"),
}

data class AiHubRouteDecision(
    val primary: AiHubRecommendation,
    val alternatives: List<AiHubRecommendation>,
    val confidence: AiHubDecisionConfidence,
    val scoreMargin: Int?,
    val explanation: String,
)

/**
 * Converts a ranked recommendation list into an explainable decision without pretending that every top score is
 * uniquely correct. This layer never changes ranking, capabilities, privacy gates or install state.
 */
object AiHubDecisionPolicy {
    fun decide(recommendations: List<AiHubRecommendation>): AiHubRouteDecision? {
        val primary = recommendations.firstOrNull() ?: return null
        val second = recommendations.getOrNull(1)
        val margin = second?.let { primary.score - it.score }
        val immediatelyRunnable = primary.entry.installState == INSTALLED ||
            primary.entry.installState == SYSTEM_AVAILABLE

        val confidence = when {
            second == null && immediatelyRunnable -> AiHubDecisionConfidence.HIGH
            second == null -> AiHubDecisionConfidence.MEDIUM
            immediatelyRunnable && margin != null && margin >= HIGH_MARGIN -> AiHubDecisionConfidence.HIGH
            immediatelyRunnable && margin != null && margin >= MEDIUM_MARGIN -> AiHubDecisionConfidence.MEDIUM
            else -> AiHubDecisionConfidence.LOW
        }

        val explanation = when (confidence) {
            AiHubDecisionConfidence.HIGH -> if (second == null) {
                "Ein klar geeigneter, direkt verfügbarer Weg"
            } else {
                "Klarer Vorsprung von ${margin ?: 0} Routing-Punkten"
            }
            AiHubDecisionConfidence.MEDIUM -> if (second == null) {
                "Bestes verfügbares Ziel, aber nicht direkt installiert"
            } else {
                "Solider Vorsprung von ${margin ?: 0} Routing-Punkten"
            }
            AiHubDecisionConfidence.LOW -> "Die führenden Wege liegen nah beieinander"
        }

        return AiHubRouteDecision(
            primary = primary,
            alternatives = recommendations.drop(1).take(MAX_ALTERNATIVES),
            confidence = confidence,
            scoreMargin = margin,
            explanation = explanation,
        )
    }

    private const val HIGH_MARGIN = 40
    private const val MEDIUM_MARGIN = 15
    private const val MAX_ALTERNATIVES = 2
}
