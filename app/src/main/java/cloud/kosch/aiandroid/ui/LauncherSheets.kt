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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.ContactPhone
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
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
import cloud.kosch.aiandroid.model.FileWorkspaceEntry
import cloud.kosch.aiandroid.model.WidgetSizePreset
import cloud.kosch.aiandroid.ai.SmartCollection
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Ink
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Violet
import cloud.kosch.aiandroid.ui.theme.Warm
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun QuickActionsRail(
    onPhone: () -> Unit,
    onFiles: () -> Unit,
    onCalendar: () -> Unit,
    onCamera: () -> Unit,
    onWidgets: () -> Unit,
    onControls: () -> Unit,
    onPen: (() -> Unit)?,
    onHelp: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickChip("Telefon", Icons.Rounded.Phone, onPhone)
        QuickChip("Datei-KI", Icons.Rounded.FolderOpen, onFiles)
        QuickChip("Kalender", Icons.Rounded.CalendarMonth, onCalendar)
        QuickChip("Kamera", Icons.Rounded.PhotoCamera, onCamera)
        QuickChip("Widgets", Icons.Rounded.Widgets, onWidgets)
        onPen?.let { QuickChip("Pen Space", Icons.Rounded.Draw, it) }
        QuickChip("FAQ", Icons.Rounded.HelpOutline, onHelp)
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
    val pages = remember(controller.stylusState.present) {
        buildList {
            add(OnboardingPage(
                eyebrow = "DEIN ANDROID · NEU GEDACHT",
                title = "Ein Launcher, der zuerst funktioniert.",
                body = "KoSch startet Apps, ordnet Szenen und versteht Systembefehle vollständig lokal. Ein Konto oder API-Schlüssel ist nicht nötig.",
                icon = Icons.Rounded.AutoAwesome,
                bullets = listOf("Local Core sofort aktiv", "Keine versteckte Cloud", "Freier Workspace statt starrem Raster"),
            ))
            add(OnboardingPage(
                eyebrow = "ECHTE HOME-APP",
                title = "Mache KoSch zu deinem Startbildschirm.",
                body = "Android zeigt die geschützte Systemauswahl. Du kannst KoSch testen, überspringen oder später jederzeit wieder wechseln.",
                icon = Icons.Rounded.Home,
                bullets = listOf("Android entscheidet die Rolle", "Keine Tricks mit Zurück-Tasten", "Notausgang immer im Kontrollzentrum"),
            ))
            add(OnboardingPage(
                eyebrow = "PRIVATE BY DESIGN",
                title = "KI sitzt darunter – nicht über dir.",
                body = "Dateien werden nur nach deiner Auswahl lokal und begrenzt gelesen. Telefonate, Einstellungen und externe KI-Ziele bleiben sichtbare Android-Aktionen.",
                icon = Icons.Rounded.Security,
                bullets = listOf("SAF statt Vollspeicherzugriff", "System-Dialer statt Anrufrecht", "Vorschau vor Übergabe oder Änderung"),
            ))
            if (controller.stylusState.present) {
                add(OnboardingPage(
                    eyebrow = "SMARTPEN ERKANNT",
                    title = "Dein Stift bekommt einen eigenen Raum.",
                    body = "Pen Space reagiert auf Druck, Neigung, Hover und Radierer, sofern dein Gerät diese Werte über Android meldet. Striche bleiben lokal und werden automatisch gespeichert.",
                    icon = Icons.Rounded.Draw,
                    bullets = listOf("Druckempfindliche Vektortinte", "Fingerkontakte werden beim Zeichnen ignoriert", "Stift, Marker, Radierer und Undo"),
                ))
            }
            add(OnboardingPage(
                eyebrow = "BEREIT",
                title = "Sprich mit dem ganzen Startbildschirm.",
                body = "Tippe oder sage zum Beispiel „Öffne Kamera“, „Wähle 030…“, „Datei analysieren“, „WLAN“ oder „Szene Work“.",
                icon = Icons.Rounded.Check,
                bullets = listOf("Smart Dock und lokale Ordner", "App-Shortcuts und Widgets", "Open-Source-KI als bewusste Option"),
            ))
        }
    }
    var page by remember { mutableIntStateOf(0) }
    LaunchedEffect(pages.size) {
        if (page > pages.lastIndex) page = pages.lastIndex
    }
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
    requestContact: () -> Unit,
    requestWidget: () -> Unit,
) {
    var confirmPersonalizationReset by remember { mutableStateOf(false) }
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
                    left = ControlItem("Datei prüfen", "Ein Dokument lokal", Icons.Rounded.FolderOpen) {
                        controller.closeControlCenter()
                        requestDocument()
                    },
                    right = ControlItem("Arbeitsordner", "Sicher verwalten", Icons.Rounded.Storage) {
                        controller.closeControlCenter()
                        controller.openFileWorkspace()
                    },
                )
            }
            if (controller.workProfiles.isNotEmpty()) {
                item {
                    Text("Arbeitsprofile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                items(controller.workProfiles, key = { it.userSerialNumber }) { profile ->
                    Surface(color = RaisedSurface, shape = RoundedCornerShape(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Rounded.BusinessCenter, contentDescription = null, tint = if (profile.quietMode) Warm else Mint)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Arbeitsprofil", fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (profile.quietMode) "Pausiert · Apps, Daten und Meldungen ruhen" else "Aktiv · über Android verwaltet",
                                    color = MutedMist,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            OutlinedButton(onClick = { controller.toggleWorkProfile(profile.userSerialNumber) }) {
                                Text(if (profile.quietMode) "Aktivieren" else "Pausieren")
                            }
                        }
                    }
                }
            }
            item {
                ControlPair(
                    left = ControlItem("Kalender", "System-App", Icons.Rounded.CalendarMonth) {
                        controller.openCalendar()
                    },
                    right = ControlItem("Wecker", "Alarme & Timer", Icons.Rounded.Alarm) {
                        controller.openAlarms()
                    },
                )
            }
            item {
                ControlPair(
                    left = ControlItem("Kamera", "Herstellerunabhängig", Icons.Rounded.PhotoCamera) {
                        controller.openCamera()
                    },
                    right = ControlItem(
                        "Systemnotiz",
                        if (controller.stylusState.present) "Stiftmodus anfordern" else "Android 14+",
                        Icons.Rounded.EditNote,
                    ) {
                        controller.createSystemNote()
                    },
                )
            }
            item {
                ControlPair(
                    left = ControlItem(
                        "Verborgene Apps",
                        "${controller.hiddenAppKeys.size} lokal ausgeblendet",
                        Icons.Rounded.VisibilityOff,
                    ) {
                        controller.closeControlCenter()
                        controller.openDrawer(SmartCollection.HIDDEN)
                    },
                    right = ControlItem(
                        "Lokales Lernen",
                        if (confirmPersonalizationReset) "Noch einmal tippen: löschen" else "${controller.appUsageSignals.size} Metadaten-Signale",
                        Icons.Rounded.AutoAwesome,
                    ) {
                        if (confirmPersonalizationReset) {
                            controller.clearPersonalization(confirmed = true)
                            confirmPersonalizationReset = false
                        } else {
                            confirmPersonalizationReset = true
                        }
                    },
                )
            }
            item {
                ControlPair(
                    left = ControlItem("Telefon", "System-Wähler", Icons.Rounded.Phone) {
                        controller.closeControlCenter()
                        controller.openPhone()
                    },
                    right = ControlItem("Kontakt", "Einmalige Systemauswahl", Icons.Rounded.ContactPhone) {
                        controller.closeControlCenter()
                        requestContact()
                    },
                )
            }
            item {
                ControlPair(
                    left = ControlItem("Hintergrund", "Android-Auswahl", Icons.Rounded.Wallpaper) {
                        controller.openSystemPanel(SystemPanel.WALLPAPER)
                    },
                    right = ControlItem("Anzeige", "Display & Skalierung", Icons.Rounded.DisplaySettings) {
                        controller.openSystemPanel(SystemPanel.DISPLAY)
                    },
                )
            }
            item {
                ControlPair(
                    left = ControlItem("Ton", "Audio & Vibration", Icons.Rounded.VolumeUp) {
                        controller.openSystemPanel(SystemPanel.SOUND)
                    },
                    right = ControlItem("Akku", "Energieoptionen", Icons.Rounded.BatterySaver) {
                        controller.openSystemPanel(SystemPanel.BATTERY)
                    },
                )
            }
            item {
                ControlPair(
                    left = ControlItem("Datenschutz", "Android Privacy", Icons.Rounded.PrivacyTip) {
                        controller.openSystemPanel(SystemPanel.PRIVACY)
                    },
                    right = ControlItem("Bedienung", "Accessibility", Icons.Rounded.AccessibilityNew) {
                        controller.openSystemPanel(SystemPanel.ACCESSIBILITY)
                    },
                )
            }
            item {
                ControlPair(
                    left = ControlItem("Standard-Apps", "Android-Zuordnung", Icons.Rounded.Apps) {
                        controller.openSystemPanel(SystemPanel.DEFAULT_APPS)
                    },
                    right = ControlItem("Speicher", "Gerätespeicher", Icons.Rounded.Storage) {
                        controller.openSystemPanel(SystemPanel.STORAGE)
                    },
                )
            }
            item {
                ControlPair(
                    left = ControlItem("Backup", "Verschlüsselt", Icons.Rounded.Backup) {
                        controller.openBackup()
                    },
                    right = ControlItem("Audit", "Nur Metadaten", Icons.Rounded.History) {
                        controller.openAudit()
                    },
                )
            }
            item {
                ControlPair(
                    left = ControlItem(
                        "Pen Space",
                        if (controller.stylusState.present) controller.stylusState.capabilitySummary else "Kein Stift erkannt",
                        Icons.Rounded.Draw,
                    ) {
                        controller.openPenSpace()
                    },
                    right = ControlItem("FAQ & Hilfe", "Lokal durchsuchbar", Icons.Rounded.HelpOutline) {
                        controller.openFaq()
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
                    right = ControlItem(
                        "App-Punkte",
                        if (controller.notificationAccessGranted) "Aktiv · nur Anzahl" else "Opt-in",
                        Icons.Rounded.Notifications,
                    ) {
                        controller.openNotificationAccess()
                    },
                )
            }
            item {
                ControlPair(
                    left = ControlItem("Android", "Einstellungen", Icons.Rounded.Settings) {
                        controller.openSystemPanel(SystemPanel.ANDROID_SETTINGS)
                    },
                    right = ControlItem("Smart Space", "Dock & Ordner", Icons.Rounded.AutoAwesome) {
                        controller.closeControlCenter()
                        controller.switchHomePage(cloud.kosch.aiandroid.model.HomePage.SMART_SPACE)
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
fun PhoneSheet(controller: LauncherController, requestContact: () -> Unit) {
    var number by remember { mutableStateOf("") }
    val selectedContact = controller.selectedContact
    LaunchedEffect(selectedContact) {
        if (selectedContact != null) number = selectedContact.phoneNumber
    }
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
            OutlinedButton(onClick = requestContact, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.ContactPhone, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Kontakt einmalig auswählen")
            }
            if (selectedContact != null) {
                Surface(color = Mint.copy(alpha = 0.10f), shape = RoundedCornerShape(15.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(selectedContact.displayName, fontWeight = FontWeight.SemiBold)
                        Text("Nur für diesen Vorgang übernommen", color = MutedMist, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
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
                    "KoSch besitzt keine Anrufberechtigung. Der System-Wähler zeigt die Nummer; erst du startest den Anruf. Es gibt keinen globalen Kontaktzugriff – nur die von dir gewählte Nummer wird temporär übernommen.",
                    modifier = Modifier.padding(13.dp),
                    color = MutedMist,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { controller.dial(number.trim().ifBlank { null }) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Phone, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (number.isBlank()) "Telefon" else "Anruf")
                }
                OutlinedButton(
                    onClick = { controller.message(number.trim().ifBlank { null }) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Message, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Nachricht")
                }
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileIntelligenceSheet(
    controller: LauncherController,
    requestDocument: () -> Unit,
    forgetDocument: () -> Unit,
) {
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
                    TextButton(onClick = forgetDocument, modifier = Modifier.fillMaxWidth()) {
                        Text("Gespeicherten Dateizugriff vergessen")
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

private enum class FileWorkspaceSort(val title: String) {
    NAME("A–Z"),
    NEWEST("Neu"),
    SIZE("Größe"),
    TYPE("Typ"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileWorkspaceSheet(
    controller: LauncherController,
    requestWorkspace: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(FileWorkspaceSort.NAME) }
    var createDirectory by remember { mutableStateOf(false) }
    var createName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<FileWorkspaceEntry?>(null) }
    var renameName by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<FileWorkspaceEntry?>(null) }
    val current = controller.fileWorkspacePath.lastOrNull()
    val entries = remember(query, sort, controller.fileWorkspaceEntries) {
        val filtered = controller.fileWorkspaceEntries.filter {
            query.isBlank() || it.displayName.contains(query.trim(), ignoreCase = true) ||
                it.category.contains(query.trim(), ignoreCase = true)
        }
        val comparator = when (sort) {
            FileWorkspaceSort.NAME -> compareBy<FileWorkspaceEntry> { it.displayName.lowercase(Locale.ROOT) }
            FileWorkspaceSort.NEWEST -> compareByDescending { it.lastModifiedEpochMillis ?: Long.MIN_VALUE }
            FileWorkspaceSort.SIZE -> compareByDescending { it.sizeBytes ?: Long.MIN_VALUE }
            FileWorkspaceSort.TYPE -> compareBy<FileWorkspaceEntry> { it.category }
                .thenBy { it.displayName.lowercase(Locale.ROOT) }
        }
        filtered.sortedWith(compareByDescending<FileWorkspaceEntry> { it.isDirectory }.then(comparator))
    }

    ModalBottomSheet(
        onDismissRequest = controller::closeFileWorkspace,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DeepSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.94f).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SheetHeader(
                "Datei-Arbeitsraum",
                "Nur gewählter SAF-Ordner · lokal analysiert",
                controller::closeFileWorkspace,
            )

            if (current == null) {
                Surface(color = RaisedSurface, shape = RoundedCornerShape(20.dp)) {
                    Column(
                        Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = Sky, modifier = Modifier.size(42.dp))
                        Text("Sicheren Arbeitsordner wählen", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Android gibt KoSch ausschließlich den ausgewählten Ordner frei. Es gibt kein MANAGE_EXTERNAL_STORAGE und keinen heimlichen Gerätescan.",
                            color = MutedMist,
                        )
                        Button(onClick = requestWorkspace, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Ordner über Android auswählen")
                        }
                    }
                }
                if (controller.fileWorkspaceLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    Spacer(Modifier.weight(1f))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (controller.fileWorkspacePath.size > 1) {
                        AssistChip(
                            onClick = controller::navigateFileWorkspaceUp,
                            label = { Text("Hoch") },
                            leadingIcon = { Icon(Icons.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        )
                    }
                    AssistChip(
                        onClick = controller::refreshFileWorkspace,
                        label = { Text("Aktualisieren") },
                        leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                    AssistChip(
                        onClick = requestWorkspace,
                        label = { Text("Wechseln") },
                        leadingIcon = { Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                    if (current.canCreateChildren) {
                        AssistChip(
                            onClick = { createDirectory = true },
                            label = { Text("Ordner +") },
                            leadingIcon = { Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        )
                    }
                    if (controller.canUndoFileRename) {
                        AssistChip(
                            onClick = controller::undoFileWorkspaceRename,
                            label = { Text("Rename Undo") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        )
                    }
                }

                Surface(color = Mint.copy(alpha = 0.10f), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(current.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        val summary = controller.fileWorkspaceSummary
                        if (summary != null) {
                            Text(
                                "${summary.directoryCount} Ordner · ${summary.fileCount} Dateien · ${humanBytes(summary.knownBytes)} bekannt",
                                color = MutedMist,
                            )
                            val categories = summary.categoryCounts.entries.joinToString(" · ") { "${it.key} ${it.value}" }
                            if (categories.isNotBlank()) Text(categories, color = Sky, style = MaterialTheme.typography.labelSmall)
                            if (summary.duplicateNameGroups > 0) {
                                Text(
                                    "Lokaler Hinweis: ${summary.duplicateNameGroups} mögliche Namensduplikate",
                                    color = Warm,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            if (summary.largestFiles.isNotEmpty()) {
                                Text(
                                    "Größte sichtbare Dateien: ${summary.largestFiles.joinToString()}",
                                    color = MutedMist,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    label = { Text("Im aktuellen Ordner suchen") },
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FileWorkspaceSort.entries.forEach { mode ->
                        FilterChip(
                            selected = sort == mode,
                            onClick = { sort = mode },
                            label = { Text(mode.title) },
                        )
                    }
                }

                when {
                    controller.fileWorkspaceLoading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                    entries.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            if (query.isBlank()) "Dieser Ordner ist leer." else "Keine Datei passt zu „$query“.",
                            color = MutedMist,
                        )
                    }

                    else -> LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        items(entries, key = FileWorkspaceEntry::documentId) { entry ->
                            Card(
                                onClick = { controller.openFileWorkspaceEntry(entry) },
                                colors = CardDefaults.cardColors(containerColor = RaisedSurface),
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        if (entry.isDirectory) Icons.Rounded.Folder else Icons.Rounded.InsertDriveFile,
                                        contentDescription = null,
                                        tint = if (entry.isDirectory) Mint else Sky,
                                        modifier = Modifier.size(30.dp),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.displayName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            fileEntrySubtitle(entry),
                                            color = MutedMist,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    if (entry.canRename) {
                                        IconButton(onClick = {
                                            renameTarget = entry
                                            renameName = entry.displayName
                                        }) {
                                            Icon(Icons.Rounded.Edit, contentDescription = "${entry.displayName} umbenennen")
                                        }
                                    }
                                    if (entry.canDelete) {
                                        IconButton(onClick = { deleteTarget = entry }) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "${entry.displayName} löschen")
                                        }
                                    }
                                    if (!entry.isDirectory) {
                                        Icon(Icons.Rounded.OpenInNew, contentDescription = "${entry.displayName} öffnen", tint = Sky)
                                    }
                                }
                            }
                        }
                    }
                }

                TextButton(onClick = controller::forgetFileWorkspace, modifier = Modifier.fillMaxWidth()) {
                    Text("Arbeitsordner und Android-Freigabe vergessen")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (createDirectory) {
        FileNameDialog(
            title = "Neuen Ordner erstellen",
            value = createName,
            confirmLabel = "Erstellen",
            onValueChange = { createName = it },
            onDismiss = {
                createDirectory = false
                createName = ""
            },
            onConfirm = {
                controller.createFileWorkspaceDirectory(createName, confirmed = true)
                createDirectory = false
                createName = ""
            },
        )
    }
    renameTarget?.let { entry ->
        FileNameDialog(
            title = "${entry.displayName} umbenennen",
            value = renameName,
            confirmLabel = "Umbenennen",
            onValueChange = { renameName = it },
            onDismiss = { renameTarget = null },
            onConfirm = {
                controller.renameFileWorkspaceEntry(entry, renameName, confirmed = true)
                renameTarget = null
            },
        )
    }
    deleteTarget?.let { entry ->
        Dialog(onDismissRequest = { deleteTarget = null }) {
            Surface(color = DeepSurface, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Endgültig löschen?", style = MaterialTheme.typography.titleLarge, color = Warm)
                    Text(
                        "„${entry.displayName}“ wird über den gewählten Android-Dateianbieter gelöscht. KoSch kann diese Aktion nicht rückgängig machen.",
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { deleteTarget = null }) { Text("Abbrechen") }
                        Button(onClick = {
                            controller.deleteFileWorkspaceEntry(entry, confirmed = true)
                            deleteTarget = null
                        }) { Text("Endgültig löschen") }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileNameDialog(
    title: String,
    value: String,
    confirmLabel: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = DeepSurface, shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = value,
                    onValueChange = { onValueChange(it.take(120)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    singleLine = true,
                )
                Text(
                    "Lokale Vorschau · keine Aktion vor Bestätigung",
                    color = Mint,
                    style = MaterialTheme.typography.labelSmall,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Abbrechen") }
                    Button(onClick = onConfirm, enabled = value.trim().isNotEmpty()) { Text(confirmLabel) }
                }
            }
        }
    }
}

private fun fileEntrySubtitle(entry: FileWorkspaceEntry): String {
    val modified = entry.lastModifiedEpochMillis?.let {
        FILE_DATE_FORMAT.format(Instant.ofEpochMilli(it))
    }
    return listOfNotNull(
        entry.category,
        entry.sizeBytes?.let(::humanBytes),
        modified,
    ).joinToString(" · ")
}

private fun humanBytes(bytes: Long): String = when {
    bytes < 1_024 -> "$bytes B"
    bytes < 1_048_576 -> "%.1f KB".format(Locale.GERMAN, bytes / 1_024.0)
    bytes < 1_073_741_824 -> "%.1f MB".format(Locale.GERMAN, bytes / 1_048_576.0)
    else -> "%.1f GB".format(Locale.GERMAN, bytes / 1_073_741_824.0)
}

private val FILE_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter
    .ofPattern("dd.MM.yyyy HH:mm")
    .withZone(ZoneId.systemDefault())

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
    createWidgetView: (Context, Int, WidgetSizePreset) -> View?,
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
            SheetHeader("Widget Board", "Echte Android-Widgets · Größe, Reihenfolge und Undo", controller::closeWidgetBoard)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = requestWidget, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text("Widget hinzufügen")
                }
                OutlinedButton(
                    onClick = controller::undoWidgetOrder,
                    enabled = controller.canUndoWidgetOrder,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Widget-Reihenfolge rückgängig")
                }
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
                    itemsIndexed(controller.widgetIds, key = { _, id -> id }) { index, appWidgetId ->
                        val preset = controller.widgetSize(appWidgetId)
                        Surface(color = RaisedSurface, shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(10.dp)) {
                                Row(
                                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    WidgetSizePreset.entries.forEach { option ->
                                        FilterChip(
                                            selected = preset == option,
                                            onClick = { controller.setWidgetSize(appWidgetId, option) },
                                            label = { Text(option.title) },
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { controller.moveWidget(appWidgetId, -1) },
                                        enabled = index > 0,
                                    ) {
                                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "Widget nach oben")
                                    }
                                    IconButton(
                                        onClick = { controller.moveWidget(appWidgetId, 1) },
                                        enabled = index < controller.widgetIds.lastIndex,
                                    ) {
                                        Icon(Icons.Rounded.ArrowDownward, contentDescription = "Widget nach unten")
                                    }
                                    IconButton(onClick = { deleteWidget(appWidgetId) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Widget entfernen")
                                    }
                                }
                                key(appWidgetId, preset) {
                                    AndroidView(
                                        factory = { context ->
                                            createWidgetView(context, appWidgetId, preset) ?: TextView(context).apply {
                                                text = "Widget nicht mehr verfügbar"
                                                setTextColor(android.graphics.Color.WHITE)
                                                setPadding(24, 24, 24, 24)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(preset.boardHeightDp.dp),
                                    )
                                }
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
    var confirmUninstall by remember(app.key) { mutableStateOf(false) }
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
                    Text(
                        "${app.profile.title} · ${app.packageName}",
                        color = MutedMist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = controller::openSelectedAppStore, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Storefront, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text("Store")
                }
                OutlinedButton(onClick = controller::toggleSelectedAppHidden, modifier = Modifier.weight(1f)) {
                    Icon(
                        if (controller.isHidden(app)) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(if (controller.isHidden(app)) "Einblenden" else "Verbergen")
                }
            }
            TextButton(
                onClick = {
                    if (confirmUninstall) {
                        controller.requestSelectedAppUninstall(confirmed = true)
                    } else {
                        confirmUninstall = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (confirmUninstall) {
                        "${app.profile.title}-App im Android-Dialog prüfen"
                    } else {
                        "App deinstallieren …"
                    },
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = controller::toggleSelectedAppPin, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.PushPin, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text(if (controller.isPinned(app)) "Dock lösen" else "Ins Dock")
                }
                OutlinedButton(onClick = controller::addSelectedAppToSmartFolder, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.CreateNewFolder, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text("In Ordner")
                }
            }
            if (controller.isPinned(app)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { controller.moveSelectedPinnedApp(-1) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.ArrowUpward, contentDescription = null)
                        Spacer(Modifier.width(5.dp))
                        Text("Dock nach links")
                    }
                    OutlinedButton(
                        onClick = { controller.moveSelectedPinnedApp(1) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.ArrowDownward, contentDescription = null)
                        Spacer(Modifier.width(5.dp))
                        Text("Dock nach rechts")
                    }
                }
            }
            if (controller.folders.isNotEmpty()) {
                Text("Zu Sammlung hinzufügen", style = MaterialTheme.typography.labelLarge, color = MutedMist)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    controller.folders.forEach { folder ->
                        AssistChip(
                            onClick = { controller.addSelectedAppToFolder(folder.id) },
                            label = { Text(folder.title, maxLines = 1) },
                            leadingIcon = { Text(folder.kind.glyph, color = Mint) },
                        )
                    }
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
fun SheetHeader(title: String, subtitle: String, onClose: () -> Unit) {
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
