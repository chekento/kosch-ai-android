package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.LaunchableApp
import java.util.Locale

enum class AiProviderKind(val title: String) {
    LOCAL_OPEN_SOURCE("Lokal & Open Source"),
    CLOUD_HANDOFF("Cloud · bewusste Übergabe"),
}

enum class AiProviderCapability(val title: String) {
    TEXT_CHAT("Text"),
    VOICE("Voice"),
    IMAGE("Bild"),
    CAMERA("Kamera"),
    FILES("Dateien"),
    RESEARCH("Recherche"),
    SOURCE_NOTEBOOK("Quellen-Notebook"),
}

data class AiProviderProfile(
    val id: String,
    val name: String,
    val shortName: String,
    val description: String,
    val webUrl: String,
    val packageHints: Set<String>,
    val labelHints: Set<String>,
    val kind: AiProviderKind,
    val license: String? = null,
    /**
     * Official Play package used only as an install fallback when no launchable app matches.
     * Null means KoSch must fall back to the provider's web/source page instead of guessing a store entry.
     */
    val playStorePackageName: String? = null,
    /** Informational capabilities for routing/UI. Runtime discovery remains authoritative. */
    val capabilities: Set<AiProviderCapability> = setOf(AiProviderCapability.TEXT_CHAT),
)

object AiProviderRegistry {
    val providers = listOf(
        AiProviderProfile(
            id = "pocketpal",
            name = "PocketPal AI",
            shortName = "PAL",
            description = "GGUF-Modelle lokal per llama.cpp · frei und werbefrei",
            webUrl = "https://github.com/a-ghorbani/pocketpal-ai",
            packageHints = setOf("com.pocketpalai"),
            labelHints = setOf("pocketpal", "pocket pal"),
            kind = AiProviderKind.LOCAL_OPEN_SOURCE,
            license = "MIT",
            capabilities = setOf(AiProviderCapability.TEXT_CHAT),
        ),
        AiProviderProfile(
            id = "chatterui",
            name = "ChatterUI",
            shortName = "CUI",
            description = "Lokale GGUF-Modelle oder optionale Backends",
            webUrl = "https://github.com/Vali-98/ChatterUI",
            packageHints = emptySet(),
            labelHints = setOf("chatterui", "chatter ui"),
            kind = AiProviderKind.LOCAL_OPEN_SOURCE,
            license = "AGPL-3.0",
            capabilities = setOf(AiProviderCapability.TEXT_CHAT),
        ),
        AiProviderProfile(
            id = "maid",
            name = "Maid",
            shortName = "MAID",
            description = "Lokale llama.cpp-Inferenz, Modellimport und Chats",
            webUrl = "https://github.com/Mobile-Artificial-Intelligence/maid",
            packageHints = emptySet(),
            labelHints = setOf("maid", "mobile artificial intelligence"),
            kind = AiProviderKind.LOCAL_OPEN_SOURCE,
            license = "MIT",
            capabilities = setOf(AiProviderCapability.TEXT_CHAT),
        ),
        AiProviderProfile(
            id = "chatgpt",
            name = "ChatGPT",
            shortName = "GPT",
            description = "Installierte App, Android-Share oder Play-Store-Fallback",
            webUrl = "https://chatgpt.com/",
            packageHints = setOf("com.openai.chatgpt"),
            labelHints = setOf("chatgpt", "openai"),
            kind = AiProviderKind.CLOUD_HANDOFF,
            playStorePackageName = "com.openai.chatgpt",
            capabilities = setOf(
                AiProviderCapability.TEXT_CHAT,
                AiProviderCapability.VOICE,
                AiProviderCapability.IMAGE,
                AiProviderCapability.CAMERA,
                AiProviderCapability.FILES,
                AiProviderCapability.RESEARCH,
            ),
        ),
        AiProviderProfile(
            id = "gemini",
            name = "Gemini",
            shortName = "GEM",
            description = "Google-App erkennen, sonst direkt zum offiziellen Play-Eintrag",
            webUrl = "https://gemini.google.com/app",
            packageHints = setOf("com.google.android.apps.bard"),
            labelHints = setOf("gemini"),
            kind = AiProviderKind.CLOUD_HANDOFF,
            playStorePackageName = "com.google.android.apps.bard",
            capabilities = setOf(
                AiProviderCapability.TEXT_CHAT,
                AiProviderCapability.VOICE,
                AiProviderCapability.IMAGE,
                AiProviderCapability.CAMERA,
                AiProviderCapability.FILES,
                AiProviderCapability.RESEARCH,
            ),
        ),
        AiProviderProfile(
            id = "claude",
            name = "Claude",
            shortName = "CLD",
            description = "Bewusste Übergabe statt verdeckter App-Steuerung",
            webUrl = "https://claude.ai/new",
            packageHints = setOf("com.anthropic.claude"),
            labelHints = setOf("claude"),
            kind = AiProviderKind.CLOUD_HANDOFF,
            playStorePackageName = "com.anthropic.claude",
            capabilities = setOf(
                AiProviderCapability.TEXT_CHAT,
                AiProviderCapability.VOICE,
                AiProviderCapability.CAMERA,
                AiProviderCapability.FILES,
                AiProviderCapability.RESEARCH,
            ),
        ),
        AiProviderProfile(
            id = "grok",
            name = "Grok",
            shortName = "GRK",
            description = "Installierte App erkennen; fehlende App führt zum Play Store",
            webUrl = "https://grok.com/",
            packageHints = setOf("ai.x.grok"),
            labelHints = setOf("grok"),
            kind = AiProviderKind.CLOUD_HANDOFF,
            playStorePackageName = "ai.x.grok",
            capabilities = setOf(
                AiProviderCapability.TEXT_CHAT,
                AiProviderCapability.VOICE,
                AiProviderCapability.IMAGE,
                AiProviderCapability.CAMERA,
                AiProviderCapability.RESEARCH,
            ),
        ),
        AiProviderProfile(
            id = "meta-ai",
            name = "Meta AI",
            shortName = "META",
            description = "Explizite Übergabe über Android-Schnittstellen",
            webUrl = "https://www.meta.ai/",
            packageHints = setOf("com.facebook.stella"),
            labelHints = setOf("meta ai"),
            kind = AiProviderKind.CLOUD_HANDOFF,
            playStorePackageName = "com.facebook.stella",
            capabilities = setOf(
                AiProviderCapability.TEXT_CHAT,
                AiProviderCapability.VOICE,
                AiProviderCapability.IMAGE,
                AiProviderCapability.CAMERA,
                AiProviderCapability.FILES,
                AiProviderCapability.RESEARCH,
            ),
        ),
        AiProviderProfile(
            id = "perplexity",
            name = "Perplexity",
            shortName = "PPLX",
            description = "Recherche-App, Android-Share oder Play-Store-Fallback",
            webUrl = "https://www.perplexity.ai/",
            packageHints = setOf("ai.perplexity.app.android", "ai.perplexity.app.android.assistant"),
            labelHints = setOf("perplexity"),
            kind = AiProviderKind.CLOUD_HANDOFF,
            playStorePackageName = "ai.perplexity.app.android",
            capabilities = setOf(
                AiProviderCapability.TEXT_CHAT,
                AiProviderCapability.VOICE,
                AiProviderCapability.FILES,
                AiProviderCapability.RESEARCH,
            ),
        ),
        AiProviderProfile(
            id = "notebooklm",
            name = "NotebookLM",
            shortName = "NBLM",
            description = "Quellenarbeit, Notebook-App oder offizieller Play-Eintrag",
            webUrl = "https://notebooklm.google.com/",
            packageHints = setOf("com.google.android.apps.labs.language.tailwind"),
            labelHints = setOf("notebooklm", "notebook lm", "gemini notebook"),
            kind = AiProviderKind.CLOUD_HANDOFF,
            playStorePackageName = "com.google.android.apps.labs.language.tailwind",
            capabilities = setOf(
                AiProviderCapability.TEXT_CHAT,
                AiProviderCapability.FILES,
                AiProviderCapability.RESEARCH,
                AiProviderCapability.SOURCE_NOTEBOOK,
            ),
        ),
        AiProviderProfile(
            id = "deepseek",
            name = "DeepSeek",
            shortName = "DS",
            description = "Offizielle App erkennen oder Play-Store-Eintrag öffnen",
            webUrl = "https://chat.deepseek.com/",
            packageHints = setOf("com.deepseek.chat"),
            labelHints = setOf("deepseek"),
            kind = AiProviderKind.CLOUD_HANDOFF,
            playStorePackageName = "com.deepseek.chat",
            capabilities = setOf(
                AiProviderCapability.TEXT_CHAT,
                AiProviderCapability.FILES,
                AiProviderCapability.RESEARCH,
            ),
        ),
        AiProviderProfile(
            id = "copilot",
            name = "Microsoft Copilot",
            shortName = "COP",
            description = "Microsoft-App erkennen oder Play-Store-Eintrag öffnen",
            webUrl = "https://copilot.microsoft.com/",
            packageHints = setOf("com.microsoft.copilot"),
            labelHints = setOf("copilot"),
            kind = AiProviderKind.CLOUD_HANDOFF,
            playStorePackageName = "com.microsoft.copilot",
            capabilities = setOf(
                AiProviderCapability.TEXT_CHAT,
                AiProviderCapability.VOICE,
                AiProviderCapability.IMAGE,
                AiProviderCapability.RESEARCH,
            ),
        ),
        AiProviderProfile(
            id = "poe",
            name = "Poe",
            shortName = "POE",
            description = "Multi-Modell-App erkennen oder Play-Store-Eintrag öffnen",
            webUrl = "https://poe.com/",
            packageHints = setOf("com.poe.android"),
            labelHints = setOf("poe"),
            kind = AiProviderKind.CLOUD_HANDOFF,
            playStorePackageName = "com.poe.android",
            capabilities = setOf(
                AiProviderCapability.TEXT_CHAT,
                AiProviderCapability.IMAGE,
                AiProviderCapability.FILES,
            ),
        ),
        AiProviderProfile(
            id = "mistral",
            name = "Vibe by Mistral",
            shortName = "VIBE",
            description = "Mistral-App (ehemals Le Chat) erkennen oder Store-Fallback",
            webUrl = "https://chat.mistral.ai/",
            packageHints = setOf("ai.mistral.chat"),
            labelHints = setOf("mistral", "le chat", "vibe"),
            kind = AiProviderKind.CLOUD_HANDOFF,
            playStorePackageName = "ai.mistral.chat",
            capabilities = setOf(
                AiProviderCapability.TEXT_CHAT,
                AiProviderCapability.FILES,
                AiProviderCapability.RESEARCH,
            ),
        ),
    )

    fun installedApp(
        provider: AiProviderProfile,
        apps: List<LaunchableApp>,
    ): LaunchableApp? = apps.firstOrNull { app ->
        app.packageName in provider.packageHints || provider.labelHints.any { hint ->
            app.label.lowercase(Locale.ROOT).contains(hint)
        }
    }

    fun installedProviderPackages(apps: List<LaunchableApp>): Set<String> = providers
        .mapNotNull { installedApp(it, apps)?.packageName }
        .toSet()
}
