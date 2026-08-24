package cloud.kosch.aiandroid

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.speech.RecognizerIntent
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
import cloud.kosch.aiandroid.data.PendingDocumentKind
import cloud.kosch.aiandroid.data.PendingDocumentStore
import cloud.kosch.aiandroid.system.HomeRoleController
import cloud.kosch.aiandroid.system.DocumentGrantManager
import cloud.kosch.aiandroid.system.ProfessionalShortcut
import cloud.kosch.aiandroid.system.ProfessionalShortcutResolver
import cloud.kosch.aiandroid.system.WidgetHostController
import cloud.kosch.aiandroid.ui.LauncherRoot
import cloud.kosch.aiandroid.ui.theme.KoSchLauncherTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val launcherViewModel: LauncherViewModel by viewModels()
    private val controller: LauncherController get() = launcherViewModel.controller
    private lateinit var widgetHostController: WidgetHostController
    private lateinit var documentGrantManager: DocumentGrantManager
    private lateinit var pendingDocumentStore: PendingDocumentStore
    private var pendingWidgetId: Int? = null
    private var pendingBackupExportToken: String? = null
    private var pendingAuditExportToken: String? = null
    private var pendingInkExportToken: String? = null

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
            KoSchLauncherTheme {
                LauncherRoot(
                    controller = controller,
                    requestHomeRole = ::requestHomeRole,
                    requestVoiceInput = ::requestVoiceInput,
                    requestDocument = ::requestDocument,
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
        }
    }

    override fun onStart() {
        super.onStart()
        widgetHostController.startListening()
    }

    override fun onResume() {
        super.onResume()
        controller.refreshSystemState()
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
            ProfessionalShortcut.COMMAND -> controller.requestCommandFocus()
            ProfessionalShortcut.APPS -> controller.openDrawer()
            ProfessionalShortcut.PRO_DESK -> controller.openProDesk()
            ProfessionalShortcut.CONTROL_CENTER -> controller.openControlCenter()
            ProfessionalShortcut.PHONE -> controller.openPhone()
            ProfessionalShortcut.FILES -> requestDocument()
            ProfessionalShortcut.BACKUP -> controller.openBackup()
            ProfessionalShortcut.AUDIT -> controller.openAudit()
            ProfessionalShortcut.PEN_SPACE -> controller.openPenSpace()
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
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
                KeyboardShortcutInfo("Command Bar", KeyEvent.KEYCODE_K, KeyEvent.META_CTRL_ON),
                KeyboardShortcutInfo("Apps", KeyEvent.KEYCODE_SPACE, KeyEvent.META_CTRL_ON),
                KeyboardShortcutInfo("Pro Desk", KeyEvent.KEYCODE_H, KeyEvent.META_CTRL_ON),
                KeyboardShortcutInfo("Kontrollzentrum", KeyEvent.KEYCODE_COMMA, KeyEvent.META_CTRL_ON),
                KeyboardShortcutInfo("Telefon", KeyEvent.KEYCODE_D, KeyEvent.META_CTRL_ON),
                KeyboardShortcutInfo("Datei-KI", KeyEvent.KEYCODE_O, KeyEvent.META_CTRL_ON),
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
