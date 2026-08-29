package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiHubDecisionPolicyTest {
    @Test
    fun largeMarginOnInstalledTargetIsHighConfidence() {
        val decision = AiHubDecisionPolicy.decide(
            listOf(
                recommendation("ai:primary", 220, AiHubInstallState.INSTALLED),
                recommendation("ai:second", 160, AiHubInstallState.INSTALLED),
            ),
        )

        assertEquals(AiHubDecisionConfidence.HIGH, decision!!.confidence)
        assertEquals(60, decision.scoreMargin)
        assertTrue(decision.explanation.contains("60"))
    }

    @Test
    fun closeScoresStayLowConfidenceInsteadOfClaimingUniqueBest() {
        val decision = AiHubDecisionPolicy.decide(
            listOf(
                recommendation("ai:first", 201, AiHubInstallState.INSTALLED),
                recommendation("ai:second", 197, AiHubInstallState.INSTALLED),
            ),
        )

        assertEquals(AiHubDecisionConfidence.LOW, decision!!.confidence)
        assertEquals("ai:second", decision.alternatives.single().entry.stableId)
    }

    @Test
    fun singleInstalledTargetIsHighConfidence() {
        val decision = AiHubDecisionPolicy.decide(
            listOf(recommendation("ai:local", 300, AiHubInstallState.INSTALLED)),
        )

        assertEquals(AiHubDecisionConfidence.HIGH, decision!!.confidence)
        assertNull(decision.scoreMargin)
    }

    @Test
    fun singleStoreTargetIsOnlyMediumConfidence() {
        val decision = AiHubDecisionPolicy.decide(
            listOf(recommendation("ai:store", 50, AiHubInstallState.STORE_AVAILABLE)),
        )

        assertEquals(AiHubDecisionConfidence.MEDIUM, decision!!.confidence)
    }

    private fun recommendation(
        id: String,
        score: Int,
        state: AiHubInstallState,
    ) = AiHubRecommendation(
        entry = AiHubEntry(
            stableId = id,
            title = id,
            subtitle = id,
            kind = AiHubEntryKind.LLM_APP,
            installState = state,
        ),
        intent = AiHubTaskIntent.GENERAL_CHAT,
        score = score,
        reason = "Basis",
    )
}
