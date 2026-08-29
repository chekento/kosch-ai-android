package cloud.kosch.aiandroid.data

import android.content.Context
import cloud.kosch.aiandroid.ai.WidgetStackPolicy
import cloud.kosch.aiandroid.model.WidgetStack
import cloud.kosch.aiandroid.model.WidgetStackMode
import org.json.JSONArray
import org.json.JSONObject

/**
 * Device-local persistence for AppWidgetHost stack membership.
 *
 * This store intentionally uses a separate preferences file and is not referenced by portable backup codecs.
 */
class WidgetStackStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): List<WidgetStack> = runCatching {
        val array = JSONArray(preferences.getString(KEY_STACKS, "[]"))
        buildList {
            repeat(array.length().coerceAtMost(WidgetStackPolicy.MAX_STACKS)) { index ->
                val item = array.optJSONObject(index) ?: return@repeat
                val idsJson = item.optJSONArray("widgetIds") ?: JSONArray()
                val ids = buildList {
                    repeat(idsJson.length().coerceAtMost(WidgetStackPolicy.MAX_WIDGETS_PER_STACK)) { widgetIndex ->
                        idsJson.optInt(widgetIndex, -1).takeIf { it > 0 }?.let(::add)
                    }
                }
                val mode = runCatching {
                    WidgetStackMode.valueOf(item.optString("mode", WidgetStackMode.MANUAL.name))
                }.getOrDefault(WidgetStackMode.MANUAL)
                add(
                    WidgetStack(
                        id = item.optString("id"),
                        title = item.optString("title", "Widget Stack"),
                        appWidgetIds = ids,
                        activeIndex = item.optInt("activeIndex", 0),
                        mode = mode,
                        autoCycleSeconds = item.optInt("autoCycleSeconds", 0),
                    ),
                )
            }
        }
    }.map(WidgetStackPolicy::normalizeAll).getOrDefault(emptyList())

    fun save(stacks: List<WidgetStack>): Boolean {
        val normalized = WidgetStackPolicy.normalizeAll(stacks)
        val array = JSONArray().apply {
            normalized.forEach { stack ->
                put(
                    JSONObject()
                        .put("id", stack.id)
                        .put("title", stack.title)
                        .put("widgetIds", JSONArray(stack.appWidgetIds))
                        .put("activeIndex", stack.activeIndex)
                        .put("mode", stack.mode.name)
                        .put("autoCycleSeconds", stack.autoCycleSeconds),
                )
            }
        }
        return preferences.edit().putString(KEY_STACKS, array.toString()).commit()
    }

    fun clear(): Boolean = preferences.edit().remove(KEY_STACKS).commit()

    companion object {
        const val PREFERENCES_NAME = "kosch_widget_stacks_device_local"
        private const val KEY_STACKS = "stacks_v1"
    }
}
