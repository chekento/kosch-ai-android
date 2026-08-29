package cloud.kosch.aiandroid.assistant

/**
 * Pure user-facing description of the already configured generative provider state.
 *
 * This object never changes provider settings. It only explains the three explicit decisions required before the
 * KAL Assistant may send a free-form prompt: provider connection, Cloud Access and model selection.
 */
object AssistantProviderReadiness {
    fun describe(
        connected: Boolean,
        cloudExecutionEnabled: Boolean,
        selectedModelId: String,
    ): String = when {
        !connected -> "OpenRouter ist nicht verbunden. Launcher-Befehle funktionieren lokal; freie KI-Fragen können im AI Hub eingerichtet werden."
        !cloudExecutionEnabled -> "OpenRouter ist verbunden, aber Cloud Access ist aus. Es wird keine freie KI-Frage extern gesendet."
        selectedModelId.isBlank() -> "OpenRouter und Cloud Access sind bereit, aber es ist noch kein Modell ausgewählt. Wähle ein Modell im AI Hub."
        else -> "Direkte KI-Antworten sind bereit · OpenRouter · ${selectedModelId.trim().take(MAX_MODEL_LABEL_CHARS)}"
    }

    private const val MAX_MODEL_LABEL_CHARS = 120
}
