package cloud.kosch.aiandroid

import android.app.Application
import androidx.lifecycle.AndroidViewModel

/**
 * Owns launcher, unified Home and assistant runtimes across Activity recreation.
 *
 * The launcher controller keeps listeners and its single-threaded worker alive while Android recreates the
 * Activity for rotation, window-size changes or fold transitions. The v7 Home controller retains active page
 * and undo state, while assistant conversation and agent state remain session-scoped. Only durable user choices
 * are persisted. Device-local TTS voice assignments stay outside portable launcher configuration.
 */
class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    val controller = LauncherController(application).also(LauncherController::start)
    val homeWorkspace = WorkspaceHomeController(application)
    val assistant = AssistantSessionController(application)
    val assistantAgent = AssistantAgentController(application).also {
        it.setAssistantEnabled(assistant.settings.enabled)
    }
    val assistantVoice = AssistantVoiceController(application)

    override fun onCleared() {
        controller.close()
        super.onCleared()
    }
}
