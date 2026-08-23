package cloud.kosch.aiandroid

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import cloud.kosch.aiandroid.system.HomeRoleController
import cloud.kosch.aiandroid.system.WidgetHostController
import cloud.kosch.aiandroid.ui.LauncherRoot
import cloud.kosch.aiandroid.ui.theme.KoSchLauncherTheme

class MainActivity : ComponentActivity() {
    private lateinit var controller: LauncherController
    private lateinit var widgetHostController: WidgetHostController
    private var pendingWidgetId: Int? = null

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
            controller.submitCommand(spoken, ::requestVoiceInput, ::requestDocument)
        }
    }

    private val documentRequest = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            controller.inspectDocument(uri)
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
        controller = LauncherController(applicationContext)
        widgetHostController = WidgetHostController(applicationContext)
        controller.start()
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
                    requestWidget = ::requestWidget,
                    createWidgetView = widgetHostController::createView,
                    deleteWidget = ::deleteWidget,
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

    override fun onDestroy() {
        controller.close()
        super.onDestroy()
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
}
