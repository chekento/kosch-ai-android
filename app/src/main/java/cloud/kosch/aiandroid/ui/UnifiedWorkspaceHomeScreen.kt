package cloud.kosch.aiandroid.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.LauncherController
import cloud.kosch.aiandroid.WorkspaceHomeController
import cloud.kosch.aiandroid.ai.SmartCollection
import cloud.kosch.aiandroid.model.AdaptiveHomePresentation
import cloud.kosch.aiandroid.model.AdaptiveHomePresentationPolicy
import cloud.kosch.aiandroid.model.AdaptiveLauncherEnvironment
import cloud.kosch.aiandroid.model.AdaptiveLauncherPlanner
import cloud.kosch.aiandroid.model.AppProfile
import cloud.kosch.aiandroid.model.DefaultWorkspace
import cloud.kosch.aiandroid.model.LabelMode
import cloud.kosch.aiandroid.model.LaunchableApp
import cloud.kosch.aiandroid.model.LauncherFolder
import cloud.kosch.aiandroid.model.TileAction
import cloud.kosch.aiandroid.model.WorkspaceItem
import cloud.kosch.aiandroid.model.WorkspaceItemContent
import cloud.kosch.aiandroid.model.WorkspaceObjectStyleResolver
import cloud.kosch.aiandroid.model.WorkspacePage
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Ink
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Warm
import java.util.Locale
import kotlin.math.roundToInt

/**
 * First user-facing surface backed directly by WorkspaceDocument v7.
 *
 * Every placement operation remains available through explicit controls so keyboard and accessibility users are
 * not dependent on direct manipulation. V7 widgets keep their Android host ids outside the portable document.
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
    val askFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var askText by rememberSaveable { mutableStateOf("") }
    var editMode by remember { mutableStateOf(false) }
    var addVisible by remember { mutableStateOf(false) }
    var pageDialog by remember { mutableStateOf<PageDialogMode?>(null) }
    var itemEditorId by remember { mutableStateOf<String?>(null) }
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
    LaunchedEffect(controller.commandFocusRequest) {
        if (controller.commandFocusRequest > 0L) {
            askFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val submitAsk: () -> Unit = {
        val command = askText.trim()
        if (command.isNotEmpty()) {
            controller.submitCommand(command, requestVoiceInput, requestDocument, requestContact)
            askText = ""
            keyboardController?.hide()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { contentPadding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Ink)
                .padding(contentPadding),
        ) {
            val hasStylus = controller.stylusState.present
            val adaptivePlan = remember(maxWidth, maxHeight, hasStylus) {
                AdaptiveLauncherPlanner.plan(
                    AdaptiveLauncherEnvironment(
                        widthDp = maxWidth.value.roundToInt().coerceAtLeast(1),
                        heightDp = maxHeight.value.roundToInt().coerceAtLeast(1),
                        hasStylus = hasStylus,
                    ),
                )
            }
            val presentation = remember(adaptivePlan, hasStylus) {
                AdaptiveHomePresentationPolicy.from(adaptivePlan, hasStylus)
            }

            NeuralGlassBackground(Modifier.fillMaxSize())
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = presentation.horizontalPaddingDp.dp,
                        vertical = presentation.verticalPaddingDp.dp,
                    )
                    .padding(end = if (presentation.showEdgePowerRail) 62.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(presentation.verticalGapDp.dp),
            ) {
                UnifiedHomeHeader(
                    controller = controller,
                    home = home,
                    editMode = editMode,
                    onEditModeChange = { editMode = it },
                    onAdd = { addVisible = true },
                    onPageManage = { pageDialog = PageDialogMode.MANAGE },
                )
                PageRail(home)
                UnifiedGrid(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    controller = controller,
                    home = home,
                    editMode = editMode,
                    onEditItem = { itemEditorId = it },
                    onOpenFolder = { folderPreviewId = it },
                    onAsk = {
                        askFocusRequester.requestFocus()
                        keyboardController?.show()
                    },
                )
                UnifiedHomeDock(
                    controller = controller,
                    presentation = presentation,
                    onOpenApps = { controller.openDrawer() },
                    onAsk = {
                        askFocusRequester.requestFocus()
                        keyboardController?.show()
                    },
                )
                UnifiedCommandDock(
                    text = askText,
                    onTextChange = { askText = it.take(MAX_COMMAND_LENGTH) },
                    focusRequester = askFocusRequester,
                    onSubmit = submitAsk,
                    requestVoiceInput = requestVoiceInput,
                )
            }

            AdaptiveEdgePowerRail(
                presentation = presentation,
                onOpenApps = controller::openDrawer,
                onAsk = {
                    askFocusRequester.requestFocus()
                    keyboardController?.show()
                },
                onControls = controller::openControlCenter,
                onPen = controller::openPenSpace,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
            )
        }
    }

    if (addVisible) {
        AddToHomeSheet(
            controller = controller,
            home = home,
            onDismiss = { addVisible = false },
        )
    }
    pageDialog?.let { mode ->
        PageManagementDialog(
            mode = mode,
            home = home,
            onDismiss = { pageDialog = null },
        )
    }
    itemEditorId?.let { itemId ->
        WorkspaceItemEditor(
            home = home,
            itemId = itemId,
            onDismiss = { itemEditorId = null },
        )
    }
    folderPreviewId?.let { folderId ->
        InlineFolderSheet(
            controller = controller,
            folderId = folderId,
            onDismiss = { folderPreviewId = null },
        )
    }
}

@Composable
private fun UnifiedHomeHeader(
    controller: LauncherController,
    home: WorkspaceHomeController,
    editMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    onAdd: () -> Unit,
    onPageManage: () -> Unit,
) {
    Surface(
        color = DeepSurface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(modifier = Modifier.size(42.dp), color = Mint.copy(alpha = 0.14f), shape = CircleShape) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Home, contentDescription = null, tint = Mint)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    home.activePage.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (home.isUserPage()) "V7 HOME · frei platzierbar" else "KOMPATIBILITÄTSSEITE · Szene ${home.activePage.sceneAdapter?.title}",
                    color = if (home.isUserPage()) Mint else Sky,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            IconButton(
                onClick = { onEditModeChange(!editMode) },
                modifier = Modifier.clip(CircleShape).background(if (editMode) Sky.copy(alpha = 0.16f) else Color.Transparent),
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = if (editMode) "Edit-Modus beenden" else "Homescreen bearbeiten")
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Rounded.Add, contentDescription = "App, Ordner, Widget oder Seite hinzufügen")
            }
            IconButton(onClick = onPageManage) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Home-Seiten verwalten")
            }
            IconButton(onClick = controller::openControlCenter) {
                Icon(Icons.Rounded.Tune, contentDescription = "Kontrollzentrum")
            }
        }
    }
}

@Composable
private fun PageRail(home: WorkspaceHomeController) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val pages = home.document.pages
        val activeIndex = pages.indexOfFirst { it.id == home.document.activePageId }
        IconButton(
            enabled = activeIndex > 0,
            onClick = { home.activatePage(pages[activeIndex - 1].id) },
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Vorherige Home-Seite")
        }
        pages.forEach { page ->
            FilterChip(
                selected = page.id == home.document.activePageId,
                onClick = { home.activatePage(page.id) },
                label = {
                    Text(
                        if (page.sceneAdapter == null) page.title else "${page.sceneAdapter.title} · Szene",
                        maxLines = 1,
                    )
                },
            )
        }
        IconButton(
            enabled = activeIndex in 0 until pages.lastIndex,
            onClick = { home.activatePage(pages[activeIndex + 1].id) },
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Nächste Home-Seite")
        }
    }
}

@Composable
private fun UnifiedGrid(
    modifier: Modifier,
    controller: LauncherController,
    home: WorkspaceHomeController,
    editMode: Boolean,
    onEditItem: (String) -> Unit,
    onOpenFolder: (String) -> Unit,
    onAsk: () -> Unit,
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

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(DeepSurface.copy(alpha = 0.91f), Color(0xFF08131C).copy(alpha = 0.96f)),
                ),
            )
            .padding(6.dp),
    ) {
        val cellWidth = maxWidth / home.document.grid.columns
        val cellHeight = maxHeight / home.document.grid.rows

        if (styledItems.none { (_, style) -> style.visible }) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = Mint, modifier = Modifier.size(36.dp))
                if (page.items.isEmpty()) {
                    Text("Diese Home-Seite ist noch leer", fontWeight = FontWeight.SemiBold)
                    Text("Tippe oben auf + und platziere Apps, Ordner oder Widgets.", color = MutedMist)
                } else {
                    Text("Alle Elemente dieser Seite sind ausgeblendet", fontWeight = FontWeight.SemiBold)
                    Text("Öffne Home Studio, um Ghost-Objekte zu sehen und wieder einzublenden.", color = MutedMist)
                }
            }
        }

        styledItems.forEach { (item, objectStyle) ->
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
                    is WorkspaceItemContent.ActionTile -> LegacyActionItem(
                        item = item,
                        content = content,
                        controller = controller,
                        onAsk = onAsk,
                    )
                    is WorkspaceItemContent.App -> AppHomeItem(
                        item = item,
                        app = controller.apps.firstOrNull { it.key == content.appKey },
                        editMode = editMode,
                        onClick = {
                            val app = controller.apps.firstOrNull { it.key == content.appKey }
                            if (app != null) controller.launch(app) else onEditItem(item.id)
                        },
                        onEdit = { onEditItem(item.id) },
                    )
                    is WorkspaceItemContent.Folder -> FolderHomeItem(
                        item = item,
                        folder = controller.folders.firstOrNull { it.id == content.folderId },
                        editMode = editMode,
                        onClick = { onOpenFolder(content.folderId) },
                        onEdit = { onEditItem(item.id) },
                    )
                    is WorkspaceItemContent.Widget -> WorkspaceWidgetHomeItem(
                        item = item,
                        home = home,
                        editMode = editMode,
                        onEdit = { onEditItem(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LegacyActionItem(
    item: WorkspaceItem,
    content: WorkspaceItemContent.ActionTile,
    controller: LauncherController,
    onAsk: () -> Unit,
) {
    val tile = remember(content.scene, content.legacyTileId) {
        DefaultWorkspace.tiles(content.scene).firstOrNull { it.id == content.legacyTileId }
    }
    val style = LocalWorkspaceObjectStyle.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = tile?.title ?: content.legacyTileId
            }
            .clickable {
                when (content.action) {
                    TileAction.ASK -> onAsk()
                    TileAction.APPS -> controller.openDrawer()
                    TileAction.CONTEXT -> controller.showContextDetails()
                    TileAction.PROVIDERS -> controller.openProviderChooser()
                    TileAction.FOCUS -> controller.openDrawer(SmartCollection.WORK)
                    TileAction.MEDIA -> controller.openDrawer(SmartCollection.MEDIA)
                    TileAction.COMMUNICATION -> controller.openDrawer(SmartCollection.COMMUNICATION)
                    TileAction.TOOLS -> controller.openControlCenter()
                }
            },
    ) {
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = if (style.showLabel) Arrangement.SpaceBetween else Arrangement.Center,
        ) {
            Text(
                tile?.glyph ?: "◇",
                modifier = Modifier.scale(style.iconScale),
                color = Mint,
                style = MaterialTheme.typography.headlineSmall,
            )
            if (style.showLabel) {
                Column(modifier = Modifier.scale(style.labelScale)) {
                    Text(tile?.title ?: content.legacyTileId, fontWeight = FontWeight.SemiBold)
                    Text(tile?.subtitle ?: "Legacy action", color = MutedMist, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun AppHomeItem(
    item: WorkspaceItem,
    app: LaunchableApp?,
    editMode: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
) {
    val style = LocalWorkspaceObjectStyle.current
    val editModifier = if (editMode) {
        Modifier.border(1.dp, Sky.copy(alpha = 0.62f), RoundedCornerShape(18.dp))
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(editModifier)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = buildString {
                    append(app?.label ?: "Nicht verfügbare App")
                    if (editMode) append(". Tippen zum Verschieben oder Entfernen")
                }
            }
            .clickable(onClick = if (editMode) onEdit else onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (app != null) {
                Image(
                    bitmap = app.icon,
                    contentDescription = null,
                    modifier = Modifier.size((42f * style.iconScale).dp),
                )
                if (style.showLabel) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        app.label,
                        modifier = Modifier.scale(style.labelScale),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    if (app.profile != AppProfile.PERSONAL) {
                        Text(
                            app.profile.title,
                            modifier = Modifier.scale(style.labelScale),
                            color = Sky,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            } else {
                Icon(Icons.Rounded.Apps, contentDescription = null, tint = Warm)
                Text("App fehlt", color = Warm, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun FolderHomeItem(
    item: WorkspaceItem,
    folder: LauncherFolder?,
    editMode: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
) {
    val style = LocalWorkspaceObjectStyle.current
    val editModifier = if (editMode) {
        Modifier.border(1.dp, Sky.copy(alpha = 0.62f), RoundedCornerShape(18.dp))
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(editModifier)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "Ordner ${folder?.title ?: "nicht verfügbar"}"
            }
            .clickable(onClick = if (editMode) onEdit else onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Rounded.Folder,
                contentDescription = null,
                tint = Sky,
                modifier = Modifier.size((34f * style.iconScale).dp),
            )
            if (style.showLabel) {
                Spacer(Modifier.height(4.dp))
                Text(
                    folder?.title ?: "Ordner fehlt",
                    modifier = Modifier.scale(style.labelScale),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                )
                folder?.let {
                    Text(
                        "${it.appKeys.size} Apps",
                        modifier = Modifier.scale(style.labelScale),
                        color = MutedMist,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun UnifiedHomeDock(
    controller: LauncherController,
    presentation: AdaptiveHomePresentation,
    onOpenApps: () -> Unit,
    onAsk: () -> Unit,
) {
    Surface(color = DeepSurface.copy(alpha = 0.95f), shape = RoundedCornerShape(22.dp), tonalElevation = 5.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onOpenApps) {
                Icon(Icons.Rounded.Apps, contentDescription = "Alle Apps")
            }
            val pinned = controller.pinnedAppKeys.mapNotNull { key -> controller.apps.firstOrNull { it.key == key } }
            pinned.take(presentation.dockPinnedAppLimit).forEach { app ->
                IconButton(onClick = { controller.launch(app) }) {
                    Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(34.dp))
                }
            }
            if (presentation.showPenShortcut) {
                IconButton(
                    onClick = controller::openPenSpace,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (presentation.emphasizePenShortcut) Mint.copy(alpha = 0.16f) else Color.Transparent,
                        ),
                ) {
                    Icon(
                        Icons.Rounded.Draw,
                        contentDescription = if (presentation.emphasizePenShortcut) {
                            "Pen Space · für Stift priorisiert"
                        } else {
                            "Pen Space"
                        },
                        tint = if (presentation.emphasizePenShortcut) Mint else Sky,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (!presentation.showEdgePowerRail) {
                Button(onClick = onAsk) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Ask")
                }
            }
        }
    }
}

@Composable
private fun UnifiedCommandDock(
    text: String,
    onTextChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onSubmit: () -> Unit,
    requestVoiceInput: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = DeepSurface.copy(alpha = 0.95f),
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                label = { Text("⌘ Ask") },
                placeholder = { Text("App öffnen, Szene wechseln oder KI wählen") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                trailingIcon = {
                    IconButton(onClick = requestVoiceInput) {
                        Icon(Icons.Rounded.KeyboardVoice, contentDescription = "Spracheingabe")
                    }
                },
            )
            IconButton(onClick = onSubmit, enabled = text.isNotBlank()) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Ausführen")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToHomeSheet(
    controller: LauncherController,
    home: WorkspaceHomeController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(AddTab.APPS) }
    var query by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DeepSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Zum Homescreen", style = MaterialTheme.typography.headlineSmall)
                    Text("Platzierung erfolgt lokal im v7-Raster", color = MutedMist)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Schließen") }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(selected = tab == AddTab.APPS, onClick = { tab = AddTab.APPS }, label = { Text("Apps") })
                FilterChip(selected = tab == AddTab.FOLDERS, onClick = { tab = AddTab.FOLDERS }, label = { Text("Ordner") })
                FilterChip(selected = tab == AddTab.WIDGETS, onClick = { tab = AddTab.WIDGETS }, label = { Text("Widgets") })
                AssistChip(onClick = { home.createPage() }, label = { Text("+ Seite") })
            }
            if (tab != AddTab.WIDGETS) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Suchen") },
                    singleLine = true,
                )
            }
            when (tab) {
                AddTab.APPS -> {
                    val visible = controller.apps
                        .filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
                        .sortedBy { it.label.lowercase(Locale.ROOT) }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(visible, key = { it.key }) { app ->
                            Surface(color = RaisedSurface, shape = RoundedCornerShape(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(42.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(app.label, fontWeight = FontWeight.SemiBold)
                                        Text(app.profile.title, color = MutedMist, style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(onClick = { home.addApp(app.key) }) { Text("Hinzufügen") }
                                }
                            }
                        }
                    }
                }
                AddTab.FOLDERS -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(controller.folders, key = { it.id }) { folder ->
                            Surface(color = RaisedSurface, shape = RoundedCornerShape(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(Icons.Rounded.Folder, contentDescription = null, tint = Sky)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(folder.title, fontWeight = FontWeight.SemiBold)
                                        Text("${folder.appKeys.size} Apps", color = MutedMist, style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(onClick = { home.addFolder(folder.id) }) { Text("Hinzufügen") }
                                }
                            }
                        }
                        if (controller.folders.isEmpty()) {
                            item { Text("Noch keine Launcher-Ordner vorhanden.", color = MutedMist) }
                        }
                    }
                }
                AddTab.WIDGETS -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = RaisedSurface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Sky.copy(alpha = 0.24f)),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("Android-Widgets", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Die Auswahl und Konfiguration übernimmt Android. KoSch speichert nur Provider und Rasterposition portabel; die Host-ID bleibt auf diesem Gerät.",
                                color = MutedMist,
                                style = MaterialTheme.typography.bodySmall,
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

@Composable
private fun PageManagementDialog(
    mode: PageDialogMode,
    home: WorkspaceHomeController,
    onDismiss: () -> Unit,
) {
    var title by remember(home.activePage.id) { mutableStateOf(home.activePage.title) }
    val userPage = home.isUserPage()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Home-Seite verwalten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (userPage) "Freie v7-Seite · ${home.activePage.items.size} Elemente" else "Legacy-Szenenseite · geschützt während der Migration",
                    color = MutedMist,
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(160) },
                    enabled = userPage,
                    label = { Text("Seitentitel") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { home.moveActivePage(-1) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                        Text("Links")
                    }
                    OutlinedButton(onClick = { home.moveActivePage(1) }) {
                        Text("Rechts")
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                    }
                }
                if (home.canUndo) {
                    OutlinedButton(onClick = home::undo, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Letzte Änderung rückgängig")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = userPage && title.isNotBlank(),
                onClick = {
                    home.renameActivePage(title)
                    onDismiss()
                },
            ) { Text("Speichern") }
        },
        dismissButton = {
            Row {
                if (userPage) {
                    TextButton(
                        onClick = {
                            home.deleteActiveUserPage()
                            onDismiss()
                        },
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Warm)
                        Text("Löschen", color = Warm)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Schließen") }
            }
        },
    )
}

@Composable
private fun WorkspaceItemEditor(
    home: WorkspaceHomeController,
    itemId: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val item = home.activePage.items.firstOrNull { it.id == itemId }
    if (item == null) {
        LaunchedEffect(itemId) { onDismiss() }
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Element bearbeiten") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Raster: ${item.bounds.column}, ${item.bounds.row} · ${item.bounds.columnSpan}×${item.bounds.rowSpan}", color = MutedMist)
                if (item.content is WorkspaceItemContent.Widget) {
                    OutlinedButton(
                        onClick = {
                            launchWorkspaceWidgetPicker(context, itemId)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Widget neu zuordnen")
                    }
                }
                IconButton(onClick = { home.moveItemBy(itemId, 0, -1) }) {
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Element nach oben verschieben")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    IconButton(onClick = { home.moveItemBy(itemId, -1, 0) }) {
                        Icon(Icons.Rounded.KeyboardArrowLeft, contentDescription = "Element nach links verschieben")
                    }
                    IconButton(onClick = { home.moveItemBy(itemId, 1, 0) }) {
                        Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = "Element nach rechts verschieben")
                    }
                }
                IconButton(onClick = { home.moveItemBy(itemId, 0, 1) }) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Element nach unten verschieben")
                }
                Text("Kollisionen werden deterministisch auf den nächsten freien Bereich umgeleitet.", color = MutedMist, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    home.removeItem(itemId)
                    onDismiss()
                },
            ) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Warm)
                Text("Vom Home entfernen", color = Warm)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fertig") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InlineFolderSheet(
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
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.65f).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Folder, contentDescription = null, tint = Sky)
                Spacer(Modifier.width(8.dp))
                Text(folder?.title ?: "Ordner nicht verfügbar", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Schließen") }
            }
            val apps = folder?.appKeys.orEmpty().mapNotNull { key -> controller.apps.firstOrNull { it.key == key } }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(apps, key = { it.key }) { app ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { controller.launch(app) },
                        color = RaisedSurface,
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(app.label, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (apps.isEmpty()) item { Text("Keine aktuell verfügbaren Apps in diesem Ordner.", color = MutedMist) }
            }
        }
    }
}

private const val MAX_COMMAND_LENGTH = 4_096
private enum class AddTab { APPS, FOLDERS, WIDGETS }
private enum class PageDialogMode { MANAGE }
