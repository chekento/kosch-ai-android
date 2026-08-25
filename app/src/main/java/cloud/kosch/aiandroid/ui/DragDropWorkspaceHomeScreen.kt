package cloud.kosch.aiandroid.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cloud.kosch.aiandroid.LauncherController
import cloud.kosch.aiandroid.WorkspaceHomeController
import cloud.kosch.aiandroid.model.AppProfile
import cloud.kosch.aiandroid.model.LauncherFolder
import cloud.kosch.aiandroid.model.WorkspaceCellBounds
import cloud.kosch.aiandroid.model.WorkspaceItem
import cloud.kosch.aiandroid.model.WorkspaceItemContent
import cloud.kosch.aiandroid.model.WorkspacePageEditor
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Ink
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Violet
import cloud.kosch.aiandroid.ui.theme.Warm
import kotlin.math.roundToInt

/**
 * Keeps the proven v7 Home intact and adds a focused direct-manipulation surface on top of it.
 * Finger, mouse and Android stylus pointer drags all arrive through Compose pointer input. The existing
 * item editor remains the non-drag keyboard/TalkBack path.
 */
@Composable
fun DragDropWorkspaceHomeScreen(
    controller: LauncherController,
    home: WorkspaceHomeController,
    requestVoiceInput: () -> Unit,
    requestDocument: () -> Unit,
    requestContact: () -> Unit,
) {
    var arrangeVisible by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        UnifiedWorkspaceHomeScreen(
            controller = controller,
            home = home,
            requestVoiceInput = requestVoiceInput,
            requestDocument = requestDocument,
            requestContact = requestContact,
        )
        if (home.isUserPage()) {
            AssistChip(
                onClick = { arrangeVisible = true },
                label = { Text("Anordnen") },
                leadingIcon = { Icon(Icons.Rounded.DragIndicator, contentDescription = null) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 76.dp),
            )
        }
    }

    if (arrangeVisible) {
        WorkspaceArrangeDialog(
            controller = controller,
            home = home,
            onDismiss = { arrangeVisible = false },
        )
    }
}

@Composable
private fun WorkspaceArrangeDialog(
    controller: LauncherController,
    home: WorkspaceHomeController,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(modifier = Modifier.size(42.dp), color = Sky.copy(alpha = 0.14f), shape = CircleShape) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.DragIndicator, contentDescription = null, tint = Sky)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Homescreen anordnen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Ziehen · am Raster einrasten · über Seitenkante auf andere Home-Seite",
                            color = MutedMist,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    IconButton(enabled = home.canUndo, onClick = home::undo) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Letzten Homescreen-Schritt rückgängig")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Anordnen schließen")
                    }
                }

                ArrangeUserPageRail(home)

                if (!home.isUserPage()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        color = DeepSurface,
                        shape = RoundedCornerShape(28.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Szenenseiten sind geschützt. Wähle oben eine freie Home-Seite.", color = MutedMist)
                        }
                    }
                } else {
                    ArrangeCanvas(
                        controller = controller,
                        home = home,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }

                Text(
                    "Tastatur/TalkBack: Anordnen schließen und im normalen Edit-Modus ein Element antippen; die Pfeilsteuerung bleibt vollständig erhalten.",
                    color = MutedMist,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ArrangeUserPageRail(home: WorkspaceHomeController) {
    val userPages = home.document.pages.filter { it.sceneAdapter == null }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val activeIndex = userPages.indexOfFirst { it.id == home.activePage.id }
        IconButton(
            enabled = activeIndex > 0,
            onClick = { home.activatePage(userPages[activeIndex - 1].id) },
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Vorherige freie Home-Seite")
        }
        userPages.forEach { page ->
            FilterChip(
                selected = page.id == home.activePage.id,
                onClick = { home.activatePage(page.id) },
                label = { Text(page.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
        IconButton(
            enabled = activeIndex >= 0 && activeIndex < userPages.lastIndex,
            onClick = { home.activatePage(userPages[activeIndex + 1].id) },
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Nächste freie Home-Seite")
        }
    }
}

@Composable
private fun ArrangeCanvas(
    controller: LauncherController,
    home: WorkspaceHomeController,
    modifier: Modifier = Modifier,
) {
    val page = home.activePage
    val previousPageId = home.adjacentUserPageId(-1)
    val nextPageId = home.adjacentUserPageId(1)

    BoxWithConstraints(
        modifier = modifier
            .background(DeepSurface.copy(alpha = 0.96f), RoundedCornerShape(28.dp))
            .padding(6.dp),
    ) {
        val grid = home.document.grid
        val cellWidth = maxWidth / grid.columns
        val cellHeight = maxHeight / grid.rows
        val density = LocalDensity.current
        val cellWidthPx = with(density) { cellWidth.toPx() }.coerceAtLeast(1f)
        val cellHeightPx = with(density) { cellHeight.toPx() }.coerceAtLeast(1f)

        if (previousPageId != null) {
            PageEdgeHint(
                direction = -1,
                title = home.document.pages.first { it.id == previousPageId }.title,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
        if (nextPageId != null) {
            PageEdgeHint(
                direction = 1,
                title = home.document.pages.first { it.id == nextPageId }.title,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }

        if (page.items.isEmpty()) {
            Text(
                "Noch keine Elemente auf ${page.title}",
                modifier = Modifier.align(Alignment.Center),
                color = MutedMist,
            )
        }

        page.items.forEach { item ->
            if (item.content is WorkspaceItemContent.ActionTile) return@forEach
            key(item.id) {
                DraggableArrangeItem(
                    item = item,
                    controller = controller,
                    home = home,
                    cellWidthPx = cellWidthPx,
                    cellHeightPx = cellHeightPx,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    previousPageId = previousPageId,
                    nextPageId = nextPageId,
                )
            }
        }
    }
}

@Composable
private fun DraggableArrangeItem(
    item: WorkspaceItem,
    controller: LauncherController,
    home: WorkspaceHomeController,
    cellWidthPx: Float,
    cellHeightPx: Float,
    cellWidth: androidx.compose.ui.unit.Dp,
    cellHeight: androidx.compose.ui.unit.Dp,
    previousPageId: String?,
    nextPageId: String?,
) {
    var dragOffset by remember(item.id, item.bounds) { mutableStateOf(Offset.Zero) }
    var dragging by remember(item.id) { mutableStateOf(false) }
    val grid = home.document.grid
    val rawColumn = item.bounds.column + (dragOffset.x / cellWidthPx).roundToInt()
    val rawRow = item.bounds.row + (dragOffset.y / cellHeightPx).roundToInt()
    val crossTarget = when {
        rawColumn < 0 -> previousPageId
        rawColumn + item.bounds.columnSpan > grid.columns -> nextPageId
        else -> null
    }
    val requested = if (crossTarget == previousPageId && crossTarget != null) {
        item.bounds.copy(
            column = grid.columns - item.bounds.columnSpan,
            row = rawRow,
        )
    } else if (crossTarget == nextPageId && crossTarget != null) {
        item.bounds.copy(column = 0, row = rawRow)
    } else {
        item.bounds.copy(column = rawColumn, row = rawRow)
    }
    val previewBounds = if (crossTarget == null) {
        WorkspacePageEditor.nearestAvailableBounds(
            grid = grid,
            items = home.activePage.items,
            requested = requested,
            excludingItemId = item.id,
        )
    } else {
        val targetPage = home.document.pages.firstOrNull { it.id == crossTarget }
        targetPage?.let {
            WorkspacePageEditor.nearestAvailableBounds(
                grid = grid,
                items = it.items,
                requested = requested,
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (dragging && previewBounds != null && crossTarget == null) {
            Surface(
                modifier = Modifier
                    .offset(
                        x = cellWidth * previewBounds.column,
                        y = cellHeight * previewBounds.row,
                    )
                    .size(
                        width = cellWidth * previewBounds.columnSpan,
                        height = cellHeight * previewBounds.rowSpan,
                    )
                    .padding(3.dp),
                color = Sky.copy(alpha = 0.14f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Sky.copy(alpha = 0.72f)),
            ) {}
        }

        val width = cellWidth * item.bounds.columnSpan
        val height = cellHeight * item.bounds.rowSpan
        val baseX = item.bounds.column * cellWidthPx
        val baseY = item.bounds.row * cellHeightPx
        Card(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (baseX + dragOffset.x).roundToInt(),
                        y = (baseY + dragOffset.y).roundToInt(),
                    )
                }
                .size(width, height)
                .padding(3.dp)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = arrangeDescription(item, controller)
                }
                .pointerInput(item.id, item.bounds, previousPageId, nextPageId) {
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDrag = { change, delta ->
                            change.consume()
                            dragOffset += delta
                        },
                        onDragCancel = {
                            dragOffset = Offset.Zero
                            dragging = false
                        },
                        onDragEnd = {
                            val target = crossTarget
                            val bounds = previewBounds
                            if (bounds != null) {
                                if (target == null) {
                                    home.moveItemTo(item.id, bounds)
                                } else {
                                    home.moveItemToPage(item.id, target, bounds)
                                }
                            }
                            dragOffset = Offset.Zero
                            dragging = false
                        },
                    )
                },
            colors = CardDefaults.cardColors(
                containerColor = if (dragging) Sky.copy(alpha = 0.20f) else RaisedSurface.copy(alpha = 0.96f),
            ),
            border = BorderStroke(1.dp, if (dragging) Sky else Mint.copy(alpha = 0.34f)),
            shape = RoundedCornerShape(18.dp),
        ) {
            ArrangeItemContent(item, controller, crossTarget)
        }
    }
}

@Composable
private fun ArrangeItemContent(
    item: WorkspaceItem,
    controller: LauncherController,
    crossTarget: String?,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (val content = item.content) {
            is WorkspaceItemContent.App -> {
                val app = controller.apps.firstOrNull { it.key == content.appKey }
                if (app == null) {
                    Icon(Icons.Rounded.Apps, contentDescription = null, tint = Warm)
                    Text("App fehlt", color = Warm, style = MaterialTheme.typography.labelSmall)
                } else {
                    Image(app.icon, contentDescription = null, modifier = Modifier.size(38.dp))
                    Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                    if (app.profile != AppProfile.PERSONAL) {
                        Text(app.profile.title, color = Sky, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            is WorkspaceItemContent.Folder -> {
                val folder: LauncherFolder? = controller.folders.firstOrNull { it.id == content.folderId }
                Icon(Icons.Rounded.Folder, contentDescription = null, tint = Violet)
                Text(folder?.title ?: "Ordner fehlt", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
            }
            is WorkspaceItemContent.Widget -> Text("Widget · Remap", color = Warm, style = MaterialTheme.typography.labelSmall)
            is WorkspaceItemContent.ActionTile -> Unit
        }
        if (crossTarget != null) {
            Spacer(Modifier.size(2.dp))
            Text("SEITEN-DROP", color = Sky, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PageEdgeHint(direction: Int, title: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.width(46.dp).fillMaxHeight(0.72f),
        color = Sky.copy(alpha = 0.08f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Sky.copy(alpha = 0.24f)),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                if (direction < 0) Icons.AutoMirrored.Rounded.ArrowBack else Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = Sky,
            )
            Text(title.take(10), maxLines = 2, color = Sky, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun arrangeDescription(item: WorkspaceItem, controller: LauncherController): String = when (val content = item.content) {
    is WorkspaceItemContent.App -> "${controller.apps.firstOrNull { it.key == content.appKey }?.label ?: "App"}. Ziehen zum Verschieben"
    is WorkspaceItemContent.Folder -> "Ordner ${controller.folders.firstOrNull { it.id == content.folderId }?.title ?: "unbekannt"}. Ziehen zum Verschieben"
    is WorkspaceItemContent.Widget -> "Widget. Ziehen zum Verschieben"
    is WorkspaceItemContent.ActionTile -> "Geschützte Aktion"
}
