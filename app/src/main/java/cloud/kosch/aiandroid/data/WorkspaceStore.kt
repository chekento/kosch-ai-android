package cloud.kosch.aiandroid.data

import android.content.Context
import cloud.kosch.aiandroid.ai.LocalUsageModel
import cloud.kosch.aiandroid.model.AppUsageSignal
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
import cloud.kosch.aiandroid.model.WorkspaceDocument
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class WorkspaceStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val workspaceV7 = WorkspaceV7Persistence(preferences)

    init {
        migrateIfNeeded()
        workspaceV7.migrateIfAbsent(loadScene(), loadPositions())
    }

    fun loadScene(): SceneId = runCatching {
        SceneId.valueOf(preferences.getString(KEY_SCENE, SceneId.AI.name).orEmpty())
    }.getOrDefault(SceneId.AI)

    fun saveScene(scene: SceneId) {
        val mirroredV7 = encodedLegacyMirror(
            activeScene = scene,
            positions = loadPositions(),
        )
        preferences.edit().apply {
            putString(KEY_SCENE, scene.name)
            mirroredV7?.let { putString(WorkspaceV7Persistence.KEY_WORKSPACE_DOCUMENT, it) }
        }.apply()
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

    fun loadWorkspaceDocument(): WorkspaceDocument =
        workspaceV7.loadOrLegacyFallback(loadScene(), loadPositions())

    fun savePositions(
        scene: SceneId,
        positions: Map<String, TilePosition>,
    ) {
        val effectivePositions = loadPositions().toMutableMap().apply {
            val updatedScene = get(scene).orEmpty().toMutableMap()
            positions.forEach { (id, position) -> updatedScene[id] = position.clamped() }
            put(scene, updatedScene)
        }
        val mirroredV7 = encodedLegacyMirror(
            activeScene = loadScene(),
            positions = effectivePositions,
        )
        preferences.edit().apply {
            positions.forEach { (id, position) ->
                val prefix = positionPrefix(scene, id)
                putFloat("${prefix}_x", position.x)
                putFloat("${prefix}_y", position.y)
            }
            mirroredV7?.let { putString(WorkspaceV7Persistence.KEY_WORKSPACE_DOCUMENT, it) }
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

    fun saveWidgetOrder(ids: List<Int>) {
        saveWidgetIds(ids.filter { it > 0 })
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

    fun hiddenAppKeys(): List<String> = readStringArray(KEY_HIDDEN_APP_KEYS)

    fun saveHiddenAppKeys(keys: List<String>) {
        writeStringArray(KEY_HIDDEN_APP_KEYS, keys.distinct().take(MAX_HIDDEN_APPS))
    }

    fun appUsageSignals(): Map<String, AppUsageSignal> = runCatching {
        val root = JSONObject(preferences.getString(KEY_APP_USAGE, "{}"))
        buildMap {
            root.keys().forEach { key ->
                if (key.isBlank() || key.length > MAX_APP_KEY_LENGTH) return@forEach
                val item = root.optJSONObject(key) ?: return@forEach
                val count = item.optInt("count", 0).coerceIn(1, LocalUsageModel.MAX_LAUNCH_COUNT)
                val lastUsed = item.optLong("lastUsed", 0L)
                if (lastUsed <= 0L) return@forEach
                put(key, AppUsageSignal(key, count, lastUsed))
            }
        }
    }.getOrDefault(emptyMap())

    fun recordAppUsage(appKey: String, nowEpochMillis: Long = System.currentTimeMillis()): Map<String, AppUsageSignal> {
        val updated = LocalUsageModel.observe(appUsageSignals(), appKey, nowEpochMillis)
        preferences.edit().putString(KEY_APP_USAGE, usageJson(updated).toString()).apply()
        return updated
    }

    fun clearAppUsage() {
        preferences.edit().putString(KEY_APP_USAGE, "{}").apply()
    }

    fun inkStrokes(): List<InkStroke> {
        val loaded = runCatching {
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
        return InkStrokeNormalizer.normalize(loaded)
    }

    fun saveInkStrokes(strokes: List<InkStroke>) {
        preferences.edit()
            .putString(KEY_PEN_STROKES, inkJson(InkStrokeNormalizer.normalize(strokes)).toString())
            .apply()
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

    /** Idempotently repairs keys created before profile serial numbers became the stable prefix. */
    fun migrateLegacyAppKeys(aliases: Map<String, String>): Boolean {
        if (aliases.isEmpty()) return false
        val oldPinned = pinnedAppKeys()
        val oldHidden = hiddenAppKeys()
        val oldFolders = folders()
        val oldUsage = appUsageSignals()
        val newPinned = AppKeyMigration.keys(oldPinned, aliases).take(MAX_PINNED_APPS)
        val newHidden = AppKeyMigration.keys(oldHidden, aliases).take(MAX_HIDDEN_APPS)
        val newFolders = oldFolders.map { folder ->
            folder.copy(appKeys = AppKeyMigration.keys(folder.appKeys, aliases).take(MAX_FOLDER_APPS))
        }
        val newUsage = AppKeyMigration.usage(oldUsage, aliases)
        if (oldPinned == newPinned && oldHidden == newHidden && oldFolders == newFolders && oldUsage == newUsage) {
            return false
        }
        return preferences.edit()
            .putString(KEY_PINNED_APPS, JSONArray(newPinned).toString())
            .putString(KEY_HIDDEN_APP_KEYS, JSONArray(newHidden).toString())
            .putString(KEY_FOLDERS, foldersJson(newFolders).toString())
            .putString(KEY_APP_USAGE, usageJson(newUsage).toString())
            .commit()
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
            .put("hidden", JSONArray(hiddenAppKeys()))
            .put("usage", usageJson(appUsageSignals()))
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
        val mirroredV7 = encodedLegacyMirror(
            activeScene = snapshot.scene,
            positions = snapshot.positions,
        )
        preferences.edit().apply {
            putString(KEY_SCENE, snapshot.scene.name)
            putString(KEY_HOME_PAGE, snapshot.homePage.name)
            putString(KEY_RECENT, snapshot.recent.joinToString("|"))
            putString(KEY_PINNED_APPS, JSONArray(snapshot.pinned).toString())
            putString(KEY_HIDDEN_APP_KEYS, JSONArray(snapshot.hidden).toString())
            putString(KEY_APP_USAGE, usageJson(snapshot.usage).toString())
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
            mirroredV7?.let { putString(WorkspaceV7Persistence.KEY_WORKSPACE_DOCUMENT, it) }
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

    private fun encodedLegacyMirror(
        activeScene: SceneId,
        positions: Map<SceneId, Map<String, TilePosition>>,
    ): String? = runCatching {
        WorkspaceV7LegacyMirror.seedOrUpdate(
            storedDocument = workspaceV7.loadStoredOrNull(),
            hasStoredRawValue = workspaceV7.hasStoredValue(),
            activeScene = activeScene,
            positions = positions,
        )?.let(WorkspaceDocumentCodec::encode)
    }.getOrNull()

    private fun decodeSnapshot(payload: ByteArray): DecodedSnapshot {
        require(payload.isNotEmpty()) { "Backup ist leer" }
        require(payload.size <= MAX_BACKUP_BYTES) { "Backup ist zu groß" }
        val root = runCatching { JSONObject(payload.toString(StandardCharsets.UTF_8)) }
            .getOrElse { throw IllegalArgumentException("Backup enthält kein gültiges JSON", it) }
        require(root.optString("format") == BACKUP_FORMAT) { "Unbekanntes Backup-Format" }
        val version = root.optInt("version", -1)
        require(version in MIN_BACKUP_VERSION..BACKUP_VERSION) { "Nicht unterstützte Backup-Version" }
        val createdAt = root.optLong("createdAt", -1L)
        require(createdAt in 1L..(System.currentTimeMillis() + MAX_CLOCK_SKEW_MILLIS)) {
            "Ungültiger Erstellungszeitpunkt"
        }
        val scene = enumValue<SceneId>(root.optString("scene"), "Szene")
        val homePage = enumValue<HomePage>(root.optString("homePage"), "Startbereich")
        val recent = validatedStrings(root.optJSONArray("recent"), MAX_RECENT, "Verlauf")
        val pinned = validatedStrings(root.optJSONArray("pinned"), MAX_PINNED_APPS, "Pins")
        val hidden = if (version >= 2) {
            validatedStrings(root.optJSONArray("hidden"), MAX_HIDDEN_APPS, "verborgenen Apps")
        } else {
            emptyList()
        }
        val usage = if (version >= 2) parseUsage(root.optJSONObject("usage"), createdAt) else emptyMap()
        val folders = parseFolders(root.optJSONArray("folders"))
        val positions = parsePositions(root.optJSONObject("positions"))
        val ink = parseInk(root.optJSONArray("ink"))
        return DecodedSnapshot(scene, homePage, recent, pinned, hidden, usage, folders, positions, ink, createdAt)
    }

    private fun parseUsage(root: JSONObject?, createdAt: Long): Map<String, AppUsageSignal> {
        if (root == null) return emptyMap()
        require(root.length() <= LocalUsageModel.MAX_SIGNALS) { "Zu viele lokale Lernsignale" }
        return buildMap {
            root.keys().forEach { key ->
                require(key.isNotBlank() && key.length <= MAX_APP_KEY_LENGTH) { "Ungültiger Lernsignal-Schlüssel" }
                val item = root.optJSONObject(key) ?: error("Ungültiges Lernsignal")
                val count = item.optInt("count", 0)
                val lastUsed = item.optLong("lastUsed", 0L)
                require(count in 1..LocalUsageModel.MAX_LAUNCH_COUNT) { "Ungültiger Lernsignal-Zähler" }
                require(lastUsed in 1L..(createdAt + MAX_CLOCK_SKEW_MILLIS)) { "Ungültiger Lernsignal-Zeitpunkt" }
                put(key, AppUsageSignal(key, count, lastUsed))
            }
        }
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

    private fun usageJson(usage: Map<String, AppUsageSignal>): JSONObject = JSONObject().apply {
        usage.values
            .sortedByDescending(AppUsageSignal::lastUsedEpochMillis)
            .take(LocalUsageModel.MAX_SIGNALS)
            .sortedBy(AppUsageSignal::key)
            .forEach { signal ->
                put(
                    signal.key,
                    JSONObject()
                        .put("count", signal.launchCount.coerceIn(1, LocalUsageModel.MAX_LAUNCH_COUNT))
                        .put("lastUsed", signal.lastUsedEpochMillis),
                )
            }
    }

    private fun inkJson(strokes: List<InkStroke>): JSONArray = JSONArray().apply {
        InkStrokeNormalizer.normalize(strokes).forEach { stroke ->
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
        if (current < 6) {
            editor.putString(KEY_HIDDEN_APP_KEYS, preferences.getString(KEY_HIDDEN_APP_KEYS, "[]"))
            editor.putString(KEY_APP_USAGE, preferences.getString(KEY_APP_USAGE, "{}"))
            editor.putInt(KEY_SCHEMA_VERSION, 6)
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
        const val KEY_HIDDEN_APP_KEYS = "hidden_app_keys_v6"
        const val KEY_APP_USAGE = "app_usage_v6"
        const val KEY_FOLDERS = "launcher_folders_v2"
        const val KEY_FOLDERS_INITIALIZED = "launcher_folders_initialized_v3"
        const val KEY_PEN_STROKES = "pen_strokes_v3"
        const val SCHEMA_VERSION = 6
        const val MAX_RECENT = 16
        const val MAX_PINNED_APPS = 5
        const val MAX_HIDDEN_APPS = 512
        const val MAX_FOLDERS = 12
        const val MAX_FOLDER_APPS = 32
        const val MAX_INK_STROKES = InkStrokeNormalizer.MAX_STROKES
        const val MAX_POINTS_PER_STROKE = InkStrokeNormalizer.MAX_POINTS_PER_STROKE
        const val MAX_BACKUP_BYTES = 5 * 1024 * 1024
        const val MAX_APP_KEY_LENGTH = 250
        const val MAX_ID_LENGTH = 80
        const val MAX_TITLE_LENGTH = 80
        const val BACKUP_FORMAT = "cloud.kosch.workspace"
        const val MIN_BACKUP_VERSION = 1
        const val BACKUP_VERSION = 2
        const val MAX_CLOCK_SKEW_MILLIS = 24L * 60L * 60L * 1_000L
    }

    private data class DecodedSnapshot(
        val scene: SceneId,
        val homePage: HomePage,
        val recent: List<String>,
        val pinned: List<String>,
        val hidden: List<String>,
        val usage: Map<String, AppUsageSignal>,
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
            hiddenCount = hidden.size,
            usageSignalCount = usage.size,
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