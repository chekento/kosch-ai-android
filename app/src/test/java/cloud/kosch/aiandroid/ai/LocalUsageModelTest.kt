package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.AppUsageSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalUsageModelTest {
    @Test
    fun `observation is bounded and increments only metadata`() {
        val now = 1_000_000L
        val once = LocalUsageModel.observe(emptyMap(), "app", now, limit = 2)
        val twice = LocalUsageModel.observe(once, "app", now + 1, limit = 2)

        assertEquals(2, twice.getValue("app").launchCount)
        assertEquals(now + 1, twice.getValue("app").lastUsedEpochMillis)
        assertEquals(setOf("app"), twice.keys)
    }

    @Test
    fun `recent learned app ranks above unseen app while ties stay stable`() {
        val now = 10_000_000L
        val signals = mapOf("b" to AppUsageSignal("b", 2, now - 1_000))

        assertEquals(listOf("b", "a", "c"), LocalUsageModel.rankKeys(listOf("a", "b", "c"), signals, now))
        assertTrue(LocalUsageModel.score(signals["b"], now) > LocalUsageModel.score(null, now))
    }
}
