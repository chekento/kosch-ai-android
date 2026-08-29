package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiHubQuickActionPolicyTest {
    @Test
    fun actionPreservesExistingUserPrompt() {
        val result = AiHubQuickActionPolicy.apply(
            AiHubQuickAction.RESEARCH,
            "Vergleiche drei lokale LLMs",
        )
        assertTrue(result.startsWith(AiHubQuickAction.RESEARCH.instruction))
        assertTrue(result.endsWith("Vergleiche drei lokale LLMs"))
    }

    @Test
    fun sameActionIsIdempotent() {
        val once = AiHubQuickActionPolicy.apply(AiHubQuickAction.LOCAL_PRIVATE, "Analysiere diese Notiz")
        val twice = AiHubQuickActionPolicy.apply(AiHubQuickAction.LOCAL_PRIVATE, once)
        assertEquals(once, twice)
    }

    @Test
    fun quickActionNeverContainsProviderOrPermissionOverride() {
        AiHubQuickAction.entries.forEach { action ->
            val normalized = action.instruction.lowercase()
            assertFalse(normalized.contains("permission"))
            assertFalse(normalized.contains("berechtigung erteilen"))
            assertFalse(normalized.contains("api key"))
            assertFalse(normalized.contains("accessibility"))
        }
    }

    @Test
    fun resultIsBounded() {
        val huge = "x".repeat(AiHubQuickActionPolicy.MAX_PROMPT_CHARS * 2)
        val result = AiHubQuickActionPolicy.apply(AiHubQuickAction.SUMMARIZE, huge)
        assertEquals(AiHubQuickActionPolicy.MAX_PROMPT_CHARS, result.length)
    }
}
