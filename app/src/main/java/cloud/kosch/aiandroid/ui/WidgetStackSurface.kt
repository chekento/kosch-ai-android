package cloud.kosch.aiandroid.ui

import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cloud.kosch.aiandroid.WidgetStackController
import cloud.kosch.aiandroid.ai.WidgetStackPolicy
import cloud.kosch.aiandroid.model.WidgetSizePreset
import cloud.kosch.aiandroid.model.WidgetStack
import cloud.kosch.aiandroid.model.WidgetStackMode
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Violet
import cloud.kosch.aiandroid.ui.theme.Warm
import kotlinx.coroutines.delay

/** Local display metadata only. No widget RemoteViews payload is copied into KoSch state. */
private data class StackWidgetLabel(
    val appWidgetId: Int,
    val label: String,
    val providerPackage: String,
)

private fun resolveStackWidgetLabel(context: Context, appWidgetId: Int): StackWidgetLabel? {
    val manager = AppWidgetManager.getInstance(context.applicationContext)
    val info = manager.getAppWidgetInfo(appWidgetId) ?: return null
    val label = runCatching { info.loadLabel(context.packageManager) }
        .getOrNull()
        ?.toString()
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?: info.provider.shortClassName
    return StackWidgetLabel(
        appWidgetId = appWidgetId,
        label = label.take(80),
        providerPackage = info.provider.packageName,
    )
}

/**
 * Device-local manager for stacks of already-bound legacy Widget Board ids.
 *
 * Selecting a widget here never allocates/binds an AppWidgetHost id. The caller supplies the existing host view
 * factory; this surface only groups ids that Android has already granted to KoSch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetStackManagerSheet(
    stacks: WidgetStackController,
    boundWidgetIds: List<Int>,
    createWidgetView: (Context, Int, WidgetSizePreset) -> View?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val validIds = remember(boundWidgetIds) { boundWidgetIds.filter { it > 0 }.distinct() }
    val labels = remember(validIds) {
        validIds.mapNotNull { resolveStackWidgetLabel(context, it) }.associateBy { it.appWidgetId }
    }
    var selectedIds by remember(validIds) { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedStackId by remember(stacks.stacks) {
        mutableStateOf(stacks.stacks.firstOrNull()?.id)
    }

    LaunchedEffect(stacks.stacks, selectedStackId) {
        if (selectedStackId !in stacks.stacks.map { it.id }.toSet()) {
            selectedStackId = stacks.stacks.firstOrNull()?.id
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DeepSurface,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            item {
                SheetHeader(
                    title = "Widget Stacks",
                    subtitle = "Device-local · nur bereits gebundene Android-Widgets",
                    onClose = onDismiss,
                )
            }

            item {
                Surface(
                    color = Mint.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(13.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Layers, contentDescription = null, tint = Mint)
                        Column(Modifier.weight(1f)) {
                            Text("Keine zweite Widget-Bindung", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Stacks speichern ausschließlich vorhandene Host-IDs lokal auf diesem Gerät. Portable Backups enthalten sie nicht.",
                                color = MutedMist,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            if (validIds.isEmpty()) {
                item {
                    Surface(color = Warm.copy(alpha = 0.10f), shape = RoundedCornerShape(18.dp)) {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Icon(Icons.Rounded.Widgets, contentDescription = null, tint = Warm)
                            Text("Noch keine gebundenen Widgets", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Binde zuerst ein Widget über das Android Widget Board. Danach kann es hier ausdrücklich einem Stack zugeordnet werden.",
                                color = MutedMist,
                            )
                        }
                    }
                }
            } else {
                item {
                    Text("Neuen Stack zusammenstellen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                items(validIds, key = { "candidate-$it" }) { widgetId ->
                    val label = labels[widgetId]
                    Card(
                        onClick = {
                            selectedIds = if (widgetId in selectedIds) selectedIds - widgetId else selectedIds + widgetId
                        },
                        colors = CardDefaults.cardColors(containerColor = RaisedSurface),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Checkbox(
                                checked = widgetId in selectedIds,
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) selectedIds + widgetId else selectedIds - widgetId
                                },
                            )
                            Column(Modifier.weight(1f)) {
                                Text(label?.label ?: "Widget $widgetId", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    label?.providerPackage ?: "Android Widget",
                                    color = MutedMist,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                item {
                    Button(
                        onClick = {
                            if (stacks.createStack(selectedIds.toList())) {
                                selectedIds = emptySet()
                                selectedStackId = stacks.stacks.lastOrNull()?.id
                            }
                        },
                        enabled = selectedIds.isNotEmpty() && stacks.stacks.size < WidgetStackPolicy.MAX_STACKS,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("Stack aus ${selectedIds.size} Widget${if (selectedIds.size == 1) "" else "s"} erstellen")
                    }
                }
            }

            if (stacks.stacks.isNotEmpty()) {
                item { HorizontalDivider() }
                item {
                    Text("Meine Stacks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        stacks.stacks.forEach { stack ->
                            AssistChip(
                                onClick = { selectedStackId = stack.id },
                                label = { Text(stack.title, maxLines = 1) },
                            )
                        }
                    }
                }

                val stack = stacks.stacks.firstOrNull { it.id == selectedStackId }
                if (stack != null) {
                    item(key = "preview-${stack.id}-${stack.activeWidgetId}") {
                        WidgetStackPreview(
                            stack = stack,
                            labels = labels,
                            createWidgetView = createWidgetView,
                            onPrevious = { stacks.previous(stack.id) },
                            onNext = { stacks.next(stack.id) },
                        )
                    }
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            WidgetStackMode.entries.forEach { mode ->
                                val enabled = mode != WidgetStackMode.CONTEXTUAL
                                FilterChip(
                                    selected = stack.mode == mode,
                                    onClick = {
                                        if (enabled) {
                                            stacks.setMode(
                                                stackId = stack.id,
                                                mode = mode,
                                                autoCycleSeconds = if (mode == WidgetStackMode.AUTO_CYCLE) 30 else 0,
                                            )
                                        }
                                    },
                                    enabled = enabled,
                                    label = {
                                        Text(
                                            if (mode == WidgetStackMode.CONTEXTUAL) "Kontextuell · folgt" else mode.title,
                                            maxLines = 1,
                                        )
                                    },
                                )
                            }
                        }
                    }
                    items(stack.appWidgetIds, key = { "member-${stack.id}-$it" }) { widgetId ->
                        val label = labels[widgetId]
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                label?.label ?: "Widget $widgetId",
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (stack.activeWidgetId != widgetId) {
                                TextButton(onClick = { stacks.select(stack.id, widgetId) }) {
                                    Text("Zeigen")
                                }
                            }
                            IconButton(onClick = { stacks.removeWidget(stack.id, widgetId) }) {
                                Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Aus Stack entfernen")
                            }
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = { stacks.deleteStack(stack.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                            Spacer(Modifier.width(7.dp))
                            Text("Stack entfernen")
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun WidgetStackPreview(
    stack: WidgetStack,
    labels: Map<Int, StackWidgetLabel>,
    createWidgetView: (Context, Int, WidgetSizePreset) -> View?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val activeWidgetId = stack.activeWidgetId

    if (stack.mode == WidgetStackMode.AUTO_CYCLE && stack.appWidgetIds.size > 1 && stack.autoCycleSeconds > 0) {
        LaunchedEffect(stack.id, stack.activeIndex, stack.autoCycleSeconds) {
            delay(stack.autoCycleSeconds * 1_000L)
            onNext()
        }
    }

    Surface(color = RaisedSurface, shape = RoundedCornerShape(20.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Layers, contentDescription = null, tint = Violet)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(stack.title, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${stack.activeIndex + 1} / ${stack.appWidgetIds.size} · ${stack.mode.title}",
                        color = MutedMist,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                IconButton(onClick = onPrevious, enabled = stack.appWidgetIds.size > 1) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Vorheriges Widget")
                }
                IconButton(onClick = onNext, enabled = stack.appWidgetIds.size > 1) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Nächstes Widget")
                }
            }

            if (activeWidgetId == null) {
                Box(
                    Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Kein gültiges Widget im Stack", color = MutedMist)
                }
            } else {
                Text(
                    labels[activeWidgetId]?.label ?: "Widget $activeWidgetId",
                    color = Sky,
                    style = MaterialTheme.typography.labelMedium,
                )
                Box(
                    Modifier.fillMaxWidth().height(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AndroidView(
                        factory = { viewContext ->
                            createWidgetView(viewContext, activeWidgetId, WidgetSizePreset.MEDIUM)
                                ?: FrameLayout(viewContext)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
