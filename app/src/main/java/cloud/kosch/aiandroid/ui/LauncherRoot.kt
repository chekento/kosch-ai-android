package cloud.kosch.aiandroid.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.kosch.aiandroid.LauncherController
import cloud.kosch.aiandroid.ai.AiProviderProfile
import cloud.kosch.aiandroid.ai.AiProviderRegistry
import cloud.kosch.aiandroid.ai.SmartCollection
import cloud.kosch.aiandroid.model.ContextSnapshot
import cloud.kosch.aiandroid.model.PositionedTile
import cloud.kosch.aiandroid.model.SceneId
import cloud.kosch.aiandroid.model.TileAction
import cloud.kosch.aiandroid.model.TilePosition
import cloud.kosch.aiandroid.model.WorkspaceMode
import cloud.kosch.aiandroid.ui.components.CompanionFace
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Ink
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Violet
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherRoot(
    controller: LauncherController,
    requestHomeRole: () -> Unit,
    requestVoiceInput: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val askFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var askText by remember { mutableStateOf("") }
    val notice = controller.notice

    LaunchedEffect(notice) {
        if (notice != null) {
            snackbarHostState.showSnackbar(notice)
            controller.consumeNotice()
        }
    }

    BackHandler(
        enabled = controller.drawerVisible || controller.providerChooserVisible || controller.contextDetailsVisible,
    ) {
        when {
            controller.providerChooserVisible -> controller.closeProviderChooser()
            controller.contextDetailsVisible -> controller.hideContextDetails()
            controller.drawerVisible -> controller.closeDrawer()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF173548), Ink),
                        center = Offset(140f, 100f),
                        radius = 1_150f,
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .systemBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LauncherHeader(
                    controller = controller,
                    requestHomeRole = requestHomeRole,
                )
                SceneSelector(
                    activeScene = controller.activeScene,
                    suggestedScene = controller.contextSnapshot.suggestedScene,
                    onSceneSelected = controller::switchScene,
                    onUseSuggestion = controller::useSuggestedScene,
                )
                EditToolbar(controller)
                WorkspaceSurface(
                    controller = controller,
                    onAsk = {
                        askFocusRequester.requestFocus()
                        keyboardController?.show()
                    },
                )
                AskDock(
                    text = askText,
                    onTextChange = { askText = it },
                    focusRequester = askFocusRequester,
                    onSubmit = {
                        controller.submitCommand(askText, requestVoiceInput)
                        askText = ""
                        keyboardController?.hide()
                    },
                    requestVoiceInput = requestVoiceInput,
                )
            }
        }
    }

    if (controller.drawerVisible) {
        AppDrawerSheet(controller)
    }
    if (controller.providerChooserVisible) {
        ProviderChooserSheet(controller)
    }
    if (controller.contextDetailsVisible) {
        ContextDetailsSheet(
            snapshot = controller.contextSnapshot,
            onUseSuggestion = {
                controller.useSuggestedScene()
                controller.hideContextDetails()
            },
            onDismiss = controller::hideContextDetails,
        )
    }
}

@Composable
private fun LauncherHeader(
    controller: LauncherController,
    requestHomeRole: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "KoSch AI",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "programmierbarer Android-Workspace",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            ModeToggle(
                mode = controller.workspaceMode,
                onModeSelected = controller::setWorkspaceMode,
            )
        }

        if (!controller.isDefaultHome) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Rounded.Home, contentDescription = null)
                    Text(
                        text = "Noch nicht als Start-App gesetzt",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = requestHomeRole) {
                        Text("Festlegen")
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeToggle(
    mode: WorkspaceMode,
    onModeSelected: (WorkspaceMode) -> Unit,
) {
    Surface(
        color = RaisedSurface.copy(alpha = 0.82f),
        shape = CircleShape,
    ) {
        Row(modifier = Modifier.padding(3.dp)) {
            IconButton(
                onClick = { onModeSelected(WorkspaceMode.PLAY) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (mode == WorkspaceMode.PLAY) Mint.copy(alpha = 0.18f) else Color.Transparent),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play-Modus",
                    tint = if (mode == WorkspaceMode.PLAY) Mint else MutedMist,
                )
            }
            IconButton(
                onClick = { onModeSelected(WorkspaceMode.EDIT) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (mode == WorkspaceMode.EDIT) Sky.copy(alpha = 0.18f) else Color.Transparent),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit-Modus",
                    tint = if (mode == WorkspaceMode.EDIT) Sky else MutedMist,
                )
            }
        }
    }
}

@Composable
private fun SceneSelector(
    activeScene: SceneId,
    suggestedScene: SceneId,
    onSceneSelected: (SceneId) -> Unit,
    onUseSuggestion: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SceneId.entries.forEach { scene ->
                FilterChip(
                    selected = scene == activeScene,
                    onClick = { onSceneSelected(scene) },
                    label = { Text(scene.title) },
                )
            }
        }
        if (suggestedScene != activeScene) {
            AssistChip(
                onClick = onUseSuggestion,
                label = { Text("Lokal vorgeschlagen: ${suggestedScene.title}") },
                leadingIcon = {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Violet.copy(alpha = 0.12f),
                ),
            )
        }
    }
}

@Composable
private fun EditToolbar(controller: LauncherController) {
    if (controller.workspaceMode != WorkspaceMode.EDIT) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = controller::proposeSmartLayout) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Layout-Vorschau")
        }
        OutlinedButton(
            enabled = controller.canUndoLayout,
            onClick = controller::undoLayout,
        ) {
            Icon(Icons.Rounded.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Rückgängig")
        }
        OutlinedButton(onClick = controller::resetSceneLayout) {
            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Zurücksetzen")
        }
    }
}

@Composable
private fun ColumnScope.WorkspaceSurface(
    controller: LauncherController,
    onAsk: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        DeepSurface.copy(alpha = 0.92f),
                        Color(0xFF0A151E).copy(alpha = 0.96f),
                    ),
                ),
            ),
    ) {
        val density = LocalDensity.current
        val compact = maxWidth < 390.dp
        val tileWidth = if (compact) 142.dp else 168.dp
        val tileHeight = if (compact) 102.dp else 112.dp
        val tileWidthPx = with(density) { tileWidth.roundToPx() }
        val tileHeightPx = with(density) { tileHeight.roundToPx() }
        val availableX = (constraints.maxWidth - tileWidthPx).coerceAtLeast(1)
        val availableY = (constraints.maxHeight - tileHeightPx).coerceAtLeast(1)

        controller.currentTiles().forEach { positionedTile ->
            DraggableWorkspaceTile(
                positionedTile = positionedTile,
                tileWidthPx = availableX,
                tileHeightPx = availableY,
                cardWidth = tileWidth,
                cardHeight = tileHeight,
                editable = controller.workspaceMode == WorkspaceMode.EDIT,
                previewActive = controller.previewPositions != null,
                onMoved = { controller.moveTile(positionedTile.tile.id, it) },
                onClick = {
                    when (positionedTile.tile.action) {
                        TileAction.ASK -> onAsk()
                        TileAction.APPS -> controller.openDrawer()
                        TileAction.CONTEXT -> controller.showContextDetails()
                        TileAction.PROVIDERS -> controller.openProviderChooser()
                        TileAction.FOCUS -> controller.openDrawer(SmartCollection.WORK)
                        TileAction.MEDIA -> controller.openDrawer(SmartCollection.MEDIA)
                        TileAction.COMMUNICATION -> controller.openDrawer(SmartCollection.COMMUNICATION)
                        TileAction.TOOLS -> controller.openDrawer(SmartCollection.TOOLS)
                    }
                },
            )
        }

        if (controller.previewPositions != null) {
            PreviewDecisionBar(
                onApply = controller::applyLayoutPreview,
                onDiscard = controller::discardLayoutPreview,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(10.dp),
            )
        }
    }
}

@Composable
private fun DraggableWorkspaceTile(
    positionedTile: PositionedTile,
    tileWidthPx: Int,
    tileHeightPx: Int,
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
    editable: Boolean,
    previewActive: Boolean,
    onMoved: (TilePosition) -> Unit,
    onClick: () -> Unit,
) {
    var localX by remember(positionedTile.tile.id, positionedTile.position.x) {
        mutableFloatStateOf(positionedTile.position.x)
    }
    var localY by remember(positionedTile.tile.id, positionedTile.position.y) {
        mutableFloatStateOf(positionedTile.position.y)
    }

    Card(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (localX * tileWidthPx).roundToInt(),
                    y = (localY * tileHeightPx).roundToInt(),
                )
            }
            .size(cardWidth, cardHeight)
            .pointerInput(editable, previewActive, tileWidthPx, tileHeightPx) {
                if (editable && !previewActive) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            localX = (localX + dragAmount.x / tileWidthPx).coerceIn(0f, 1f)
                            localY = (localY + dragAmount.y / tileHeightPx).coerceIn(0f, 1f)
                        },
                        onDragEnd = { onMoved(TilePosition(localX, localY)) },
                    )
                }
            }
            .clickable(enabled = !editable, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (previewActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.86f)
            } else {
                RaisedSurface.copy(alpha = 0.92f)
            },
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    if (editable) Sky.copy(alpha = 0.65f) else Mint.copy(alpha = 0.34f),
                    Color.Transparent,
                ),
            ),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = positionedTile.tile.glyph,
                    color = Mint,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (editable) {
                    Text(
                        text = if (previewActive) "VORSCHAU" else "ZIEHEN",
                        color = if (previewActive) Mint else Sky,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Column {
                Text(
                    text = positionedTile.tile.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = positionedTile.tile.subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PreviewDecisionBar(
    onApply: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Ink.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Regelbasiert · lokal",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            Button(onClick = onApply) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Anwenden")
            }
            TextButton(onClick = onDiscard) {
                Text("Verwerfen")
            }
        }
    }
}

@Composable
private fun AskDock(
    text: String,
    onTextChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onSubmit: () -> Unit,
    requestVoiceInput: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = DeepSurface.copy(alpha = 0.95f),
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(9.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompanionFace(
                onClick = requestVoiceInput,
                modifier = Modifier.size(width = 78.dp, height = 66.dp),
            )
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                label = { Text("⌘ Ask") },
                placeholder = { Text("Öffne …, Szene … oder frage eine KI") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                trailingIcon = {
                    IconButton(onClick = requestVoiceInput) {
                        Icon(Icons.Rounded.KeyboardVoice, contentDescription = "Spracheingabe")
                    }
                },
            )
            IconButton(
                onClick = onSubmit,
                enabled = text.isNotBlank(),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Ausführen")
            }
        }
    }
    Text(
        text = "Befehle lokal geplant · Generative Texte nur nach Anbieterwahl",
        modifier = Modifier.padding(start = 100.dp, top = 2.dp),
        color = MutedMist,
        style = MaterialTheme.typography.labelSmall,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDrawerSheet(controller: LauncherController) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val visibleApps = remember(
        query,
        controller.apps,
        controller.drawerCollection,
        controller.recentPackages,
    ) {
        controller.rankedApps(query, controller.drawerCollection)
    }

    ModalBottomSheet(
        onDismissRequest = controller::closeDrawer,
        sheetState = sheetState,
        containerColor = DeepSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("App-Raum", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Mit LauncherApps indexiert · keine Vollzugriffs-Berechtigung",
                        color = MutedMist,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                IconButton(onClick = controller::closeDrawer) {
                    Icon(Icons.Rounded.Close, contentDescription = "Schließen")
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                label = { Text("Was möchtest du tun?") },
                singleLine = true,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SmartCollection.entries, key = { it.name }) { collection ->
                    FilterChip(
                        selected = controller.drawerCollection == collection,
                        onClick = { controller.setDrawerCollection(collection) },
                        label = { Text(collection.title) },
                    )
                }
            }

            when {
                controller.appsLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                visibleApps.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (query.isBlank()) {
                            "In dieser lokalen Sammlung ist noch nichts."
                        } else {
                            "Keine App passt zu „$query“."
                        },
                        color = MutedMist,
                    )
                }

                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(88.dp),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(visibleApps, key = { it.key }) { app ->
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { controller.launch(app) }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                            )
                            Text(
                                text = app.label,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderChooserSheet(controller: LauncherController) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = controller::closeProviderChooser,
        sheetState = sheetState,
        containerColor = DeepSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("KI-Ziel auswählen", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "KoSch entscheidet nicht heimlich für dich.",
                        color = MutedMist,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                IconButton(onClick = controller::closeProviderChooser) {
                    Icon(Icons.Rounded.Close, contentDescription = "Schließen")
                }
            }
            OutlinedTextField(
                value = controller.providerPrompt,
                onValueChange = controller::updateProviderPrompt,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Text für den Anbieter") },
                placeholder = { Text("Leer lassen, um den Anbieter nur zu öffnen") },
                minLines = 2,
                maxLines = 5,
            )
            Surface(
                color = Violet.copy(alpha = 0.11f),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = "Übergabe erst nach deinem Tippen. Verwendet werden nur Android Share, App-Start oder Web – keine verdeckte Fernsteuerung.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(AiProviderRegistry.providers, key = { it.id }) { provider ->
                    ProviderCard(
                        provider = provider,
                        installed = controller.installedProviderApp(provider) != null,
                        hasPrompt = controller.providerPrompt.isNotBlank(),
                        onClick = { controller.routeToProvider(provider) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(
    provider: AiProviderProfile,
    installed: Boolean,
    hasPrompt: Boolean,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RaisedSurface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (installed) Mint.copy(alpha = 0.18f) else Sky.copy(alpha = 0.12f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        provider.shortName,
                        color = if (installed) Mint else Sky,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(provider.name, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (installed) "APP" else "WEB",
                        color = if (installed) Mint else Sky,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(
                    provider.description,
                    color = MutedMist,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(onClick = onClick) {
                Icon(
                    imageVector = if (installed && hasPrompt) Icons.AutoMirrored.Rounded.Send else Icons.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(if (installed && hasPrompt) "Teilen" else "Öffnen")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ContextDetailsSheet(
    snapshot: ContextSnapshot,
    onUseSuggestion: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DeepSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lokaler Kontext", style = MaterialTheme.typography.headlineSmall)
                    Text("Keine Cloud nötig", color = Mint, style = MaterialTheme.typography.labelMedium)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Schließen")
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                snapshot.reasons.forEach { reason ->
                    AssistChip(onClick = {}, label = { Text(reason) })
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vorschlag: ${snapshot.suggestedScene.title}", fontWeight = FontWeight.SemiBold)
                        Text(
                            snapshot.suggestedScene.subtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(onClick = onUseSuggestion) {
                        Text("Aktivieren")
                    }
                }
            }
            HorizontalDivider()
            Text(
                text = "M1 liest nur Uhrzeit, Akkustatus, verfügbare Netzwerkverbindung und aktive Audioausgänge. Standort, Kalender und Benachrichtigungen sind nicht freigeschaltet.",
                color = MutedMist,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
