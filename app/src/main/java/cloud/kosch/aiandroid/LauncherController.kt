package cloud.kosch.aiandroid

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.kosch.aiandroid.ai.AiProviderProfile
import cloud.kosch.aiandroid.ai.AiProviderRegistry
import cloud.kosch.aiandroid.ai.AiProviderKind
import cloud.kosch.aiandroid.ai.LauncherCommand
import cloud.kosch.aiandroid.ai.LocalAppClassifier
import cloud.kosch.aiandroid.ai.LocalCommandPlanner
import cloud.kosch.aiandroid.ai.LocalFileIntelligenceEngine
import cloud.kosch.aiandroid.ai.LocalSmartOrganizer
import cloud.kosch.aiandroid.ai.PhoneNumberParser
import cloud.kosch.aiandroid.ai.SearchDocument
import cloud.kosch.aiandroid.ai.SearchRanker
import cloud.kosch.aiandroid.ai.SmartAppDescriptor
import cloud.kosch.aiandroid.ai.SmartCollection
import cloud.kosch.aiandroid.data.AppCatalog
import cloud.kosch.aiandroid.data.WorkspaceStore
import cloud.kosch.aiandroid.model.ContextSnapshot
import cloud.kosch.aiandroid.model.DefaultWorkspace
import cloud.kosch.aiandroid.model.LaunchableApp
import cloud.kosch.aiandroid.model.LaunchableShortcut
import cloud.kosch.aiandroid.model.FileInsight
import cloud.kosch.aiandroid.model.HomePage
import cloud.kosch.aiandroid.model.InkStroke
import cloud.kosch.aiandroid.model.LauncherFolder
import cloud.kosch.aiandroid.model.PositionedTile
import cloud.kosch.aiandroid.model.SceneId
import cloud.kosch.aiandroid.model.SystemPanel
import cloud.kosch.aiandroid.model.StylusCapabilities
import cloud.kosch.aiandroid.model.TilePosition
import cloud.kosch.aiandroid.model.WorkspaceMode
import cloud.kosch.aiandroid.system.HomeRoleController
import cloud.kosch.aiandroid.system.LocalContextEngine
import cloud.kosch.aiandroid.system.NotificationAccess
import cloud.kosch.aiandroid.system.NotificationBadgeRepository
import cloud.kosch.aiandroid.system.SystemActionGateway
import cloud.kosch.aiandroid.system.StylusMonitor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LauncherController(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val store = WorkspaceStore(appContext)
    private val commandPlanner = LocalCommandPlanner()
    private val contextEngine = LocalContextEngine(appContext)
    private val fileIntelligence = LocalFileIntelligenceEngine(appContext.contentResolver)
    private val systemActions = SystemActionGateway(appContext)
    private val appCatalog = AppCatalog(appContext, mainHandler, ::refreshApps)
    private val stylusMonitor = StylusMonitor(appContext, mainHandler, ::onStylusChanged)

    var apps by mutableStateOf<List<LaunchableApp>>(emptyList())
        private set
    var appsLoading by mutableStateOf(true)
        private set
    var activeScene by mutableStateOf(store.loadScene())
        private set
    var homePage by mutableStateOf(store.loadHomePage())
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
    var onboardingVisible by mutableStateOf(!store.isOnboardingComplete())
        private set
    var controlCenterVisible by mutableStateOf(false)
        private set
    var phoneVisible by mutableStateOf(false)
        private set
    var fileSheetVisible by mutableStateOf(false)
        private set
    var fileLoading by mutableStateOf(false)
        private set
    var fileInsight by mutableStateOf<FileInsight?>(null)
        private set
    var widgetBoardVisible by mutableStateOf(false)
        private set
    var widgetIds by mutableStateOf(store.widgetIds())
        private set
    var appActionsVisible by mutableStateOf(false)
        private set
    var selectedApp by mutableStateOf<LaunchableApp?>(null)
        private set
    var appShortcuts by mutableStateOf<List<LaunchableShortcut>>(emptyList())
        private set
    var shortcutsLoading by mutableStateOf(false)
        private set
    var notice by mutableStateOf<String?>(null)
        private set
    var recentPackages by mutableStateOf(store.recentPackages())
        private set
    var pinnedAppKeys by mutableStateOf(store.pinnedAppKeys())
        private set
    var folders by mutableStateOf(store.folders())
        private set
    var folderPreview by mutableStateOf<List<LauncherFolder>?>(null)
        private set
    var folderSheetVisible by mutableStateOf(false)
        private set
    var selectedFolderId by mutableStateOf<String?>(null)
        private set
    var notificationCounts by mutableStateOf<Map<String, Int>>(emptyMap())
        private set
    var notificationAccessGranted by mutableStateOf(NotificationAccess.isGranted(appContext))
        private set
    var stylusState by mutableStateOf(StylusCapabilities())
        private set
    var faqVisible by mutableStateOf(false)
        private set

    private var undoPositions: Map<SceneId, Map<String, TilePosition>>? = null
    private var shortcutRequestToken = 0L
    private var started = false
    private val badgeListener = NotificationBadgeRepository.Listener { counts ->
        mainHandler.post { notificationCounts = counts }
    }

    fun start() {
        if (started) return
        started = true
        appCatalog.startListening()
        stylusMonitor.start()
        NotificationBadgeRepository.addListener(badgeListener)
        refreshApps()
        refreshSystemState()
    }

    fun close() {
        if (!started) return
        NotificationBadgeRepository.removeListener(badgeListener)
        stylusMonitor.stop()
        appCatalog.stopListening()
        executor.shutdownNow()
        started = false
    }

    fun refreshSystemState() {
        isDefaultHome = HomeRoleController.isDefaultHome(appContext)
        contextSnapshot = contextEngine.snapshot()
        notificationAccessGranted = NotificationAccess.isGranted(appContext)
        notificationCounts = if (notificationAccessGranted) {
            NotificationBadgeRepository.snapshot()
        } else {
            emptyMap()
        }
        stylusMonitor.refreshDevices()
    }

    fun switchHomePage(page: HomePage) {
        if (page == HomePage.PEN_SPACE && !stylusState.present) {
            notice = "Pen Space wird eingeblendet, sobald Android einen Zeichenstift erkennt"
            return
        }
        homePage = page
        store.saveHomePage(page)
    }

    fun openPenSpace() {
        controlCenterVisible = false
        if (stylusState.present) {
            switchHomePage(HomePage.PEN_SPACE)
        } else {
            notice = "Kein Android-kompatibler Zeichenstift erkannt"
        }
    }

    fun observeInputEvent(event: MotionEvent) {
        stylusMonitor.observe(event)
    }

    fun loadInkStrokes(): List<InkStroke> = store.inkStrokes()

    fun saveInkStrokes(strokes: List<InkStroke>) {
        store.saveInkStrokes(strokes)
    }

    fun openFaq() {
        controlCenterVisible = false
        faqVisible = true
    }

    fun closeFaq() {
        faqVisible = false
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

    fun completeOnboarding() {
        store.completeOnboarding()
        onboardingVisible = false
        notice = "KoSch ist bereit – der lokale Kern bleibt ohne API aktiv"
    }

    fun reopenOnboarding() {
        controlCenterVisible = false
        onboardingVisible = true
    }

    fun openControlCenter() {
        controlCenterVisible = true
    }

    fun closeControlCenter() {
        controlCenterVisible = false
    }

    fun openPhone() {
        phoneVisible = true
    }

    fun closePhone() {
        phoneVisible = false
    }

    fun dial(number: String?) {
        val sanitized = number?.takeIf(String::isNotBlank)?.let(PhoneNumberParser::sanitize)
        if (!number.isNullOrBlank() && sanitized == null) {
            notice = "Bitte gib eine gültige Telefonnummer ein"
            return
        }
        systemActions.openDialer(sanitized)
            .onSuccess {
                phoneVisible = false
                notice = "Nummer im System-Telefon geöffnet – du bestätigst den Anruf"
            }
            .onFailure { notice = "Auf diesem Gerät ist kein Telefon-Wähler verfügbar" }
    }

    fun openSystemPanel(panel: SystemPanel) {
        systemActions.openPanel(panel)
            .onSuccess {
                if (panel == SystemPanel.HOME_SELECTION) {
                    notice = "Hier kannst du jederzeit einen anderen Launcher wählen"
                }
            }
            .onFailure { notice = "${panel.title} konnte nicht geöffnet werden" }
    }

    fun inspectDocument(uri: Uri) {
        fileSheetVisible = true
        fileLoading = true
        fileInsight = null
        executor.execute {
            val result = runCatching { fileIntelligence.inspect(uri) }
            mainHandler.post {
                result.onSuccess { fileInsight = it }
                    .onFailure { notice = "Die Datei konnte nicht sicher gelesen werden" }
                fileLoading = false
            }
        }
    }

    fun closeFileSheet() {
        fileSheetVisible = false
    }

    fun openInspectedFile() {
        val insight = fileInsight ?: return
        systemActions.openFile(insight)
            .onFailure { notice = "Für diesen Dateityp ist keine App verfügbar" }
    }

    fun forgetDocument(released: Boolean) {
        fileInsight = null
        fileLoading = false
        fileSheetVisible = false
        notice = if (released) {
            "Gespeicherter Dateizugriff wurde gelöst"
        } else {
            "Es war kein gespeicherter Dateizugriff vorhanden"
        }
    }

    fun openWidgetBoard() {
        widgetBoardVisible = true
    }

    fun closeWidgetBoard() {
        widgetBoardVisible = false
    }

    fun acceptWidget(appWidgetId: Int) {
        store.addWidgetId(appWidgetId)
        widgetIds = store.widgetIds()
        widgetBoardVisible = true
        notice = "Widget sicher zum Board hinzugefügt"
    }

    fun removeWidgetRecord(appWidgetId: Int) {
        store.removeWidgetId(appWidgetId)
        widgetIds = store.widgetIds()
        notice = "Widget entfernt"
    }

    fun showAppActions(app: LaunchableApp) {
        val requestToken = ++shortcutRequestToken
        drawerVisible = false
        folderSheetVisible = false
        selectedApp = app
        appActionsVisible = true
        shortcutsLoading = true
        appShortcuts = emptyList()
        executor.execute {
            val result = runCatching { appCatalog.loadShortcuts(app) }
            mainHandler.post {
                if (requestToken != shortcutRequestToken || selectedApp?.key != app.key) {
                    return@post
                }
                appShortcuts = result.getOrDefault(emptyList())
                shortcutsLoading = false
            }
        }
    }

    fun hideAppActions() {
        shortcutRequestToken += 1
        appActionsVisible = false
        shortcutsLoading = false
    }

    fun isPinned(app: LaunchableApp): Boolean = app.key in pinnedAppKeys

    fun toggleSelectedAppPin() {
        val app = selectedApp ?: return
        pinnedAppKeys = if (app.key in pinnedAppKeys) {
            pinnedAppKeys.filterNot { it == app.key }
        } else {
            (pinnedAppKeys + app.key).distinct().take(MAX_PINNED_APPS)
        }
        store.savePinnedAppKeys(pinnedAppKeys)
        notice = if (app.key in pinnedAppKeys) {
            "${app.label} ist fest im Smart Dock"
        } else {
            "${app.label} wurde aus dem Smart Dock gelöst"
        }
    }

    fun smartDockApps(): List<LaunchableApp> {
        val byKey = apps.associateBy(LaunchableApp::key)
        return LocalSmartOrganizer.smartDockKeys(
            apps = appDescriptors(),
            pinnedKeys = pinnedAppKeys,
            recentPackages = recentPackages,
            scene = activeScene,
            limit = DOCK_SIZE,
        ).mapNotNull(byKey::get)
    }

    fun proposeSmartFolders() {
        folderPreview = LocalSmartOrganizer.proposeFolders(appDescriptors())
        notice = "Lokaler Ordnervorschlag – erst nach Bestätigung gespeichert"
    }

    fun applyFolderPreview() {
        val proposal = folderPreview ?: return
        folders = proposal
        store.saveFolders(folders)
        folderPreview = null
        notice = "Smart-Ordner angewendet"
    }

    fun discardFolderPreview() {
        folderPreview = null
        notice = "Ordnervorschlag verworfen"
    }

    fun addSelectedAppToSmartFolder() {
        val app = selectedApp ?: return
        val descriptor = app.toSmartDescriptor()
        val kind = LocalSmartOrganizer.bestFolderKind(descriptor)
        val existing = folders.firstOrNull { it.kind == kind }
        folders = if (existing == null) {
            folders + LauncherFolder(
                id = "local-${kind.name.lowercase()}",
                title = kind.title,
                kind = kind,
                appKeys = listOf(app.key),
            )
        } else {
            folders.map { folder ->
                if (folder.id == existing.id) {
                    folder.copy(appKeys = (folder.appKeys + app.key).distinct())
                } else {
                    folder
                }
            }
        }
        store.saveFolders(folders)
        notice = "${app.label} liegt jetzt in ${kind.title}"
    }

    fun openFolder(folderId: String) {
        if (folders.none { it.id == folderId }) return
        selectedFolderId = folderId
        folderSheetVisible = true
    }

    fun closeFolder() {
        folderSheetVisible = false
        selectedFolderId = null
    }

    fun selectedFolder(): LauncherFolder? =
        selectedFolderId?.let { id -> folders.firstOrNull { it.id == id } }

    fun folderApps(folder: LauncherFolder): List<LaunchableApp> {
        val byKey = apps.associateBy(LaunchableApp::key)
        return folder.appKeys.mapNotNull(byKey::get)
    }

    fun removeFolder(folderId: String) {
        val removed = folders.firstOrNull { it.id == folderId } ?: return
        folders = folders.filterNot { it.id == folderId }
        store.saveFolders(folders)
        if (selectedFolderId == folderId) closeFolder()
        notice = "Ordner ${removed.title} entfernt; Apps bleiben installiert"
    }

    fun openNotificationAccess() {
        openSystemPanel(SystemPanel.NOTIFICATION_ACCESS)
    }

    fun launch(shortcut: LaunchableShortcut) {
        runCatching { appCatalog.launch(shortcut) }
            .onSuccess {
                store.recordRecent(shortcut.packageName)
                recentPackages = store.recentPackages()
                hideAppActions()
                drawerVisible = false
            }
            .onFailure { notice = "${shortcut.label} konnte nicht gestartet werden" }
    }

    fun openSelectedAppInfo() {
        val app = selectedApp ?: return
        systemActions.openAppInfo(app.packageName)
            .onFailure { notice = "App-Info konnte nicht geöffnet werden" }
    }

    fun submitCommand(
        text: String,
        requestVoice: () -> Unit,
        requestDocument: () -> Unit,
    ) {
        when (val command = commandPlanner.plan(text)) {
            LauncherCommand.Empty -> Unit
            LauncherCommand.OpenDrawer -> openDrawer()
            LauncherCommand.StartVoice -> requestVoice()
            LauncherCommand.OpenFiles -> requestDocument()
            LauncherCommand.OpenControls -> openControlCenter()
            LauncherCommand.OpenWidgets -> openWidgetBoard()
            LauncherCommand.OpenFaq -> openFaq()
            LauncherCommand.OpenPenSpace -> openPenSpace()
            is LauncherCommand.OpenPhone -> if (command.number == null) openPhone() else dial(command.number)
            is LauncherCommand.OpenSystemPanel -> openSystemPanel(command.panel)
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
                hideAppActions()
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
                if (provider.kind == AiProviderKind.LOCAL_OPEN_SOURCE) {
                    "Open-Source-Projekt ${provider.name} geöffnet"
                } else {
                    "${provider.name} im Browser geöffnet"
                }
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
                    if (!store.areFoldersInitialized() && apps.isNotEmpty()) {
                        folders = LocalSmartOrganizer.proposeFolders(appDescriptors())
                        store.saveFolders(folders)
                    }
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

    private fun appDescriptors(): List<SmartAppDescriptor> = apps.map { it.toSmartDescriptor() }

    private fun LaunchableApp.toSmartDescriptor() = SmartAppDescriptor(
        key = key,
        label = label,
        packageName = packageName,
    )

    private fun onStylusChanged(capabilities: StylusCapabilities) {
        val appeared = capabilities.present && !stylusState.present
        stylusState = capabilities
        if (appeared && started) {
            notice = "Smartpen erkannt – Pen Space ist jetzt verfügbar"
        }
    }

    private companion object {
        const val MAX_PINNED_APPS = 5
        const val DOCK_SIZE = 5
    }
}
