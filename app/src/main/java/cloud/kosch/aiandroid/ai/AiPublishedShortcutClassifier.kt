package cloud.kosch.aiandroid.ai

import java.util.Locale

enum class AiPublishedShortcutKind(val title: String) {
    AI_ASSISTANT("KI-Assistent"),
    NEW_CHAT("Neuer Chat"),
    VOICE("Voice"),
    RESEARCH("Recherche"),
    IMAGE("Bild"),
}

/**
 * Conservative classifier for labels that another app deliberately publishes as Android launcher shortcuts.
 *
 * A result only describes the published label; it does not grant KoSch any hidden capability. Browser cards may use
 * only AI_ASSISTANT results as direct browser-AI entry points so generic browser shortcuts never become fake AI UI.
 */
object AiPublishedShortcutClassifier {
    fun classify(label: String): AiPublishedShortcutKind? {
        val normalized = label.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return null
        return when {
            AI_ASSISTANT_TERMS.any(normalized::contains) -> AiPublishedShortcutKind.AI_ASSISTANT
            VOICE_TERMS.any(normalized::contains) -> AiPublishedShortcutKind.VOICE
            RESEARCH_TERMS.any(normalized::contains) -> AiPublishedShortcutKind.RESEARCH
            IMAGE_TERMS.any(normalized::contains) -> AiPublishedShortcutKind.IMAGE
            CHAT_TERMS.any(normalized::contains) -> AiPublishedShortcutKind.NEW_CHAT
            else -> null
        }
    }

    private val AI_ASSISTANT_TERMS = setOf(
        "copilot", "aria", "opera ai", "opera ki", "brave leo", " leo", "leo ",
        "duck.ai", "duck ai", "gemini", "ai assistant", "ki-assistent", "ki assistent",
        "ai chat", "ki-chat", "ki chat",
    )
    private val VOICE_TERMS = setOf("voice", "sprachchat", "voice chat", "audio chat")
    private val RESEARCH_TERMS = setOf("research", "recherche", "deep research")
    private val IMAGE_TERMS = setOf("image", "bild", "create image", "bild erstellen")
    private val CHAT_TERMS = setOf("new chat", "neuer chat", "start chat", "chat starten")
}
