package cloud.kosch.aiandroid

import android.content.Context
import android.content.pm.LauncherApps
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.kosch.aiandroid.ai.AiHubCatalog
import cloud.kosch.aiandroid.ai.AiHubEntry
import cloud.kosch.aiandroid.ai.AiHubLaunchPlan
import cloud.kosch.aiandroid.ai.AiHubLaunchPlanner
import cloud.kosch.aiandroid.ai.AiHubRecommendation
import cloud.kosch.aiandroid.ai.AiHubTaskIntent
import cloud.kosch.aiandroid.ai.AiHubTaskRouter
import cloud.kosch.aiandroid.data.DismissedSuggestionStore
import cloud.kosch.aiandroid.model.LaunchableApp
import cloud.kosch.aiandroid.system.SystemActionGateway

/**
 * User-owned AI/browser overview state.
 *
 * The controller stores only dismissed card ids and transient UI text. Installed-app inventory remains owned by
 * LauncherController/AppCatalog and is supplied as an immutable snapshot. Launching uses Android's official
 * LauncherApps, Share, browser and Play Store routes; no Accessibility automation or undocumented prompt injection.
 */
class AiHubController(context: Context) {
    private val appContext = context.applicationContext
    private val dismissedStore = DismissedSuggestionStore(appContext)
    private val systemActions = SystemActionGateway(appContext)
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)

    var visible by mutableStateOf(false)
        private set
    var prompt by mutableStateOf("")
        private set
    var notice by mutableStateOf<String?>(null)
        private set
    var hiddenIds by mutableStateOf(dismissedStore.hiddenIds())
        private set

    fun entries(apps: List<LaunchableApp>): List<AiHubEntry> = AiHubCatalog.entries(apps, hiddenIds)

    fun recommendations(
        apps: List<LaunchableApp>,
        limit: Int = 4,
    ): List<AiHubRecommendation> = AiHubTaskRouter.rank(prompt, entries(apps), limit)

    fun inferredTask(): AiHubTaskIntent = AiHubTaskRouter.infer(prompt)

    fun open(initialPrompt: String = "") {
        prompt = initialPrompt.take(MAX_PROMPT_CHARS)
        notice = null
        visible = true
    }

    fun close() {
        visible = false
        notice = null
    }

    fun updatePrompt(value: String) {
        prompt = value.take(MAX_PROMPT_CHARS)
    }

    fun execute(entry: AiHubEntry) {
        val plan = AiHubLaunchPlanner.plan(entry, prompt)
        val result = when (plan) {
            is AiHubLaunchPlan.LaunchInstalled -> runCatching {
                launcherApps.startMainActivity(plan.app.componentName, plan.app.user, null, null)
            }
            is AiHubLaunchPlan.SharePrompt -> systemActions.shareTextWithPackage(
                plan.app.packageName,
                plan.prompt,
            )
            is AiHubLaunchPlan.OpenPlayStore -> systemActions.openStoreListing(plan.packageName)
            is AiHubLaunchPlan.OpenWeb -> systemActions.openWeb(plan.url)
            AiHubLaunchPlan.OpenSystemBrowser -> systemActions.openDefaultBrowser()
            AiHubLaunchPlan.Unavailable -> Result.failure(IllegalStateException("No launch route"))
        }
        result.onSuccess {
            notice = when (plan) {
                is AiHubLaunchPlan.SharePrompt -> "Text bewusst an ${entry.title} übergeben"
                is AiHubLaunchPlan.OpenPlayStore -> "Play-Store-Eintrag für ${entry.title} geöffnet"
                AiHubLaunchPlan.OpenSystemBrowser -> "Android-Systembrowser geöffnet"
                else -> "${entry.title} geöffnet"
            }
        }.onFailure {
            notice = "${entry.title} konnte über die sichere Android-Route nicht geöffnet werden"
        }
    }

    fun consumeNotice() {
        notice = null
    }

    fun dismiss(entry: AiHubEntry): Boolean {
        if (!entry.dismissible) return false
        val saved = dismissedStore.dismiss(entry.stableId)
        if (saved) {
            hiddenIds = dismissedStore.hiddenIds()
            notice = "${entry.title} ausgeblendet"
        }
        return saved
    }

    fun restore(stableId: String): Boolean {
        val saved = dismissedStore.restore(stableId)
        if (saved) hiddenIds = dismissedStore.hiddenIds()
        return saved
    }

    fun restoreAll(): Boolean {
        val saved = dismissedStore.restoreAll()
        if (saved) {
            hiddenIds = emptySet()
            notice = "Ausgeblendete Vorschläge wiederhergestellt"
        }
        return saved
    }

    private companion object {
        const val MAX_PROMPT_CHARS = 32_000
    }
}
