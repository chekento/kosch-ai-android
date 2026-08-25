package cloud.kosch.aiandroid

import android.app.Application
import androidx.lifecycle.AndroidViewModel

/**
 * Owns launcher and assistant runtimes across Activity recreation.
 *
 * The launcher controller keeps listeners and its single-threaded worker alive while Android recreates the
 * Activity for rotation, window-size changes or fold transitions. The assistant keeps its in-memory transcript
 * across those recreations, while only opt-in settings are persisted to disk.
 */
class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    val controller = LauncherController(application).also(LauncherController::start)
    val assistant = AssistantSessionController(application)

    override fun onCleared() {
        controller.close()
        super.onCleared()
    }
}
