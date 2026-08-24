package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.FaqCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FaqRegistryTest {
    @Test
    fun `faq covers the launcher safety and pen surface broadly`() {
        assertTrue(FaqRegistry.entries.size >= 57)
        assertTrue(FaqRegistry.entries.any { it.id == "escape" })
        assertTrue(FaqRegistry.entries.any { it.id == "pen-space" })
        assertTrue(FaqRegistry.entries.any { it.id == "private-space" })
        assertTrue(FaqRegistry.entries.any { it.id == "secure-backup" })
        assertTrue(FaqRegistry.entries.any { it.id == "audit" })
        assertTrue(FaqRegistry.entries.any { it.id == "m2-4-rating" })
        assertTrue(FaqRegistry.entries.any { it.id == "quality-gates" })
        assertTrue(FaqRegistry.entries.any { it.id == "process-recovery" })
        assertTrue(FaqRegistry.entries.any { it.id == "pen-svg" })
        assertTrue(FaqRegistry.entries.any { it.id == "local-learning" })
        assertTrue(FaqRegistry.entries.any { it.id == "file-workspace" })
        assertTrue(FaqRegistry.entries.any { it.id == "file-workspace-safety" })
        assertTrue(FaqRegistry.entries.any { it.id == "file-refresh" })
        assertTrue(FaqRegistry.entries.any { it.id == "manual-folders" })
        assertTrue(FaqRegistry.entries.any { it.id == "messages" })
        assertTrue(FaqRegistry.entries.any { it.id == "work-profile-pause" })
        assertTrue(FaqRegistry.entries.any { it.id == "system-note" })
    }

    @Test
    fun `search is punctuation tolerant`() {
        val result = FaqRegistry.search("S-Pen")
        assertTrue(result.any { it.category == FaqCategory.PEN })
    }

    @Test
    fun `category filter never leaks another category`() {
        val result = FaqRegistry.search("", FaqCategory.AI)
        assertTrue(result.isNotEmpty())
        assertEquals(setOf(FaqCategory.AI), result.map { it.category }.toSet())
    }

    @Test
    fun `private space answer does not overclaim access`() {
        val answer = FaqRegistry.entries.first { it.id == "private-space" }.answer
        assertTrue(answer.contains("noch nicht"))
        assertTrue(answer.contains("ACCESS_HIDDEN_PROFILES"))
    }
}
