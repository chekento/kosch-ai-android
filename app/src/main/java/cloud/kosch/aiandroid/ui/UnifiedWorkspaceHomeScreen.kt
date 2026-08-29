package cloud.kosch.aiandroid.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.LauncherController
import cloud.kosch.aiandroid.WorkspaceHomeController
import cloud.kosch.aiandroid.ai.SmartCollection
import cloud.kosch.aiandroid.model.AppProfile
import cloud.kosch.aiandroid.model.DefaultWorkspace
import cloud.kosch.aiandroid.model.LabelMode
import cloud.kosch.aiandroid.model.LaunchableApp
import cloud.kosch.aiandroid.model.LauncherFolder
import cloud.kosch.aiandroid.model.TileAction
import cloud.kosch.aiandroid.model.WorkspaceItem
import cloud.kosch.aiandroid.model.WorkspaceItemContent
import cloud.kosch.aiandroid.model.WorkspaceObjectStyleResolver
import cloud.kosch.aiandroid.model.WorkspacePageIndicatorPolicy
import cloud.kosch.aiandroid.model.WorkspacePagePolicy
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Violet
import cloud.kosch.aiandroid.ui.theme.Warm
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * KAL reference Home: the wallpaper and the user's content are the product surface. Editing, page management,
 * diagnostics and AI routing deliberately live outside normal Home instead of occupying permanent dashboard chrome.
 */
@Composable
fun UnifiedWorkspaceHomeScreen(
    controller: LauncherController,
    home: WorkspaceHomeController,
    requestVoiceInput: () -> Unit,
    requestDocument: () -> Unit,
    requestContact: () -> Unit,
) {
    val snackbarHost = remember { SnackbarHostState() }
    var addVisible by remember { mutableStateOf(false) }
    var folderPreviewId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(home.statusMessage) {
        home.statusMessage?.let {
            snackbarHost.showSnackbar(it)
            home.consumeStatus()
        }
    }
    LaunchedEffect(controller.notice) {
        controller.notice?.let {
            snackbarHost.showSnackbar(it)
            controller.consumeNotice()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            // Readability scrim only where chrome lives; the wallpaper remains visible through the working area.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.38f),
                            0.22f to Color.Transparent,
                            0.72f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.46f),
                        ),
                    ),
            )

            KalHomeClock(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 22.dp, top = 18.dp),
            )

            ReferenceWorkspaceGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
                    .padding(top = 104.dp, bottom = 112.dp),
                controller = controller,
                home = home,
                onOpenFolder = { folderPreviewId = it },
            )

            CompactPageDots(
                home = home,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 82.dp),
            )

            ReferenceHomeDock(
                controller = controller,
                onAdd = { addVisible = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            )
        }
    }

    if (addVisible) {
        ReferenceAddToHomeSheet(
            controller = controller,
            home = home,
            onDismiss = { addVisible = false },
        )
    }
    folderPreviewId?.let { folderId ->
        ReferenceFolderSheet(
            controller = controller,
            folderId = folderId,
            onDismiss = { folderPreviewId = null },
        )
    }
}

@Composable
private fun KalHomeClock(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            val current = LocalDateTime.now()
            now = current
            delay((60_000L - (current.second * 1_000L + current.nano / 1_000_000L)).coerceAtLeast(1_000L))
        }
    }
    val locale = Locale.getDefault()
    val time = remember(now.hour, now.minute, locale) {
        now.format(DateTimeFormatter.ofPattern("HH:mm", locale))
    }
    val date = remember(now.toLocalDate(), locale) {
        now.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM", locale))
    }
    Column(modifier = modifier) {
        Text(
            text = time,
            color = Color.White,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Light,
        )
        Text(
            text = date,
            color = Color.White.copy(alpha = 0.90f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ReferenceWorkspaceGrid(
    modifier: Modifier,
    controller: LauncherController,
    home: WorkspaceHomeController,
    onOpenFolder: (String) -> Unit,
) {
    val page = home.activePage
    val launcherSettings = LocalLauncherSettings.current
    val scopedSettings = LocalScopedSettings.current
    val styledItems = page.items.map { item ->
        item to WorkspaceObjectStyleResolver.resolve(
            document = scopedSettings,
            pageId = page.id,
            itemId = item.id,
            globalIconScale = launcherSettings.home.iconScale,
            globalShowLabels = launcherSettings.home.labelMode != LabelMode.NEVER,
        )
    }

    BoxWithConstraints(modifier = modifier) {
        val cellWidth = maxWidth / home.document.grid.columns
        val cellHeight = maxHeight / home.document.grid.rows

        styledItems.forEach { (item, objectStyle) ->
            if (!objectStyle.visible) return@forEach
            val width = cellWidth * item.bounds.columnSpan
            val height = cellHeight * item.bounds.rowSpan
            WorkspaceObjectStyleFrame(
                style = objectStyle,
                editing = false,
                modifier = Modifier
                    .offset(x = cellWidth * item.bounds.column, y = cellHeight * item.bounds.row)
                    .size(width = width, height = height)
                    .padding(3.dp),
            ) {
                when (val content = item.content) {
                    is WorkspaceItemContent.ActionTile -> ReferenceLegacyActionItem(content, controller)
                    is WorkspaceItemContent.App -> ReferenceAppItem(
                        app = controller.apps.firstOrNull { it.key == content.appKey },
                        onClick = {
                            controller.apps.firstOrNull { it.key == content.appKey }?.let(controller::launch)
                        },
                    )
                    is WorkspaceItemContent.Folder -> ReferenceFolderItem(
                        folder = controller.folders.firstOrNull { it.id == content.folderId },
                        onClick = { onOpenFolder(content.folderId) },
                    )
                    is WorkspaceItemContent.Widget -> WorkspaceWidgetHomeItem(
                        item = item,
                        home = home,
                        editMode = false,
                        onEdit = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferenceAppItem(app: LaunchableApp?, onClick: () -> Unit) {
    val style = LocalWorkspaceObjectStyle.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = app?.label ?: "Nicht verfügbare App"
            }
            .clickable(enabled = app != null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (app == null) {
                Icon(Icons.Rounded.Apps, contentDescription = null, tint = Warm, modifier = Modifier.size(34.dp))
                Text("App fehlt", color = Warm, style = MaterialTheme.typography.labelSmall)
            } else {
                Image(
                    bitmap = app.icon,
                    contentDescription = null,
                    modifier = Modifier.size((46f * style.iconScale).dp),
                )
                if (style.showLabel) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        app.label,
                        modifier = Modifier.scale(style.labelScale),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (app.profile != AppProfile.PERSONAL) {
                        Text(app.profile.title, color = Sky, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferenceFolderItem(folder: LauncherFolder?, onClick: () -> Unit) {
    val style = LocalWorkspaceObjectStyle.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "Ordner ${folder?.title ?: "nicht verfügbar"}"
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size((48f * style.iconScale).dp),
                color = DeepSurface.copy(alpha = 0.76f),
                shape = RoundedCornerShape(15.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Folder, contentDescription = null, tint = Sky, modifier = Modifier.size(30.dp))
                }
            }
            if (style.showLabel) {
                Spacer(Modifier.height(5.dp))
                Text(
                    folder?.title ?: "Ordner fehlt",
                    modifier = Modifier.scale(style.labelScale),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ReferenceLegacyActionItem(
    content: WorkspaceItemContent.ActionTile,
    controller: LauncherController,
) {
    val tile = remember(content.scene, content.legacyTileId) {
        DefaultWorkspace.tiles(content.scene).firstOrNull { it.id == content.legacyTileId }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                when (content.action) {
                    TileAction.ASK -> controller.openProviderChooser()
                    TileAction.APPS -> controller.openDrawer()
                    TileAction.CONTEXT -> controller.showContextDetails()
                    TileAction.PROVIDERS -> controller.openProviderChooser()
                    TileAction.FOCUS -> controller.openDrawer(SmartCollection.WORK)
                    TileAction.MEDIA -> controller.openDrawer(SmartCollection.MEDIA)
                    TileAction.COMMUNICATION -> controller.openDrawer(SmartCollection.COMMUNICATION)
                    TileAction.TOOLS -> controller.openControlCenter()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(tile?.glyph ?: "◇", color = Mint, style = MaterialTheme.typography.headlineMedium)
            Text(
                tile?.title ?: content.legacyTileId,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CompactPageDots(home: WorkspaceHomeController, modifier: Modifier = Modifier) {
    val pages = home.document.pages
    if (pages.size <= 1) return
    val activeIndex = pages.indexOfFirst { it.id == home.document.activePageId }.coerceAtLeast(0)
    val slots = WorkspacePageIndicatorPolicy.slots(pages.size, activeIndex)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        slots.forEach { pageIndex ->
            if (pageIndex == null) {
                Text(
                    text = "…",
                    color = Color.White.copy(alpha = 0.52f),
                    style = MaterialTheme.typography.labelSmall,
                )
            } else {
                val page = pages[pageIndex]
                val selected = page.id == home.document.activePageId
                val system = WorkspacePagePolicy.isSystem(page)
                val baseColor = if (system) Sky else Color.White
                Surface(
                    modifier = Modifier
                        .size(if (selected) 9.dp else 7.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = buildString {
                                append(page.title)
                                append(", Seite ${pageIndex + 1} von ${pages.size}")
                                if (system) append(", KAL-Bereich") else append(", persönliche Seite")
                            }
                        }
                        .clickable { home.activatePage(page.id) },
                    shape = CircleShape,
                    color = if (selected) baseColor else baseColor.copy(alpha = 0.38f),
                ) {}
            }
        }
    }
}

@Composable
private fun ReferenceHomeDock(
    controller: LauncherController,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.widthIn(max = 440.dp),
        color = DeepSurface.copy(alpha = 0.76f),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = controller::openDrawer) {
                Icon(Icons.Rounded.Apps, contentDescription = "Alle Apps", tint = Color.White)
            }
            val pinned = controller.pinnedAppKeys
                .mapNotNull { key -> controller.apps.firstOrNull { it.key == key } }
                .take(5)
            pinned.forEach { app ->
                IconButton(onClick = { controller.launch(app) }) {
                    Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(38.dp))
                }
            }
            if (controller.stylusState.present) {
                IconButton(onClick = controller::openPenSpace) {
                    Icon(Icons.Rounded.Draw, contentDescription = "Pen Space", tint = Mint)
                }
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Rounded.Add, contentDescription = "Zum Homescreen hinzufügen", tint = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReferenceAddToHomeSheet(
    controller: LauncherController,
    home: WorkspaceHomeController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(ReferenceAddTab.APPS) }
    var query by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DeepSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Zum Home hinzufügen", style = MaterialTheme.typography.headlineSmall)
            Text("Apps, Ordner und Widgets bleiben frei platzierbar im Home Studio.", color = MutedMist)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = tab == ReferenceAddTab.APPS,
                    onClick = { tab = ReferenceAddTab.APPS },
                    label = { Text("Apps") },
                    leadingIcon = { Icon(Icons.Rounded.Apps, contentDescription = null) },
                )
                FilterChip(
                    selected = tab == ReferenceAddTab.FOLDERS,
                    onClick = { tab = ReferenceAddTab.FOLDERS },
                    label = { Text("Ordner") },
                    leadingIcon = { Icon(Icons.Rounded.Folder, contentDescription = null) },
                )
                FilterChip(
                    selected = tab == ReferenceAddTab.WIDGETS,
                    onClick = { tab = ReferenceAddTab.WIDGETS },
                    label = { Text("Widgets") },
                    leadingIcon = { Icon(Icons.Rounded.Widgets, contentDescription = null) },
                )
            }
            if (tab != ReferenceAddTab.WIDGETS) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Suchen") },
                    singleLine = true,
                )
            }
            when (tab) {
                ReferenceAddTab.APPS -> {
                    val visible = controller.apps
                        .filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
                        .sortedBy { it.label.lowercase(Locale.ROOT) }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(visible, key = { it.key }) { app ->
                            Surface(color = RaisedSurface, shape = RoundedCornerShape(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(44.dp))
                                    Text(app.label, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                    OutlinedButton(onClick = { home.addApp(app.key) }) { Text("Hinzufügen") }
                                }
                            }
                        }
                    }
                }
                ReferenceAddTab.FOLDERS -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(controller.folders, key = { it.id }) { folder ->
                            Surface(color = RaisedSurface, shape = RoundedCornerShape(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Icon(Icons.Rounded.Folder, contentDescription = null, tint = Sky)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(folder.title, fontWeight = FontWeight.SemiBold)
                                        Text("${folder.appKeys.size} Apps", color = MutedMist, style = MaterialTheme.typography.bodySmall)
                                    }
                                    OutlinedButton(onClick = { home.addFolder(folder.id) }) { Text("Hinzufügen") }
                                }
                            }
                        }
                    }
                }
                ReferenceAddTab.WIDGETS -> {
                    Surface(color = RaisedSurface, shape = RoundedCornerShape(20.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("Android-Widgets", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Android übernimmt Auswahl und Konfiguration. KAL speichert nur die portable Platzierung; die Geräte-ID bleibt lokal.",
                                color = MutedMist,
                            )
                            Button(
                                onClick = {
                                    launchWorkspaceWidgetPicker(context)
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Widget auswählen")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReferenceFolderSheet(
    controller: LauncherController,
    folderId: String,
    onDismiss: () -> Unit,
) {
    val folder = controller.folders.firstOrNull { it.id == folderId }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DeepSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.62f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(folder?.title ?: "Ordner", style = MaterialTheme.typography.headlineSmall)
            val apps = folder?.appKeys.orEmpty().mapNotNull { key -> controller.apps.firstOrNull { it.key == key } }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(apps, key = { it.key }) { app ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { controller.launch(app) },
                        color = RaisedSurface,
                        shape = RoundedCornerShape(17.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(42.dp))
                            Text(app.label, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (apps.isEmpty()) {
                    item { Text("Keine aktuell verfügbaren Apps in diesem Ordner.", color = MutedMist) }
                }
            }
        }
    }
}

private enum class ReferenceAddTab { APPS, FOLDERS, WIDGETS }
