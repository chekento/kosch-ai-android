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
 * Bindings are one-to-one: one workspace item owns at most one host id and one host id can never back two items.
 */
class WorkspaceWidgetBindingStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): Map<String, Int> = decode(preferences.getString(KEY_BINDINGS, null))

    fun bindingFor(workspaceItemId: String): Int? = load()[workspaceItemId]

    /**
     * Adds an idempotent one-to-one binding.
     *
     * Changing an existing item's host id must be an explicit unbind + bind operation so the caller can release
     * the old AppWidgetHost id. Likewise, a host id already owned by another item is rejected rather than aliased.
     */
    fun bind(binding: DeviceWidgetBinding): Boolean {
        val current = load().toMutableMap()
        val existingForItem = current[binding.workspaceItemId]
        if (existingForItem != null) return existingForItem == binding.appWidgetId
        if (current.any { (itemId, appWidgetId) ->
                itemId != binding.workspaceItemId && appWidgetId == binding.appWidgetId
            }
        ) return false
        if (current.size >= MAX_BINDINGS) return false
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
     * Keeps only exact item→host pairs that were independently validated by the caller.
     *
     * This is intentionally pair-aware: checking the set of item ids and the set of host ids independently can
     * accidentally preserve a crossed binding after restore or host-id reuse. Returned ids are safe to release.
     */
    fun prune(validBindings: Map<String, Int>): Set<Int> {
        val current = load()
        val kept = current.filter { (itemId, appWidgetId) -> validBindings[itemId] == appWidgetId }
        if (kept == current) return emptySet()
        if (!persist(kept)) return emptySet()
        return current
            .filter { (itemId, appWidgetId) -> kept[itemId] != appWidgetId }
            .values
            .filter { it > 0 }
            .toSet()
    }

    fun clear(): Boolean = preferences.edit().remove(KEY_BINDINGS).commit()

    private fun persist(bindings: Map<String, Int>): Boolean {
        val usedHostIds = mutableSetOf<Int>()
        val normalized = bindings.entries
            .asSequence()
            .filter { (itemId, appWidgetId) -> isValidItemId(itemId) && appWidgetId > 0 }
            .sortedBy { it.key }
            .filter { (_, appWidgetId) -> usedHostIds.add(appWidgetId) }
            .take(MAX_BINDINGS)
            .associate { it.key to it.value }
        val root = JSONObject()
        normalized.forEach { (itemId, appWidgetId) -> root.put(itemId, appWidgetId) }
        return preferences.edit().putString(KEY_BINDINGS, root.toString()).commit()
    }

    private fun decode(raw: String?): Map<String, Int> = runCatching {
        if (raw.isNullOrBlank()) return@runCatching emptyMap()
        val root = JSONObject(raw)
        val usedHostIds = mutableSetOf<Int>()
        buildMap {
            val keys = root.keys().asSequence().toList().sorted()
            for (itemId in keys) {
                if (size >= MAX_BINDINGS) break
                if (!isValidItemId(itemId)) continue
                val appWidgetId = root.optInt(itemId, -1)
                if (appWidgetId <= 0 || !usedHostIds.add(appWidgetId)) continue
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
