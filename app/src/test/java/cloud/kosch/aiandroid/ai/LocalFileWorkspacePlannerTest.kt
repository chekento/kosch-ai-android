package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalFileWorkspacePlannerTest {
    @Test
    fun validatesNamesWithoutAllowingPathsOrControlCharacters() {
        assertEquals("Projekt 2026", LocalFileWorkspacePlanner.validateName("  Projekt 2026 ").getOrThrow())
        assertTrue(LocalFileWorkspacePlanner.validateName("../secret").isFailure)
        assertTrue(LocalFileWorkspacePlanner.validateName("a/b").isFailure)
        assertTrue(LocalFileWorkspacePlanner.validateName("a\\b").isFailure)
        assertTrue(LocalFileWorkspacePlanner.validateName("x\u0000y").isFailure)
        assertFalse(LocalFileWorkspacePlanner.validateName("Bericht.final.pdf").isFailure)
    }

    @Test
    fun producesContentFreeBoundedWorkspaceSignals() {
        val entries = listOf(
            fact("A.pdf", "application/pdf", 20L),
            fact("a.pdf", "application/pdf", 40L),
            fact("Bild.png", "image/png", 30L),
            fact("Projekte", LocalFileWorkspacePlanner.DIRECTORY_MIME_TYPE, null, directory = true),
        )

        val summary = LocalFileWorkspacePlanner.analyzeFacts(entries)

        assertEquals(3, summary.fileCount)
        assertEquals(1, summary.directoryCount)
        assertEquals(90L, summary.knownBytes)
        assertEquals(1, summary.duplicateNameGroups)
        assertEquals(2, summary.categoryCounts["PDF"])
        assertEquals(listOf("a.pdf", "Bild.png", "A.pdf"), summary.largestFiles)
    }

    private fun fact(
        name: String,
        mime: String,
        size: Long?,
        directory: Boolean = false,
    ) = LocalFileWorkspacePlanner.Fact(
        displayName = name,
        sizeBytes = size,
        isDirectory = directory,
        category = LocalFileWorkspacePlanner.categoryFor(mime, name),
    )
}
