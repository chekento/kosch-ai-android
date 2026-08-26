package cloud.kosch.aiandroid

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.data.WorkspaceWidgetBindingStore
import cloud.kosch.aiandroid.model.DeviceWidgetBinding
import cloud.kosch.aiandroid.system.WidgetHostController
import cloud.kosch.aiandroid.ui.theme.KoSchLauncherTheme

/**
 * Internal transaction boundary for adding one Android widget to the v7 Home.
 *
 * The system picker/configuration flow owns the Android appWidgetId. KoSch persists the portable workspace
 * item first, then commits the device-only binding. Any failed second step rolls the v7 item back and releases
 * the host id, so a cancelled or partial flow cannot leave a phantom widget behind.
 */
class WorkspaceWidgetPlacementActivity : ComponentActivity() {
    private lateinit var widgetHost: WidgetHostController
    private lateinit var bindingStore: WorkspaceWidgetBindingStore
    private lateinit var home: WorkspaceHomeController
    private var pendingWidgetId: Int? = null

    private val pickRequest = registerForActivityResult(
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
        val configure = widgetHost.configurationIntent(appWidgetId)
        if (configure == null) {
            commitWidget(appWidgetId)
        } else {
            runCatching { configureRequest.launch(configure) }
                .onFailure { abandonAndFinish() }
        }
    }

    private val configureRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val appWidgetId = pendingWidgetId
        if (result.resultCode == Activity.RESULT_OK && appWidgetId != null) {
            commitWidget(appWidgetId)
        } else {
            abandonAndFinish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        widgetHost = WidgetHostController(applicationContext)
        bindingStore = WorkspaceWidgetBindingStore(applicationContext)
        home = WorkspaceHomeController(applicationContext)
        pendingWidgetId = savedInstanceState
            ?.getInt(STATE_PENDING_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?.takeUnless { it == AppWidgetManager.INVALID_APPWIDGET_ID }

        setContent {
            KoSchLauncherTheme {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Icon(Icons.Rounded.Widgets, contentDescription = null)
                        Text("Widget zum Home hinzufügen", style = MaterialTheme.typography.titleMedium)
                        CircularProgressIndicator()
                        Text(
                            "Android verwaltet Auswahl und Konfiguration. Bei Abbruch wird keine Host-ID behalten.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        if (pendingWidgetId == null) {
            startPick()
        }
    }

    override fun onStart() {
        super.onStart()
        widgetHost.startListening()
    }

    override fun onStop() {
        widgetHost.stopListening()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pendingWidgetId?.let { outState.putInt(STATE_PENDING_WIDGET_ID, it) }
        super.onSaveInstanceState(outState)
    }

    private fun startPick() {
        val appWidgetId = widgetHost.allocateId()
        pendingWidgetId = appWidgetId
        runCatching { pickRequest.launch(widgetHost.pickIntent(appWidgetId)) }
            .onFailure { abandonAndFinish() }
    }

    private fun commitWidget(appWidgetId: Int) {
        if (!widgetHost.isValid(appWidgetId)) {
            abandonAndFinish()
            return
        }
        val providerComponent = widgetHost.providerComponent(appWidgetId)
        if (providerComponent == null) {
            abandonAndFinish()
            return
        }
        val workspaceItemId = home.addWidget(providerComponent)
        if (workspaceItemId == null) {
            abandonAndFinish()
            return
        }
        val bound = bindingStore.bind(DeviceWidgetBinding(workspaceItemId, appWidgetId))
        if (!bound) {
            home.removeItem(workspaceItemId)
            widgetHost.deleteId(appWidgetId)
            pendingWidgetId = null
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        pendingWidgetId = null
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtra(EXTRA_WORKSPACE_ITEM_ID, workspaceItemId)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
        )
        finish()
    }

    private fun abandonAndFinish() {
        pendingWidgetId?.let(widgetHost::deleteId)
        pendingWidgetId = null
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    companion object {
        const val EXTRA_WORKSPACE_ITEM_ID = "workspace_widget_item_id"
        private const val STATE_PENDING_WIDGET_ID = "pending_workspace_widget_id"
    }
}
