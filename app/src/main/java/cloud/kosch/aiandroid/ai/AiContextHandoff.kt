package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.FileInsight
import java.util.UUID

/** Context types that may cross from a local launcher surface into an AI task after explicit user confirmation. */
enum class AiContextHandoffKind(val title: String) {
    FILE("Datei"),
    PEN_SKETCH("Skizze"),
    WEB_PAGE("Webseite"),
    SCREEN_FRAME("Bildschirmframe"),
    SELECTED_TEXT("Markierter Text"),
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

/**
 * User-owned disclosure scope. Minimal is deliberately conservative: title + local summary only.
 * MIME/size and the longer excerpt require separate opt-in and can be exposed independently in UI.
 */
data class AiContextHandoffSelection(
    val includeTitle: Boolean = true,
    val includeMetadata: Boolean = false,
    val includeSummary: Boolean = true,
    val includeExcerpt: Boolean = false,
) {
    init {
        require(includeTitle || includeMetadata || includeSummary || includeExcerpt) {
            "At least one context field must be selected"
        }
    }

    companion object {
        val MINIMAL = AiContextHandoffSelection()
        val SUMMARY_AND_EXCERPT = AiContextHandoffSelection(includeExcerpt = true)
        val FULL_TEXT_PREVIEW = AiContextHandoffSelection(
            includeMetadata = true,
            includeExcerpt = true,
        )
    }
}

data class AiConfirmedContextHandoff(
    val draftId: String,
    val kind: AiContextHandoffKind,
    val prompt: String,
    val selection: AiContextHandoffSelection,
)

/**
 * Builds a local preview first. No method in this object can create an outbound prompt without an explicit
 * `userConfirmed=true` argument supplied by the UI/controller handling the user action.
 */
object AiContextHandoffPolicy {
    fun fromFile(insight: FileInsight): AiContextHandoffDraft = fromTextContext(
        kind = AiContextHandoffKind.FILE,
        title = insight.displayName,
        mimeType = insight.mimeType,
        sizeBytes = insight.sizeBytes,
        summary = insight.summary,
        excerpt = insight.preview,
        fallbackTitle = "Ausgewählte Datei",
    )

    fun fromPenSketch(
        title: String,
        summary: String,
        textualDescription: String? = null,
        sizeBytes: Long? = null,
    ): AiContextHandoffDraft = fromTextContext(
        kind = AiContextHandoffKind.PEN_SKETCH,
        title = title,
        mimeType = "image/svg+xml",
        sizeBytes = sizeBytes,
        summary = summary,
        excerpt = textualDescription,
        fallbackTitle = "Ausgewählte Skizze",
    )

    /** URL/navigation locator is intentionally not part of this model. Only the locally selected/extracted text is. */
    fun fromWebPage(
        title: String,
        summary: String,
        selectedText: String? = null,
    ): AiContextHandoffDraft = fromTextContext(
        kind = AiContextHandoffKind.WEB_PAGE,
        title = title,
        mimeType = "text/html",
        sizeBytes = null,
        summary = summary,
        excerpt = selectedText,
        fallbackTitle = "Ausgewählte Webseite",
    )

    /** Binary pixels stay outside prompt state. This represents only a local textual description of one chosen frame. */
    fun fromScreenFrameDescription(
        title: String,
        summary: String,
        localDescription: String? = null,
    ): AiContextHandoffDraft = fromTextContext(
        kind = AiContextHandoffKind.SCREEN_FRAME,
        title = title,
        mimeType = "image/jpeg",
        sizeBytes = null,
        summary = summary,
        excerpt = localDescription,
        fallbackTitle = "Ausgewählter Bildschirmframe",
    )

    /**
     * Android PROCESS_TEXT / ACTION_SEND entry. The caller supplies only text explicitly selected/shared by the user;
     * source package, clipboard history and surrounding document content are intentionally not represented here.
     */
    fun fromSelectedText(
        text: CharSequence,
        title: String? = null,
    ): AiContextHandoffDraft {
        val selected = text.toString().trim().take(AiContextHandoffDraft.MAX_EXCERPT_CHARS)
        require(selected.isNotBlank()) { "Selected text must not be blank" }
        return fromTextContext(
            kind = AiContextHandoffKind.SELECTED_TEXT,
            title = title.orEmpty(),
            mimeType = "text/plain",
            sizeBytes = selected.toByteArray(Charsets.UTF_8).size.toLong(),
            summary = "Vom Nutzer über Android bewusst ausgewählter Text · ${selected.length} Zeichen. Der eigentliche Text bleibt bis zum Auszug-Opt-in lokal.",
            excerpt = selected,
            fallbackTitle = "Ausgewählter Text",
        )
    }

    fun confirm(
        draft: AiContextHandoffDraft,
        userPrompt: String,
        userConfirmed: Boolean,
        selection: AiContextHandoffSelection = AiContextHandoffSelection.MINIMAL,
    ): AiConfirmedContextHandoff {
        require(userConfirmed) { "AI context handoff requires explicit user confirmation" }
        val instruction = userPrompt.trim().take(MAX_USER_PROMPT_CHARS)
        val prompt = buildString {
            if (instruction.isNotBlank()) {
                append(instruction).append("\n\n")
            }
            append("Explizit freigegebener Kontext (${draft.kind.title}):\n")
            if (selection.includeTitle) {
                append("Titel: ").append(draft.title).append('\n')
            }
            if (selection.includeMetadata) {
                draft.mimeType?.let { append("Typ: ").append(it).append('\n') }
                draft.sizeBytes?.let { append("Größe: ").append(it).append(" Bytes\n") }
            }
            if (selection.includeSummary && draft.localSummary.isNotBlank()) {
                append("Lokale Zusammenfassung: ").append(draft.localSummary).append('\n')
            }
            if (selection.includeExcerpt) {
                draft.localExcerpt?.let {
                    append("\nBegrenzter lokaler Auszug:\n").append(it)
                }
            }
        }.take(MAX_CONFIRMED_PROMPT_CHARS)
        return AiConfirmedContextHandoff(
            draftId = draft.id,
            kind = draft.kind,
            prompt = prompt,
            selection = selection,
        )
    }

    private fun fromTextContext(
        kind: AiContextHandoffKind,
        title: String,
        mimeType: String?,
        sizeBytes: Long?,
        summary: String,
        excerpt: String?,
        fallbackTitle: String,
    ): AiContextHandoffDraft = AiContextHandoffDraft(
        id = UUID.randomUUID().toString(),
        kind = kind,
        title = title.trim().ifBlank { fallbackTitle }.take(MAX_TITLE_CHARS),
        mimeType = mimeType?.trim()?.takeIf(String::isNotBlank)?.take(MAX_MIME_CHARS),
        sizeBytes = sizeBytes?.takeIf { it >= 0L },
        localSummary = summary.trim().take(AiContextHandoffDraft.MAX_SUMMARY_CHARS),
        localExcerpt = excerpt
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?.take(AiContextHandoffDraft.MAX_EXCERPT_CHARS),
    )

    private const val MAX_TITLE_CHARS = 240
    private const val MAX_MIME_CHARS = 160
    private const val MAX_USER_PROMPT_CHARS = 4_000
    private const val MAX_CONFIRMED_PROMPT_CHARS = 12_000
}
