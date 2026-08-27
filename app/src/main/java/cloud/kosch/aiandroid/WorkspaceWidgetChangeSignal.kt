package cloud.kosch.aiandroid

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue

/**
 * Process-local signal used only to refresh the already-open launcher after the internal widget picker commits.
 * WorkspaceDocument and device bindings remain the source of truth; process death simply resets this counter and
 * the normal controller restore path reloads persisted state.
 */
object WorkspaceWidgetChangeSignal {
    var revision by mutableLongStateOf(0L)
        private set

    fun notifyChanged() {
        revision = if (revision == Long.MAX_VALUE) 1L else revision + 1L
    }
}
