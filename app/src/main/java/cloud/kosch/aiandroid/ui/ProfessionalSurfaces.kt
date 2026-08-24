package cloud.kosch.aiandroid.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContactPhone
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.LauncherController
import cloud.kosch.aiandroid.model.AppProfile
import cloud.kosch.aiandroid.model.AuditEvent
import cloud.kosch.aiandroid.security.PortableBackupCodec
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Violet
import cloud.kosch.aiandroid.ui.theme.Warm
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class ProAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val action: () -> Unit,
)

@Composable
fun ColumnScope.ProfessionalHubSurface(
    controller: LauncherController,
    onAsk: () -> Unit,
    requestDocument: () -> Unit,
    requestContact: () -> Unit,
) {
    val workApps = controller.apps.count { it.profile == AppProfile.WORK }
    val actions = listOf(
        ProAction("Command Bar", "Ctrl/⌘ + K", Icons.Rounded.AutoAwesome, onAsk),
        ProAction("Alle Apps", "Ctrl/⌘ + Leertaste", Icons.Rounded.Apps, controller::openDrawer),
        ProAction("Datei-KI", "Gezielte SAF-Auswahl", Icons.Rounded.FolderOpen, requestDocument),
        ProAction("Kontakt", "Einmalig auswählen", Icons.Rounded.ContactPhone, requestContact),
        ProAction("Telefon", "System-Wähler", Icons.Rounded.Phone, controller::openPhone),
        ProAction("Widgets", "Android Host Board", Icons.Rounded.Widgets, controller::openWidgetBoard),
        ProAction("Backup", "AES-256-GCM", Icons.Rounded.Backup, controller::openBackup),
        ProAction("Audit", "Nur Metadaten", Icons.Rounded.History, controller::openAudit),
    )
    val recentApps = controller.smartDockApps().take(6)

    Surface(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        color = DeepSurface.copy(alpha = 0.95f),
        shape = RoundedCornerShape(30.dp),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pro Desk", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Deine professionelle Android-Kommandozentrale",
                            color = MutedMist,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Surface(color = Mint.copy(alpha = 0.14f), shape = RoundedCornerShape(14.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Security, contentDescription = null, tint = Mint, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Local Core", color = Mint, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ProMetric(
                        title = "HOME",
                        value = if (controller.isDefaultHome) "Aktiv" else "Testmodus",
                        healthy = controller.isDefaultHome,
                        modifier = Modifier.weight(1f),
                    )
                    ProMetric(
                        title = "ARBEIT",
                        value = "$workApps Apps",
                        healthy = true,
                        modifier = Modifier.weight(1f),
                    )
                    ProMetric(
                        title = "AUDIT",
                        value = "${controller.auditEvents.size} lokal",
                        healthy = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Surface(color = Sky.copy(alpha = 0.09f), shape = RoundedCornerShape(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(13.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Sky)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Adaptive Local Core", fontWeight = FontWeight.SemiBold)
                            Text(
                                "${controller.appUsageSignals.size} lokale Nutzungssignale · ${controller.hiddenAppKeys.size} verborgene Apps · jederzeit löschbar",
                                color = MutedMist,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            item {
                Text("Werkzeuge", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            items(actions.chunked(2)) { pair ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { item -> ProActionCard(item, Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            if (recentApps.isNotEmpty()) {
                item {
                    Text("Im Zugriff", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentApps.take(5).forEach { app ->
                            Card(
                                onClick = { controller.launch(app) },
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = RaisedSurface),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Image(app.icon, contentDescription = app.label, modifier = Modifier.size(34.dp))
                                    Text(
                                        app.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Surface(color = Violet.copy(alpha = 0.10f), shape = RoundedCornerShape(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(13.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Keyboard, contentDescription = null, tint = Sky)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hardware-Tastatur bereit", fontWeight = FontWeight.SemiBold)
                            Text("Halte Ctrl/⌘ gedrückt, um Androids Shortcut-Hilfe aufzurufen.", color = MutedMist, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProMetric(title: String, value: String, healthy: Boolean, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = RaisedSurface, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = MutedMist, style = MaterialTheme.typography.labelSmall)
            Text(value, color = if (healthy) Mint else Warm, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun ProActionCard(item: ProAction, modifier: Modifier = Modifier) {
    Card(
        onClick = item.action,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = RaisedSurface),
        shape = RoundedCornerShape(19.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(13.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(color = Sky.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp)) {
                Icon(item.icon, contentDescription = null, tint = Sky, modifier = Modifier.padding(8.dp).size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(item.subtitle, color = MutedMist, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSheet(
    controller: LauncherController,
    requestExport: (String) -> Unit,
    requestImport: () -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    var showPassphrase by remember { mutableStateOf(false) }
    var restoreAcknowledged by remember(controller.backupPreview) { mutableStateOf(false) }
    val passphraseValid = passphrase.length >= PortableBackupCodec.MIN_PASSPHRASE_LENGTH

    ModalBottomSheet(
        onDismissRequest = controller::closeBackup,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DeepSurface,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.94f).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            item { SheetHeader("Sicheres Backup", "Portabel · verschlüsselt · vor Restore geprüft", controller::closeBackup) }
            item {
                Surface(color = Mint.copy(alpha = 0.11f), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = Mint)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AES-256-GCM + PBKDF2-HMAC-SHA-256", fontWeight = FontWeight.SemiBold)
                            Text("Die Passphrase verlässt dieses Gerät nicht und wird nicht gespeichert.", color = MutedMist, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                Surface(color = RaisedSurface, shape = RoundedCornerShape(17.dp)) {
                    Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Exportumfang", fontWeight = FontWeight.SemiBold)
                        Text("Szene, Home-Seite, Layout, Verlauf, Pins, verborgene Apps, lokale Lernsignale, Ordner und Pen-Striche", color = MutedMist, style = MaterialTheme.typography.bodySmall)
                        Text("Ohne Widgets, Dateifreigaben, Secrets, Benachrichtigungsdaten und Audit", color = Sky, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it.take(256) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Passphrase") },
                    supportingText = { Text("Mindestens ${PortableBackupCodec.MIN_PASSPHRASE_LENGTH} Zeichen; bei Verlust nicht wiederherstellbar") },
                    visualTransformation = if (showPassphrase) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassphrase = !showPassphrase }) {
                            Icon(
                                if (showPassphrase) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (showPassphrase) "Passphrase verbergen" else "Passphrase anzeigen",
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    singleLine = true,
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            requestExport(passphrase)
                            passphrase = ""
                        },
                        enabled = passphraseValid && !controller.backupBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Backup, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("Export")
                    }
                    OutlinedButton(onClick = requestImport, enabled = !controller.backupBusy, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Restore, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("Import")
                    }
                }
            }
            if (controller.backupFileStaged && controller.backupPreview == null) {
                item {
                    Button(
                        onClick = {
                            controller.previewStagedBackup(passphrase.toCharArray())
                            passphrase = ""
                        },
                        enabled = passphraseValid && !controller.backupBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Security, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("Entschlüsseln und prüfen")
                    }
                }
            }
            if (controller.backupBusy) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 3.dp)
                    }
                }
            }
            controller.backupPreview?.let { preview ->
                item {
                    Surface(color = RaisedSurface, shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Mint)
                                Spacer(Modifier.width(8.dp))
                                Text("Backup validiert", fontWeight = FontWeight.SemiBold)
                            }
                            Text("${preview.scene.title} · ${preview.homePage.title} · ${preview.folderCount} Ordner · ${preview.pinnedCount} Pins · ${preview.hiddenCount} verborgen · ${preview.usageSignalCount} Lernsignale · ${preview.inkStrokeCount} Stiftstriche")
                            preview.skippedItems.forEach { Text("• $it", color = MutedMist, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = restoreAcknowledged, onCheckedChange = { restoreAcknowledged = it })
                        Text("Vorhandenes Workspace-Layout, Pins und Ordner werden ersetzt.", modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Button(
                        onClick = { controller.applyBackupPreview(restoreAcknowledged) },
                        enabled = restoreAcknowledged && !controller.backupBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("Geprüften Workspace wiederherstellen")
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditSheet(
    controller: LauncherController,
    requestExport: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = controller::closeAudit,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DeepSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.94f).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SheetHeader("Lokales Audit", "90 Tage · maximal 250 Ereignisse · keine Inhalte", controller::closeAudit)
            Surface(color = Violet.copy(alpha = 0.10f), shape = RoundedCornerShape(17.dp)) {
                Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.AssignmentTurnedIn, contentDescription = null, tint = Sky)
                    Text(
                        "Gespeichert werden nur Zeitpunkt, fest definierter Aktionstyp und Ergebnis. Nie Prompts, Telefonnummern, Namen, Dateipfade oder Benachrichtigungstexte.",
                        color = MutedMist,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = requestExport, enabled = controller.auditEvents.isNotEmpty(), modifier = Modifier.weight(1f)) {
                    Text("CSV exportieren")
                }
                OutlinedButton(
                    onClick = {
                        if (confirmClear) {
                            controller.clearAudit(confirmed = true)
                            confirmClear = false
                        } else {
                            confirmClear = true
                        }
                    },
                    enabled = controller.auditEvents.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (confirmClear) "Wirklich löschen" else "Alles löschen")
                }
            }
            if (controller.auditEvents.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.History, contentDescription = null, tint = Sky, modifier = Modifier.size(42.dp))
                        Spacer(Modifier.height(9.dp))
                        Text("Keine Audit-Ereignisse")
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(controller.auditEvents) { event -> AuditRow(event) }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AuditRow(event: AuditEvent) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Badge, contentDescription = null, tint = Mint, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.action.title, fontWeight = FontWeight.Medium)
            Text(AUDIT_TIME_FORMAT.format(Instant.ofEpochMilli(event.timestampEpochMillis)), color = MutedMist, style = MaterialTheme.typography.labelSmall)
        }
        Text(event.outcome.title, color = if (event.outcome.name == "SUCCESS") Mint else Warm, style = MaterialTheme.typography.labelMedium)
    }
    HorizontalDivider(color = RaisedSurface)
}

private val AUDIT_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter
    .ofPattern("dd.MM.yyyy · HH:mm")
    .withZone(ZoneId.systemDefault())
