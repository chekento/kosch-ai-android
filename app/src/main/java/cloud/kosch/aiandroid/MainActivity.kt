package cloud.kosch.aiandroid

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.speech.RecognizerIntent
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.KeyboardShortcutInfo
import android.view.Menu
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.data.PendingDocumentKind
import cloud.kosch.aiandroid.data.PendingDocumentStore
import cloud.kosch.aiandroid.model.AssistantAnchor
import cloud.kosch.aiandroid.model.GestureAction
import cloud.kosch.aiandroid.model.GestureTrigger
import cloud.kosch.aiandroid.model.HapticProfile
import cloud.kosch.aiandroid.model.HomePage
import cloud.kosch.aiandroid.model.LauncherPresentationPlanner
import cloud.kosch.aiandroid.model.SettingsSection
import cloud.kosch.aiandroid.model.SystemPanel
import cloud.kosch.aiandroid.model.WorkspaceMode
import cloud.kosch.aiandroid.system.DocumentGrantManager
import cloud.kosch.aiandroid.system.HomeRoleController
import cloud.kosch.aiandroid.system.LauncherGestureBindingResolver
import cloud.kosch.aiandroid.system.ProfessionalShortcut
import cloud.kosch.aiandroid.system.ProfessionalShortcutResolver
import cloud.kosch.aiandroid.system.WidgetHostController
import cloud.kosch.aiandroid.ui.AiHubEntryButton
import cloud.kosch.aiandroid.ui.AiHubSurface
import cloud.kosch.aiandroid.ui.DragDropWorkspaceHomeScreen
import cloud.kosch.aiandroid.ui.LauncherRoot
import cloud.kosch.aiandroid.ui.PersonalizationEntryButton
import cloud.kosch.aiandroid.ui.PersonalizationQuickSurface
import cloud.kosch.aiandroid.ui.SettingsCenterSurface
import cloud.kosch.aiandroid.ui.SettingsEntryButton
import cloud.kosch.aiandroid.ui.UniversalSearchEntryButton
import cloud.kosch.aiandroid.ui.UniversalSearchSurface
import cloud.kosch.aiandroid.ui.components.CompanionFace
import cloud.kosch.aiandroid.ui.launcherGestureSurface
import cloud.kosch.aiandroid.ui.theme.KoSchLauncherTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val launcherViewModel: LauncherViewModel by viewModels()
    private val controller: LauncherController get() = launcherViewModel.controller
    private lateinit var widgetHostController: WidgetHostController
    private lateinit var documentGrantManager: DocumentGrantManager
    private lateinit var pendingDocumentStore: PendingDocumentStore
    private lateinit var universalSearchDispatcher: UniversalSearchRuntimeDispatcher
    private var pendingWidgetId: Int? = null
    private var pendingBackupExportToken: String? = null
    private var pendingAuditExportToken: String? = null
    private var pendingInkExportToken: String? = null
    private var personalizationVisible by mutableStateOf(false)

    private val homeRoleRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        controller.refreshSystemState()
    }

    private val voiceRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (spoken.isNullOrBlank()) {
            controller.postNotice("Keine Spracheingabe übernommen")
        } else {
            controller.submitCommand(spoken, ::requestVoiceInput, ::requestDocument, ::requestContact)
        }
    }

    private val documentRequest = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            documentGrantManager.adopt(uri)
                .onFailure {
                    controller.postNotice("Datei wird geprüft; dauerhafter Lesezugriff wurde nicht gespeichert")
                }
            controller.inspectDocument(uri)
        }
    }

    private val workspaceTreeRequest = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri == null) {
            controller.postNotice("Kein Arbeitsordner ausgewählt")
        } else {
            controller.adoptFileWorkspace(uri)
        }
    }

    private val contactRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            controller.consumePickedContact(uri)
        } else {
            controller.postNotice("Keine Kontaktdaten übernommen")
        }
    }

    private val backupCreateRequest = registerForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE),
    ) { uri ->
        val token = pendingBackupExportToken
        pendingBackupExportToken = null
        val payload = token?.let { pendingDocumentStore.consume(PendingDocumentKind.BACKUP, it) }
        if (uri == null || payload == null) {
            payload?.fill(0)
            controller.recordBackupExport(false)
        } else {
            controller.writeUserDocument(uri, payload, controller::recordBackupExport)
        }
    }

    private val backupOpenRequest = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            controller.postNotice("Kein Backup ausgewählt")
        } else {
            controller.stageEncryptedBackup(uri)
        }
    }

    private val auditCreateRequest = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        val token = pendingAuditExportToken
        pendingAuditExportToken = null
        val payload = token?.let { pendingDocumentStore.consume(PendingDocumentKind.AUDIT, it) }
        if (uri == null || payload == null) {
            payload?.fill(0)
            controller.recordAuditExport(false)
        } else {
            controller.writeUserDocument(uri, payload, controller::recordAuditExport)
        }
    }

    private val inkCreateRequest = registerForActivityResult(
        ActivityResultContracts.CreateDocument("image/svg+xml"),
    ) { uri ->
        val token = pendingInkExportToken
        pendingInkExportToken = null
        val payload = token?.let { pendingDocumentStore.consume(PendingDocumentKind.INK_SVG, it) }
        if (uri == null || payload == null) {
            payload?.fill(0)
            controller.recordInkExport(false)
        } else {
            controller.writeUserDocument(uri, payload, controller::recordInkExport)
        }
    }

    private val widgetPickRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val appWidgetId = result.data?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )?.takeUnless { it == AppWidgetManager.INVALID_APPWIDGET_ID } ?: pendingWidgetId
        if (result.resultCode != Activity.RESULT_OK || appWidgetId == null) {
            abandonPendingWidget()
            return@registerForActivityResult
        }
        pendingWidgetId = appWidgetId
        val configurationIntent = widgetHostController.configurationIntent(appWidgetId)
        if (configurationIntent == null) {
            finishWidgetSetup(appWidgetId)
        } else {
            runCatching { widgetConfigureRequest.launch(configurationIntent) }
                .onFailure {
                    abandonPendingWidget()
                    controller.postNotice("Widget-Konfiguration konnte nicht geöffnet werden")
                }
        }
    }

    private val widgetConfigureRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val appWidgetId = pendingWidgetId
        if (result.resultCode == Activity.RESULT_OK && appWidgetId != null) {
            finishWidgetSetup(appWidgetId)
        } else {
            abandonPendingWidget()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingWidgetId = savedInstanceState
            ?.getInt(STATE_PENDING_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?.takeUnless { it == AppWidgetManager.INVALID_APPWIDGET_ID }
        widgetHostController = WidgetHostController(applicationContext)
        documentGrantManager = DocumentGrantManager(applicationContext)
        pendingDocumentStore = PendingDocumentStore(applicationContext)
        universalSearchDispatcher = UniversalSearchRuntimeDispatcher(
            context = applicationContext,
            viewModel = launcherViewModel,
            requestVoiceInput = ::requestVoiceInput,
            requestDocument = ::requestDocument,
        )
        pendingBackupExportToken = savedInstanceState?.getString(STATE_PENDING_BACKUP_EXPORT)
            ?.takeIf { pendingDocumentStore.contains(PendingDocumentKind.BACKUP, it) }
        pendingAuditExportToken = savedInstanceState?.getString(STATE_PENDING_AUDIT_EXPORT)
            ?.takeIf { pendingDocumentStore.contains(PendingDocumentKind.AUDIT, it) }
        pendingInkExportToken = savedInstanceState?.getString(STATE_PENDING_INK_EXPORT)
            ?.takeIf { pendingDocumentStore.contains(PendingDocumentKind.INK_SVG, it) }
        controller.widgetIds.toList()
            .filterNot(widgetHostController::isValid)
            .forEach(controller::removeWidgetRecord)

        setContent {
            val settings = launcherViewModel.settings
            val aiHub = launcherViewModel.aiHub
            val universalSearch = launcherViewModel.universalSearch
            val assistantPresentation = settings.document.assistant
            val gestureSurfaceEnabled = !controller.onboardingVisible &&
                !settings.visible &&
                !personalizationVisible &&
                !aiHub.visible &&
                !universalSearch.visible &&
                !controller.drawerVisible &&
                !controller.providerChooserVisible &&
                !controller.contextDetailsVisible &&
                !controller.controlCenterVisible &&
                !controller.phoneVisible &&
                !controller.fileSheetVisible &&
                !controller.fileWorkspaceVisible &&
                !controller.widgetBoardVisible &&
                !controller.appActionsVisible &&
                !controller.folderSheetVisible &&
                !controller.faqVisible &&
                !controller.backupVisible &&
                !controller.auditVisible

            KoSchLauncherTheme(
                dynamicColor = settings.document.appearance.useMaterialYouAccents,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .launcherGestureSurface(
                            settings = settings.document.gestures,
                            enabled = gestureSurfaceEnabled,
                            onTrigger = ::handleLauncherGesture,
                        ),
                ) {
                    val unifiedHomeSelected = controller.homePage == HomePage.WORKSPACE && !controller.onboardingVisible
                    val legacyOverlayVisible = controller.drawerVisible ||
                        controller.providerChooserVisible ||
                        controller.contextDetailsVisible ||
                        controller.controlCenterVisible ||
                        controller.phoneVisible ||
                        controller.fileSheetVisible ||
                        controller.fileWorkspaceVisible ||
                        controller.widgetBoardVisible ||
                        controller.appActionsVisible ||
                        controller.folderSheetVisible ||
                        controller.faqVisible ||
                        controller.backupVisible ||
                        controller.auditVisible
                    val unifiedHomeVisible = unifiedHomeSelected && !legacyOverlayVisible

                    if (unifiedHomeVisible) {
                        DragDropWorkspaceHomeScreen(
                            controller = controller,
                            home = launcherViewModel.homeWorkspace,
                            settings = settings,
                            scopedSettings = launcherViewModel.scopedSettings,
                            requestVoiceInput = ::requestVoiceInput,
                            requestDocument = ::requestDocument,
                            requestContact = ::requestContact,
                        )
                    } else {
                        LauncherRoot(
                            controller = controller,
                            requestHomeRole = ::requestHomeRole,
                            requestVoiceInput = ::requestVoiceInput,
                            requestDocument = ::requestDocument,
                            requestFileWorkspace = ::requestFileWorkspace,
                            requestContact = ::requestContact,
                            requestWidget = ::requestWidget,
                            requestBackupExport = ::requestBackupExport,
                            requestBackupImport = ::requestBackupImport,
                            requestAuditExport = ::requestAuditExport,
                            requestInkExport = ::requestInkExport,
                            createWidgetView = widgetHostController::createView,
                            deleteWidget = ::deleteWidget,
                            forgetDocument = ::forgetDocument,
                        )
                    }

                    if (
                        unifiedHomeVisible &&
                        !aiHub.visible &&
                        !settings.visible &&
                        !personalizationVisible &&
                        !universalSearch.visible
                    ) {
                        SettingsEntryButton(
                            onClick = {
                                personalizationVisible = false
                                universalSearch.close()
                                aiHub.close()
                                settings.open()
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 18.dp, top = 76.dp),
                        )
                        PersonalizationEntryButton(
                            onClick = {
                                universalSearch.close()
                                settings.close()
                                aiHub.close()
                                personalizationVisible = true
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 18.dp, top = 122.dp),
                        )
                        UniversalSearchEntryButton(
                            onClick = {
                                personalizationVisible = false
                                launcherViewModel.openUniversalSearch()
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 18.dp, top = 168.dp),
                        )
                        AiHubEntryButton(
                            onClick = {
                                personalizationVisible = false
                                universalSearch.close()
                                settings.close()
                                aiHub.open()
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 18.dp, top = 214.dp),
                        )
                    }

                    // The portable document controls presentation only. Runtime enable/disable remains authoritative
                    // in AssistantSessionController, so a disabled Assistant still exposes its explicit setup entry.
                    if (unifiedHomeVisible) {
                        val assistantAlignment = when (assistantPresentation.anchor) {
                            AssistantAnchor.LEFT -> Alignment.BottomStart
                            AssistantAnchor.CENTER -> Alignment.BottomCenter
                            AssistantAnchor.RIGHT, AssistantAnchor.FREE -> Alignment.BottomEnd
                        }
                        val scale = assistantPresentation.scale
                        CompanionFace(
                            onClick = ::requestVoiceInput,
                            modifier = Modifier
                                .align(assistantAlignment)
                                .padding(horizontal = 18.dp, vertical = 150.dp)
                                .size(width = (76f * scale).dp, height = (68f * scale).dp)
                                .alpha(assistantPresentation.opacity),
                        )
                    }

                    if (settings.visible) {
                        SettingsCenterSurface(
                            settings = settings,
                            home = launcherViewModel.homeWorkspace,
                            assistant = launcherViewModel.assistant,
                            onDismiss = settings::close,
                        )
                    }
                    if (personalizationVisible) {
                        PersonalizationQuickSurface(
                            settings = settings,
                            onDismiss = { personalizationVisible = false },
                        )
                    }
                    if (aiHub.visible) {
                        AiHubSurface(
                            hub = aiHub,
                            apps = controller.apps,
                        )
                    }
                    if (universalSearch.visible) {
                        UniversalSearchSurface(
                            search = universalSearch,
                            onExecute = universalSearchDispatcher::execute,
                            onDismiss = universalSearch::close,
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        widgetHostController.startListening()
    }

    override fun onResume() {
        super.onResume()
        controller.refreshSystemState()
        launcherViewModel.universalSearch.refresh()
    }

    override fun onStop() {
        widgetHostController.stopListening()
        super.onStop()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        controller.observeInputEvent(event)
        return super.dispatchTouchEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        controller.observeInputEvent(event)
        return super.dispatchGenericMotionEvent(event)
    }

    override fun onKeyShortcut(keyCode: Int, event: KeyEvent): Boolean {
        if (controller.onboardingVisible) {
            return super.onKeyShortcut(keyCode, event)
        }
        val shortcut = ProfessionalShortcutResolver.resolve(
            keyCode = keyCode,
            isCtrlPressed = event.isCtrlPressed,
            isMetaPressed = event.isMetaPressed,
            isShiftPressed = event.isShiftPressed,
        ) ?: return super.onKeyShortcut(keyCode, event)
        controller.closeTopSurface()
        when (shortcut) {
            ProfessionalShortcut.COMMAND -> launcherViewModel.openUniversalSearch()
            ProfessionalShortcut.APPS -> controller.openDrawer()
            ProfessionalShortcut.PRO_DESK -> controller.openProDesk()
            ProfessionalShortcut.CONTROL_CENTER -> controller.openControlCenter()
            ProfessionalShortcut.PHONE -> controller.openPhone()
            ProfessionalShortcut.FILES -> requestDocument()
            ProfessionalShortcut.FILE_WORKSPACE -> controller.openFileWorkspace()
            ProfessionalShortcut.BACKUP -> controller.openBackup()
            ProfessionalShortcut.AUDIT -> controller.openAudit()
            ProfessionalShortcut.PEN_SPACE -> controller.openPenSpace()
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ESCAPE && launcherViewModel.universalSearch.visible) {
            launcherViewModel.universalSearch.close()
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_ESCAPE && personalizationVisible) {
            personalizationVisible = false
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_ESCAPE && launcherViewModel.aiHub.visible) {
            launcherViewModel.aiHub.close()
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_ESCAPE && launcherViewModel.settings.visible) {
            launcherViewModel.settings.close()
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_ESCAPE && controller.closeTopSurface()) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onProvideKeyboardShortcuts(
        data: MutableList<KeyboardShortcutGroup>,
        menu: Menu?,
        deviceId: Int,
    ) {
        data += KeyboardShortcutGroup(
            "KoSch Professional",
            listOf(
                KeyboardShortcutInfo("Universal Search", KeyEvent.KEYCODE_K, KeyEvent.META_CTRL_ON),
                KeyboardShortcutInfo("Apps", KeyEvent.KEYCODE_SPACE, KeyEvent.META_CTRL_ON),
                KeyboardShortcutInfo("Pro Desk", KeyEvent.KEYCODE_H, KeyEvent.META_CTRL_ON),
                KeyboardShortcutInfo("Kontrollzentrum", KeyEvent.KEYCODE_COMMA, KeyEvent.META_CTRL_ON),
                KeyboardShortcutInfo("Telefon", KeyEvent.KEYCODE_D, KeyEvent.META_CTRL_ON),
                KeyboardShortcutInfo("Datei-KI", KeyEvent.KEYCODE_O, KeyEvent.META_CTRL_ON),
                KeyboardShortcutInfo(
                    "Datei-Arbeitsraum",
                    KeyEvent.KEYCODE_O,
                    KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON,
                ),
                KeyboardShortcutInfo("Backup", KeyEvent.KEYCODE_B, KeyEvent.META_CTRL_ON),
                KeyboardShortcutInfo("Audit", KeyEvent.KEYCODE_L, KeyEvent.META_CTRL_ON),
                KeyboardShortcutInfo(
                    "Pen Space",
                    KeyEvent.KEYCODE_P,
                    KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON,
                ),
            ),
        )
        super.onProvideKeyboardShortcuts(data, menu, deviceId)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pendingWidgetId?.let { outState.putInt(STATE_PENDING_WIDGET_ID, it) }
        pendingBackupExportToken?.let { outState.putString(STATE_PENDING_BACKUP_EXPORT, it) }
        pendingAuditExportToken?.let { outState.putString(STATE_PENDING_AUDIT_EXPORT, it) }
        pendingInkExportToken?.let { outState.putString(STATE_PENDING_INK_EXPORT, it) }
        super.onSaveInstanceState(outState)
    }

    private fun handleLauncherGesture(trigger: GestureTrigger) {
        val gestureSettings = launcherViewModel.settings.document.gestures
        val action = LauncherGestureBindingResolver.actionFor(gestureSettings, trigger)
        if (action == GestureAction.NONE) return
        if (gestureSettings.haptics != HapticProfile.OFF) {
            window.decorView.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }

        when (action) {
            GestureAction.NONE -> Unit
            GestureAction.OPEN_DRAWER -> controller.openDrawer()
            GestureAction.OPEN_SEARCH,
            GestureAction.OPEN_COMMAND_PALETTE -> launcherViewModel.openUniversalSearch()
            GestureAction.OPEN_HOME_STUDIO -> {
                controller.switchHomePage(HomePage.WORKSPACE)
                controller.selectWorkspaceMode(WorkspaceMode.EDIT)
                controller.postNotice("Home-Bearbeitung aktiviert · Drag/Resize über Home Studio")
            }
            GestureAction.OPEN_SETTINGS -> launcherViewModel.settings.open()
            GestureAction.OPEN_ASSISTANT -> {
                if (launcherViewModel.assistant.settings.enabled) {
                    requestVoiceInput()
                } else {
                    launcherViewModel.settings.open(SettingsSection.ASSISTANT)
                }
            }
            GestureAction.OPEN_NOTIFICATIONS -> controller.openSystemPanel(SystemPanel.NOTIFICATIONS)
            GestureAction.PREVIOUS_PAGE -> moveGesturePage(-1)
            GestureAction.NEXT_PAGE -> moveGesturePage(1)
            GestureAction.SYSTEM_QUICK_SETTINGS -> controller.openControlCenter()
            GestureAction.LOCK_DEVICE_ROUTE -> controller.postNotice(
                "Gerätesperre bleibt ohne ausdrücklich eingerichtete Android-Systemrolle blockiert",
            )
            GestureAction.CUSTOM_SHORTCUT -> {
                val target = LauncherGestureBindingResolver.customTargetFor(gestureSettings, trigger)
                controller.postNotice(
                    if (target == null) {
                        "Für diese Geste ist kein gültiges eigenes Ziel hinterlegt"
                    } else {
                        "Eigene Gesten-Ziele werden erst nach sicherer Action-Auflösung ausgeführt"
                    },
                )
            }
        }
    }

    private fun moveGesturePage(direction: Int) {
        val home = launcherViewModel.homeWorkspace
        val pages = home.document.pages
        val currentIndex = pages.indexOfFirst { it.id == home.document.activePageId }
        val nextIndex = LauncherPresentationPlanner.adjacentPageIndex(
            settings = launcherViewModel.settings.document.pages,
            currentIndex = currentIndex,
            pageCount = pages.size,
            direction = direction,
        )
        if (nextIndex == currentIndex || nextIndex !in pages.indices) {
            controller.postNotice("Keine weitere Home-Seite in dieser Richtung")
            return
        }
        controller.switchHomePage(HomePage.WORKSPACE)
        home.activatePage(pages[nextIndex].id)
    }

    private fun requestHomeRole() {
        val intent = HomeRoleController.requestIntent(this)
        if (intent == null) {
            controller.refreshSystemState()
        } else {
            homeRoleRequest.launch(intent)
        }
    }

    private fun requestVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Was möchtest du tun?")
        }
        runCatching { voiceRequest.launch(intent) }
            .onFailure { controller.postNotice("Auf diesem Gerät ist keine Spracheingabe verfügbar") }
    }

    private fun requestDocument() {
        runCatching { documentRequest.launch(arrayOf("*/*")) }
            .onFailure { controller.postNotice("Die Android-Dateiauswahl ist nicht verfügbar") }
    }

    private fun requestFileWorkspace() {
        runCatching { workspaceTreeRequest.launch(null) }
            .onFailure { controller.postNotice("Die Android-Ordnerauswahl ist nicht verfügbar") }
    }

    private fun requestContact() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI).apply {
            putExtra(EXTRA_USE_SYSTEM_CONTACTS_PICKER, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { contactRequest.launch(intent) }
            .onFailure { controller.postNotice("Die Android-Kontaktauswahl ist nicht verfügbar") }
    }

    private fun requestBackupExport(passphrase: String) {
        controller.buildEncryptedBackup(passphrase.toCharArray()) { result ->
            result.onSuccess { payload ->
                pendingBackupExportToken?.let { pendingDocumentStore.discard(PendingDocumentKind.BACKUP, it) }
                pendingDocumentStore.stage(PendingDocumentKind.BACKUP, payload)
                    .onSuccess { token ->
                        pendingBackupExportToken = token
                        runCatching {
                            backupCreateRequest.launch("kosch-workspace-${LocalDate.now()}.koschbackup")
                        }.onFailure {
                            pendingDocumentStore.discard(PendingDocumentKind.BACKUP, pendingBackupExportToken)
                            pendingBackupExportToken = null
                            controller.recordBackupExport(false)
                        }
                    }
                    .onFailure { controller.recordBackupExport(false) }
            }
        }
    }

    private fun requestBackupImport() {
        runCatching { backupOpenRequest.launch(arrayOf(BACKUP_MIME_TYPE, "application/octet-stream", "text/plain")) }
            .onFailure { controller.postNotice("Die Android-Dateiauswahl ist nicht verfügbar") }
    }

    private fun requestAuditExport() {
        pendingAuditExportToken?.let { pendingDocumentStore.discard(PendingDocumentKind.AUDIT, it) }
        pendingDocumentStore.stage(PendingDocumentKind.AUDIT, controller.auditCsv())
            .onSuccess { token ->
                pendingAuditExportToken = token
                runCatching { auditCreateRequest.launch("kosch-audit-${LocalDate.now()}.csv") }
                    .onFailure {
                        pendingDocumentStore.discard(PendingDocumentKind.AUDIT, pendingAuditExportToken)
                        pendingAuditExportToken = null
                        controller.recordAuditExport(false)
                    }
            }
            .onFailure { controller.recordAuditExport(false) }
    }

    private fun requestInkExport() {
        pendingInkExportToken?.let { pendingDocumentStore.discard(PendingDocumentKind.INK_SVG, it) }
        pendingDocumentStore.stage(PendingDocumentKind.INK_SVG, controller.inkSvg())
            .onSuccess { token ->
                pendingInkExportToken = token
                runCatching { inkCreateRequest.launch("kosch-pen-space-${LocalDate.now()}.svg") }
                    .onFailure {
                        pendingDocumentStore.discard(PendingDocumentKind.INK_SVG, pendingInkExportToken)
                        pendingInkExportToken = null
                        controller.recordInkExport(false)
                    }
            }
            .onFailure { controller.recordInkExport(false) }
    }

    private fun requestWidget() {
        if (pendingWidgetId != null) return
        val appWidgetId = widgetHostController.allocateId()
        pendingWidgetId = appWidgetId
        runCatching { widgetPickRequest.launch(widgetHostController.pickIntent(appWidgetId)) }
            .onFailure {
                abandonPendingWidget()
                controller.postNotice("Die Android-Widget-Auswahl ist nicht verfügbar")
            }
    }

    private fun finishWidgetSetup(appWidgetId: Int) {
        pendingWidgetId = null
        if (widgetHostController.isValid(appWidgetId)) {
            controller.acceptWidget(appWidgetId)
        } else {
            widgetHostController.deleteId(appWidgetId)
            controller.postNotice("Das Widget wurde nicht gebunden")
        }
    }

    private fun abandonPendingWidget() {
        pendingWidgetId?.let(widgetHostController::deleteId)
        pendingWidgetId = null
    }

    private fun deleteWidget(appWidgetId: Int) {
        widgetHostController.deleteId(appWidgetId)
        controller.removeWidgetRecord(appWidgetId)
    }

    private fun forgetDocument() {
        documentGrantManager.releaseCurrent()
            .onSuccess { released ->
                controller.forgetDocument(released)
            }
            .onFailure {
                controller.postNotice("Der gespeicherte Dateizugriff konnte nicht vollständig gelöst werden")
            }
    }

    private companion object {
        const val STATE_PENDING_WIDGET_ID = "pending_widget_id"
        const val STATE_PENDING_BACKUP_EXPORT = "pending_backup_export"
        const val STATE_PENDING_AUDIT_EXPORT = "pending_audit_export"
        const val STATE_PENDING_INK_EXPORT = "pending_ink_export"
        const val BACKUP_MIME_TYPE = "application/vnd.kosch.workspace-backup"
        const val EXTRA_USE_SYSTEM_CONTACTS_PICKER = "android.intent.extra.USE_SYSTEM_CONTACTS_PICKER"
    }
}
