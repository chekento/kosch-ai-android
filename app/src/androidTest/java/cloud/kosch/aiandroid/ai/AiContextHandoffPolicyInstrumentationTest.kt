package cloud.kosch.aiandroid.ai

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.model.FileInsight
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiContextHandoffPolicyInstrumentationTest {
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
        assertFalse(draft.localExcerpt.contains("content://test"))
    }

    @Test
    fun confirm_requiresExplicitUserConfirmation() {
        val draft = AiContextHandoffPolicy.fromFile(insight())
        assertThrows(IllegalArgumentException::class.java) {
            AiContextHandoffPolicy.confirm(draft, "Erkläre das", userConfirmed = false)
        }
    }

    @Test
    fun confirmedPrompt_defaultsToMinimalContextAndRequiresExcerptOptIn() {
        val draft = AiContextHandoffPolicy.fromFile(insight())

        val minimal = AiContextHandoffPolicy.confirm(
            draft = draft,
            userPrompt = "Erkläre das",
            userConfirmed = true,
        )
        assertTrue(minimal.prompt.contains("Plan Q4.txt"))
        assertTrue(minimal.prompt.contains("Lokale Zusammenfassung"))
        assertFalse(minimal.prompt.contains("Kurzer Dateiinhalt"))
        assertFalse(minimal.prompt.contains("content://test/secret-document"))
        assertTrue(minimal.prompt.length <= 12_000)

        val withExcerpt = AiContextHandoffPolicy.confirm(
            draft = draft,
            userPrompt = "Erkläre das",
            userConfirmed = true,
            selection = AiContextHandoffSelection.SUMMARY_AND_EXCERPT,
        )
        assertTrue(withExcerpt.prompt.contains("Plan Q4.txt"))
        assertTrue(withExcerpt.prompt.contains("Lokale Zusammenfassung"))
        assertTrue(withExcerpt.prompt.contains("Kurzer Dateiinhalt"))
        assertFalse(withExcerpt.prompt.contains("content://test/secret-document"))
        assertTrue(withExcerpt.prompt.length <= 12_000)
    }
}
