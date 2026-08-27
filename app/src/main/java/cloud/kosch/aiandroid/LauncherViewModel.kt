package cloud.kosch.aiandroid

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import cloud.kosch.aiandroid.data.WorkspaceWidgetHostRecovery

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
    val aiHub = AiHubController(application)
    val assistant = AssistantSessionController(application)
    val assistantAgent = AssistantAgentController(application).also {
        it.setAssistantEnabled(assistant.settings.enabled)
    }
    val assistantVoice = AssistantVoiceController(application)

    override fun onCleared() {
        assistant.close()
        controller.close()
        super.onCleared()
    }
}
