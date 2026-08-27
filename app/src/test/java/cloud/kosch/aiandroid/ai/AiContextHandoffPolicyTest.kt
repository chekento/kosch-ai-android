package cloud.kosch.aiandroid.ai

import android.net.Uri
import cloud.kosch.aiandroid.model.FileInsight
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AiContextHandoffPolicyTest {
    private fun insight(
        preview: String? = "Kurzer Dateiinhalt",
        summary: String = "Lokale Zusammenfassung",
    ) = FileInsight(
        uri = Uri.parse("content://test/secret-document"),
        displayName = "Plan Q4.txt",
        mimeType = "text/plain",
        sizeBytes = 42L,
        category = "Dokument",
        summary = summary,
        preview = preview,
        suggestedName = null,
        safetyNote = "lokal",
    )

    @Test
    fun prepareFile_keepsContextLocalAndBounded() {
        val draft = AiContextHandoffPolicy.fromFile(
            insight(
                preview = "x".repeat(20_000),
                summary = "s".repeat(5_000),
            ),
        )
        assertTrue(draft.localExcerpt!!.length <= AiContextHandoffDraft.MAX_EXCERPT_CHARS)
        assertTrue(draft.localSummary.length <= AiContextHandoffDraft.MAX_SUMMARY_CHARS)
        assertFalse(draft.localExcerpt!!.contains("content://test"))
    }

    @Test
    fun confirm_requiresExplicitUserConfirmation() {
        val draft = AiContextHandoffPolicy.fromFile(insight())
        assertThrows(IllegalArgumentException::class.java) {
            AiContextHandoffPolicy.confirm(draft, "Erkläre das", userConfirmed = false)
        }
    }

    @Test
    fun confirmedPrompt_containsBoundedContentButNeverOriginalUri() {
        val draft = AiContextHandoffPolicy.fromFile(insight())
        val confirmed = AiContextHandoffPolicy.confirm(draft, "Erkläre das", userConfirmed = true)
        assertTrue(confirmed.prompt.contains("Plan Q4.txt"))
        assertTrue(confirmed.prompt.contains("Kurzer Dateiinhalt"))
        assertFalse(confirmed.prompt.contains("content://test/secret-document"))
        assertTrue(confirmed.prompt.length <= 12_000)
    }
}
