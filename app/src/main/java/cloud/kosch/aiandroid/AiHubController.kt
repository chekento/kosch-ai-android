package cloud.kosch.aiandroid

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.kosch.aiandroid.ai.AiHubCatalog
import cloud.kosch.aiandroid.ai.AiHubEntry
import cloud.kosch.aiandroid.data.DismissedSuggestionStore
import cloud.kosch.aiandroid.model.LaunchableApp

/**
 * User-owned AI/browser overview state.
 *
 * This controller stores only dismissed card ids. Installed-app inventory remains owned by LauncherController/AppCatalog
 * and is supplied as an immutable snapshot when entries are requested.
 */
class AiHubController(context: Context) {
    private val dismissedStore = DismissedSuggestionStore(context)

    var hiddenIds by mutableStateOf(dismissedStore.hiddenIds())
        private set

    fun entries(apps: List<LaunchableApp>): List<AiHubEntry> = AiHubCatalog.entries(apps, hiddenIds)

    fun dismiss(entry: AiHubEntry): Boolean {
        if (!entry.dismissible) return false
        val saved = dismissedStore.dismiss(entry.stableId)
        if (saved) hiddenIds = dismissedStore.hiddenIds()
        return saved
    }

    fun restore(stableId: String): Boolean {
        val saved = dismissedStore.restore(stableId)
        if (saved) hiddenIds = dismissedStore.hiddenIds()
        return saved
    }

    fun restoreAll(): Boolean {
        val saved = dismissedStore.restoreAll()
        if (saved) hiddenIds = emptySet()
        return saved
    }
}
