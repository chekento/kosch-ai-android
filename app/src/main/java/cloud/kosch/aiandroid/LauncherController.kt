package cloud.kosch.aiandroid

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.kosch.aiandroid.ai.AiProviderProfile
import cloud.kosch.aiandroid.ai.AiProviderRegistry
import cloud.kosch.aiandroid.ai.LauncherCommand
import cloud.kosch.aiandroid.ai.LocalAppClassifier
import cloud.kosch.aiandroid.ai.LocalCommandPlanner
import cloud.kosch.aiandroid.ai.SearchDocument
import cloud.kosch.aiandroid.ai.SearchRanker
import cloud.kosch.aiandroid.ai.SmartCollection
import cloud.kosch.aiandroid.data.AppCatalog
import cloud.kosch.aiandroid.data.WorkspaceStore
import cloud.kosch.aiandroid.model.ContextSnapshot
import cloud.kosch.aiandroid.model.DefaultWorkspace
import cloud.kosch.aiandroid.model.LaunchableApp
import cloud.kosch.aiandroid.model.PositionedTile
import cloud.kosch.aiandroid.model.SceneId
import cloud.kosch.aiandroid.model.TilePosition
import cloud.kosch.aiandroid.model.WorkspaceMode
import cloud.kosch.aiandroid.system.HomeRoleController
import cloud.kosch.aiandroid.system.LocalContextEngine
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LauncherController(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val store = WorkspaceStore(appContext)
    private val commandPlanner = LocalCommandPlanner()
    private val contextEngine = LocalContextEngine(appContext)
    private val appCatalog = AppCatalog(appContext, mainHandler, ::refreshApps)

    var apps by mutableStateOf<List<LaunchableApp>>(emptyList())
        private set
    var appsLoading by mutableStateOf(true)
        private set
    var activeScene by mutableStateOf(store.loadScene())
        private set
    var workspaceMode by mutableStateOf(WorkspaceMode.PLAY)
        private set
    var workspacePositions by mutableStateOf(store.loadPositions())
        private set
    var previewPositions by mutableStateOf<Map<String, TilePosition>?>(null)
        private set
    var canUndoLayout by mutableStateOf(false)
        private set
    var contextSnapshot by mutableStateOf(contextEngine.snapshot())
        private set
    var isDefaultHome by mutableStateOf(HomeRoleController.isDefaultHome(appContext))
        private set
    var drawerVisible by mutableStateOf(false)
        private set
    var drawerCollection by mutableStateOf(SmartCollection.ALL)
        private set
    var providerChooserVisible by mutableStateOf(false)
        private set
    var providerPrompt by mutableStateOf("")
        private set
    var contextDetailsVisible by mutableStateOf(false)
        private set
    var notice by mutableStateOf<String?>(null)
        private set
    var recentPackages by mutableStateOf(store.recentPackages())
        private set

    private var undoPositions: Map<SceneId, Map<String, TilePosition>>? = null
    private var started = false

    fun start() {
        if (started) return
        started = true
        appCatalog.startListening()
        refreshApps()
        refreshSystemState()
    }

    fun close() {
        if (!started) return
        appCatalog.stopListening()
        executor.shutdownNow()
        started = false
    }

    fun refreshSystemState() {
        isDefaultHome = HomeRoleController.isDefaultHome(appContext)
        contextSnapshot = contextEngine.snapshot()
    }

    fun selectWorkspaceMode(mode: WorkspaceMode) {
        workspaceMode = mode
        if (mode == WorkspaceMode.PLAY) previewPositions = null
    }

    fun switchScene(scene: SceneId) {
        activeScene = scene
        store.saveScene(scene)
        previewPositions = null
        notice = "Szene ${scene.title} aktiv"
    }

    fun useSuggestedScene() {
        switchScene(contextSnapshot.suggestedScene)
    }

    fun currentTiles(): List<PositionedTile> {
        val positions = previewPositions ?: workspacePositions[activeScene].orEmpty()
        return DefaultWorkspace.tiles(activeScene).map { tile ->
            PositionedTile(
                tile = tile,
                position = positions[tile.id] ?: tile.defaultPosition,
            )
        }
    }

    fun moveTile(id: String, position: TilePosition) {
        rememberUndoPoint()
        val current = workspacePositions[activeScene].orEmpty()
        val updated = current + (id to position.clamped())
        workspacePositions = workspacePositions + (activeScene to updated)
        store.savePositions(activeScene, updated)
    }

    fun proposeSmartLayout() {
        if (workspaceMode != WorkspaceMode.EDIT) return
        val slots = listOf(
            TilePosition(0.04f, 0.04f),
            TilePosition(0.54f, 0.04f),
            TilePosition(0.04f, 0.52f),
            TilePosition(0.54f, 0.52f),
        )
        previewPositions = DefaultWorkspace.tiles(activeScene)
            .mapIndexed { index, tile -> tile.id to slots[index % slots.size] }
            .toMap()
        notice = "Lokaler Layout-Vorschlag – noch nicht angewendet"
    }

    fun applyLayoutPreview() {
        val preview = previewPositions ?: return
        rememberUndoPoint()
        workspacePositions = workspacePositions + (activeScene to preview)
        store.savePositions(activeScene, preview)
        previewPositions = null
        notice = "Layout angewendet – Rückgängig ist verfügbar"
    }

    fun discardLayoutPreview() {
        previewPositions = null
        notice = "Vorschlag verworfen"
    }

    fun resetSceneLayout() {
        rememberUndoPoint()
        val defaults = DefaultWorkspace.tiles(activeScene).associate { it.id to it.defaultPosition }
        workspacePositions = workspacePositions + (activeScene to defaults)
        store.savePositions(activeScene, defaults)
        previewPositions = null
        notice = "Szene auf Standard zurückgesetzt"
    }

    fun undoLayout() {
        val previous = undoPositions ?: return
        val current = workspacePositions
        workspacePositions = previous
        previous.forEach(store::savePositions)
        undoPositions = current
        canUndoLayout = true
        previewPositions = null
        notice = "Layout-Schritt rückgängig gemacht"
    }

    fun openDrawer(collection: SmartCollection = SmartCollection.ALL) {
        drawerCollection = collection
        drawerVisible = true
    }

    fun closeDrawer() {
        drawerVisible = false
    }

    fun selectDrawerCollection(collection: SmartCollection) {
        drawerCollection = collection
    }

    fun openProviderChooser(prompt: String = "") {
        providerPrompt = prompt
        providerChooserVisible = true
    }

    fun closeProviderChooser() {
        providerChooserVisible = false
    }

    fun updateProviderPrompt(value: String) {
        providerPrompt = value
    }

    fun showContextDetails() {
        refreshSystemState()
        contextDetailsVisible = true
    }

    fun hideContextDetails() {
        contextDetailsVisible = false
    }

    fun submitCommand(
        text: String,
        requestVoice: () -> Unit,
    ) {
        when (val command = commandPlanner.plan(text)) {
            LauncherCommand.Empty -> Unit
            LauncherCommand.OpenDrawer -> openDrawer()
            LauncherCommand.StartVoice -> requestVoice()
            is LauncherCommand.SwitchScene -> switchScene(command.scene)
            is LauncherCommand.LaunchApp -> launchBestMatch(command.query)
            is LauncherCommand.RoutePrompt -> openProviderChooser(command.prompt)
        }
    }

    fun launch(app: LaunchableApp) {
        runCatching { appCatalog.launch(app) }
            .onSuccess {
                store.recordRecent(app.packageName)
                recentPackages = store.recentPackages()
                drawerVisible = false
            }
            .onFailure { notice = "${app.label} konnte nicht gestartet werden" }
    }

    fun rankedApps(
        query: String,
        collection: SmartCollection,
    ): List<LaunchableApp> {
        val providerPackages = AiProviderRegistry.installedProviderPackages(apps)
        val filtered = apps.filter { app ->
            LocalAppClassifier.belongsTo(
                collection = collection,
                label = app.label,
                packageName = app.packageName,
                recentPackages = recentPackages,
                providerPackages = providerPackages,
            )
        }
        val byKey = filtered.associateBy { it.key }
        val ranked = SearchRanker.rank(
            query = query,
            documents = filtered.map { app ->
                SearchDocument(
                    id = app.key,
                    title = app.label,
                    keywords = listOf(app.packageName),
                )
            },
        ).mapNotNull { byKey[it.id] }
        return if (collection == SmartCollection.RECENT && query.isBlank()) {
            ranked.sortedBy { recentPackages.indexOf(it.packageName).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE }
        } else {
            ranked
        }
    }

    fun installedProviderApp(provider: AiProviderProfile): LaunchableApp? =
        AiProviderRegistry.installedApp(provider, apps)

    fun routeToProvider(provider: AiProviderProfile) {
        val prompt = providerPrompt.trim()
        val installed = installedProviderApp(provider)

        val result = when {
            installed != null && prompt.isBlank() -> runCatching { appCatalog.launch(installed) }
            installed != null -> shareTextWithPackage(installed.packageName, prompt)
            else -> openWeb(provider.webUrl)
        }

        result.onSuccess {
            providerChooserVisible = false
            notice = if (installed == null) {
                "${provider.name} im Browser geöffnet"
            } else if (prompt.isBlank()) {
                "${provider.name} geöffnet"
            } else {
                "Text bewusst an ${provider.name} übergeben"
            }
        }.onFailure {
            openWeb(provider.webUrl)
                .onSuccess {
                    providerChooserVisible = false
                    notice = "App-Übergabe nicht möglich – ${provider.name} im Browser geöffnet"
                }
                .onFailure { notice = "${provider.name} konnte nicht geöffnet werden" }
        }
    }

    fun consumeNotice() {
        notice = null
    }

    fun postNotice(message: String) {
        notice = message
    }

    private fun launchBestMatch(query: String) {
        val match = rankedApps(query, SmartCollection.ALL).firstOrNull()
        if (match == null) {
            notice = "Keine passende App für „$query“ gefunden"
        } else {
            launch(match)
        }
    }

    private fun shareTextWithPackage(packageName: String, text: String): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage(packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(intent)
    }

    private fun openWeb(url: String): Result<Unit> = runCatching {
        appContext.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun refreshApps() {
        if (executor.isShutdown) return
        appsLoading = true
        executor.execute {
            val loaded = runCatching { appCatalog.loadApps() }
            mainHandler.post {
                loaded.onSuccess { catalog ->
                    apps = catalog.filterNot { it.packageName == appContext.packageName }
                    appsLoading = false
                }.onFailure {
                    appsLoading = false
                    notice = "App-Katalog konnte nicht geladen werden"
                }
            }
        }
    }

    private fun rememberUndoPoint() {
        undoPositions = workspacePositions.mapValues { (_, positions) -> positions.toMap() }
        canUndoLayout = true
    }
}
