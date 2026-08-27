package cloud.kosch.aiandroid.ui

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cloud.kosch.aiandroid.WorkspaceHomeController
import cloud.kosch.aiandroid.WorkspaceWidgetPickerActivity
import cloud.kosch.aiandroid.model.WorkspaceItem
import cloud.kosch.aiandroid.model.WorkspaceItemContent
import cloud.kosch.aiandroid.system.WidgetHostController
import cloud.kosch.aiandroid.system.WidgetHostOwnership
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Warm
import kotlin.math.roundToInt

fun launchWorkspaceWidgetPicker(context: Context, remapWorkspaceItemId: String? = null) {
    runCatching {
        context.startActivity(WorkspaceWidgetPickerActivity.intent(context, remapWorkspaceItemId))
    }
}

/**
 * Interactive AppWidget host for one V7 workspace item.
 *
 * Play mode leaves the Android host view interactive. Edit mode places an explicit launcher-owned overlay above
 * it so widget taps cannot trigger provider actions while the user intends to move/resize the launcher object.
 */
@Composable
fun WorkspaceWidgetHomeItem(
    item: WorkspaceItem,
    home: WorkspaceHomeController,
    editMode: Boolean,
    onEdit: () -> Unit,
) {
    val context = LocalContext.current
    val widget = item.content as? WorkspaceItemContent.Widget ?: return
    val host = remember(context.applicationContext) {
        WorkspaceWidgetHostPool.acquire(context.applicationContext)
    }

    DisposableEffect(host) {
        onDispose { WorkspaceWidgetHostPool.release(host) }
    }

    LaunchedEffect(host, home.widgetBindings) {
        val hostedProviders = host.hostedProviderComponents()
        val releasedBindings = home.pruneWidgetBindings(hostedProviders)
        val orphanedHostIds = WidgetHostOwnership.orphanedIds(
            hostedIds = hostedProviders.keys,
            ownedIds = home.widgetBindings.values.toSet(),
        )
        (releasedBindings + orphanedHostIds).forEach(host::deleteId)
    }

    val appWidgetId = home.widgetBindingFor(item.id)
    val expectedProvider = widget.providerComponent
    val actualProvider = appWidgetId?.let(host::providerComponent)
    val validBinding = appWidgetId != null &&
        expectedProvider != null &&
        expectedProvider == actualProvider &&
        host.isValid(appWidgetId)

    if (!validBinding) {
        MissingWorkspaceWidget(
            providerComponent = expectedProvider,
            onRemap = { launchWorkspaceWidgetPicker(context, item.id) },
        )
        return
    }

    key(appWidgetId, item.bounds.columnSpan, item.bounds.rowSpan) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val widthDp = maxWidth.value.roundToInt().coerceAtLeast(40)
            val heightDp = maxHeight.value.roundToInt().coerceAtLeast(40)
            AndroidView(
                factory = { viewContext ->
                    host.createWorkspaceView(
                        context = viewContext,
                        appWidgetId = appWidgetId,
                        widthDp = widthDp,
                        heightDp = heightDp,
                    ) ?: FrameLayout(viewContext)
                },
                modifier = Modifier.fillMaxSize(),
            )

            if (editMode) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onEdit),
                    color = Sky.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Sky.copy(alpha = 0.60f)),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentAlignment = Alignment.TopEnd,
                    ) {
                        Text("Widget bearbeiten", color = Sky, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun MissingWorkspaceWidget(
    providerComponent: String?,
    onRemap: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onRemap),
        color = Warm.copy(alpha = 0.10f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Warm.copy(alpha = 0.34f)),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (providerComponent == null) {
                    "Widget neu zuordnen"
                } else {
                    "Widget nicht gebunden · neu zuordnen"
                },
                color = if (providerComponent == null) Warm else MutedMist,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/** One AppWidgetHost listener per process for all V7 widgets; Compose items hold lightweight references only. */
private object WorkspaceWidgetHostPool {
    private var controller: WidgetHostController? = null
    private var references: Int = 0

    @Synchronized
    fun acquire(context: Context): WidgetHostController {
        val host = controller ?: WidgetHostController(
            context.applicationContext,
            WidgetHostController.WORKSPACE_HOST_ID,
        ).also { controller = it }
        if (references == 0) host.startListening()
        references += 1
        return host
    }

    @Synchronized
    fun release(host: WidgetHostController) {
        if (controller !== host || references <= 0) return
        references -= 1
        if (references == 0) host.stopListening()
    }
}
