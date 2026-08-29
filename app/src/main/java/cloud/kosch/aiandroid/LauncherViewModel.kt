package cloud.kosch.aiandroid

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cloud.kosch.aiandroid.ai.AiContextHandoffPolicy
import cloud.kosch.aiandroid.ai.AiContextHandoffSelection
import cloud.kosch.aiandroid.ai.AiHubContextSignal
import cloud.kosch.aiandroid.ai.AiHubOrigin
import cloud.kosch.aiandroid.ai.AiHubRoutingContext
import cloud.kosch.aiandroid.ai.PenAiContextPlanner
import cloud.kosch.aiandroid.ai.UniversalSearchSourcesFactory
import cloud.kosch.aiandroid.data.WorkspaceWidgetHostRecovery
import cloud.kosch.aiandroid.model.AdaptiveInputRuntimeState
import cloud.kosch.aiandroid.model.HomePage
import cloud.kosch.aiandroid.model.SceneId
import cloud.kosch.aiandroid.model.SettingsSection
import cloud.kosch.aiandroid.system.AdaptiveInputDeviceMonitor
import cloud.kosch.aiandroid.system.UniversalSearchShortcutRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Owns launcher, unified Home, Settings Center, AI/browser Hub, Universal Search, portable custom actions and
 * Assistant runtimes across Activity recreation.
 *
 * Device-local widget host ownership is reconciled before Home loads. Portable launcher settings and scoped
 * page/object overrides stay independent from Assistant session/agent/device-voice stores: the Settings Center may
 * edit those runtimes, but it is not a second source of truth for capture grants, agent state or device-local TTS
 * assignments. Custom actions are typed/validated and contain no arbitrary Android Intent extras.
 */
class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    // Settings initialize the process-local privacy gates before LauncherController can load/rank usage signals or
    // read the local audit. A persisted OFF switch therefore applies from the first launcher frame after process start.
    val settings = LauncherSettingsController(application)
    val controller = LauncherController(application).also(LauncherController::start)
    private val adaptiveInputMonitor = AdaptiveInputDeviceMonitor(
        context = application,
        callbackHandler = Handler(Looper.getMainLooper()),
        onChanged = AdaptiveInputRuntimeState::publish,
    ).also(AdaptiveInputDeviceMonitor::start)

    init {
        // AppWidgetHost ids survive process death. Reconcile them before Home loads the device-local binding map.
        WorkspaceWidgetHostRecovery(application).reconcile()
    }

    val homeWorkspace = WorkspaceHomeController(application)
    val widgetStacks = WidgetStackController(application).also { it.repair(controller.widgetIds) }
    val directProvider = OpenRouterDirectController(
        context = application,
        scope = viewModelScope,
        settingsProvider = { settings.document.ai to settings.document.privacy },
    )
    val scopedSettings = ScopedSettingsController(application).also {
        // Startup/process-death reconciliation removes only overrides whose portable page/item owner no longer exists.
        it.reconcile(homeWorkspace.document)
    }
    val customActions = CustomLauncherActionController(application)
    val aiContextHandoff = AiContextHandoffController()
    val aiHub = AiHubController(application).also { hub ->
        // Even old/direct aiHub.open() call sites receive the same abstract context; explicit origins may override it.
        hub.setDefaultRoutingContextProvider { currentAiHubContext(AiHubOrigin.HOME) }
        // Direct network execution remains ViewModel-owned; the Hub only receives the already gated runtime.
        hub.setDirectProviderController(directProvider)
        hub.setProviderSettingsOpener { settings.open(SettingsSection.API) }
    }
    private val universalSearchShortcuts = UniversalSearchShortcutRepository(application)
    val universalSearch = UniversalSearchController {
        UniversalSearchSourcesFactory.buildFromShortcutSources(
            apps = controller.apps,
            shortcutSources = universalSearchShortcuts.shortcuts,
            folders = controller.folders,
            pages = homeWorkspace.document.pages,
            customActions = customActions.actions,
        )
    }

    init {
        // Migrate every legacy provider entry path to the task-aware AI Hub without duplicating command parsing.
        // A non-generative launcher command still stays fully local inside LauncherController; only the old provider
        // surface is replaced. Existing tiles, command bar and legacy screens therefore gain the new Hub at once.
        viewModelScope.launch {
            snapshotFlow { controller.providerChooserVisible to controller.providerPrompt }
                .collect { (visible, prompt) ->
                    if (visible) {
                        controller.closeProviderChooser()
                        openAiHub(
                            initialPrompt = prompt,
                            requestedOrigin = if (prompt.isBlank()) AiHubOrigin.LEGACY_PROVIDER else AiHubOrigin.COMMAND,
                        )
                    }
                }
        }
        // Stack membership contains device-bound AppWidgetHost ids only. Every binding change repairs orphaned ids;
        // no portable workspace/settings document ever receives this membership.
        viewModelScope.launch {
            snapshotFlow { controller.widgetIds }
                .collect { widgetIds -> widgetStacks.repair(widgetIds) }
        }
    }

    val assistant = AssistantSessionController(application)
    val assistantAgent = AssistantAgentController(application).also {
        it.setAssistantEnabled(assistant.settings.enabled)
    }
    val assistantVoice = AssistantVoiceController(application)

    init {
        // Free-form Assistant text may use the already configured direct provider, but this bridge cannot turn cloud
        // access on, connect credentials or choose a model. sendForAssistant returns false unless all three decisions
        // have already been made by the user; the Assistant then retains its explicit provider-handoff fallback.
        assistant.setGenerativeRequester { prompt ->
            directProvider.sendForAssistant(
                prompt = prompt,
                onSuccess = { reply -> assistant.consumeGenerativeResponse(reply.text) },
                onFailure = { reason -> assistant.consumeGenerativeFailure(reason, prompt) },
            )
        }
    }

    fun openAiHub(
        initialPrompt: String = "",
        requestedOrigin: AiHubOrigin = AiHubOrigin.HOME,
    ) {
        universalSearch.close()
        settings.close()
        directProvider.refreshState()
        aiHub.open(
            initialPrompt = initialPrompt,
            context = currentAiHubContext(requestedOrigin),
        )
    }

    fun openUniversalSearch(initialQuery: String = "") {
        aiHub.close()
        settings.close()
        controller.closeTopSurface()
        universalSearch.open(initialQuery)
        // Open immediately with the last local snapshot, then refresh Android-published shortcuts off the UI thread.
        // Execution re-resolves the target again, so a stale session entry can never become an authorization decision.
        viewModelScope.launch {
            universalSearchShortcuts.refresh(controller.apps)
            universalSearch.refresh()
        }
    }

    /** Prepares a memory-only preview. It does not open the AI Hub and transfers no file content. */
    fun prepareCurrentFileAiHandoff(): Boolean {
        val insight = controller.fileInsight ?: return false
        aiContextHandoff.prepareFile(insight)
        return true
    }

    /**
     * Pen Space is summarized locally into aggregate statistics. Raw stroke coordinates and SVG bytes are deliberately
     * not placed into prompt state; a future multimodal image handoff remains a separate explicit consent path.
     */
    fun prepareCurrentPenAiHandoff(): Boolean {
        val strokes = controller.loadInkStrokes()
        if (strokes.isEmpty()) return false
        val summary = PenAiContextPlanner.summarize(strokes)
        aiContextHandoff.prepare(
            AiContextHandoffPolicy.fromPenSketch(
                title = "Pen-Space-Skizze",
                summary = summary.text,
                textualDescription = "Lokale Aggregatanalyse der Skizze; keine Rohkoordinaten oder SVG-Daten enthalten.",
            ),
        )
        return true
    }

    fun cancelCurrentAiHandoff() {
        aiContextHandoff.cancel()
    }

    fun confirmCurrentFileAiHandoff(
        userPrompt: String,
        userConfirmed: Boolean,
        selection: AiContextHandoffSelection = AiContextHandoffSelection.MINIMAL,
    ): Boolean = confirmCurrentAiHandoff(
        userPrompt = userPrompt,
        userConfirmed = userConfirmed,
        selection = selection,
        requestedOrigin = AiHubOrigin.FILE,
        closeSourceSurface = controller::closeFileSheet,
    )

    fun confirmCurrentPenAiHandoff(
        userPrompt: String,
        userConfirmed: Boolean,
        selection: AiContextHandoffSelection = AiContextHandoffSelection.MINIMAL,
    ): Boolean = confirmCurrentAiHandoff(
        userPrompt = userPrompt,
        userConfirmed = userConfirmed,
        selection = selection,
        requestedOrigin = AiHubOrigin.PEN,
        closeSourceSurface = {},
    )

    /** Only an explicit UI confirmation may pass userConfirmed=true. */
    private fun confirmCurrentAiHandoff(
        userPrompt: String,
        userConfirmed: Boolean,
        selection: AiContextHandoffSelection,
        requestedOrigin: AiHubOrigin,
        closeSourceSurface: () -> Unit,
    ): Boolean {
        val confirmed = aiContextHandoff.confirm(
            userPrompt = userPrompt,
            userConfirmed = userConfirmed,
            selection = selection,
        ) ?: return false
        closeSourceSurface()
        openAiHub(
            initialPrompt = confirmed.prompt,
            requestedOrigin = requestedOrigin,
        )
        return true
    }

    /**
     * Projects launcher state into abstract routing hints only. No raw file metadata, prompt history, battery values,
     * contacts, screen content or camera state enters this object. Observation permissions remain fully separate.
     */
    private fun currentAiHubContext(requestedOrigin: AiHubOrigin): AiHubRoutingContext {
        val snapshot = controller.contextSnapshot
        val fileContext = controller.fileWorkspaceVisible || controller.fileSheetVisible || controller.fileInsight != null
        val penContext = controller.homePage == HomePage.PEN_SPACE && controller.stylusState.present
        val proDeskContext = controller.homePage == HomePage.PRO_DESK
        val origin = when {
            requestedOrigin != AiHubOrigin.HOME -> requestedOrigin
            fileContext -> AiHubOrigin.FILE
            penContext -> AiHubOrigin.PEN
            proDeskContext -> AiHubOrigin.PRO_DESK
            else -> AiHubOrigin.HOME
        }
        val signals = buildSet {
            if (!snapshot.hasNetwork) add(AiHubContextSignal.OFFLINE)
            if (snapshot.hasPersonalAudioOutput) add(AiHubContextSignal.PERSONAL_AUDIO)
            if (fileContext) add(AiHubContextSignal.FILE_CONTEXT)
            if (penContext) add(AiHubContextSignal.PEN_CONTEXT)
            if (controller.activeScene == SceneId.WORK) add(AiHubContextSignal.WORK_CONTEXT)
            if (controller.activeScene == SceneId.STUDIO) add(AiHubContextSignal.STUDIO_CONTEXT)
            if (proDeskContext) add(AiHubContextSignal.PRO_DESK_CONTEXT)
        }
        return AiHubRoutingContext(origin = origin, signals = signals)
    }

    override fun onCleared() {
        adaptiveInputMonitor.stop()
        universalSearch.close()
        universalSearchShortcuts.clear()
        aiContextHandoff.cancel()
        assistant.close()
        controller.close()
        super.onCleared()
    }
}
