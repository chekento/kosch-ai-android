package cloud.kosch.aiandroid.ai

import java.util.Locale

enum class AiHubTaskIntent(val title: String) {
    GENERAL_CHAT("Allgemeiner KI-Dialog"),
    RESEARCH("Recherche"),
    BROWSER_PAGE("Seiten-Kontext"),
    LOCAL_PRIVATE("Lokal & privat"),
    VOICE("Voice"),
    IMAGE("Bild"),
    SOURCE_NOTEBOOK("Quellen-Notebook"),
}

data class AiHubRecommendation(
    val entry: AiHubEntry,
    val intent: AiHubTaskIntent,
    val score: Int,
    val reason: String,
)

/**
 * Explainable, offline recommendation layer for the AI & Browser Hub.
 *
 * It never invents a capability and never treats a store listing as equivalent to an installed target. Ranking uses
 * only the user's text, registry/runtime capability projection and actual install state. A future model may propose an
 * intent, but the deterministic scorer remains the authority for availability and safe routing.
 */
object AiHubTaskRouter {
    fun infer(prompt: String): AiHubTaskIntent {
        val text = prompt.trim().lowercase(Locale.ROOT)
        if (text.isBlank()) return AiHubTaskIntent.GENERAL_CHAT

        return when {
            containsAny(text, SOURCE_NOTEBOOK_TERMS) -> AiHubTaskIntent.SOURCE_NOTEBOOK
            containsAny(text, LOCAL_TERMS) -> AiHubTaskIntent.LOCAL_PRIVATE
            containsAny(text, PAGE_TERMS) -> AiHubTaskIntent.BROWSER_PAGE
            containsAny(text, IMAGE_TERMS) -> AiHubTaskIntent.IMAGE
            containsAny(text, VOICE_TERMS) -> AiHubTaskIntent.VOICE
            containsAny(text, RESEARCH_TERMS) -> AiHubTaskIntent.RESEARCH
            else -> AiHubTaskIntent.GENERAL_CHAT
        }
    }

    fun rank(
        prompt: String,
        entries: List<AiHubEntry>,
        limit: Int = 4,
    ): List<AiHubRecommendation> {
        if (limit <= 0) return emptyList()
        val intent = infer(prompt)
        return entries
            .asSequence()
            .filter { it.installState != AiHubInstallState.UNAVAILABLE }
            .map { entry ->
                AiHubRecommendation(
                    entry = entry,
                    intent = intent,
                    score = score(entry, intent),
                    reason = reason(entry, intent),
                )
            }
            .sortedWith(
                compareByDescending<AiHubRecommendation> { it.score }
                    .thenBy { it.entry.title.lowercase(Locale.ROOT) },
            )
            .take(limit)
            .toList()
    }

    private fun score(entry: AiHubEntry, intent: AiHubTaskIntent): Int {
        var score = when (entry.installState) {
            AiHubInstallState.INSTALLED -> 120
            AiHubInstallState.SYSTEM_AVAILABLE -> 75
            AiHubInstallState.STORE_AVAILABLE -> 20
            AiHubInstallState.WEB_ONLY -> 12
            AiHubInstallState.UNAVAILABLE -> -1_000
        }

        score += when (entry.kind) {
            AiHubEntryKind.LOCAL_LLM_APP -> 28
            AiHubEntryKind.LLM_APP -> 22
            AiHubEntryKind.BROWSER -> 10
            AiHubEntryKind.SYSTEM_BROWSER -> 4
        }

        val capabilities = entry.aiCapabilities
        when (intent) {
            AiHubTaskIntent.GENERAL_CHAT -> {
                if (entry.kind == AiHubEntryKind.LOCAL_LLM_APP) score += 45
                if (entry.kind == AiHubEntryKind.LLM_APP) score += 40
                if (entry.kind == AiHubEntryKind.BROWSER && capabilities.isNotEmpty()) score += 10
            }

            AiHubTaskIntent.RESEARCH -> {
                if ("Recherche" in capabilities) score += 110
                if (entry.kind == AiHubEntryKind.BROWSER) score += 35
                if ("Quellen-Notebook" in capabilities) score += 25
            }

            AiHubTaskIntent.BROWSER_PAGE -> {
                if ("Seite fragen" in capabilities) score += 125
                if ("Seite zusammenfassen" in capabilities) score += 125
                if (entry.kind == AiHubEntryKind.BROWSER) score += 45
                if (entry.kind == AiHubEntryKind.SYSTEM_BROWSER) score += 20
            }

            AiHubTaskIntent.LOCAL_PRIVATE -> {
                if (entry.kind == AiHubEntryKind.LOCAL_LLM_APP) score += 190
                if (entry.kind == AiHubEntryKind.LLM_APP) score -= 45
                if (entry.kind == AiHubEntryKind.BROWSER || entry.kind == AiHubEntryKind.SYSTEM_BROWSER) score -= 65
            }

            AiHubTaskIntent.VOICE -> {
                if ("Voice" in capabilities) score += 145
                if (entry.kind == AiHubEntryKind.LLM_APP) score += 20
            }

            AiHubTaskIntent.IMAGE -> {
                if ("Bild" in capabilities || "Bildgenerierung" in capabilities) score += 145
                if (entry.kind == AiHubEntryKind.LLM_APP) score += 20
            }

            AiHubTaskIntent.SOURCE_NOTEBOOK -> {
                if ("Quellen-Notebook" in capabilities) score += 210
                if ("Dateien" in capabilities) score += 55
                if ("Recherche" in capabilities) score += 35
            }
        }
        return score
    }

    private fun reason(entry: AiHubEntry, intent: AiHubTaskIntent): String {
        val availability = when (entry.installState) {
            AiHubInstallState.INSTALLED -> "Installiert"
            AiHubInstallState.SYSTEM_AVAILABLE -> "Android-Standard"
            AiHubInstallState.STORE_AVAILABLE -> "Installierbar"
            AiHubInstallState.WEB_ONLY -> "Web verfügbar"
            AiHubInstallState.UNAVAILABLE -> "Nicht verfügbar"
        }
        val fit = when (intent) {
            AiHubTaskIntent.GENERAL_CHAT -> when (entry.kind) {
                AiHubEntryKind.LOCAL_LLM_APP -> "lokale KI ohne Cloud-Zwang"
                AiHubEntryKind.LLM_APP -> "KI-Dialog"
                AiHubEntryKind.BROWSER -> "Browser mit KI-Funktionen"
                AiHubEntryKind.SYSTEM_BROWSER -> "neutraler Browserweg"
            }

            AiHubTaskIntent.RESEARCH -> when {
                "Recherche" in entry.aiCapabilities -> "Recherche-Fähigkeit"
                entry.kind == AiHubEntryKind.BROWSER -> "Browser-Recherche"
                else -> "allgemeiner KI-Zugang"
            }

            AiHubTaskIntent.BROWSER_PAGE -> when {
                "Seite fragen" in entry.aiCapabilities || "Seite zusammenfassen" in entry.aiCapabilities -> "Seiten-Kontext"
                entry.kind == AiHubEntryKind.BROWSER -> "Browser-Ziel"
                else -> "ergänzender KI-Zugang"
            }

            AiHubTaskIntent.LOCAL_PRIVATE -> if (entry.kind == AiHubEntryKind.LOCAL_LLM_APP) {
                "lokale Inferenz bevorzugt"
            } else {
                "nur als Fallback"
            }

            AiHubTaskIntent.VOICE -> if ("Voice" in entry.aiCapabilities) "Voice-Fähigkeit" else "KI-Fallback"
            AiHubTaskIntent.IMAGE -> if (
                "Bild" in entry.aiCapabilities || "Bildgenerierung" in entry.aiCapabilities
            ) {
                "Bild-Fähigkeit"
            } else {
                "KI-Fallback"
            }

            AiHubTaskIntent.SOURCE_NOTEBOOK -> when {
                "Quellen-Notebook" in entry.aiCapabilities -> "Quellen-Notebook"
                "Dateien" in entry.aiCapabilities -> "Datei-Kontext"
                else -> "Recherche-Fallback"
            }
        }
        return "$availability · $fit"
    }

    private fun containsAny(text: String, terms: Set<String>): Boolean = terms.any(text::contains)

    private val SOURCE_NOTEBOOK_TERMS = setOf(
        "notebooklm", "notebook lm", "quellen-notebook", "quellen notebook", "source notebook",
    )
    private val LOCAL_TERMS = setOf(
        "lokal", "offline", "privat", "on-device", "on device", "ohne cloud", "ohne internet",
    )
    private val PAGE_TERMS = setOf(
        "diese seite", "webseite zusammen", "seite zusammen", "artikel zusammen", "frag die seite",
        "ask page", "summarize page", "summarise page", "this page",
    )
    private val IMAGE_TERMS = setOf(
        "bild", "grafik", "image", "picture", "foto", "illustration", "generiere ein",
    )
    private val VOICE_TERMS = setOf(
        "voice", "sprich", "sprache", "sprachchat", "audio", "vorlesen",
    )
    private val RESEARCH_TERMS = setOf(
        "recherche", "recherchier", "research", "quelle", "aktuell", "latest", "websuche", "suche im web",
    )
}
