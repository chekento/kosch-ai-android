package cloud.kosch.aiandroid

import android.app.Application
import androidx.lifecycle.AndroidViewModel

/**
 * Owns launcher, unified Home, Settings Center and assistant runtimes across Activity recreation.
 *
 * The launcher controller keeps listeners and its single-threaded worker alive while Android recreates the
 * Activity for rotation, window-size changes or fold transitions. The v7 Home controller retains active page
 * and undo state, Settings Center retains portable preferences, while the assistant keeps its in-memory transcript.
 */
class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    val controller = LauncherController(application).also(LauncherController::start)
    val homeWorkspace = WorkspaceHomeController(application)
    val settings = LauncherSettingsController(application)
    val assistant = AssistantSessionController(application)

    override fun onCleared() {
        controller.close()
        super.onCleared()
    }
}
