package cloud.kosch.aiandroid.data

import android.content.Context
import cloud.kosch.aiandroid.model.DeviceWidgetBinding
import cloud.kosch.aiandroid.model.MAX_WORKSPACE_ID_LENGTH
import org.json.JSONObject

/**
 * Device-only mapping from stable v7 workspace item ids to Android AppWidgetHost ids.
 *
 * This store deliberately uses a separate SharedPreferences file from WorkspaceStore so portable backups
 * cannot accidentally include appWidgetId values. Provider identity and geometry live in WorkspaceDocument.
 */
class WorkspaceWidgetBindingStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun bindings(): List<DeviceWidgetBinding> = runCatching {
        val root = JSONObject(preferences.getString(KEY_BINDINGS, "{}"))
        buildList {
            val keys = root.keys().asSequence().toList().sorted().take(MAX_BINDINGS)
            keys.forEach { workspaceItemId ->
                if (workspaceItemId.isBlank() || workspaceItemId.length > MAX_WORKSPACE_ID_LENGTH) return@forEach
                val appWidgetId = root.optInt(workspaceItemId, -1)
                if (appWidgetId <= 0) return@forEach
                add(DeviceWidgetBinding(workspaceItemId, appWidgetId))
            }
        }
    }.getOrDefault(emptyList())

    fun appWidgetIdFor(workspaceItemId: String): Int? = bindings()
        .firstOrNull { it.workspaceItemId == workspaceItemId }
        ?.appWidgetId

    fun bind(binding: DeviceWidgetBinding): Boolean {
        val current = bindings().associate { it.workspaceItemId to it.appWidgetId }.toMutableMap()
        if (binding.workspaceItemId !in current && current.size >= MAX_BINDINGS) return false
        current[binding.workspaceItemId] = binding.appWidgetId
        return write(current)
    }

    fun unbind(workspaceItemId: String): Int? {
        if (workspaceItemId.isBlank() || workspaceItemId.length > MAX_WORKSPACE_ID_LENGTH) return null
        val current = bindings().associate { it.workspaceItemId to it.appWidgetId }.toMutableMap()
        val removed = current.remove(workspaceItemId) ?: return null
        return if (write(current)) removed else null
    }

    /** Removes bindings for workspace items that no longer exist and returns host ids safe to release. */
    fun prune(validWorkspaceItemIds: Set<String>): List<Int> {
        val safeIds = validWorkspaceItemIds
            .asSequence()
            .filter { it.isNotBlank() && it.length <= MAX_WORKSPACE_ID_LENGTH }
            .take(MAX_BINDINGS)
            .toSet()
        val current = bindings()
        val stale = current.filterNot { it.workspaceItemId in safeIds }
        if (stale.isEmpty()) return emptyList()
        val kept = current
            .filter { it.workspaceItemId in safeIds }
            .associate { it.workspaceItemId to it.appWidgetId }
        return if (write(kept)) stale.map(DeviceWidgetBinding::appWidgetId) else emptyList()
    }

    private fun write(bindings: Map<String, Int>): Boolean {
        if (bindings.size > MAX_BINDINGS) return false
        val root = JSONObject()
        bindings.toSortedMap().forEach { (workspaceItemId, appWidgetId) ->
            if (workspaceItemId.isBlank() || workspaceItemId.length > MAX_WORKSPACE_ID_LENGTH || appWidgetId <= 0) {
                return false
            }
            root.put(workspaceItemId, appWidgetId)
        }
        return preferences.edit().putString(KEY_BINDINGS, root.toString()).commit()
    }

    companion object {
        const val MAX_BINDINGS = 128
        private const val PREFERENCES_NAME = "kosch_workspace_widget_bindings_v1"
        private const val KEY_BINDINGS = "bindings"
    }
}
