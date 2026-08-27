package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.FileInsight
import java.util.UUID

/** Context types that may cross from a local launcher surface into an AI task after explicit user confirmation. */
enum class AiContextHandoffKind(val title: String) {
    FILE("Datei"),
    PEN_SKETCH("Skizze"),
    WEB_PAGE("Webseite"),
    SCREEN_FRAME("Bildschirmframe"),
}

data class AiContextHandoffDraft(
    val id: String,
    val kind: AiContextHandoffKind,
    val title: String,
    val mimeType: String?,
    val sizeBytes: Long?,
    val localSummary: String,
    val localExcerpt: String?,
) {
    init {
        require(id.isNotBlank())
        require(title.isNotBlank())
        require(localSummary.length <= MAX_SUMMARY_CHARS)
        require((localExcerpt?.length ?: 0) <= MAX_EXCERPT_CHARS)
    }

    companion object {
        const val MAX_SUMMARY_CHARS = 2_000
        const val MAX_EXCERPT_CHARS = 8_000
    }
}

data class AiConfirmedContextHandoff(
    val draftId: String,
    val kind: AiContextHandoffKind,
    val prompt: String,
)

/**
 * Builds a local preview first. No method in this object can create an outbound prompt without an explicit
 * `userConfirmed=true` argument supplied by the UI/controller handling the user action.
 */
object AiContextHandoffPolicy {
    fun fromFile(insight: FileInsight): AiContextHandoffDraft = AiContextHandoffDraft(
        id = UUID.randomUUID().toString(),
        kind = AiContextHandoffKind.FILE,
        title = insight.displayName.trim().ifBlank { "Ausgewählte Datei" }.take(MAX_TITLE_CHARS),
        mimeType = insight.mimeType.trim().takeIf(String::isNotBlank)?.take(MAX_MIME_CHARS),
        sizeBytes = insight.sizeBytes?.takeIf { it >= 0L },
        localSummary = insight.summary.trim().take(AiContextHandoffDraft.MAX_SUMMARY_CHARS),
        localExcerpt = insight.preview
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?.take(AiContextHandoffDraft.MAX_EXCERPT_CHARS),
    )

    fun confirm(
        draft: AiContextHandoffDraft,
        userPrompt: String,
        userConfirmed: Boolean,
    ): AiConfirmedContextHandoff {
        require(userConfirmed) { "AI context handoff requires explicit user confirmation" }
        val instruction = userPrompt.trim().take(MAX_USER_PROMPT_CHARS)
        val prompt = buildString {
            if (instruction.isNotBlank()) {
                append(instruction).append("\n\n")
            }
            append("Explizit freigegebener Kontext (${draft.kind.title}):\n")
            append("Titel: ").append(draft.title).append('\n')
            draft.mimeType?.let { append("Typ: ").append(it).append('\n') }
            draft.sizeBytes?.let { append("Größe: ").append(it).append(" Bytes\n") }
            if (draft.localSummary.isNotBlank()) {
                append("Lokale Zusammenfassung: ").append(draft.localSummary).append('\n')
            }
            draft.localExcerpt?.let {
                append("\nBegrenzter lokaler Auszug:\n").append(it)
            }
        }.take(MAX_CONFIRMED_PROMPT_CHARS)
        return AiConfirmedContextHandoff(
            draftId = draft.id,
            kind = draft.kind,
            prompt = prompt,
        )
    }

    private const val MAX_TITLE_CHARS = 240
    private const val MAX_MIME_CHARS = 160
    private const val MAX_USER_PROMPT_CHARS = 4_000
    private const val MAX_CONFIRMED_PROMPT_CHARS = 12_000
}
