package cloud.kosch.aiandroid.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.AiHubController
import cloud.kosch.aiandroid.ai.AiHubEntry
import cloud.kosch.aiandroid.ai.AiHubEntryKind
import cloud.kosch.aiandroid.ai.AiHubInstallState
import cloud.kosch.aiandroid.model.LaunchableApp
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Violet

private enum class AiHubFilter(val title: String) {
    SMART("Smart"),
    ALL("Alle"),
    AI("KI-Apps"),
    LOCAL("Lokal"),
    BROWSERS("Browser"),
}

@Composable
fun AiHubEntryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = onClick,
        label = { Text("AI Hub") },
        leadingIcon = {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiHubSurface(
    hub: AiHubController,
    apps: List<LaunchableApp>,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var filter by remember { mutableStateOf(AiHubFilter.SMART) }
    val allEntries = hub.entries(apps)
    val recommendations = hub.recommendations(apps)
    val recommendationReasons = recommendations.associate { it.entry.stableId to it.reason }
    val entries = when (filter) {
        AiHubFilter.SMART -> recommendations.map { it.entry }
        AiHubFilter.ALL -> allEntries
        AiHubFilter.AI -> allEntries.filter { it.kind == AiHubEntryKind.LLM_APP }
        AiHubFilter.LOCAL -> allEntries.filter { it.kind == AiHubEntryKind.LOCAL_LLM_APP }
        AiHubFilter.BROWSERS -> allEntries.filter {
            it.kind == AiHubEntryKind.BROWSER || it.kind == AiHubEntryKind.SYSTEM_BROWSER
        }
    }

    ModalBottomSheet(
        onDismissRequest = hub::close,
        sheetState = sheetState,
        containerColor = DeepSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI & Browser Hub", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Local Core → On-device → veröffentlichte App-Schnittstelle → Play Store",
                        color = MutedMist,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                IconButton(onClick = hub::close) {
                    Icon(Icons.Rounded.Close, contentDescription = "AI Hub schließen")
                }
            }

            Surface(
                color = Mint.copy(alpha = 0.10f),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("API-KEY-FREI ZUERST", color = Mint, style = MaterialTheme.typography.labelMedium)
                    Text(
                        "KoSch nutzt lokale Regeln und veröffentlichte Android-Funktionen zuerst. Browser-KI wird nur als direkter Einstieg angeboten, wenn die App selbst einen Shortcut oder ein Widget veröffentlicht.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            OutlinedTextField(
                value = hub.prompt,
                onValueChange = hub::updatePrompt,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Was möchtest du tun?") },
                placeholder = { Text("z. B. Recherchiere aktuelle Quellen · Fasse diese Seite zusammen · lokal/offline") },
                minLines = 2,
                maxLines = 4,
            )

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AiHubFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option.title) },
                    )
                }
                if (hub.hiddenIds.isNotEmpty()) {
                    AssistChip(
                        onClick = { hub.restoreAll() },
                        label = { Text("${hub.hiddenIds.size} wiederherstellen") },
                        leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
            }

            if (filter == AiHubFilter.SMART) {
                Surface(
                    color = Sky.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Sky)
                        Column {
                            Text(
                                "KoSch empfiehlt · ${hub.inferredTask().title}",
                                color = Sky,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Verfügbarkeit und bestätigte Fähigkeiten werden lokal bewertet.",
                                color = MutedMist,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            hub.notice?.let { message ->
                Surface(color = Violet.copy(alpha = 0.10f), shape = RoundedCornerShape(14.dp)) {
                    Text(
                        message,
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Keine sichtbaren Einträge", color = MutedMist)
                        if (hub.hiddenIds.isNotEmpty()) {
                            TextButton(onClick = { hub.restoreAll() }) { Text("Ausgeblendete Vorschläge zurückholen") }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(entries, key = AiHubEntry::stableId) { entry ->
                        AiHubCard(
                            entry = entry,
                            hasPrompt = hub.prompt.isNotBlank(),
                            recommendationReason = if (filter == AiHubFilter.SMART) {
                                recommendationReasons[entry.stableId]
                            } else {
                                null
                            },
                            onOpen = { hub.execute(entry) },
                            onDismiss = { hub.dismiss(entry) },
                        )
                    }
                    item { Spacer(Modifier.size(12.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiHubCard(
    entry: AiHubEntry,
    hasPrompt: Boolean,
    recommendationReason: String?,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RaisedSurface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = when (entry.kind) {
                        AiHubEntryKind.BROWSER, AiHubEntryKind.SYSTEM_BROWSER -> Sky.copy(alpha = 0.14f)
                        AiHubEntryKind.LOCAL_LLM_APP -> Mint.copy(alpha = 0.16f)
                        AiHubEntryKind.LLM_APP -> Violet.copy(alpha = 0.14f)
                    },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (entry.kind == AiHubEntryKind.BROWSER || entry.kind == AiHubEntryKind.SYSTEM_BROWSER) {
                                Icons.Rounded.Language
                            } else {
                                Icons.Rounded.AutoAwesome
                            },
                            contentDescription = null,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.title, fontWeight = FontWeight.SemiBold)
                    Text(
                        installLabel(entry.installState),
                        color = installColor(entry.installState),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                if (entry.dismissible) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "${entry.title} ausblenden")
                    }
                }
            }

            recommendationReason?.let { reason ->
                Text(
                    "EMPFOHLEN · $reason",
                    color = Mint,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Text(
                entry.subtitle,
                color = MutedMist,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            entry.aiStatusLabel?.let { status ->
                Text(status, color = Sky, style = MaterialTheme.typography.labelSmall)
            }

            if (entry.aiCapabilities.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    entry.aiCapabilities.take(6).forEach { capability ->
                        AssistChip(onClick = {}, label = { Text(capability) })
                    }
                }
            }

            Button(
                onClick = onOpen,
                enabled = entry.installState != AiHubInstallState.UNAVAILABLE,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val icon = when (entry.installState) {
                    AiHubInstallState.STORE_AVAILABLE -> Icons.Rounded.Storefront
                    else -> if (hasPrompt && (entry.kind == AiHubEntryKind.LLM_APP || entry.kind == AiHubEntryKind.LOCAL_LLM_APP)) {
                        Icons.AutoMirrored.Rounded.Send
                    } else {
                        Icons.Rounded.OpenInNew
                    }
                }
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(actionLabel(entry, hasPrompt))
            }
        }
    }
}

private fun installLabel(state: AiHubInstallState): String = when (state) {
    AiHubInstallState.INSTALLED -> "INSTALLIERT"
    AiHubInstallState.STORE_AVAILABLE -> "PLAY STORE"
    AiHubInstallState.WEB_ONLY -> "WEB"
    AiHubInstallState.SYSTEM_AVAILABLE -> "ANDROID STANDARD"
    AiHubInstallState.UNAVAILABLE -> "NICHT VERFÜGBAR"
}

private fun installColor(state: AiHubInstallState): Color = when (state) {
    AiHubInstallState.INSTALLED, AiHubInstallState.SYSTEM_AVAILABLE -> Mint
    AiHubInstallState.STORE_AVAILABLE -> Sky
    AiHubInstallState.WEB_ONLY -> Violet
    AiHubInstallState.UNAVAILABLE -> MutedMist
}

private fun actionLabel(entry: AiHubEntry, hasPrompt: Boolean): String = when (entry.installState) {
    AiHubInstallState.STORE_AVAILABLE -> "Im Play Store ansehen"
    AiHubInstallState.SYSTEM_AVAILABLE -> "Systembrowser öffnen"
    AiHubInstallState.WEB_ONLY -> "Web öffnen"
    AiHubInstallState.UNAVAILABLE -> "Nicht verfügbar"
    AiHubInstallState.INSTALLED -> if (
        hasPrompt && (entry.kind == AiHubEntryKind.LLM_APP || entry.kind == AiHubEntryKind.LOCAL_LLM_APP)
    ) {
        "Text übergeben"
    } else {
        "Öffnen"
    }
}
