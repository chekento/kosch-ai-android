package cloud.kosch.aiandroid.ai

import java.util.Locale

enum class AiHubOrigin(val title: String) {
    HOME("Home"),
    COMMAND("⌘ Ask"),
    LEGACY_PROVIDER("KI-Einstieg"),
    FILE("Datei-Kontext"),
    PEN("Pen Space"),
    PRO_DESK("Pro Desk"),
}

enum class AiHubContextSignal(val title: String) {
    OFFLINE("Offline"),
    PERSONAL_AUDIO("Persönliche Audioausgabe"),
    FILE_CONTEXT("Datei-Kontext"),
    PEN_CONTEXT("Pen-Kontext"),
    WORK_CONTEXT("Arbeitskontext"),
    STUDIO_CONTEXT("Studio-Kontext"),
    PRO_DESK_CONTEXT("Pro-Desk-Kontext"),
}

/**
 * Privacy-minimal routing context. It contains no file names, content, URIs, battery values, contacts, screen pixels
 * or camera data. Signals may change ranking only; they never grant an Android capability or observation permission.
 */
data class AiHubRoutingContext(
    val origin: AiHubOrigin = AiHubOrigin.HOME,
    val signals: Set<AiHubContextSignal> = emptySet(),
) {
    val summary: String?
        get() = signals
            .map(AiHubContextSignal::title)
            .takeIf(List<String>::isNotEmpty)
            ?.joinToString(" · ")
}

/** Applies already-known local launcher context after semantic task scoring and before user preference ranking. */
object AiHubContextPolicy {
    fun apply(
        context: AiHubRoutingContext,
        recommendations: List<AiHubRecommendation>,
    ): List<AiHubRecommendation> {
        if (recommendations.isEmpty()) return emptyList()

        val offline = AiHubContextSignal.OFFLINE in context.signals
        val installedLocal = recommendations.filter {
            it.entry.kind == AiHubEntryKind.LOCAL_LLM_APP && it.entry.installState == AiHubInstallState.INSTALLED
        }
        val candidates = if (offline && installedLocal.isNotEmpty()) installedLocal else recommendations

        return candidates
            .map { recommendation -> adjust(recommendation, context) }
            .sortedWith(
                compareByDescending<AiHubRecommendation> { it.score }
                    .thenBy { it.entry.title.lowercase(Locale.ROOT) },
            )
    }

    private fun adjust(
        recommendation: AiHubRecommendation,
        context: AiHubRoutingContext,
    ): AiHubRecommendation {
        var delta = 0
        val reasons = mutableListOf<String>()
        val entry = recommendation.entry
        val capabilities = entry.aiCapabilities

        if (AiHubContextSignal.OFFLINE in context.signals) {
            if (entry.kind == AiHubEntryKind.LOCAL_LLM_APP && entry.installState == AiHubInstallState.INSTALLED) {
                delta += 180
                reasons += "offline lokal"
            } else {
                delta -= 220
            }
        }

        if (AiHubContextSignal.FILE_CONTEXT in context.signals) {
            when {
                hasCapability(capabilities, "Quellen-Notebook") -> {
                    delta += 70
                    reasons += "Datei-/Quellenkontext"
                }
                hasCapability(capabilities, "Dateien") -> {
                    delta += 52
                    reasons += "Dateikontext"
                }
                hasCapability(capabilities, "Recherche") -> delta += 18
            }
        }

        if (
            AiHubContextSignal.PEN_CONTEXT in context.signals &&
            recommendation.intent == AiHubTaskIntent.IMAGE &&
            hasCapability(capabilities, "Bild")
        ) {
            delta += 38
            reasons += "Pen-Kontext"
        }

        if (
            AiHubContextSignal.PERSONAL_AUDIO in context.signals &&
            recommendation.intent == AiHubTaskIntent.VOICE &&
            hasCapability(capabilities, "Voice")
        ) {
            delta += 28
            reasons += "Audio bereit"
        }

        if (
            AiHubContextSignal.WORK_CONTEXT in context.signals &&
            recommendation.intent in setOf(AiHubTaskIntent.RESEARCH, AiHubTaskIntent.SOURCE_NOTEBOOK) &&
            (hasCapability(capabilities, "Recherche") || hasCapability(capabilities, "Dateien"))
        ) {
            delta += 18
            reasons += "Arbeitskontext"
        }

        if (
            AiHubContextSignal.STUDIO_CONTEXT in context.signals &&
            recommendation.intent in setOf(AiHubTaskIntent.IMAGE, AiHubTaskIntent.VOICE) &&
            (hasCapability(capabilities, "Bild") || hasCapability(capabilities, "Voice"))
        ) {
            delta += 14
            reasons += "Studio-Kontext"
        }

        if (
            AiHubContextSignal.PRO_DESK_CONTEXT in context.signals &&
            recommendation.intent in setOf(AiHubTaskIntent.RESEARCH, AiHubTaskIntent.SOURCE_NOTEBOOK) &&
            (hasCapability(capabilities, "Recherche") || hasCapability(capabilities, "Dateien"))
        ) {
            delta += 12
            reasons += "Pro Desk"
        }

        if (delta == 0) return recommendation
        val contextReason = reasons.distinct().joinToString(" · ")
        return recommendation.copy(
            score = recommendation.score + delta,
            reason = if (contextReason.isBlank()) recommendation.reason else "${recommendation.reason} · $contextReason",
        )
    }

    private fun hasCapability(capabilities: Set<String>, title: String): Boolean = capabilities.any {
        it.equals(title, ignoreCase = true)
    }
}
