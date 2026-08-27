package cloud.kosch.aiandroid

import android.app.Application
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cloud.kosch.aiandroid.ai.AiHubContextSignal
import cloud.kosch.aiandroid.ai.AiHubOrigin
import cloud.kosch.aiandroid.ai.AiHubRoutingContext
import cloud.kosch.aiandroid.data.WorkspaceWidgetHostRecovery
import cloud.kosch.aiandroid.model.HomePage
import cloud.kosch.aiandroid.model.SceneId
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Owns launcher, unified Home, Settings Center, AI/browser Hub, portable custom actions and Assistant runtimes across
 * Activity recreation.
 *
 * Device-local widget host ownership is reconciled before Home loads. Portable launcher settings and scoped
 * page/object overrides stay independent from Assistant session/agent/device-voice stores: the Settings Center may
 * edit those runtimes, but it is not a second source of truth for capture grants, agent state or device-local TTS
 * assignments. Custom actions are typed/validated and contain no arbitrary Android Intent extras.
 */
class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    val controller = LauncherController(application).also(LauncherController::start)

    init {
        // AppWidgetHost ids survive process death. Reconcile them before Home loads the device-local binding map.
        WorkspaceWidgetHostRecovery(application).reconcile()
    }

    val homeWorkspace = WorkspaceHomeController(application)
    val settings = LauncherSettingsController(application)
    val scopedSettings = ScopedSettingsController(application).also {
        // Startup/process-death reconciliation removes only overrides whose portable page/item owner no longer exists.
        it.reconcile(homeWorkspace.document)
    }
    val customActions = CustomLauncherActionController(application)
    val aiContextHandoff = AiContextHandoffController()
    val aiHub = AiHubController(application).also { hub ->
        // Even old/direct aiHub.open() call sites receive the same abstract context; explicit origins may override it.
        hub.setDefaultRoutingContextProvider { currentAiHubContext(AiHubOrigin.HOME) }
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
    }

    val assistant = AssistantSessionController(application)
    val assistantAgent = AssistantAgentController(application).also {
        it.setAssistantEnabled(assistant.settings.enabled)
    }
    val assistantVoice = AssistantVoiceController(application)

    fun openAiHub(
        initialPrompt: String = "",
        requestedOrigin: AiHubOrigin = AiHubOrigin.HOME,
    ) {
        settings.close()
        aiHub.open(
            initialPrompt = initialPrompt,
            context = currentAiHubContext(requestedOrigin),
        )
    }

    /** Prepares a memory-only preview. It does not open the AI Hub and transfers no file content. */
    fun prepareCurrentFileAiHandoff(): Boolean {
        val insight = controller.fileInsight ?: return false
        aiContextHandoff.prepareFile(insight)
        return true
    }

    fun cancelCurrentAiHandoff() {
        aiContextHandoff.cancel()
    }

    /**
     * Only the explicit UI confirmation path may call this with userConfirmed=true. The handoff draft is consumed once
     * and the AI Hub receives only the bounded confirmed prompt payload, never the original file URI implicitly.
     */
    fun confirmCurrentFileAiHandoff(
        userPrompt: String,
        userConfirmed: Boolean,
    ): Boolean {
        val confirmed = aiContextHandoff.confirm(
            userPrompt = userPrompt,
            userConfirmed = userConfirmed,
        ) ?: return false
        controller.closeFileSheet()
        openAiHub(
            initialPrompt = confirmed.prompt,
            requestedOrigin = AiHubOrigin.FILE,
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
        aiContextHandoff.cancel()
        assistant.close()
        controller.close()
        super.onCleared()
    }
}
