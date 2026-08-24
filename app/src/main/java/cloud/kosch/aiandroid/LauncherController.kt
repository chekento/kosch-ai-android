package cloud.kosch.aiandroid

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
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
import cloud.kosch.aiandroid.data.LocalAuditLog
import cloud.kosch.aiandroid.data.WorkspaceStore
import cloud.kosch.aiandroid.model.AuditAction
import cloud.kosch.aiandroid.model.AuditEvent
import cloud.kosch.aiandroid.model.AuditOutcome
import cloud.kosch.aiandroid.model.BackupPreview
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
import cloud.kosch.aiandroid.model.SelectedContact
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
import cloud.kosch.aiandroid.security.PortableBackupCodec
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LauncherController(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val store = WorkspaceStore(appContext)
    private val auditLog = LocalAuditLog(appContext)
    private val backupCodec = PortableBackupCodec()
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
    var backupVisible by mutableStateOf(false)
        private set
    var backupBusy by mutableStateOf(false)
        private set
    var backupFileStaged by mutableStateOf(false)
        private set
    var backupPreview by mutableStateOf<BackupPreview?>(null)
        private set
    var auditVisible by mutableStateOf(false)
        private set
    var auditEvents by mutableStateOf(auditLog.events())
        private set
    var selectedContact by mutableStateOf<SelectedContact?>(null)
        private set
    var commandFocusRequest by mutableStateOf(0L)
        private set

    private var undoPositions: Map<SceneId, Map<String, TilePosition>>? = null
    private var stagedBackupEnvelope: String? = null
    private var stagedWorkspacePayload: ByteArray? = null
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
        stagedWorkspacePayload?.fill(0)
        stagedWorkspacePayload = null
        stagedBackupEnvelope = null
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

    fun openProDesk() {
        closeTopSurface()
        switchHomePage(HomePage.PRO_DESK)
    }

    fun requestCommandFocus() {
        closeTopSurface()
        commandFocusRequest += 1
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
        audit(AuditAction.PEN_SAVE, AuditOutcome.SUCCESS)
    }

    fun openFaq() {
        controlCenterVisible = false
        faqVisible = true
    }

    fun closeFaq() {
        faqVisible = false
    }

    fun openBackup() {
        controlCenterVisible = false
        auditVisible = false
        backupVisible = true
    }

    fun closeBackup() {
        backupVisible = false
        backupBusy = false
        backupPreview = null
        backupFileStaged = false
        stagedBackupEnvelope = null
        stagedWorkspacePayload?.fill(0)
        stagedWorkspacePayload = null
    }

    fun buildEncryptedBackup(
        passphrase: CharArray,
        onReady: (Result<ByteArray>) -> Unit,
    ) {
        if (backupBusy) {
            passphrase.fill('\u0000')
            return
        }
        backupBusy = true
        executor.execute {
            val result = runCatching {
                val snapshot = store.createPortableSnapshot()
                try {
                    backupCodec.encrypt(snapshot, passphrase).toByteArray(StandardCharsets.UTF_8)
                } finally {
                    snapshot.fill(0)
                }
            }
            passphrase.fill('\u0000')
            mainHandler.post {
                backupBusy = false
                result.onFailure { notice = it.message ?: "Backup konnte nicht vorbereitet werden" }
                onReady(result)
            }
        }
    }

    fun stageEncryptedBackup(uri: Uri) {
        if (backupBusy) return
        backupBusy = true
        backupPreview = null
        stagedWorkspacePayload?.fill(0)
        stagedWorkspacePayload = null
        executor.execute {
            val result = runCatching {
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    readBounded(input, MAX_BACKUP_ENVELOPE_BYTES)
                } ?: error("Backup-Datei konnte nicht geöffnet werden")
            }.map { bytes ->
                try {
                    bytes.toString(StandardCharsets.UTF_8).trim()
                } finally {
                    bytes.fill(0)
                }
            }
            mainHandler.post {
                backupBusy = false
                result.onSuccess { envelope ->
                    stagedBackupEnvelope = envelope
                    backupFileStaged = true
                    notice = "Backup geladen – Passphrase eingeben und zuerst prüfen"
                }.onFailure {
                    backupFileStaged = false
                    stagedBackupEnvelope = null
                    notice = it.message ?: "Backup-Datei konnte nicht sicher gelesen werden"
                }
            }
        }
    }

    fun previewStagedBackup(passphrase: CharArray) {
        val envelope = stagedBackupEnvelope
        if (envelope == null || backupBusy) {
            passphrase.fill('\u0000')
            return
        }
        backupBusy = true
        executor.execute {
            val result = runCatching {
                val payload = backupCodec.decrypt(envelope, passphrase)
                payload to store.previewPortableSnapshot(payload)
            }
            passphrase.fill('\u0000')
            mainHandler.post {
                backupBusy = false
                result.onSuccess { (payload, preview) ->
                    stagedWorkspacePayload?.fill(0)
                    stagedWorkspacePayload = payload
                    backupPreview = preview
                    notice = "Backup geprüft – Restore wartet auf deine Bestätigung"
                }.onFailure {
                    backupPreview = null
                    notice = it.message ?: "Backup konnte nicht geprüft werden"
                }
            }
        }
    }

    fun applyBackupPreview() {
        val payload = stagedWorkspacePayload ?: return
        if (backupBusy) return
        backupBusy = true
        executor.execute {
            val result = runCatching { store.restorePortableSnapshot(payload) }
            payload.fill(0)
            mainHandler.post {
                stagedWorkspacePayload = null
                backupBusy = false
                result.onSuccess {
                    reloadWorkspaceState()
                    backupPreview = null
                    backupFileStaged = false
                    stagedBackupEnvelope = null
                    audit(AuditAction.BACKUP_IMPORT, AuditOutcome.SUCCESS)
                    notice = "Workspace wiederhergestellt; Widgets und Freigaben bleiben gerätegebunden"
                }.onFailure {
                    audit(AuditAction.BACKUP_IMPORT, AuditOutcome.FAILED)
                    notice = "Restore wurde nicht angewendet: ${it.message ?: "Validierung fehlgeschlagen"}"
                }
            }
        }
    }

    fun recordBackupExport(success: Boolean) {
        audit(AuditAction.BACKUP_EXPORT, if (success) AuditOutcome.SUCCESS else AuditOutcome.FAILED)
        notice = if (success) "Verschlüsseltes Backup gespeichert" else "Backup wurde nicht gespeichert"
    }

    fun openAudit() {
        controlCenterVisible = false
        backupVisible = false
        auditEvents = auditLog.events()
        auditVisible = true
    }

    fun closeAudit() {
        auditVisible = false
    }

    fun clearAudit() {
        auditLog.clear()
        auditEvents = emptyList()
        notice = "Lokales Audit vollständig gelöscht"
    }

    fun auditCsv(): ByteArray = auditLog.exportCsv()

    fun recordAuditExport(success: Boolean) {
        audit(AuditAction.AUDIT_EXPORT, if (success) AuditOutcome.SUCCESS else AuditOutcome.FAILED)
        notice = if (success) "Audit als CSV gespeichert" else "Audit wurde nicht gespeichert"
    }

    fun writeUserDocument(
        uri: Uri,
        payload: ByteArray,
        onComplete: (Boolean) -> Unit,
    ) {
        executor.execute {
            val success = runCatching {
                appContext.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                    output.write(payload)
                    output.flush()
                } ?: error("Zieldatei konnte nicht geöffnet werden")
            }.isSuccess
            payload.fill(0)
            mainHandler.post { onComplete(success) }
        }
    }

    /** Closes exactly the top-most transient surface and never changes Android's HOME role. */
    fun closeTopSurface(): Boolean = when {
        onboardingVisible -> false
        faqVisible -> true.also { closeFaq() }
        backupVisible -> true.also { closeBackup() }
        auditVisible -> true.also { closeAudit() }
        appActionsVisible -> true.also { hideAppActions() }
        folderSheetVisible -> true.also { closeFolder() }
        phoneVisible -> true.also { closePhone() }
        fileSheetVisible -> true.also { closeFileSheet() }
        widgetBoardVisible -> true.also { closeWidgetBoard() }
        controlCenterVisible -> true.also { closeControlCenter() }
        providerChooserVisible -> true.also { closeProviderChooser() }
        contextDetailsVisible -> true.also { hideContextDetails() }
        drawerVisible -> true.also { closeDrawer() }
        else -> false
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
        audit(AuditAction.SCENE_SWITCH, AuditOutcome.SUCCESS)
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
        audit(AuditAction.LAYOUT_CHANGE, AuditOutcome.SUCCESS)
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
        audit(AuditAction.LAYOUT_CHANGE, AuditOutcome.SUCCESS)
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
        audit(AuditAction.LAYOUT_CHANGE, AuditOutcome.SUCCESS)
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
        audit(AuditAction.LAYOUT_CHANGE, AuditOutcome.SUCCESS)
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
        selectedContact = null
    }

    fun selectContact(contact: SelectedContact) {
        selectedContact = contact
        phoneVisible = true
        audit(AuditAction.CONTACT_PICKER, AuditOutcome.SUCCESS)
    }

    fun consumePickedContact(uri: Uri) {
        executor.execute {
            val result = runCatching {
                val projection = arrayOf(
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.Data.DATA1,
                )
                appContext.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                    val mimeIndex = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
                    val dataIndex = cursor.getColumnIndex(ContactsContract.Data.DATA1)
                    var selected: SelectedContact? = null
                    while (cursor.moveToNext() && selected == null) {
                        val mime = mimeIndex.takeIf { it >= 0 }?.let(cursor::getString)
                        val value = dataIndex.takeIf { it >= 0 }?.let(cursor::getString).orEmpty()
                        if ((mime == null || mime == ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE) &&
                            PhoneNumberParser.sanitize(value) != null
                        ) {
                            selected = SelectedContact(
                                displayName = nameIndex.takeIf { it >= 0 }?.let(cursor::getString)
                                    ?.trim().orEmpty().ifBlank { "Ausgewählter Kontakt" },
                                phoneNumber = value,
                            )
                        }
                    }
                    selected
                } ?: error("Kontakt konnte nicht gelesen werden")
            }
            mainHandler.post {
                result.onSuccess { contact ->
                    if (contact == null) {
                        audit(AuditAction.CONTACT_PICKER, AuditOutcome.REJECTED)
                        notice = "Der gewählte Kontakt enthält keine nutzbare Telefonnummer"
                    } else {
                        selectContact(contact)
                    }
                }.onFailure {
                    audit(AuditAction.CONTACT_PICKER, AuditOutcome.FAILED)
                    notice = "Kontakt konnte nicht sicher übernommen werden"
                }
            }
        }
    }

    fun clearSelectedContact() {
        selectedContact = null
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
                selectedContact = null
                notice = "Nummer im System-Telefon geöffnet – du bestätigst den Anruf"
                audit(AuditAction.DIALER, AuditOutcome.SUCCESS)
            }
            .onFailure {
                notice = "Auf diesem Gerät ist kein Telefon-Wähler verfügbar"
                audit(AuditAction.DIALER, AuditOutcome.FAILED)
            }
    }

    fun openSystemPanel(panel: SystemPanel) {
        systemActions.openPanel(panel)
            .onSuccess {
                audit(AuditAction.SYSTEM_PANEL, AuditOutcome.SUCCESS)
                if (panel == SystemPanel.HOME_SELECTION) {
                    notice = "Hier kannst du jederzeit einen anderen Launcher wählen"
                }
            }
            .onFailure {
                notice = "${panel.title} konnte nicht geöffnet werden"
                audit(AuditAction.SYSTEM_PANEL, AuditOutcome.FAILED)
            }
    }

    fun inspectDocument(uri: Uri) {
        fileSheetVisible = true
        fileLoading = true
        fileInsight = null
        executor.execute {
            val result = runCatching { fileIntelligence.inspect(uri) }
            mainHandler.post {
                result.onSuccess { fileInsight = it }
                    .onSuccess { audit(AuditAction.DOCUMENT_INSPECT, AuditOutcome.SUCCESS) }
                    .onFailure {
                        notice = "Die Datei konnte nicht sicher gelesen werden"
                        audit(AuditAction.DOCUMENT_INSPECT, AuditOutcome.FAILED)
                    }
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
        audit(AuditAction.WIDGET_CHANGE, AuditOutcome.SUCCESS)
    }

    fun removeWidgetRecord(appWidgetId: Int) {
        store.removeWidgetId(appWidgetId)
        widgetIds = store.widgetIds()
        notice = "Widget entfernt"
        audit(AuditAction.WIDGET_CHANGE, AuditOutcome.SUCCESS)
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
                audit(AuditAction.APP_SHORTCUT, AuditOutcome.SUCCESS)
            }
            .onFailure {
                notice = "${shortcut.label} konnte nicht gestartet werden"
                audit(AuditAction.APP_SHORTCUT, AuditOutcome.FAILED)
            }
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
        val command = commandPlanner.plan(text)
        if (command !is LauncherCommand.Empty) audit(AuditAction.COMMAND, AuditOutcome.SUCCESS)
        when (command) {
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
                audit(AuditAction.APP_LAUNCH, AuditOutcome.SUCCESS)
            }
            .onFailure {
                notice = "${app.label} konnte nicht gestartet werden"
                audit(AuditAction.APP_LAUNCH, AuditOutcome.FAILED)
            }
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

    private fun reloadWorkspaceState() {
        activeScene = store.loadScene()
        homePage = store.loadHomePage()
        workspacePositions = store.loadPositions()
        recentPackages = store.recentPackages()
        pinnedAppKeys = store.pinnedAppKeys()
        folders = store.folders()
        folderPreview = null
        previewPositions = null
        undoPositions = null
        canUndoLayout = false
    }

    private fun audit(action: AuditAction, outcome: AuditOutcome) {
        auditLog.append(action, outcome)
        if (auditVisible) auditEvents = auditLog.events()
    }

    private fun readBounded(input: InputStream, limit: Int): ByteArray {
        val output = ByteArrayOutputStream(minOf(limit, 64 * 1024))
        val buffer = ByteArray(16 * 1024)
        var total = 0
        try {
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                require(total <= limit) { "Backup-Datei ist zu groß" }
                output.write(buffer, 0, read)
            }
            return output.toByteArray()
        } finally {
            buffer.fill(0)
        }
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
        const val MAX_BACKUP_ENVELOPE_BYTES = 8 * 1024 * 1024
    }
}
