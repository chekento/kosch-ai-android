package cloud.kosch.aiandroid.system

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.kosch.aiandroid.ai.UniversalShortcutSource
import cloud.kosch.aiandroid.model.LaunchableApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Device-local cache of app-published launcher shortcuts for Universal Search.
 *
 * Discovery uses Android's official LauncherApps API and runs off the UI thread. Only enabled shortcut id/label and
 * the owning launcher app key are retained. Intents, extras and icon payloads never enter the search index. A device
 * that does not grant shortcut visibility simply produces an empty/partial snapshot instead of bypassing Android.
 */
class UniversalSearchShortcutRepository(context: Context) {
    private val launcherApps = context.applicationContext.getSystemService(LauncherApps::class.java)

    var shortcuts by mutableStateOf<List<UniversalShortcutSource>>(emptyList())
        private set

    suspend fun refresh(apps: List<LaunchableApp>) {
        val next = withContext(Dispatchers.IO) { discover(apps) }
        shortcuts = next
    }

    fun clear() {
        shortcuts = emptyList()
    }

    private fun discover(apps: List<LaunchableApp>): List<UniversalShortcutSource> {
        if (apps.isEmpty()) return emptyList()
        val appsByOwner = apps.associateBy { it.userSerialNumber to it.packageName }
        val users = apps
            .distinctBy { it.userSerialNumber }
            .sortedBy { it.userSerialNumber }
            .take(MAX_PROFILES)

        return buildList {
            users.forEach { representative ->
                if (size >= MAX_TOTAL_SHORTCUTS) return@forEach
                val query = LauncherApps.ShortcutQuery().setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
                )
                val remaining = MAX_TOTAL_SHORTCUTS - size
                val discovered = runCatching {
                    launcherApps.getShortcuts(query, representative.user).orEmpty()
                }.getOrDefault(emptyList())
                    .asSequence()
                    .filter(ShortcutInfo::isEnabled)
                    .mapNotNull { shortcut ->
                        val owner = appsByOwner[representative.userSerialNumber to shortcut.`package`]
                            ?: return@mapNotNull null
                        UniversalShortcutSource(
                            appKey = owner.key,
                            shortcutId = shortcut.id.take(MAX_SHORTCUT_ID_CHARS),
                            label = shortcut.shortLabel?.toString()
                                ?.trim()
                                ?.take(MAX_LABEL_CHARS)
                                ?.ifBlank { shortcut.id.take(MAX_LABEL_CHARS) }
                                ?: shortcut.id.take(MAX_LABEL_CHARS),
                            appLabel = owner.label.take(MAX_LABEL_CHARS),
                        )
                    }
                    .distinctBy { it.appKey to it.shortcutId }
                    .sortedWith(
                        compareBy<UniversalShortcutSource> { it.appLabel.lowercase(Locale.ROOT) }
                            .thenBy { it.label.lowercase(Locale.ROOT) }
                            .thenBy { it.shortcutId },
                    )
                    .take(remaining)
                    .toList()
                addAll(discovered)
            }
        }.take(MAX_TOTAL_SHORTCUTS)
    }

    private companion object {
        const val MAX_PROFILES = 8
        const val MAX_TOTAL_SHORTCUTS = 2_048
        const val MAX_SHORTCUT_ID_CHARS = 512
        const val MAX_LABEL_CHARS = 160
    }
}
