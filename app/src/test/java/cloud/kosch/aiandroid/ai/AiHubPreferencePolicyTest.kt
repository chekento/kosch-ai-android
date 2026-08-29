package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiHubPreferencePolicyTest {
    @Test
    fun localPrivate_neverAllowsCloudPreference() {
        val cloud = entry("ai:cloud", AiHubEntryKind.LLM_APP, setOf("Text"))
        val local = entry("ai:local", AiHubEntryKind.LOCAL_LLM_APP, setOf("Text"))

        assertFalse(AiHubPreferencePolicy.canPrefer(AiHubTaskIntent.LOCAL_PRIVATE, cloud))
        assertTrue(AiHubPreferencePolicy.canPrefer(AiHubTaskIntent.LOCAL_PRIVATE, local))
    }

    @Test
    fun preferenceOnlyAppliesToReadySemanticallyValidTarget() {
        val research = entry("ai:research", AiHubEntryKind.LLM_APP, setOf("Recherche"))
        val generic = entry("ai:generic", AiHubEntryKind.LLM_APP, setOf("Text"))
        val ranked = listOf(
            recommendation(generic, 200),
            recommendation(research, 180),
        )

        val applied = AiHubPreferencePolicy.apply(
            AiHubTaskIntent.RESEARCH,
            "ai:research",
            ranked,
        )

        assertEquals("ai:research", applied.first().entry.stableId)
        assertTrue(applied.first().reason.startsWith("Bevorzugt"))
    }

    @Test
    fun storeOnlyTargetCannotBecomePreference() {
        val installLater = AiHubEntry(
            stableId = "ai:later",
            title = "Later",
            subtitle = "Later",
            kind = AiHubEntryKind.LLM_APP,
            installState = AiHubInstallState.STORE_AVAILABLE,
            aiCapabilities = setOf("Recherche"),
        )

        assertFalse(AiHubPreferencePolicy.canPrefer(AiHubTaskIntent.RESEARCH, installLater))
    }

    private fun entry(
        id: String,
        kind: AiHubEntryKind,
        capabilities: Set<String>,
    ) = AiHubEntry(
        stableId = id,
        title = id,
        subtitle = id,
        kind = kind,
        installState = AiHubInstallState.INSTALLED,
        aiCapabilities = capabilities,
    )

    private fun recommendation(entry: AiHubEntry, score: Int) = AiHubRecommendation(
        entry = entry,
        intent = AiHubTaskIntent.RESEARCH,
        score = score,
        reason = "Installiert",
    )
}
