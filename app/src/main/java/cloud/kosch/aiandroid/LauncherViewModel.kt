package cloud.kosch.aiandroid

import android.app.Application
import androidx.lifecycle.AndroidViewModel

/**
 * Owns the launcher runtime across Activity recreation.
 *
 * The controller keeps listeners and its single-threaded worker alive while Android recreates the
 * Activity for rotation, window-size changes or fold transitions. It is closed only when the
 * ViewModel is permanently cleared.
 */
class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    val controller = LauncherController(application).also(LauncherController::start)

    override fun onCleared() {
        controller.close()
        super.onCleared()
    }
}
