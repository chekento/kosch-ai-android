package cloud.kosch.aiandroid.data

import android.content.Context
import cloud.kosch.aiandroid.model.DefaultWorkspace
import cloud.kosch.aiandroid.model.SceneId
import cloud.kosch.aiandroid.model.TilePosition

class WorkspaceStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadScene(): SceneId = runCatching {
        SceneId.valueOf(preferences.getString(KEY_SCENE, SceneId.AI.name).orEmpty())
    }.getOrDefault(SceneId.AI)

    fun saveScene(scene: SceneId) {
        preferences.edit().putString(KEY_SCENE, scene.name).apply()
    }

    fun loadPositions(): Map<SceneId, Map<String, TilePosition>> = SceneId.entries.associateWith { scene ->
        DefaultWorkspace.tiles(scene).associate { tile ->
            val prefix = positionPrefix(scene, tile.id)
            tile.id to TilePosition(
                x = preferences.getFloat("${prefix}_x", tile.defaultPosition.x),
                y = preferences.getFloat("${prefix}_y", tile.defaultPosition.y),
            ).clamped()
        }
    }

    fun savePositions(
        scene: SceneId,
        positions: Map<String, TilePosition>,
    ) {
        preferences.edit().apply {
            positions.forEach { (id, position) ->
                val prefix = positionPrefix(scene, id)
                putFloat("${prefix}_x", position.x)
                putFloat("${prefix}_y", position.y)
            }
        }.apply()
    }

    fun recentPackages(): List<String> = preferences
        .getString(KEY_RECENT, null)
        ?.split('|')
        ?.filter(String::isNotBlank)
        .orEmpty()

    fun recordRecent(packageName: String) {
        val updated = (listOf(packageName) + recentPackages())
            .distinct()
            .take(MAX_RECENT)
        preferences.edit().putString(KEY_RECENT, updated.joinToString("|")).apply()
    }

    fun isOnboardingComplete(): Boolean = preferences.getBoolean(KEY_ONBOARDING_COMPLETE, false)

    fun completeOnboarding() {
        preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply()
    }

    fun widgetIds(): List<Int> = preferences
        .getString(KEY_WIDGET_IDS, null)
        ?.split('|')
        ?.mapNotNull(String::toIntOrNull)
        ?.distinct()
        .orEmpty()

    fun addWidgetId(appWidgetId: Int) {
        saveWidgetIds(widgetIds() + appWidgetId)
    }

    fun removeWidgetId(appWidgetId: Int) {
        saveWidgetIds(widgetIds().filterNot { it == appWidgetId })
    }

    private fun saveWidgetIds(ids: List<Int>) {
        preferences.edit()
            .putString(KEY_WIDGET_IDS, ids.distinct().joinToString("|"))
            .apply()
    }

    private fun positionPrefix(scene: SceneId, tileId: String) =
        "position_${scene.name.lowercase()}_$tileId"

    private companion object {
        const val PREFERENCES_NAME = "kosch_launcher_workspace"
        const val KEY_SCENE = "active_scene"
        const val KEY_RECENT = "recent_packages"
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete_v2"
        const val KEY_WIDGET_IDS = "widget_ids_v1"
        const val MAX_RECENT = 16
    }
}
