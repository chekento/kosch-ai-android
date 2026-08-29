package cloud.kosch.aiandroid

import android.content.Context
import android.content.pm.LauncherApps
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.kosch.aiandroid.ai.AiExternalHandoffCandidate
import cloud.kosch.aiandroid.ai.AiExternalHandoffDecision
import cloud.kosch.aiandroid.ai.AiExternalHandoffGate
import cloud.kosch.aiandroid.ai.AiHubCatalog
import cloud.kosch.aiandroid.ai.AiHubContextPolicy
import cloud.kosch.aiandroid.ai.AiHubDecisionConfidence
import cloud.kosch.aiandroid.ai.AiHubDecisionPolicy
import cloud.kosch.aiandroid.ai.AiHubEntry
import cloud.kosch.aiandroid.ai.AiHubEntryKind
import cloud.kosch.aiandroid.ai.AiHubLaunchPlan
import cloud.kosch.aiandroid.ai.AiHubLaunchPlanner
import cloud.kosch.aiandroid.ai.AiHubPreferencePolicy
import cloud.kosch.aiandroid.ai.AiHubRecommendation
import cloud.kosch.aiandroid.ai.AiHubRouteDecision
import cloud.kosch.aiandroid.ai.AiHubRoutingContext
import cloud.kosch.aiandroid.ai.AiHubShortcutRoutePolicy
import cloud.kosch.aiandroid.ai.AiHubTaskIntent
import cloud.kosch.aiandroid.ai.AiHubTaskRouter
import cloud.kosch.aiandroid.ai.AiPublishedShortcutKind
import cloud.kosch.aiandroid.data.AiHubPreferenceStore
import cloud.kosch.aiandroid.data.DismissedSuggestionStore
import cloud.kosch.aiandroid.model.LaunchableApp
import cloud.kosch.aiandroid.system.AiPublishedShortcutSurface
import cloud.kosch.aiandroid.system.AiPublishedSurfaceDiscovery
import cloud.kosch.aiandroid.system.AiPublishedSurfaceSnapshot
import cloud.kosch.aiandroid.system.SystemActionGateway

/**
 * User-owned AI/browser overview state.
 *
 * The controller stores only dismissed card ids, per-task preferred stable ids and transient UI text. Installed-app
 * inventory remains owned by LauncherController/AppCatalog and is supplied as an immutable snapshot. Launching uses
 * Android's official LauncherApps, Share, browser, shortcut and Play Store routes; no Accessibility automation or
 * undocumented prompt injection.
 *
 * Routing order is deliberate: semantic task -> privacy-minimal local context -> valid device-local preference ->
 * confidence explanation -> published Android shortcut / normal safe Android route. Context never grants a capability
 * or observation right, and confidence never changes the underlying ranking.
 *
 * Prompt handoff is deliberately two-step and process-local. The first user gesture only stages the exact destination
 * and prompt and shows a disclosure. A second unchanged gesture is required before Android receives the Share intent.
 * Direct provider execution is injected from LauncherViewModel and remains independently gated by KalCloudAccessPolicy.
 */
class AiHubController(context: Context) {
    private val appContext = context.applicationContext
    private val dismissedStore = DismissedSuggestionStore(appContext)
    private val preferenceStore = AiHubPreferenceStore(appContext)
    private val systemActions = SystemActionGateway(appContext)
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val publishedSurfaceDiscovery = AiPublishedSurfaceDiscovery(appContext)
    private val publishedSurfaceCache = mutableMapOf<String, AiPublishedSurfaceSnapshot>()
    private val externalHandoffGate = AiExternalHandoffGate()
    private var defaultRoutingContextProvider: () -> AiHubRoutingContext = { AiHubRoutingContext() }
    private var providerSettingsOpener: () -> Unit = {}

    var visible by mutableStateOf(false)
        private set
    var prompt by mutableStateOf("")
        private set
    var notice by mutableStateOf<String?>(null)
        private set
    var hiddenIds by mutableStateOf(dismissedStore.hiddenIds())
        private set
    var preferredTargetIds by mutableStateOf(preferenceStore.snapshot())
        private set
    var routingContext by mutableStateOf(AiHubRoutingContext())
        private set
    var directProvider by mutableStateOf<OpenRouterDirectController?>(null)
        private set

    /** Supplies abstract launcher context for old/direct open() call sites without coupling this controller to Home. */
    fun setDefaultRoutingContextProvider(provider: () -> AiHubRoutingContext) {
        defaultRoutingContextProvider = provider
    }

    /** Injects the ViewModel-owned direct-provider runtime so Compose never creates a duplicate network state holder. */
    fun setDirectProviderController(controller: OpenRouterDirectController) {
        directProvider = controller
    }

    /** Keeps Settings navigation behind the Hub boundary rather than coupling AiHubSurface to MainActivity. */
    fun setProviderSettingsOpener(opener: () -> Unit) {
        providerSettingsOpener = opener
    }

    fun openProviderSettings() {
        close()
        providerSettingsOpener()
    }

    fun entries(apps: List<LaunchableApp>): List<AiHubEntry> = AiHubCatalog.entries(apps, hiddenIds)

    fun recommendations(
        apps: List<LaunchableApp>,
        limit: Int = 4,
    ): List<AiHubRecommendation> {
        val ranked = rankedRecommendations(apps, limit)
        val decision = AiHubDecisionPolicy.decide(ranked) ?: return ranked
        return ranked.mapIndexed { index, recommendation ->
            when {
                index == 0 -> recommendation.copy(
                    reason = "${decision.confidence.title} · ${decision.explanation} · ${recommendation.reason}",
                )
                index == 1 && decision.confidence == AiHubDecisionConfidence.LOW -> recommendation.copy(
                    reason = "Alternative auf Augenhöhe · ${recommendation.reason}",
                )
                else -> recommendation
            }
        }
    }

    fun routeDecision(apps: List<LaunchableApp>): AiHubRouteDecision? =
        AiHubDecisionPolicy.decide(rankedRecommendations(apps, DEFAULT_RECOMMENDATION_LIMIT))

    fun bestRecommendation(apps: List<LaunchableApp>): AiHubRecommendation? =
        routeDecision(apps)?.primary

    fun inferredTask(): AiHubTaskIntent = AiHubTaskRouter.infer(prompt)

    fun canPreferForCurrentTask(entry: AiHubEntry): Boolean =
        AiHubPreferencePolicy.canPrefer(inferredTask(), entry)

    fun isPreferredForCurrentTask(entry: AiHubEntry): Boolean =
        preferredTargetIds[inferredTask()] == entry.stableId

    fun togglePreferredForCurrentTask(entry: AiHubEntry): Boolean {
        clearPendingExternalHandoff()
        val intent = inferredTask()
        if (!AiHubPreferencePolicy.canPrefer(intent, entry)) return false
        val alreadyPreferred = preferredTargetIds[intent] == entry.stableId
        val saved = if (alreadyPreferred) {
            preferenceStore.clear(intent)
        } else {
            preferenceStore.setPreferred(intent, entry.stableId)
        }
        if (saved) {
            preferredTargetIds = preferenceStore.snapshot()
            notice = if (alreadyPreferred) {
                "Bevorzugtes Ziel für ${intent.title} entfernt"
            } else {
                "${entry.title} wird für ${intent.title} bevorzugt"
            }
        }
        return saved
    }

    fun publishedSurfaces(entry: AiHubEntry): AiPublishedSurfaceSnapshot {
        val app = entry.installedApp ?: return AiPublishedSurfaceSnapshot()
        val discovered = publishedSurfaceCache.getOrPut(app.key) {
            publishedSurfaceDiscovery.snapshot(app)
        }
        val shortcuts = when (entry.kind) {
            AiHubEntryKind.BROWSER -> discovered.shortcuts.filter { it.kind == AiPublishedShortcutKind.AI_ASSISTANT }
            AiHubEntryKind.SYSTEM_BROWSER -> emptyList()
            AiHubEntryKind.LLM_APP, AiHubEntryKind.LOCAL_LLM_APP -> discovered.shortcuts
        }
        return discovered.copy(shortcuts = shortcuts)
    }

    fun bestPublishedShortcut(entry: AiHubEntry): AiPublishedShortcutSurface? {
        val shortcuts = publishedSurfaces(entry).shortcuts
        val preferredKind = AiHubShortcutRoutePolicy.preferredKind(
            inferredTask(),
            shortcuts.map { it.kind },
        ) ?: return null
        return shortcuts
            .asSequence()
            .filter { it.kind == preferredKind }
            .sortedBy { it.label.lowercase() }
            .firstOrNull()
    }

    /** Executes any ranked recommendation through the same official shortcut -> safe Android fallback chain. */
    fun executeRecommendation(recommendation: AiHubRecommendation): Boolean {
        val entry = recommendation.entry
        val shortcut = bestPublishedShortcut(entry)
        if (shortcut != null) {
            clearPendingExternalHandoff()
            val shortcutResult = launchPublishedShortcut(shortcut)
            if (shortcutResult.isSuccess) {
                notice = "${shortcut.label} für ${recommendation.intent.title} geöffnet"
                return true
            }
        }
        execute(entry)
        return true
    }

    /** Executes the top safe route now. A stale published shortcut falls back to the normal Android launch plan. */
    fun executeBestRoute(apps: List<LaunchableApp>): Boolean {
        val recommendation = routeDecision(apps)?.primary ?: return false
        return executeRecommendation(recommendation)
    }

    fun open(
        initialPrompt: String = "",
        context: AiHubRoutingContext? = null,
    ) {
        publishedSurfaceCache.clear()
        clearPendingExternalHandoff()
        directProvider?.refreshState()
        routingContext = context ?: defaultRoutingContextProvider()
        prompt = initialPrompt.take(MAX_PROMPT_CHARS)
        notice = null
        visible = true
    }

    fun close() {
        clearPendingExternalHandoff()
        visible = false
        notice = null
    }

    fun updatePrompt(value: String) {
        val updated = value.take(MAX_PROMPT_CHARS)
        if (updated != prompt) clearPendingExternalHandoff()
        prompt = updated
    }

    fun execute(entry: AiHubEntry) {
        val plan = AiHubLaunchPlanner.plan(entry, prompt)
        if (plan is AiHubLaunchPlan.SharePrompt && !confirmOrStageExternalHandoff(entry, plan)) return

        if (plan !is AiHubLaunchPlan.SharePrompt) clearPendingExternalHandoff()
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

    fun execute(surface: AiPublishedShortcutSurface) {
        clearPendingExternalHandoff()
        launchPublishedShortcut(surface)
            .onSuccess {
                notice = "${surface.label} über den von der App veröffentlichten Android-Shortcut geöffnet"
            }
            .onFailure {
                notice = "Der veröffentlichte Shortcut ${surface.label} ist aktuell nicht startbar"
            }
    }

    private fun confirmOrStageExternalHandoff(
        entry: AiHubEntry,
        plan: AiHubLaunchPlan.SharePrompt,
    ): Boolean {
        val decision = externalHandoffGate.evaluate(
            AiExternalHandoffCandidate(
                stableTargetId = entry.stableId,
                packageName = plan.app.packageName,
                prompt = plan.prompt,
            ),
        )
        if (decision == AiExternalHandoffDecision.CONFIRMED) return true

        notice = "Externe Übergabe vorbereitet: ${entry.title} erhält den eingegebenen Text. " +
            "Für die weitere Verarbeitung gelten die Bedingungen der Ziel-App. " +
            "Zum Bestätigen erneut „Text übergeben“ tippen; Ändern des Textes oder Ziels bricht ab."
        return false
    }

    private fun clearPendingExternalHandoff() {
        externalHandoffGate.clear()
    }

    private fun launchPublishedShortcut(surface: AiPublishedShortcutSurface): Result<Unit> = runCatching {
        launcherApps.startShortcut(
            surface.packageName,
            surface.shortcutId,
            null,
            null,
            surface.user,
        )
    }

    fun consumeNotice() {
        notice = null
    }

    fun dismiss(entry: AiHubEntry): Boolean {
        clearPendingExternalHandoff()
        if (!entry.dismissible) return false
        val saved = dismissedStore.dismiss(entry.stableId)
        if (saved) {
            hiddenIds = dismissedStore.hiddenIds()
            notice = "${entry.title} ausgeblendet"
        }
        return saved
    }

    fun restore(stableId: String): Boolean {
        clearPendingExternalHandoff()
        val saved = dismissedStore.restore(stableId)
        if (saved) hiddenIds = dismissedStore.hiddenIds()
        return saved
    }

    fun restoreAll(): Boolean {
        clearPendingExternalHandoff()
        val saved = dismissedStore.restoreAll()
        if (saved) {
            hiddenIds = emptySet()
            notice = "Ausgeblendete Vorschläge wiederhergestellt"
        }
        return saved
    }

    private fun rankedRecommendations(
        apps: List<LaunchableApp>,
        limit: Int,
    ): List<AiHubRecommendation> {
        if (limit <= 0) return emptyList()
        val intent = inferredTask()
        val availableEntries = entries(apps)
        val base = AiHubTaskRouter.rank(prompt, availableEntries, availableEntries.size)
        val contextual = AiHubContextPolicy.apply(routingContext, base)
        return AiHubPreferencePolicy
            .apply(intent, preferredTargetIds[intent], contextual)
            .take(limit)
    }

    private companion object {
        const val MAX_PROMPT_CHARS = 32_000
        const val DEFAULT_RECOMMENDATION_LIMIT = 4
    }
}
