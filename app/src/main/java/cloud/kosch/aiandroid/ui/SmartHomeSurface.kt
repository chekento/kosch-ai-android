package cloud.kosch.aiandroid.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cloud.kosch.aiandroid.LauncherController
import cloud.kosch.aiandroid.data.WorkspaceCollectionEditor
import cloud.kosch.aiandroid.model.HomePage
import cloud.kosch.aiandroid.model.FolderKind
import cloud.kosch.aiandroid.model.LaunchableApp
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Violet

@Composable
fun HomePageSelector(controller: LauncherController) {
    val pages = HomePage.entries.filter { page ->
        page != HomePage.PEN_SPACE || controller.stylusState.present || controller.homePage == HomePage.PEN_SPACE
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        pages.forEach { page ->
            FilterChip(
                selected = controller.homePage == page,
                onClick = { controller.switchHomePage(page) },
                label = { Text(page.title) },
                leadingIcon = when (page) {
                    HomePage.PRO_DESK -> {
                        { Icon(Icons.Rounded.BusinessCenter, contentDescription = null, modifier = Modifier.size(17.dp)) }
                    }

                    HomePage.SMART_SPACE -> {
                        { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(17.dp)) }
                    }

                    HomePage.PEN_SPACE -> {
                        { Icon(Icons.Rounded.Draw, contentDescription = null, modifier = Modifier.size(17.dp)) }
                    }

                    HomePage.WORKSPACE -> null
                },
            )
        }
    }
}

@Composable
fun ColumnScope.SmartHomeSurface(controller: LauncherController) {
    val visibleFolders = controller.folderPreview ?: controller.folders
    var createFolderVisible by remember { mutableStateOf(false) }
    var folderTitle by remember { mutableStateOf("") }
    var folderKind by remember { mutableStateOf(FolderKind.OTHER) }
    Surface(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        color = DeepSurface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Smart Space", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "lokal geordnet · szenenbewusst · jederzeit änderbar",
                        color = MutedMist,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    TextButton(onClick = { createFolderVisible = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Neu")
                    }
                    OutlinedButton(onClick = controller::proposeSmartFolders) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Ordnen")
                    }
                }
            }

            if (controller.folderPreview != null) {
                Surface(color = Violet.copy(alpha = 0.14f), shape = RoundedCornerShape(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Vorschau – noch nicht gespeichert", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                        TextButton(onClick = controller::discardFolderPreview) { Text("Verwerfen") }
                        Button(onClick = controller::applyFolderPreview) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Anwenden")
                        }
                    }
                }
            }

            if (visibleFolders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Sky, modifier = Modifier.size(40.dp))
                        Text("Noch keine lokalen Sammlungen")
                        TextButton(onClick = controller::proposeSmartFolders) { Text("Vorschlag erstellen") }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(142.dp),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    items(visibleFolders, key = { it.id }) { folder ->
                        Card(
                            onClick = { if (controller.folderPreview == null) controller.openFolder(folder.id) },
                            colors = CardDefaults.cardColors(containerColor = RaisedSurface),
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().height(112.dp).padding(13.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(folder.kind.glyph, color = Mint, style = MaterialTheme.typography.headlineSmall)
                                    Spacer(Modifier.weight(1f))
                                    Text("${controller.folderApps(folder).size}", color = Sky, style = MaterialTheme.typography.labelLarge)
                                }
                                Column {
                                    Text(folder.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        if (folder.generatedLocally) "Lokale Smart-Sammlung" else "Eigene Sammlung",
                                        color = MutedMist,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (createFolderVisible) {
        Dialog(onDismissRequest = { createFolderVisible = false }) {
            Surface(color = DeepSurface, shape = RoundedCornerShape(26.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    Text("Eigene Sammlung", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Name und Typ bleiben ausschließlich lokal.", color = MutedMist)
                    OutlinedTextField(
                        value = folderTitle,
                        onValueChange = { folderTitle = it.take(WorkspaceCollectionEditor.MAX_TITLE_LENGTH) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ordnername") },
                        singleLine = true,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        FolderKind.entries.forEach { kind ->
                            FilterChip(
                                selected = folderKind == kind,
                                onClick = { folderKind = kind },
                                label = { Text("${kind.glyph} ${kind.title}") },
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(onClick = { createFolderVisible = false }, modifier = Modifier.weight(1f)) {
                            Text("Abbrechen")
                        }
                        Button(
                            onClick = {
                                controller.createFolder(folderTitle, folderKind)
                                folderTitle = ""
                                folderKind = FolderKind.OTHER
                                createFolderVisible = false
                            },
                            enabled = WorkspaceCollectionEditor.normalizedTitle(folderTitle) != null,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Erstellen")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PersistentSmartDock(controller: LauncherController) {
    val dockApps = controller.smartDockApps()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = DeepSurface.copy(alpha = 0.96f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dockApps.forEach { app ->
                DockApp(
                    app = app,
                    badgeCount = controller.notificationCounts[app.packageName] ?: 0,
                    pinned = app.key in controller.pinnedAppKeys,
                    modifier = Modifier.weight(1f),
                    onClick = { controller.launch(app) },
                    onLongClick = { controller.showAppActions(app) },
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .combinedClickable(onClick = { controller.openDrawer() }, onLongClick = { controller.openDrawer() })
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Apps, contentDescription = "Alle Apps", tint = Sky, modifier = Modifier.size(36.dp))
                    Text("Apps", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockApp(
    app: LaunchableApp,
    badgeCount: Int,
    pinned: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = buildString {
                    append(app.label)
                    if (badgeCount > 0) append(", $badgeCount Benachrichtigungen")
                    if (pinned) append(", angeheftet")
                    append(". Tippen zum Öffnen, lange drücken für App-Aktionen")
                }
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Image(app.icon, contentDescription = app.label, modifier = Modifier.size(38.dp))
            if (badgeCount > 0) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).size(if (badgeCount > 9) 20.dp else 16.dp),
                    color = Mint,
                    shape = CircleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (badgeCount > 99) "99+" else badgeCount.toString(), color = DeepSurface, style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else if (pinned) {
                Icon(
                    Icons.Rounded.PushPin,
                    contentDescription = "Fest angeheftet",
                    tint = Mint,
                    modifier = Modifier.align(Alignment.TopEnd).size(14.dp).background(DeepSurface, CircleShape),
                )
            }
        }
        Text(app.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FolderSheet(controller: LauncherController) {
    val folder = controller.selectedFolder() ?: return
    val apps = controller.folderApps(folder)
    var managing by remember(folder.id) { mutableStateOf(false) }
    var titleDraft by remember(folder.id, folder.title) { mutableStateOf(folder.title) }
    var confirmDelete by remember(folder.id) { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = controller::closeFolder,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DeepSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.82f).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(folder.kind.glyph, color = Mint, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(folder.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${apps.size} Apps · ${if (folder.generatedLocally) "lokal klassifiziert" else "eigene Sammlung"}",
                        color = MutedMist,
                    )
                }
                IconButton(onClick = { managing = !managing }) {
                    Icon(Icons.Rounded.Edit, contentDescription = if (managing) "Bearbeitung schließen" else "Ordner bearbeiten")
                }
            }
            if (managing) {
                OutlinedTextField(
                    value = titleDraft,
                    onValueChange = { titleDraft = it.take(WorkspaceCollectionEditor.MAX_TITLE_LENGTH) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ordnername") },
                    trailingIcon = {
                        TextButton(
                            onClick = { controller.renameFolder(folder.id, titleDraft) },
                            enabled = WorkspaceCollectionEditor.normalizedTitle(titleDraft) != null && titleDraft.trim() != folder.title,
                        ) { Text("Speichern") }
                    },
                    singleLine = true,
                )
                Text(
                    "Mit den Pfeilen bestimmst du die Reihenfolge. Entfernst du die letzte App, wird der leere Ordner dauerhaft gelöscht.",
                    color = MutedMist,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (apps.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(apps, key = { _, app -> app.key }) { index, app ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = RaisedSurface),
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Image(app.icon, contentDescription = null, modifier = Modifier.size(40.dp))
                                    Text(app.label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    IconButton(
                                        onClick = { controller.moveFolderApp(folder.id, app.key, -1) },
                                        enabled = index > 0,
                                    ) {
                                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "${app.label} nach oben")
                                    }
                                    IconButton(
                                        onClick = { controller.moveFolderApp(folder.id, app.key, 1) },
                                        enabled = index < apps.lastIndex,
                                    ) {
                                        Icon(Icons.Rounded.ArrowDownward, contentDescription = "${app.label} nach unten")
                                    }
                                    IconButton(onClick = { controller.removeAppFromFolder(folder.id, app.key) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "${app.label} aus Ordner entfernen")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                OutlinedButton(
                    onClick = {
                        if (confirmDelete) controller.removeFolder(folder.id) else confirmDelete = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text(if (confirmDelete) "Ordner wirklich entfernen" else "Ordner entfernen …")
                }
            } else if (apps.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Keine installierten Apps in dieser Sammlung", color = MutedMist)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(92.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    items(apps, key = { it.key }) { app ->
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .combinedClickable(
                                    onClick = { controller.launch(app) },
                                    onLongClick = { controller.showAppActions(app) },
                                )
                                .padding(9.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Image(app.icon, contentDescription = app.label, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(5.dp))
                            Text(app.label, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
