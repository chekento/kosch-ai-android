package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AiContextHandoffSelectionTest {
    private val draft = AiContextHandoffPolicy.fromWebPage(
        title = "Interne Projektseite",
        summary = "Kurze lokale Zusammenfassung",
        selectedText = "Längerer ausgewählter Seitentext",
    )

    @Test
    fun minimalSelection_excludesMetadataAndExcerpt() {
        val confirmed = AiContextHandoffPolicy.confirm(
            draft = draft,
            userPrompt = "Erkläre das",
            userConfirmed = true,
        )
        assertTrue(confirmed.prompt.contains("Interne Projektseite"))
        assertTrue(confirmed.prompt.contains("Kurze lokale Zusammenfassung"))
        assertFalse(confirmed.prompt.contains("text/html"))
        assertFalse(confirmed.prompt.contains("Längerer ausgewählter Seitentext"))
        assertTrue(confirmed.selection == AiContextHandoffSelection.MINIMAL)
    }

    @Test
    fun excerptAndMetadata_requireExplicitSelection() {
        val confirmed = AiContextHandoffPolicy.confirm(
            draft = draft,
            userPrompt = "",
            userConfirmed = true,
            selection = AiContextHandoffSelection.FULL_TEXT_PREVIEW,
        )
        assertTrue(confirmed.prompt.contains("text/html"))
        assertTrue(confirmed.prompt.contains("Längerer ausgewählter Seitentext"))
    }

    @Test
    fun noContextFields_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            AiContextHandoffSelection(
                includeTitle = false,
                includeMetadata = false,
                includeSummary = false,
                includeExcerpt = false,
            )
        }
    }

    @Test
    fun webpageFactory_neverAcceptsOrStoresUrlLocator() {
        assertFalse(draft.title.contains("http", ignoreCase = true))
        assertFalse((draft.localExcerpt ?: "").contains("http", ignoreCase = true))
    }
}
