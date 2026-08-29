package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiHubRoutingContextTest {
    @Test
    fun offlineContextKeepsInstalledLocalTargetAheadOfCloudPreferenceCandidates() {
        val local = recommendation(
            id = "ai:local",
            kind = AiHubEntryKind.LOCAL_LLM_APP,
            state = AiHubInstallState.INSTALLED,
            score = 100,
            capabilities = setOf("Text"),
        )
        val cloud = recommendation(
            id = "ai:cloud",
            kind = AiHubEntryKind.LLM_APP,
            state = AiHubInstallState.INSTALLED,
            score = 300,
            capabilities = setOf("Text", "Recherche"),
        )

        val ranked = AiHubContextPolicy.apply(
            AiHubRoutingContext(signals = setOf(AiHubContextSignal.OFFLINE)),
            listOf(cloud, local),
        )

        assertEquals(listOf("ai:local"), ranked.map { it.entry.stableId })
        assertTrue(ranked.single().reason.contains("offline lokal"))
    }

    @Test
    fun fileContextBoostsFileCapableTargetWithoutInspectingFileData() {
        val files = recommendation(
            id = "ai:files",
            kind = AiHubEntryKind.LLM_APP,
            state = AiHubInstallState.INSTALLED,
            score = 100,
            capabilities = setOf("Text", "Dateien"),
        )
        val plain = recommendation(
            id = "ai:plain",
            kind = AiHubEntryKind.LLM_APP,
            state = AiHubInstallState.INSTALLED,
            score = 120,
            capabilities = setOf("Text"),
        )

        val ranked = AiHubContextPolicy.apply(
            AiHubRoutingContext(
                origin = AiHubOrigin.FILE,
                signals = setOf(AiHubContextSignal.FILE_CONTEXT),
            ),
            listOf(plain, files),
        )

        assertEquals("ai:files", ranked.first().entry.stableId)
        assertEquals("Datei-Kontext", AiHubRoutingContext(AiHubOrigin.FILE, setOf(AiHubContextSignal.FILE_CONTEXT)).summary)
    }

    @Test
    fun personalAudioDoesNotTurnGeneralChatIntoVoice() {
        val voice = recommendation(
            id = "ai:voice",
            kind = AiHubEntryKind.LLM_APP,
            state = AiHubInstallState.INSTALLED,
            score = 100,
            capabilities = setOf("Text", "Voice"),
            intent = AiHubTaskIntent.GENERAL_CHAT,
        )
        val plain = recommendation(
            id = "ai:plain",
            kind = AiHubEntryKind.LLM_APP,
            state = AiHubInstallState.INSTALLED,
            score = 101,
            capabilities = setOf("Text"),
            intent = AiHubTaskIntent.GENERAL_CHAT,
        )

        val ranked = AiHubContextPolicy.apply(
            AiHubRoutingContext(signals = setOf(AiHubContextSignal.PERSONAL_AUDIO)),
            listOf(voice, plain),
        )

        assertEquals("ai:plain", ranked.first().entry.stableId)
        assertFalse(ranked.first().reason.contains("Audio bereit"))
    }

    @Test
    fun routingContextContainsOnlyAbstractSignals() {
        val context = AiHubRoutingContext(
            origin = AiHubOrigin.PEN,
            signals = setOf(AiHubContextSignal.PEN_CONTEXT, AiHubContextSignal.OFFLINE),
        )

        assertEquals(AiHubOrigin.PEN, context.origin)
        assertTrue(context.summary!!.contains("Pen-Kontext"))
        assertTrue(context.summary!!.contains("Offline"))
    }

    private fun recommendation(
        id: String,
        kind: AiHubEntryKind,
        state: AiHubInstallState,
        score: Int,
        capabilities: Set<String>,
        intent: AiHubTaskIntent = AiHubTaskIntent.GENERAL_CHAT,
    ) = AiHubRecommendation(
        entry = AiHubEntry(
            stableId = id,
            title = id,
            subtitle = id,
            kind = kind,
            installState = state,
            aiCapabilities = capabilities,
        ),
        intent = intent,
        score = score,
        reason = "Basis",
    )
}
