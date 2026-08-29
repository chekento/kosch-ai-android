package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.SelectedContact

/**
 * Privacy-first communication intelligence.
 *
 * KoSch can help prepare a call, draft a message or create a follow-up prompt without ever copying a phone number
 * into an AI prompt. The phone number remains only an Android routing target. Contact labels are included only after
 * the user has explicitly selected that contact for the current interaction.
 */
enum class CommunicationAiTask(val title: String) {
    CALL_PREP("Gespräch vorbereiten"),
    MESSAGE_DRAFT("Nachricht entwerfen"),
    FOLLOW_UP("Follow-up planen"),
    POST_CALL_NOTE("Gesprächsnotiz strukturieren"),
}

data class CommunicationAiPrompt(
    val task: CommunicationAiTask,
    val text: String,
    val containsPhoneNumber: Boolean = false,
)

object CommunicationAiPolicy {
    fun callPrep(contact: SelectedContact, userContext: String = ""): CommunicationAiPrompt {
        val name = safeLabel(contact.displayName)
        val context = userContext.trim().take(MAX_CONTEXT_LENGTH)
        val text = buildString {
            append("Bereite ein kurzes, sachliches Gespräch mit ")
            append(name)
            append(" vor. Erstelle Ziele, drei Kernpunkte und zwei sinnvolle Rückfragen.")
            if (context.isNotBlank()) append(" Kontext: ").append(context)
        }
        return CommunicationAiPrompt(CommunicationAiTask.CALL_PREP, text)
    }

    fun messageDraft(
        contact: SelectedContact,
        intent: String,
        tone: String = "freundlich und professionell",
    ): CommunicationAiPrompt {
        val name = safeLabel(contact.displayName)
        val safeIntent = intent.trim().take(MAX_CONTEXT_LENGTH)
        val safeTone = tone.trim().take(MAX_TONE_LENGTH).ifBlank { "freundlich und professionell" }
        val text = "Entwirf eine kurze Nachricht an $name. Ziel: $safeIntent. Ton: $safeTone. Keine Telefonnummer ausgeben."
        return CommunicationAiPrompt(CommunicationAiTask.MESSAGE_DRAFT, text)
    }

    fun followUp(contact: SelectedContact, outcome: String): CommunicationAiPrompt {
        val name = safeLabel(contact.displayName)
        val safeOutcome = outcome.trim().take(MAX_CONTEXT_LENGTH)
        return CommunicationAiPrompt(
            CommunicationAiTask.FOLLOW_UP,
            "Plane ein datensparsames Follow-up zu $name. Ergebnis des Gesprächs: $safeOutcome. " +
                "Schlage nächsten Schritt und einen neutralen Reminder-Text vor.",
        )
    }

    fun postCallNote(rawNote: String): CommunicationAiPrompt = CommunicationAiPrompt(
        CommunicationAiTask.POST_CALL_NOTE,
        "Strukturiere diese lokale Gesprächsnotiz in Ergebnis, To-dos, offene Fragen und Follow-up. " +
            "Erfinde keine personenbezogenen Daten. Notiz: ${rawNote.trim().take(MAX_CONTEXT_LENGTH)}",
    )

    private fun safeLabel(value: String): String = value
        .trim()
        .replace(Regex("[\\r\\n\\t]+"), " ")
        .take(MAX_LABEL_LENGTH)
        .ifBlank { "dem ausgewählten Kontakt" }

    private const val MAX_LABEL_LENGTH = 120
    private const val MAX_CONTEXT_LENGTH = 2_000
    private const val MAX_TONE_LENGTH = 120
}
