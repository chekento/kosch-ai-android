package cloud.kosch.aiandroid

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.pm.LauncherApps
import android.os.Bundle
import androidx.activity.ComponentActivity
import cloud.kosch.aiandroid.system.WidgetHostController

/**
 * Default-launcher endpoint for AppWidgetManager.requestPinAppWidget().
 *
 * Android's PinItemRequest contract says the requesting app has already handled configuration; KoSch therefore shows
 * only its own explicit confirmation, accepts the request with a fresh WORKSPACE_HOST_ID appWidgetId and places that
 * binding on Workspace v7. No provider configuration activity is launched here.
 */
class LauncherPinWidgetActivity : ComponentActivity() {
    private lateinit var launcherApps: LauncherApps
    private var dialogShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcherApps = getSystemService(LauncherApps::class.java)
        dialogShown = savedInstanceState?.getBoolean(STATE_DIALOG_SHOWN, false) ?: false
        if (!dialogShown) showRequestOrFinish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_DIALOG_SHOWN, dialogShown)
        super.onSaveInstanceState(outState)
    }

    private fun showRequestOrFinish() {
        val request = runCatching { launcherApps.getPinItemRequest(intent) }.getOrNull()
        if (request == null ||
            request.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_APPWIDGET ||
            !request.isValid
        ) {
            finishCanceled()
            return
        }
        val providerInfo = request.getAppWidgetProviderInfo(this)
        val label = runCatching { providerInfo?.loadLabel(packageManager) }
            .getOrNull()
            ?.toString()
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: providerInfo?.provider?.packageName
            ?: "Diese App"

        dialogShown = true
        AlertDialog.Builder(this)
            .setTitle("Widget hinzufügen?")
            .setMessage("$label möchte ein Widget auf deinem KoSch-Homescreen anheften.")
            .setNegativeButton("Abbrechen") { _, _ -> finishCanceled() }
            .setPositiveButton("Hinzufügen") { _, _ -> acceptWidget(request) }
            .setOnCancelListener { finishCanceled() }
            .show()
    }

    private fun acceptWidget(request: LauncherApps.PinItemRequest) {
        if (!request.isValid) {
            finishCanceled()
            return
        }
        val providerComponent = request.getAppWidgetProviderInfo(this)
            ?.provider
            ?.flattenToString()
            ?.takeIf(String::isNotBlank)
        if (providerComponent == null) {
            finishCanceled()
            return
        }

        val widgetHost = WidgetHostController(applicationContext, WidgetHostController.WORKSPACE_HOST_ID)
        val appWidgetId = widgetHost.allocateId()
        val accepted = runCatching {
            request.accept(
                Bundle().apply {
                    putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                },
            )
        }.getOrDefault(false)
        if (!accepted) {
            widgetHost.deleteId(appWidgetId)
            finishCanceled()
            return
        }

        val committed = WorkspaceHomeController(
            context = applicationContext,
            registerAsActive = false,
        ).addWidget(appWidgetId, providerComponent)
        if (!committed) {
            // The external request was accepted, but KoSch could not persist placement. Drop host ownership instead of
            // leaving an invisible/orphaned widget id; startup reconciliation covers process death in this tiny window.
            widgetHost.deleteId(appWidgetId)
            finishCanceled()
            return
        }

        WorkspaceHomeController.notifyPersistedChange()
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun finishCanceled() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    companion object {
        private const val STATE_DIALOG_SHOWN = "pin_widget_dialog_shown"
    }
}
