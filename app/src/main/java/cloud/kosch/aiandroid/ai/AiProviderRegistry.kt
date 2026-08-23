package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.LaunchableApp
import java.util.Locale

enum class AiProviderKind(val title: String) {
    LOCAL_OPEN_SOURCE("Lokal & Open Source"),
    CLOUD_HANDOFF("Cloud · bewusste Übergabe"),
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
        ),
        AiProviderProfile(
            id = "chatgpt",
            name = "ChatGPT",
            shortName = "GPT",
            description = "Per Share-Intent an die App oder im Web öffnen",
            webUrl = "https://chatgpt.com/",
            packageHints = setOf("com.openai.chatgpt"),
            labelHints = setOf("chatgpt", "openai"),
            kind = AiProviderKind.CLOUD_HANDOFF,
        ),
        AiProviderProfile(
            id = "gemini",
            name = "Gemini",
            shortName = "GEM",
            description = "Google-App erkennen, sonst Web-Fallback",
            webUrl = "https://gemini.google.com/app",
            packageHints = setOf("com.google.android.apps.bard"),
            labelHints = setOf("gemini"),
            kind = AiProviderKind.CLOUD_HANDOFF,
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
        ),
        AiProviderProfile(
            id = "grok",
            name = "Grok",
            shortName = "GRK",
            description = "Installierte App nach Profil erkennen oder Web öffnen",
            webUrl = "https://grok.com/",
            packageHints = setOf("ai.x.grok"),
            labelHints = setOf("grok"),
            kind = AiProviderKind.CLOUD_HANDOFF,
        ),
        AiProviderProfile(
            id = "meta-ai",
            name = "Meta AI",
            shortName = "META",
            description = "Explizite Übergabe über Android-Schnittstellen",
            webUrl = "https://www.meta.ai/",
            packageHints = setOf("com.meta.ai"),
            labelHints = setOf("meta ai"),
            kind = AiProviderKind.CLOUD_HANDOFF,
        ),
        AiProviderProfile(
            id = "perplexity",
            name = "Perplexity",
            shortName = "PPLX",
            description = "Recherche-App oder Web als Ziel",
            webUrl = "https://www.perplexity.ai/",
            packageHints = setOf("ai.perplexity.app.android", "ai.perplexity.app.android.assistant"),
            labelHints = setOf("perplexity"),
            kind = AiProviderKind.CLOUD_HANDOFF,
        ),
        AiProviderProfile(
            id = "notebooklm",
            name = "NotebookLM",
            shortName = "NBLM",
            description = "NotebookLM erkennen oder Browser öffnen",
            webUrl = "https://notebooklm.google.com/",
            packageHints = setOf("com.google.android.apps.labs.language.tailwind"),
            labelHints = setOf("notebooklm", "notebook lm"),
            kind = AiProviderKind.CLOUD_HANDOFF,
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
