package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiHubTaskRouterTest {
    @Test
    fun infer_detectsCoreTaskFamilies() {
        assertEquals(AiHubTaskIntent.LOCAL_PRIVATE, AiHubTaskRouter.infer("Bitte lokal und offline beantworten"))
        assertEquals(AiHubTaskIntent.RESEARCH, AiHubTaskRouter.infer("Recherchiere aktuelle Quellen"))
        assertEquals(AiHubTaskIntent.BROWSER_PAGE, AiHubTaskRouter.infer("Fasse diese Seite zusammen"))
        assertEquals(AiHubTaskIntent.VOICE, AiHubTaskRouter.infer("Starte einen Voice Chat"))
        assertEquals(AiHubTaskIntent.IMAGE, AiHubTaskRouter.infer("Generiere ein Bild"))
        assertEquals(AiHubTaskIntent.SOURCE_NOTEBOOK, AiHubTaskRouter.infer("Öffne mein Quellen-Notebook"))
    }

    @Test
    fun localIntent_prefersInstalledLocalModelOverCloudAndStore() {
        val local = entry(
            id = "ai:local",
            kind = AiHubEntryKind.LOCAL_LLM_APP,
            state = AiHubInstallState.INSTALLED,
        )
        val cloud = entry(
            id = "ai:cloud",
            kind = AiHubEntryKind.LLM_APP,
            state = AiHubInstallState.INSTALLED,
            capabilities = setOf("Voice", "Recherche"),
        )
        val storeLocal = entry(
            id = "ai:store-local",
            kind = AiHubEntryKind.LOCAL_LLM_APP,
            state = AiHubInstallState.STORE_AVAILABLE,
        )

        val ranked = AiHubTaskRouter.rank("Nutze bitte eine lokale private KI", listOf(cloud, storeLocal, local))

        assertEquals("ai:local", ranked.first().entry.stableId)
        assertTrue(ranked.first().reason.contains("lokale Inferenz"))
    }

    @Test
    fun pageIntent_prefersInstalledBrowserWithPageCapabilities() {
        val browser = entry(
            id = "browser:smart",
            kind = AiHubEntryKind.BROWSER,
            state = AiHubInstallState.INSTALLED,
            capabilities = setOf("Seite fragen", "Seite zusammenfassen"),
        )
        val genericAi = entry(
            id = "ai:generic",
            kind = AiHubEntryKind.LLM_APP,
            state = AiHubInstallState.INSTALLED,
            capabilities = setOf("Text"),
        )

        val ranked = AiHubTaskRouter.rank("Fasse diese Webseite zusammen", listOf(genericAi, browser))

        assertEquals("browser:smart", ranked.first().entry.stableId)
        assertTrue(ranked.first().reason.contains("Seiten-Kontext"))
    }

    @Test
    fun installedResearchTarget_beatsStoreOnlyResearchTarget() {
        val installed = entry(
            id = "ai:installed",
            kind = AiHubEntryKind.LLM_APP,
            state = AiHubInstallState.INSTALLED,
            capabilities = setOf("Recherche"),
        )
        val storeOnly = entry(
            id = "ai:store",
            kind = AiHubEntryKind.LLM_APP,
            state = AiHubInstallState.STORE_AVAILABLE,
            capabilities = setOf("Recherche"),
        )

        val ranked = AiHubTaskRouter.rank("Recherchiere das aktuell", listOf(storeOnly, installed))

        assertEquals("ai:installed", ranked.first().entry.stableId)
    }

    @Test
    fun unavailableEntries_areNeverRecommended() {
        val unavailable = entry(
            id = "ai:missing",
            kind = AiHubEntryKind.LLM_APP,
            state = AiHubInstallState.UNAVAILABLE,
            capabilities = setOf("Recherche", "Voice", "Bild"),
        )

        assertTrue(AiHubTaskRouter.rank("Recherche", listOf(unavailable)).isEmpty())
    }

    private fun entry(
        id: String,
        kind: AiHubEntryKind,
        state: AiHubInstallState,
        capabilities: Set<String> = emptySet(),
    ) = AiHubEntry(
        stableId = id,
        title = id,
        subtitle = id,
        kind = kind,
        installState = state,
        aiCapabilities = capabilities,
    )
}
