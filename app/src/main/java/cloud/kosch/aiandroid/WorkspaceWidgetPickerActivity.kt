package cloud.kosch.aiandroid

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import cloud.kosch.aiandroid.data.WorkspaceWidgetRemapCoordinator
import cloud.kosch.aiandroid.system.WidgetHostController

/**
 * Internal, non-exported Android widget picker/configuration bridge for Workspace v7.
 *
 * It deliberately uses a host id separate from the legacy Widget Board. The activity owns only the temporary
 * selection flow; portable layout and device-local binding stores remain the source of truth.
 */
class WorkspaceWidgetPickerActivity : ComponentActivity() {
    private lateinit var widgetHost: WidgetHostController
    private var pendingWidgetId: Int? = null
    private var remapWorkspaceItemId: String? = null

    private val widgetPickRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val appWidgetId = result.data?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )?.takeUnless { it == AppWidgetManager.INVALID_APPWIDGET_ID } ?: pendingWidgetId
        if (result.resultCode != Activity.RESULT_OK || appWidgetId == null) {
            abandonAndFinish()
            return@registerForActivityResult
        }
        pendingWidgetId = appWidgetId
        val configurationIntent = widgetHost.configurationIntent(appWidgetId)
        if (configurationIntent == null) {
            finishWidgetSetup(appWidgetId)
        } else {
            runCatching { widgetConfigureRequest.launch(configurationIntent) }
                .onFailure { abandonAndFinish() }
        }
    }

    private val widgetConfigureRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val appWidgetId = pendingWidgetId
        if (result.resultCode == Activity.RESULT_OK && appWidgetId != null) {
            finishWidgetSetup(appWidgetId)
        } else {
            abandonAndFinish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetHost = WidgetHostController(applicationContext, WidgetHostController.WORKSPACE_HOST_ID)
        pendingWidgetId = savedInstanceState
            ?.getInt(STATE_PENDING_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?.takeUnless { it == AppWidgetManager.INVALID_APPWIDGET_ID }
        remapWorkspaceItemId = savedInstanceState?.getString(STATE_REMAP_ITEM_ID)
            ?: intent.getStringExtra(EXTRA_REMAP_ITEM_ID)?.trim()?.takeIf(String::isNotBlank)

        if (pendingWidgetId == null) launchPicker()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pendingWidgetId?.let { outState.putInt(STATE_PENDING_WIDGET_ID, it) }
        remapWorkspaceItemId?.let { outState.putString(STATE_REMAP_ITEM_ID, it) }
        super.onSaveInstanceState(outState)
    }

    private fun launchPicker() {
        val appWidgetId = widgetHost.allocateId()
        pendingWidgetId = appWidgetId
        runCatching { widgetPickRequest.launch(widgetHost.pickIntent(appWidgetId)) }
            .onFailure { abandonAndFinish() }
    }

    private fun finishWidgetSetup(appWidgetId: Int) {
        pendingWidgetId = null
        val providerComponent = widgetHost.providerComponent(appWidgetId)
        if (!widgetHost.isValid(appWidgetId) || providerComponent.isNullOrBlank()) {
            widgetHost.deleteId(appWidgetId)
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val remapItemId = remapWorkspaceItemId
        val committed = if (remapItemId == null) {
            WorkspaceHomeController(
                context = applicationContext,
                registerAsActive = false,
            ).addWidget(appWidgetId, providerComponent)
        } else {
            val result = WorkspaceWidgetRemapCoordinator(applicationContext).remap(
                workspaceItemId = remapItemId,
                appWidgetId = appWidgetId,
                providerComponent = providerComponent,
            )
            if (result.committed) {
                result.releasedAppWidgetId
                    ?.takeIf { it != appWidgetId }
                    ?.let(widgetHost::deleteId)
            }
            result.committed
        }

        if (!committed) {
            widgetHost.deleteId(appWidgetId)
        }
        WorkspaceHomeController.notifyPersistedChange()
        setResult(if (committed) Activity.RESULT_OK else Activity.RESULT_CANCELED)
        finish()
    }

    private fun abandonAndFinish() {
        pendingWidgetId?.let(widgetHost::deleteId)
        pendingWidgetId = null
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    companion object {
        private const val EXTRA_REMAP_ITEM_ID = "workspace_widget_remap_item_id"
        private const val STATE_PENDING_WIDGET_ID = "workspace_widget_pending_id"
        private const val STATE_REMAP_ITEM_ID = "workspace_widget_remap_item_state"

        fun intent(context: Context, remapWorkspaceItemId: String? = null): Intent =
            Intent(context, WorkspaceWidgetPickerActivity::class.java).apply {
                remapWorkspaceItemId?.trim()?.takeIf(String::isNotBlank)?.let {
                    putExtra(EXTRA_REMAP_ITEM_ID, it)
                }
            }
    }
}
