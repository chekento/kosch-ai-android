package cloud.kosch.aiandroid.data

import android.content.Context
import cloud.kosch.aiandroid.model.DefaultWorkspace
import cloud.kosch.aiandroid.model.BackupPreview
import cloud.kosch.aiandroid.model.FolderKind
import cloud.kosch.aiandroid.model.HomePage
import cloud.kosch.aiandroid.model.InkPoint
import cloud.kosch.aiandroid.model.InkStroke
import cloud.kosch.aiandroid.model.InkTool
import cloud.kosch.aiandroid.model.LauncherFolder
import cloud.kosch.aiandroid.model.SceneId
import cloud.kosch.aiandroid.model.TilePosition
import cloud.kosch.aiandroid.model.WidgetSizePreset
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class WorkspaceStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    init {
        migrateIfNeeded()
    }

    fun loadScene(): SceneId = runCatching {
        SceneId.valueOf(preferences.getString(KEY_SCENE, SceneId.AI.name).orEmpty())
    }.getOrDefault(SceneId.AI)

    fun saveScene(scene: SceneId) {
        preferences.edit().putString(KEY_SCENE, scene.name).apply()
    }

    fun loadHomePage(): HomePage = runCatching {
        HomePage.valueOf(preferences.getString(KEY_HOME_PAGE, HomePage.PRO_DESK.name).orEmpty())
    }.getOrDefault(HomePage.PRO_DESK)

    fun saveHomePage(page: HomePage) {
        preferences.edit().putString(KEY_HOME_PAGE, page.name).apply()
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
        setWidgetSize(appWidgetId, WidgetSizePreset.STANDARD)
    }

    fun removeWidgetId(appWidgetId: Int) {
        saveWidgetIds(widgetIds().filterNot { it == appWidgetId })
        val updated = widgetSizes().filterKeys { it != appWidgetId }
        preferences.edit().putString(KEY_WIDGET_SIZES, widgetSizesJson(updated).toString()).apply()
    }

    fun widgetSizes(): Map<Int, WidgetSizePreset> = runCatching {
        val root = JSONObject(preferences.getString(KEY_WIDGET_SIZES, "{}"))
        buildMap {
            root.keys().forEach { key ->
                val id = key.toIntOrNull()?.takeIf { it > 0 } ?: return@forEach
                val preset = runCatching { WidgetSizePreset.valueOf(root.optString(key)) }.getOrNull()
                    ?: return@forEach
                put(id, preset)
            }
        }
    }.getOrDefault(emptyMap())

    fun setWidgetSize(appWidgetId: Int, preset: WidgetSizePreset) {
        require(appWidgetId > 0)
        val updated = widgetSizes() + (appWidgetId to preset)
        preferences.edit().putString(KEY_WIDGET_SIZES, widgetSizesJson(updated).toString()).apply()
    }

    fun pinnedAppKeys(): List<String> = readStringArray(KEY_PINNED_APPS)

    fun savePinnedAppKeys(keys: List<String>) {
        writeStringArray(KEY_PINNED_APPS, keys.distinct().take(MAX_PINNED_APPS))
    }

    fun inkStrokes(): List<InkStroke> = runCatching {
        val strokes = JSONArray(preferences.getString(KEY_PEN_STROKES, "[]"))
        buildList {
            repeat(strokes.length().coerceAtMost(MAX_INK_STROKES)) strokeLoop@{ strokeIndex ->
                val strokeJson = strokes.optJSONObject(strokeIndex) ?: return@strokeLoop
                val tool = runCatching {
                    InkTool.valueOf(strokeJson.optString("tool", InkTool.PEN.name))
                }.getOrDefault(InkTool.PEN)
                val pointsJson = strokeJson.optJSONArray("points") ?: JSONArray()
                val points = buildList {
                    repeat(pointsJson.length().coerceAtMost(MAX_POINTS_PER_STROKE)) pointLoop@{ pointIndex ->
                        val point = pointsJson.optJSONArray(pointIndex) ?: return@pointLoop
                        if (point.length() < 2) return@pointLoop
                        add(
                            InkPoint(
                                x = point.optDouble(0, 0.0).toFloat().coerceIn(0f, 1f),
                                y = point.optDouble(1, 0.0).toFloat().coerceIn(0f, 1f),
                                pressure = point.optDouble(2, 0.5).toFloat().coerceIn(0f, 1f),
                                tiltRadians = point.optDouble(3, 0.0).toFloat(),
                            ),
                        )
                    }
                }
                if (points.isNotEmpty()) add(InkStroke(tool, points))
            }
        }
    }.getOrDefault(emptyList())

    fun saveInkStrokes(strokes: List<InkStroke>) {
        val strokesJson = JSONArray()
        strokes.takeLast(MAX_INK_STROKES).forEach { stroke ->
            val pointsJson = JSONArray()
            stroke.points.take(MAX_POINTS_PER_STROKE).forEach { point ->
                pointsJson.put(
                    JSONArray()
                        .put(point.x.toDouble())
                        .put(point.y.toDouble())
                        .put(point.pressure.toDouble())
                        .put(point.tiltRadians.toDouble()),
                )
            }
            strokesJson.put(
                JSONObject()
                    .put("tool", stroke.tool.name)
                    .put("points", pointsJson),
            )
        }
        preferences.edit().putString(KEY_PEN_STROKES, strokesJson.toString()).apply()
    }

    fun folders(): List<LauncherFolder> = runCatching {
        val array = JSONArray(preferences.getString(KEY_FOLDERS, "[]"))
        buildList {
            repeat(array.length()) { index ->
                val item = array.getJSONObject(index)
                val appKeysJson = item.optJSONArray("appKeys") ?: JSONArray()
                val appKeys = buildList {
                    repeat(appKeysJson.length()) { appIndex ->
                        appKeysJson.optString(appIndex).takeIf(String::isNotBlank)?.let(::add)
                    }
                }.distinct()
                val kind = runCatching {
                    FolderKind.valueOf(item.optString("kind", FolderKind.OTHER.name))
                }.getOrDefault(FolderKind.OTHER)
                val id = item.optString("id").takeIf(String::isNotBlank) ?: return@repeat
                add(
                    LauncherFolder(
                        id = id,
                        title = item.optString("title", kind.title).ifBlank { kind.title },
                        kind = kind,
                        appKeys = appKeys,
                        generatedLocally = item.optBoolean("generatedLocally", true),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    fun areFoldersInitialized(): Boolean = preferences.getBoolean(KEY_FOLDERS_INITIALIZED, false)

    fun saveFolders(folders: List<LauncherFolder>) {
        val array = foldersJson(folders)
        preferences.edit()
            .putString(KEY_FOLDERS, array.toString())
            .putBoolean(KEY_FOLDERS_INITIALIZED, true)
            .apply()
    }

    fun createPortableSnapshot(nowEpochMillis: Long = System.currentTimeMillis()): ByteArray {
        val positionsJson = JSONObject()
        loadPositions().forEach { (scene, positions) ->
            val sceneJson = JSONObject()
            positions.forEach { (id, position) ->
                sceneJson.put(id, JSONArray().put(position.x.toDouble()).put(position.y.toDouble()))
            }
            positionsJson.put(scene.name, sceneJson)
        }
        val root = JSONObject()
            .put("format", BACKUP_FORMAT)
            .put("version", BACKUP_VERSION)
            .put("createdAt", nowEpochMillis)
            .put("scene", loadScene().name)
            .put("homePage", loadHomePage().name)
            .put("recent", JSONArray(recentPackages()))
            .put("pinned", JSONArray(pinnedAppKeys()))
            .put("folders", foldersJson(folders()))
            .put("positions", positionsJson)
            .put("ink", inkJson(inkStrokes()))
            .put(
                "excluded",
                JSONArray(listOf("widgetHostIds", "documentGrants", "credentials", "notificationData", "auditLog")),
            )
        return root.toString().toByteArray(StandardCharsets.UTF_8)
    }

    fun previewPortableSnapshot(payload: ByteArray): BackupPreview = decodeSnapshot(payload).preview()

    fun restorePortableSnapshot(payload: ByteArray): BackupPreview {
        val snapshot = decodeSnapshot(payload)
        preferences.edit().apply {
            putString(KEY_SCENE, snapshot.scene.name)
            putString(KEY_HOME_PAGE, snapshot.homePage.name)
            putString(KEY_RECENT, snapshot.recent.joinToString("|"))
            putString(KEY_PINNED_APPS, JSONArray(snapshot.pinned).toString())
            putString(KEY_FOLDERS, foldersJson(snapshot.folders).toString())
            putBoolean(KEY_FOLDERS_INITIALIZED, true)
            putString(KEY_PEN_STROKES, inkJson(snapshot.inkStrokes).toString())
            putInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION)
            snapshot.positions.forEach { (scene, positions) ->
                positions.forEach { (id, position) ->
                    val prefix = positionPrefix(scene, id)
                    putFloat("${prefix}_x", position.x)
                    putFloat("${prefix}_y", position.y)
                }
            }
        }.commit().also { committed ->
            check(committed) { "Workspace restore could not be committed" }
        }
        return snapshot.preview()
    }

    private fun saveWidgetIds(ids: List<Int>) {
        preferences.edit()
            .putString(KEY_WIDGET_IDS, ids.distinct().joinToString("|"))
            .apply()
    }

    private fun widgetSizesJson(sizes: Map<Int, WidgetSizePreset>): JSONObject = JSONObject().apply {
        sizes.toSortedMap().forEach { (id, preset) -> put(id.toString(), preset.name) }
    }

    private fun readStringArray(key: String): List<String> = runCatching {
        val array = JSONArray(preferences.getString(key, "[]"))
        buildList {
            repeat(array.length()) { index ->
                array.optString(index).takeIf(String::isNotBlank)?.let(::add)
            }
        }.distinct()
    }.getOrDefault(emptyList())

    private fun writeStringArray(key: String, values: List<String>) {
        preferences.edit().putString(key, JSONArray(values).toString()).apply()
    }

    private fun decodeSnapshot(payload: ByteArray): DecodedSnapshot {
        require(payload.isNotEmpty()) { "Backup ist leer" }
        require(payload.size <= MAX_BACKUP_BYTES) { "Backup ist zu groß" }
        val root = runCatching { JSONObject(payload.toString(StandardCharsets.UTF_8)) }
            .getOrElse { throw IllegalArgumentException("Backup enthält kein gültiges JSON", it) }
        require(root.optString("format") == BACKUP_FORMAT) { "Unbekanntes Backup-Format" }
        require(root.optInt("version", -1) == BACKUP_VERSION) { "Nicht unterstützte Backup-Version" }
        val createdAt = root.optLong("createdAt", -1L)
        require(createdAt in 1L..(System.currentTimeMillis() + MAX_CLOCK_SKEW_MILLIS)) {
            "Ungültiger Erstellungszeitpunkt"
        }
        val scene = enumValue<SceneId>(root.optString("scene"), "Szene")
        val homePage = enumValue<HomePage>(root.optString("homePage"), "Startbereich")
        val recent = validatedStrings(root.optJSONArray("recent"), MAX_RECENT, "Verlauf")
        val pinned = validatedStrings(root.optJSONArray("pinned"), MAX_PINNED_APPS, "Pins")
        val folders = parseFolders(root.optJSONArray("folders"))
        val positions = parsePositions(root.optJSONObject("positions"))
        val ink = parseInk(root.optJSONArray("ink"))
        return DecodedSnapshot(scene, homePage, recent, pinned, folders, positions, ink, createdAt)
    }

    private fun parsePositions(root: JSONObject?): Map<SceneId, Map<String, TilePosition>> {
        require(root != null) { "Positionsdaten fehlen" }
        return SceneId.entries.associateWith { scene ->
            val sceneJson = root.optJSONObject(scene.name) ?: JSONObject()
            DefaultWorkspace.tiles(scene).associate { tile ->
                val point = sceneJson.optJSONArray(tile.id)
                val x = point?.optDouble(0, tile.defaultPosition.x.toDouble())?.toFloat()
                    ?: tile.defaultPosition.x
                val y = point?.optDouble(1, tile.defaultPosition.y.toDouble())?.toFloat()
                    ?: tile.defaultPosition.y
                require(x.isFinite() && y.isFinite() && x in 0f..1f && y in 0f..1f) {
                    "Ungültige Position für ${tile.id}"
                }
                tile.id to TilePosition(x, y)
            }
        }
    }

    private fun parseFolders(array: JSONArray?): List<LauncherFolder> {
        if (array == null) return emptyList()
        require(array.length() <= MAX_FOLDERS) { "Zu viele Ordner im Backup" }
        return buildList {
            repeat(array.length()) { index ->
                val item = array.optJSONObject(index) ?: error("Ungültiger Ordner")
                val id = item.optString("id").trim()
                val title = item.optString("title").trim()
                require(id.isNotBlank() && id.length <= MAX_ID_LENGTH) { "Ungültige Ordner-ID" }
                require(title.isNotBlank() && title.length <= MAX_TITLE_LENGTH) { "Ungültiger Ordnername" }
                val kind = enumValue<FolderKind>(item.optString("kind"), "Ordnertyp")
                add(
                    LauncherFolder(
                        id = id,
                        title = title,
                        kind = kind,
                        appKeys = validatedStrings(item.optJSONArray("appKeys"), MAX_FOLDER_APPS, "Ordner-Apps"),
                        generatedLocally = item.optBoolean("generatedLocally", true),
                    ),
                )
            }
        }.distinctBy(LauncherFolder::id)
    }

    private fun parseInk(array: JSONArray?): List<InkStroke> {
        if (array == null) return emptyList()
        require(array.length() <= MAX_INK_STROKES) { "Zu viele Stiftstriche im Backup" }
        return buildList {
            repeat(array.length()) { strokeIndex ->
                val strokeJson = array.optJSONObject(strokeIndex) ?: error("Ungültiger Stiftstrich")
                val tool = enumValue<InkTool>(strokeJson.optString("tool"), "Stiftwerkzeug")
                val pointsJson = strokeJson.optJSONArray("points") ?: error("Stiftpunkte fehlen")
                require(pointsJson.length() in 1..MAX_POINTS_PER_STROKE) { "Ungültige Anzahl Stiftpunkte" }
                val points = buildList {
                    repeat(pointsJson.length()) { pointIndex ->
                        val point = pointsJson.optJSONArray(pointIndex) ?: error("Ungültiger Stiftpunkt")
                        require(point.length() >= 4) { "Unvollständiger Stiftpunkt" }
                        val x = point.optDouble(0).toFloat()
                        val y = point.optDouble(1).toFloat()
                        val pressure = point.optDouble(2).toFloat()
                        val tilt = point.optDouble(3).toFloat()
                        require(x.isFinite() && y.isFinite() && pressure.isFinite() && tilt.isFinite()) {
                            "Nicht-endlicher Stiftwert"
                        }
                        require(x in 0f..1f && y in 0f..1f && pressure in 0f..1f) {
                            "Stiftwert außerhalb des Bereichs"
                        }
                        add(InkPoint(x, y, pressure, tilt))
                    }
                }
                add(InkStroke(tool, points))
            }
        }
    }

    private fun validatedStrings(array: JSONArray?, limit: Int, label: String): List<String> {
        if (array == null) return emptyList()
        require(array.length() <= limit) { "Zu viele Einträge in $label" }
        return buildList {
            repeat(array.length()) { index ->
                val value = array.optString(index).trim()
                require(value.isNotBlank() && value.length <= MAX_APP_KEY_LENGTH) {
                    "Ungültiger Eintrag in $label"
                }
                add(value)
            }
        }.distinct()
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String, label: String): T =
        enumValues<T>().firstOrNull { it.name == value }
            ?: throw IllegalArgumentException("Ungültige Angabe für $label")

    private fun foldersJson(folders: List<LauncherFolder>): JSONArray = JSONArray().apply {
        folders.distinctBy(LauncherFolder::id).take(MAX_FOLDERS).forEach { folder ->
            put(
                JSONObject()
                    .put("id", folder.id)
                    .put("title", folder.title)
                    .put("kind", folder.kind.name)
                    .put("generatedLocally", folder.generatedLocally)
                    .put("appKeys", JSONArray(folder.appKeys.distinct().take(MAX_FOLDER_APPS))),
            )
        }
    }

    private fun inkJson(strokes: List<InkStroke>): JSONArray = JSONArray().apply {
        strokes.takeLast(MAX_INK_STROKES).forEach { stroke ->
            val points = JSONArray()
            stroke.points.take(MAX_POINTS_PER_STROKE).forEach { point ->
                points.put(
                    JSONArray()
                        .put(point.x.toDouble())
                        .put(point.y.toDouble())
                        .put(point.pressure.toDouble())
                        .put(point.tiltRadians.toDouble()),
                )
            }
            put(JSONObject().put("tool", stroke.tool.name).put("points", points))
        }
    }

    private fun migrateIfNeeded() {
        val current = preferences.getInt(KEY_SCHEMA_VERSION, 0)
        if (current >= SCHEMA_VERSION) return

        val editor = preferences.edit()
        if (current < 1) {
            // M1/M2 values already used stable keys. Version 1 records that baseline.
            editor.putInt(KEY_SCHEMA_VERSION, 1)
        }
        if (current < 2) {
            // New collections are JSON arrays so component keys cannot corrupt delimiter parsing.
            editor.putString(KEY_PINNED_APPS, preferences.getString(KEY_PINNED_APPS, "[]"))
            editor.putString(KEY_FOLDERS, preferences.getString(KEY_FOLDERS, "[]"))
            editor.putInt(KEY_SCHEMA_VERSION, 2)
        }
        if (current < 3) {
            editor.putString(KEY_PEN_STROKES, preferences.getString(KEY_PEN_STROKES, "[]"))
            editor.putBoolean(KEY_FOLDERS_INITIALIZED, preferences.contains(KEY_FOLDERS))
            editor.putInt(KEY_SCHEMA_VERSION, 3)
        }
        if (current < 4) {
            editor.putInt(KEY_SCHEMA_VERSION, 4)
        }
        if (current < 5) {
            editor.putString(KEY_WIDGET_SIZES, preferences.getString(KEY_WIDGET_SIZES, "{}"))
            editor.putInt(KEY_SCHEMA_VERSION, 5)
        }
        editor.apply()
    }

    private fun positionPrefix(scene: SceneId, tileId: String) =
        "position_${scene.name.lowercase()}_$tileId"

    private companion object {
        const val PREFERENCES_NAME = "kosch_launcher_workspace"
        const val KEY_SCENE = "active_scene"
        const val KEY_HOME_PAGE = "home_page_v2"
        const val KEY_RECENT = "recent_packages"
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete_v2"
        const val KEY_WIDGET_IDS = "widget_ids_v1"
        const val KEY_WIDGET_SIZES = "widget_sizes_v5"
        const val KEY_SCHEMA_VERSION = "schema_version"
        const val KEY_PINNED_APPS = "pinned_app_keys_v2"
        const val KEY_FOLDERS = "launcher_folders_v2"
        const val KEY_FOLDERS_INITIALIZED = "launcher_folders_initialized_v3"
        const val KEY_PEN_STROKES = "pen_strokes_v3"
        const val SCHEMA_VERSION = 5
        const val MAX_RECENT = 16
        const val MAX_PINNED_APPS = 5
        const val MAX_FOLDERS = 12
        const val MAX_FOLDER_APPS = 32
        const val MAX_INK_STROKES = 100
        const val MAX_POINTS_PER_STROKE = 2_048
        const val MAX_BACKUP_BYTES = 5 * 1024 * 1024
        const val MAX_APP_KEY_LENGTH = 250
        const val MAX_ID_LENGTH = 80
        const val MAX_TITLE_LENGTH = 80
        const val BACKUP_FORMAT = "cloud.kosch.workspace"
        const val BACKUP_VERSION = 1
        const val MAX_CLOCK_SKEW_MILLIS = 24L * 60L * 60L * 1_000L
    }

    private data class DecodedSnapshot(
        val scene: SceneId,
        val homePage: HomePage,
        val recent: List<String>,
        val pinned: List<String>,
        val folders: List<LauncherFolder>,
        val positions: Map<SceneId, Map<String, TilePosition>>,
        val inkStrokes: List<InkStroke>,
        val createdAtEpochMillis: Long,
    ) {
        fun preview() = BackupPreview(
            scene = scene,
            homePage = homePage,
            positionCount = positions.values.sumOf(Map<String, TilePosition>::size),
            recentCount = recent.size,
            pinnedCount = pinned.size,
            folderCount = folders.size,
            inkStrokeCount = inkStrokes.size,
            createdAtEpochMillis = createdAtEpochMillis,
            skippedItems = listOf(
                "Widgets müssen wegen gerätegebundener Host-IDs neu hinzugefügt werden",
                "Dateifreigaben, Zugangsdaten, Benachrichtigungsdaten und Audit bleiben auf diesem Gerät",
            ),
        )
    }
}
