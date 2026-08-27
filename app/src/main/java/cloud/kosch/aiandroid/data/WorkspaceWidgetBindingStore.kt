package cloud.kosch.aiandroid.data

import android.content.Context
import cloud.kosch.aiandroid.model.DeviceWidgetBinding
import cloud.kosch.aiandroid.model.MAX_WORKSPACE_ID_LENGTH
import org.json.JSONObject

/**
 * Device-only bridge between portable Workspace v7 widget items and Android AppWidgetHost ids.
 *
 * This store deliberately lives outside WorkspaceDocument and the portable backup codec. A restored or
 * duplicated widget therefore keeps only its provider identity and must receive a fresh Android host binding.
 */
class WorkspaceWidgetBindingStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): Map<String, Int> = decode(preferences.getString(KEY_BINDINGS, null))

    fun bindingFor(workspaceItemId: String): Int? = load()[workspaceItemId]

    fun bind(binding: DeviceWidgetBinding): Boolean {
        val current = load().toMutableMap()
        if (binding.workspaceItemId !in current && current.size >= MAX_BINDINGS) return false
        current[binding.workspaceItemId] = binding.appWidgetId
        return persist(current)
    }

    /** Returns the released Android host id only when the updated map was committed. */
    fun unbind(workspaceItemId: String): Int? {
        val safeId = workspaceItemId.trim()
        if (safeId.isBlank()) return null
        val current = load().toMutableMap()
        val appWidgetId = current.remove(safeId) ?: return null
        return appWidgetId.takeIf { persist(current) }
    }

    /**
     * Drops bindings whose portable item disappeared or whose Android host id is no longer valid.
     * The returned ids are safe for the caller to release from AppWidgetHost.
     */
    fun prune(
        validWorkspaceItemIds: Set<String>,
        validAppWidgetIds: Set<Int>,
    ): Set<Int> {
        val current = load()
        val kept = current.filter { (itemId, appWidgetId) ->
            itemId in validWorkspaceItemIds && appWidgetId in validAppWidgetIds
        }
        if (kept == current) return emptySet()
        if (!persist(kept)) return emptySet()
        return current
            .filterKeys { it !in kept }
            .values
            .filter { it > 0 }
            .toSet()
    }

    fun clear(): Boolean = preferences.edit().remove(KEY_BINDINGS).commit()

    private fun persist(bindings: Map<String, Int>): Boolean {
        val normalized = bindings.entries
            .asSequence()
            .filter { (itemId, appWidgetId) -> isValidItemId(itemId) && appWidgetId > 0 }
            .distinctBy { it.key }
            .take(MAX_BINDINGS)
            .sortedBy { it.key }
            .associate { it.key to it.value }
        val root = JSONObject()
        normalized.forEach { (itemId, appWidgetId) -> root.put(itemId, appWidgetId) }
        return preferences.edit().putString(KEY_BINDINGS, root.toString()).commit()
    }

    private fun decode(raw: String?): Map<String, Int> = runCatching {
        if (raw.isNullOrBlank()) return@runCatching emptyMap()
        val root = JSONObject(raw)
        buildMap {
            val keys = root.keys().asSequence().toList().sorted()
            for (itemId in keys) {
                if (size >= MAX_BINDINGS) break
                if (!isValidItemId(itemId)) continue
                val appWidgetId = root.optInt(itemId, -1)
                if (appWidgetId <= 0) continue
                put(itemId, appWidgetId)
            }
        }
    }.getOrDefault(emptyMap())

    private fun isValidItemId(value: String): Boolean =
        value.isNotBlank() && value.length <= MAX_WORKSPACE_ID_LENGTH

    private companion object {
        const val PREFERENCES_NAME = "kosch_workspace_widget_bindings_v1"
        const val KEY_BINDINGS = "bindings"
        const val MAX_BINDINGS = 128
    }
}
