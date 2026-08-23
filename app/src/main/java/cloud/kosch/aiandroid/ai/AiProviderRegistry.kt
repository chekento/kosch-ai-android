package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.LaunchableApp
import java.util.Locale

data class AiProviderProfile(
    val id: String,
    val name: String,
    val shortName: String,
    val description: String,
    val webUrl: String,
    val packageHints: Set<String>,
    val labelHints: Set<String>,
)

object AiProviderRegistry {
    val providers = listOf(
        AiProviderProfile(
            id = "chatgpt",
            name = "ChatGPT",
            shortName = "GPT",
            description = "Per Share-Intent an die App oder im Web öffnen",
            webUrl = "https://chatgpt.com/",
            packageHints = setOf("com.openai.chatgpt"),
            labelHints = setOf("chatgpt", "openai"),
        ),
        AiProviderProfile(
            id = "gemini",
            name = "Gemini",
            shortName = "GEM",
            description = "Google-App erkennen, sonst Web-Fallback",
            webUrl = "https://gemini.google.com/app",
            packageHints = setOf("com.google.android.apps.bard"),
            labelHints = setOf("gemini"),
        ),
        AiProviderProfile(
            id = "claude",
            name = "Claude",
            shortName = "CLD",
            description = "Bewusste Übergabe statt verdeckter App-Steuerung",
            webUrl = "https://claude.ai/new",
            packageHints = setOf("com.anthropic.claude"),
            labelHints = setOf("claude"),
        ),
        AiProviderProfile(
            id = "grok",
            name = "Grok",
            shortName = "GRK",
            description = "Installierte App nach Profil erkennen oder Web öffnen",
            webUrl = "https://grok.com/",
            packageHints = setOf("ai.x.grok"),
            labelHints = setOf("grok"),
        ),
        AiProviderProfile(
            id = "meta-ai",
            name = "Meta AI",
            shortName = "META",
            description = "Explizite Übergabe über Android-Schnittstellen",
            webUrl = "https://www.meta.ai/",
            packageHints = setOf("com.meta.ai"),
            labelHints = setOf("meta ai"),
        ),
        AiProviderProfile(
            id = "perplexity",
            name = "Perplexity",
            shortName = "PPLX",
            description = "Recherche-App oder Web als Ziel",
            webUrl = "https://www.perplexity.ai/",
            packageHints = setOf("ai.perplexity.app.android", "ai.perplexity.app.android.assistant"),
            labelHints = setOf("perplexity"),
        ),
        AiProviderProfile(
            id = "notebooklm",
            name = "NotebookLM",
            shortName = "NBLM",
            description = "NotebookLM erkennen oder Browser öffnen",
            webUrl = "https://notebooklm.google.com/",
            packageHints = setOf("com.google.android.apps.labs.language.tailwind"),
            labelHints = setOf("notebooklm", "notebook lm"),
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

