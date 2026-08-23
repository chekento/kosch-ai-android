package cloud.kosch.aiandroid.ui

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cloud.kosch.aiandroid.LauncherController
import cloud.kosch.aiandroid.ai.LocalRuntimeRegistry
import cloud.kosch.aiandroid.ai.RuntimeStage
import cloud.kosch.aiandroid.model.SystemPanel
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Ink
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Violet
import cloud.kosch.aiandroid.ui.theme.Warm

@Composable
fun QuickActionsRail(
    onPhone: () -> Unit,
    onFiles: () -> Unit,
    onWidgets: () -> Unit,
    onControls: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickChip("Telefon", Icons.Rounded.Phone, onPhone)
        QuickChip("Datei-KI", Icons.Rounded.FolderOpen, onFiles)
        QuickChip("Widgets", Icons.Rounded.Widgets, onWidgets)
        QuickChip("Kontrollzentrum", Icons.Rounded.Tune, onControls)
    }
}

@Composable
private fun QuickChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
    )
}

private data class OnboardingPage(
    val eyebrow: String,
    val title: String,
    val body: String,
    val icon: ImageVector,
    val bullets: List<String>,
)

@Composable
fun OnboardingExperience(
    controller: LauncherController,
    requestHomeRole: () -> Unit,
) {
    val pages = remember {
        listOf(
            OnboardingPage(
                eyebrow = "DEIN ANDROID · NEU GEDACHT",
                title = "Ein Launcher, der zuerst funktioniert.",
                body = "KoSch startet Apps, ordnet Szenen und versteht Systembefehle vollständig lokal. Ein Konto oder API-Schlüssel ist nicht nötig.",
                icon = Icons.Rounded.AutoAwesome,
                bullets = listOf("Local Core sofort aktiv", "Keine versteckte Cloud", "Freier Workspace statt starrem Raster"),
            ),
            OnboardingPage(
                eyebrow = "ECHTE HOME-APP",
                title = "Mache KoSch zu deinem Startbildschirm.",
                body = "Android zeigt die geschützte Systemauswahl. Du kannst KoSch testen, überspringen oder später jederzeit wieder wechseln.",
                icon = Icons.Rounded.Home,
                bullets = listOf("Android entscheidet die Rolle", "Keine Tricks mit Zurück-Tasten", "Notausgang immer im Kontrollzentrum"),
            ),
            OnboardingPage(
                eyebrow = "PRIVATE BY DESIGN",
                title = "KI sitzt darunter – nicht über dir.",
                body = "Dateien werden nur nach deiner Auswahl lokal und begrenzt gelesen. Telefonate, Einstellungen und externe KI-Ziele bleiben sichtbare Android-Aktionen.",
                icon = Icons.Rounded.Security,
                bullets = listOf("SAF statt Vollspeicherzugriff", "System-Dialer statt Anrufrecht", "Vorschau vor Übergabe oder Änderung"),
            ),
            OnboardingPage(
                eyebrow = "BEREIT",
                title = "Sprich mit dem ganzen Startbildschirm.",
                body = "Tippe oder sage zum Beispiel „Öffne Kamera“, „Wähle 030…“, „Datei analysieren“, „WLAN“ oder „Szene Work“.",
                icon = Icons.Rounded.Check,
                bullets = listOf("App-Shortcuts per langem Druck", "Widgets im eigenen Board", "Open-Source-KI als bewusste Option"),
            ),
        )
    }
    var page by remember { mutableIntStateOf(0) }
    val current = pages[page]

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Ink),
        ) {
            NeuralGlassBackground(Modifier.fillMaxSize())
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("KoSch AI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${page + 1} / ${pages.size}", color = Mint, style = MaterialTheme.typography.labelLarge)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        pages.indices.forEach { index ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .background(
                                        if (index <= page) Mint else RaisedSurface,
                                        RoundedCornerShape(9.dp),
                                    ),
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        color = Mint.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(current.icon, contentDescription = null, tint = Mint, modifier = Modifier.size(34.dp))
                        }
                    }
                    Text(current.eyebrow, color = Sky, style = MaterialTheme.typography.labelLarge)
                    Text(
                        current.title,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        current.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        current.bullets.forEach { bullet ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(7.dp).background(Mint, CircleShape))
                                Text(bullet, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    if (page == 1 && !controller.isDefaultHome) {
                        OutlinedButton(onClick = requestHomeRole, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.Home, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Android-Start-App auswählen")
                        }
                    }
                    if (page == 2) {
                        TextButton(
                            onClick = { controller.openSystemPanel(SystemPanel.HOME_SELECTION) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Notausgang jetzt ansehen: Start-App-Auswahl")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = controller::completeOnboarding) {
                        Text("Tour überspringen")
                    }
                    Button(
                        onClick = {
                            if (page == pages.lastIndex) controller.completeOnboarding() else page += 1
                        },
                    ) {
                        Text(if (page == pages.lastIndex) "Launcher starten" else "Weiter")
                        Spacer(Modifier.width(7.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlCenterSheet(
    controller: LauncherController,
    requestDocument: () -> Unit,
    requestWidget: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = controller::closeControlCenter,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DeepSurface,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SheetHeader(
                    title = "Kontrollzentrum",
                    subtitle = "Sichere Android-Wege · Local Core aktiv",
                    onClose = controller::closeControlCenter,
                )
            }
            item {
                Surface(color = Mint.copy(alpha = 0.12f), shape = RoundedCornerShape(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Security, contentDescription = null, tint = Mint)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("API-frei betriebsbereit", fontWeight = FontWeight.SemiBold)
                            Text("Kein INTERNET-Recht, kein Konto, keine Modellkosten", color = MutedMist)
                        }
                    }
                }
            }
            item {
                ControlPair(
                    left = ControlItem("Telefon", "System-Wähler", Icons.Rounded.Phone) {
                        controller.closeControlCenter()
                        controller.openPhone()
                    },
                    right = ControlItem("Datei-KI", "Lokal prüfen", Icons.Rounded.FolderOpen) {
                        controller.closeControlCenter()
                        requestDocument()
                    },
                )
            }
            item {
                ControlPair(
                    left = ControlItem("Widgets", "Board öffnen", Icons.Rounded.Widgets) {
                        controller.closeControlCenter()
                        controller.openWidgetBoard()
                    },
                    right = ControlItem("Widget +", "Android-Auswahl", Icons.Rounded.Add) {
                        controller.closeControlCenter()
                        requestWidget()
                    },
                )
            }
            item {
                ControlPair(
                    left = ControlItem("WLAN", "Einstellungen", Icons.Rounded.Wifi) {
                        controller.openSystemPanel(SystemPanel.WIFI)
                    },
                    right = ControlItem("Bluetooth", "Einstellungen", Icons.Rounded.Bluetooth) {
                        controller.openSystemPanel(SystemPanel.BLUETOOTH)
                    },
                )
            }
            item {
                ControlPair(
                    left = ControlItem("Meldungen", "Systembereich", Icons.Rounded.Notifications) {
                        controller.openSystemPanel(SystemPanel.NOTIFICATIONS)
                    },
                    right = ControlItem("Android", "Einstellungen", Icons.Rounded.Settings) {
                        controller.openSystemPanel(SystemPanel.ANDROID_SETTINGS)
                    },
                )
            }
            item {
                Surface(color = Warm.copy(alpha = 0.12f), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Sicherheitsausgang", color = Warm, style = MaterialTheme.typography.labelLarge)
                        Text("Falls KoSch Probleme macht, öffnet dieser Weg direkt Androids Auswahl der Start-App.")
                        Button(
                            onClick = { controller.openSystemPanel(SystemPanel.HOME_SELECTION) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.Home, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Anderen Launcher wählen")
                        }
                    }
                }
            }
            item {
                Text("Lokale KI-Runtimes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            items(LocalRuntimeRegistry.runtimes, key = { it.id }) { runtime ->
                Surface(color = RaisedSurface, shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(runtime.name, fontWeight = FontWeight.SemiBold)
                            Text(runtime.description, color = MutedMist, style = MaterialTheme.typography.bodySmall)
                            Text(runtime.license, color = Sky, style = MaterialTheme.typography.labelSmall)
                        }
                        Text(
                            runtime.stage.label,
                            color = if (runtime.stage == RuntimeStage.ACTIVE) Mint else Sky,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            item {
                TextButton(onClick = controller::reopenOnboarding, modifier = Modifier.fillMaxWidth()) {
                    Text("Einführung erneut ansehen")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private data class ControlItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val action: () -> Unit,
)

@Composable
private fun ControlPair(left: ControlItem, right: ControlItem) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ControlCard(left, Modifier.weight(1f))
        ControlCard(right, Modifier.weight(1f))
    }
}

@Composable
private fun ControlCard(item: ControlItem, modifier: Modifier = Modifier) {
    Card(
        onClick = item.action,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = RaisedSurface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(item.icon, contentDescription = null, tint = Sky)
            Text(item.title, fontWeight = FontWeight.SemiBold)
            Text(item.subtitle, color = MutedMist, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneSheet(controller: LauncherController) {
    var number by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = controller::closePhone,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DeepSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SheetHeader("Telefon", "Sicher über Android ACTION_DIAL", controller::closePhone)
            OutlinedTextField(
                value = number,
                onValueChange = { number = it.filter { char -> char.isDigit() || char in "+ ()-/" } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nummer (optional)") },
                placeholder = { Text("+49 …") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
            )
            Surface(color = Violet.copy(alpha = 0.10f), shape = RoundedCornerShape(16.dp)) {
                Text(
                    "KoSch besitzt keine Anrufberechtigung. Der System-Wähler zeigt die Nummer; erst du startest den Anruf. Kontakte werden nicht gelesen.",
                    modifier = Modifier.padding(13.dp),
                    color = MutedMist,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(onClick = { controller.dial(number.trim().ifBlank { null }) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Phone, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (number.isBlank()) "Telefon öffnen" else "Im Telefon vorbereiten")
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileIntelligenceSheet(controller: LauncherController, requestDocument: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = controller::closeFileSheet,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DeepSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SheetHeader("Datei-Intelligenz", "Lokal · begrenzter Lesezugriff · keine Mutation", controller::closeFileSheet)
            when {
                controller.fileLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator()
                        Text("Analysiere lokal …", color = MutedMist)
                    }
                }
                controller.fileInsight == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Keine Dateiinformation verfügbar", color = MutedMist)
                }
                else -> {
                    val insight = controller.fileInsight ?: return@ModalBottomSheet
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                        item {
                            Surface(color = RaisedSurface, shape = RoundedCornerShape(18.dp)) {
                                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(insight.category.uppercase(), color = Mint, style = MaterialTheme.typography.labelMedium)
                                    Text(insight.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                    Text(insight.mimeType, color = MutedMist, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        item {
                            InsightBlock("Lokale Zusammenfassung", insight.summary)
                        }
                        insight.suggestedName?.let { suggestion ->
                            item {
                                InsightBlock(
                                    "Sicherer Namensvorschlag",
                                    "$suggestion\n\nNur Vorschau – KoSch benennt nichts automatisch um.",
                                )
                            }
                        }
                        insight.preview?.let { preview ->
                            item {
                                InsightBlock("Textausschnitt · max. 4.096 Zeichen", preview.take(1_200))
                            }
                        }
                        item {
                            Text(insight.safetyNote, color = Mint, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = requestDocument, modifier = Modifier.weight(1f)) {
                            Text("Andere Datei")
                        }
                        Button(onClick = controller::openInspectedFile, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.OpenInNew, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text("Öffnen")
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun InsightBlock(title: String, body: String) {
    Surface(color = Violet.copy(alpha = 0.09f), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, color = Sky, style = MaterialTheme.typography.labelLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetBoardSheet(
    controller: LauncherController,
    requestWidget: () -> Unit,
    createWidgetView: (Context, Int) -> View?,
    deleteWidget: (Int) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = controller::closeWidgetBoard,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DeepSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.94f).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SheetHeader("Widget Board", "Echte Android-Widgets · persistente Host-IDs", controller::closeWidgetBoard)
            Button(onClick = requestWidget, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("Widget hinzufügen")
            }
            if (controller.widgetIds.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.Widgets, contentDescription = null, tint = Sky, modifier = Modifier.size(42.dp))
                        Text("Noch keine Widgets", style = MaterialTheme.typography.titleMedium)
                        Text("Die Auswahl und Konfiguration übernimmt Android.", color = MutedMist)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                    items(controller.widgetIds, key = { it }) { appWidgetId ->
                        Surface(color = RaisedSurface, shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(10.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    IconButton(onClick = { deleteWidget(appWidgetId) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Widget entfernen")
                                    }
                                }
                                AndroidView(
                                    factory = { context ->
                                        createWidgetView(context, appWidgetId) ?: TextView(context).apply {
                                            text = "Widget nicht mehr verfügbar"
                                            setTextColor(android.graphics.Color.WHITE)
                                            setPadding(24, 24, 24, 24)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 420.dp),
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(22.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsSheet(controller: LauncherController) {
    val app = controller.selectedApp ?: return
    ModalBottomSheet(
        onDismissRequest = controller::hideAppActions,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DeepSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SheetHeader(app.label, "App-Aktionen und veröffentlichte Shortcuts", controller::hideAppActions)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(app.icon, contentDescription = null, modifier = Modifier.size(58.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.packageName, color = MutedMist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Langer Druck öffnet diesen sicheren Aktionsraum", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { controller.launch(app) }, modifier = Modifier.weight(1f)) {
                    Text("App öffnen")
                }
                OutlinedButton(onClick = controller::openSelectedAppInfo, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Info, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text("App-Info")
                }
            }
            HorizontalDivider()
            Text("Shortcuts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            when {
                controller.shortcutsLoading -> Box(Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                controller.appShortcuts.isEmpty() -> Text(
                    "Diese App veröffentlicht keine für den Launcher sichtbaren Shortcuts.",
                    color = MutedMist,
                )
                else -> controller.appShortcuts.forEach { shortcut ->
                    Card(
                        onClick = { controller.launch(shortcut) },
                        colors = CardDefaults.cardColors(containerColor = RaisedSurface),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            shortcut.icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(38.dp)) }
                            Text(shortcut.label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = Sky)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SheetHeader(title: String, subtitle: String, onClose: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MutedMist, style = MaterialTheme.typography.labelMedium)
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, contentDescription = "Schließen")
        }
    }
}
