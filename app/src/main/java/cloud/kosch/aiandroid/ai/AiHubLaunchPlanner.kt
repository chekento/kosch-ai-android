package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.LaunchableApp

sealed interface AiHubLaunchPlan {
    data class LaunchInstalled(val app: LaunchableApp) : AiHubLaunchPlan
    data class SharePrompt(val app: LaunchableApp, val prompt: String) : AiHubLaunchPlan
    data class OpenPlayStore(val packageName: String) : AiHubLaunchPlan
    data class OpenWeb(val url: String) : AiHubLaunchPlan
    data object OpenSystemBrowser : AiHubLaunchPlan
    data object Unavailable : AiHubLaunchPlan
}

/**
 * Deterministic execution planner for AI Hub cards.
 *
 * Browser-AI marketing capabilities never imply a prompt injection interface. Browsers are launched as browsers;
 * KoSch will use an AI-specific browser shortcut only when that shortcut is discovered through Android at runtime.
 */
object AiHubLaunchPlanner {
    fun plan(entry: AiHubEntry, prompt: String = ""): AiHubLaunchPlan {
        val cleanPrompt = prompt.trim().take(MAX_PROMPT_CHARS)
        if (entry.kind == AiHubEntryKind.SYSTEM_BROWSER) return AiHubLaunchPlan.OpenSystemBrowser

        entry.installedApp?.let { app ->
            val canSharePrompt = entry.kind == AiHubEntryKind.LLM_APP || entry.kind == AiHubEntryKind.LOCAL_LLM_APP
            return if (canSharePrompt && cleanPrompt.isNotBlank()) {
                AiHubLaunchPlan.SharePrompt(app, cleanPrompt)
            } else {
                AiHubLaunchPlan.LaunchInstalled(app)
            }
        }
        entry.playStorePackageName?.let { return AiHubLaunchPlan.OpenPlayStore(it) }
        entry.webUrl?.let { return AiHubLaunchPlan.OpenWeb(it) }
        return AiHubLaunchPlan.Unavailable
    }

    private const val MAX_PROMPT_CHARS = 32_000
}
