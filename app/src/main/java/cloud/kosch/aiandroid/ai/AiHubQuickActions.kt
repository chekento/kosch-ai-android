package cloud.kosch.aiandroid.ai

/**
 * Deterministic power-user prompt helpers for the Smart AI Hub.
 *
 * A quick action only shapes user-authored text. It never chooses a provider, grants a capability, changes privacy
 * state, enables network access or bypasses the normal task/context routing and explicit handoff gates.
 */
enum class AiHubQuickAction(
    val title: String,
    val instruction: String,
) {
    RESEARCH(
        title = "Recherche",
        instruction = "Recherchiere das gründlich, priorisiere aktuelle belastbare Quellen und trenne Fakten von Unsicherheit.",
    ),
    SUMMARIZE(
        title = "Zusammenfassen",
        instruction = "Fasse den freigegebenen Inhalt präzise zusammen und nenne die wichtigsten Aussagen, Entscheidungen und offenen Punkte.",
    ),
    LOCAL_PRIVATE(
        title = "Lokal & privat",
        instruction = "Bearbeite diese Aufgabe lokal und datensparsam. Nutze kein Cloud- oder Browser-Ziel.",
    ),
    IMAGE(
        title = "Bild",
        instruction = "Bearbeite diese Aufgabe als Bild- oder Visual-Aufgabe und nutze nur tatsächlich verfügbare Bildfähigkeiten.",
    ),
    VOICE(
        title = "Voice",
        instruction = "Bearbeite diese Aufgabe im Voice-Kontext und bevorzuge nur tatsächlich verfügbare Sprachfähigkeiten.",
    ),
    SOURCE_NOTEBOOK(
        title = "Quellen",
        instruction = "Bearbeite diese Aufgabe quellenorientiert und bevorzuge ein geeignetes Quellen- oder Notebook-Ziel.",
    ),
}

object AiHubQuickActionPolicy {
    const val MAX_PROMPT_CHARS = 32_000

    /**
     * Applies the action as an explicit instruction prefix. Reapplying the same action is idempotent to avoid prompt
     * inflation during rapid taps. Existing user text is preserved verbatim except for outer whitespace and length.
     */
    fun apply(
        action: AiHubQuickAction,
        currentPrompt: String,
    ): String {
        val current = currentPrompt.trim().take(MAX_PROMPT_CHARS)
        if (current.startsWith(action.instruction)) return current
        return buildString {
            append(action.instruction)
            if (current.isNotBlank()) {
                append("\n\n")
                append(current)
            }
        }.take(MAX_PROMPT_CHARS)
    }
}
