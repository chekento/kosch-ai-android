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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Language
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.AiHubController
import cloud.kosch.aiandroid.OpenRouterDirectController
import cloud.kosch.aiandroid.ai.AiHubDecisionConfidence
import cloud.kosch.aiandroid.ai.AiHubEntry
import cloud.kosch.aiandroid.ai.AiHubEntryKind
import cloud.kosch.aiandroid.ai.AiHubInstallState
import cloud.kosch.aiandroid.ai.AiHubQuickAction
import cloud.kosch.aiandroid.ai.AiHubQuickActionPolicy
import cloud.kosch.aiandroid.ai.AiHubRecommendation
import cloud.kosch.aiandroid.ai.AiHubRouteDecision
import cloud.kosch.aiandroid.model.LaunchableApp
import cloud.kosch.aiandroid.system.AiPublishedShortcutSurface
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
    val routeDecision = hub.routeDecision(apps)
    val currentTask = hub.inferredTask()
    val directProvider = hub.directProvider
    val recommendationReasons = recommendations.associate { it.entry.stableId to it.reason }
    val bestRecommendation = recommendations.firstOrNull()
    val bestShortcut = if (bestRecommendation != null) {
        remember(bestRecommendation.entry.stableId, bestRecommendation.entry.installedApp?.key, hub.prompt) {
            hub.bestPublishedShortcut(bestRecommendation.entry)
        }
    } else {
        null
    }
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
                        "Local Core → On-device → verbundener Provider → App-Schnittstelle → Play Store",
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
                    Text("SMART AI · LOCAL-FIRST", color = Mint, style = MaterialTheme.typography.labelMedium)
                    Text(
                        "KoSch bewertet Aufgabe, echte Gerätefähigkeiten, Privacy-Kontext und deine lokale Präferenz. Kein Provider erhält Inhalt allein durch eine Empfehlung.",
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

            PowerPromptStrip(
                onAction = { action ->
                    hub.updatePrompt(AiHubQuickActionPolicy.apply(action, hub.prompt))
                    filter = AiHubFilter.SMART
                },
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
                        leadingIcon = {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                    )
                }
            }

            if (filter == AiHubFilter.SMART) {
                SmartDecisionHeader(
                    taskTitle = currentTask.title,
                    decision = routeDecision,
                )

                if (bestRecommendation != null && routeDecision != null) {
                    AiHubBestRouteCard(
                        recommendation = bestRecommendation,
                        decision = routeDecision,
                        shortcut = bestShortcut,
                        hasPrompt = hub.prompt.isNotBlank(),
                        canPrefer = hub.canPreferForCurrentTask(bestRecommendation.entry),
                        preferred = hub.isPreferredForCurrentTask(bestRecommendation.entry),
                        onTogglePreferred = { hub.togglePreferredForCurrentTask(bestRecommendation.entry) },
                        onExecute = { hub.executeBestRoute(apps) },
                        onExecuteAlternative = { alternative -> hub.executeRecommendation(alternative) },
                    )
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

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (directProvider != null) {
                    item(key = "direct-openrouter") {
                        DirectOpenRouterCard(
                            direct = directProvider,
                            prompt = hub.prompt,
                            onOpenProviderSettings = hub::openProviderSettings,
                        )
                    }
                }
                if (entries.isEmpty()) {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Keine sichtbaren App-/Browser-Einträge", color = MutedMist)
                                if (hub.hiddenIds.isNotEmpty()) {
                                    TextButton(onClick = { hub.restoreAll() }) {
                                        Text("Ausgeblendete Vorschläge zurückholen")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(entries, key = AiHubEntry::stableId) { entry ->
                        val published = remember(entry.stableId, entry.installedApp?.key) {
                            hub.publishedSurfaces(entry)
                        }
                        AiHubCard(
                            entry = entry,
                            hasPrompt = hub.prompt.isNotBlank(),
                            taskTitle = currentTask.title,
                            recommendationReason = if (filter == AiHubFilter.SMART) {
                                recommendationReasons[entry.stableId]
                            } else {
                                null
                            },
                            canPrefer = hub.canPreferForCurrentTask(entry),
                            preferred = hub.isPreferredForCurrentTask(entry),
                            publishedShortcuts = published.shortcuts,
                            publishedWidgetCount = published.widgets.size,
                            onTogglePreferred = { hub.togglePreferredForCurrentTask(entry) },
                            onPublishedShortcut = hub::execute,
                            onOpen = { hub.execute(entry) },
                            onDismiss = { hub.dismiss(entry) },
                        )
                    }
                }
                item { Spacer(Modifier.size(12.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PowerPromptStrip(
    onAction: (AiHubQuickAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "POWER ACTIONS",
            color = MutedMist,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            AiHubQuickAction.entries.forEach { action ->
                AssistChip(
                    onClick = { onAction(action) },
                    label = { Text(action.title) },
                    leadingIcon = {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                    },
                )
            }
        }
    }
}

@Composable
private fun DirectOpenRouterCard(
    direct: OpenRouterDirectController,
    prompt: String,
    onOpenProviderSettings: () -> Unit,
) {
    val stateLabel = when {
        direct.connected && direct.cloudExecutionEnabled -> "VERBUNDEN · FREIGEGEBEN"
        direct.connected -> "VERBUNDEN · ROUTING AUS"
        else -> "NICHT VERBUNDEN"
    }
    val stateColor = when {
        direct.connected && direct.cloudExecutionEnabled -> Mint
        direct.connected -> Sky
        else -> MutedMist
    }

    Surface(
        color = Violet.copy(alpha = 0.10f),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "DIRECT PROVIDER · OPENROUTER",
                        color = Violet,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Optionaler direkter HTTPS-Pfad; Local Core und App-Handoffs bleiben unabhängig verfügbar.",
                        color = MutedMist,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(stateLabel, color = stateColor, style = MaterialTheme.typography.labelSmall)
            }

            when {
                !direct.connected -> {
                    Text(
                        "Kein Inhalt wird an OpenRouter gesendet. Verbinde den Provider zuerst bewusst in den API-/Provider-Einstellungen.",
                        color = MutedMist,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(onClick = onOpenProviderSettings, modifier = Modifier.fillMaxWidth()) {
                        Text("Provider verbinden")
                    }
                }
                !direct.cloudExecutionEnabled -> {
                    Text(
                        "OpenRouter ist verbunden, aber die getrennten Netzwerk- und AI-Routing-Freigaben sind noch AUS. Ohne beide Gates findet keine Modellanfrage statt.",
                        color = MutedMist,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(onClick = onOpenProviderSettings, modifier = Modifier.fillMaxWidth()) {
                        Text("Freigaben prüfen")
                    }
                }
                else -> {
                    Text(
                        "Nur ein Tipp auf „Direkt an OpenRouter senden“ überträgt den aktuell sichtbaren Prompt. Screen, Kamera, Dateien oder weitere Launcher-Daten werden nicht automatisch angehängt.",
                        color = MutedMist,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = direct::loadModels,
                            enabled = !direct.loadingModels && !direct.sending,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (direct.loadingModels) "Lade Modelle …" else "Modelle laden")
                        }
                        OutlinedButton(onClick = onOpenProviderSettings) {
                            Text("Provider")
                        }
                    }

                    OutlinedTextField(
                        value = direct.selectedModelId,
                        onValueChange = direct::updateModelId,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("OpenRouter Modell-ID") },
                        placeholder = { Text("Modelle laden oder ID eingeben") },
                        singleLine = true,
                    )

                    if (direct.models.isNotEmpty()) {
                        Text(
                            "${direct.models.size} kompatible Textmodelle geladen · Auswahlvorschläge",
                            color = MutedMist,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            direct.models.take(8).forEach { model ->
                                FilterChip(
                                    selected = direct.selectedModelId == model.id,
                                    onClick = { direct.chooseModel(model.id) },
                                    label = {
                                        Text(
                                            model.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { direct.send(prompt) },
                        enabled = prompt.isNotBlank() &&
                            direct.selectedModelId.isNotBlank() &&
                            !direct.loadingModels &&
                            !direct.sending,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(if (direct.sending) "Sende …" else "Direkt an OpenRouter senden")
                    }
                }
            }

            direct.notice?.let { message ->
                Text(message, color = Sky, style = MaterialTheme.typography.bodySmall)
            }

            direct.response?.let { response ->
                Surface(
                    color = DeepSurface.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text(
                            "OPENROUTER · ${response.modelId}",
                            color = Mint,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            response.text.take(MAX_VISIBLE_PROVIDER_RESPONSE_CHARS),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 24,
                            overflow = TextOverflow.Ellipsis,
                        )
                        response.costUsd?.let { cost ->
                            Text(
                                "Vom Provider gemeldete Kosten: $cost USD",
                                color = MutedMist,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        TextButton(onClick = direct::clearResponse) {
                            Text("Antwort schließen")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartDecisionHeader(
    taskTitle: String,
    decision: AiHubRouteDecision?,
) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "KoSch empfiehlt · $taskTitle",
                    color = Sky,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    decision?.explanation
                        ?: "Verfügbarkeit, bestätigte Fähigkeiten und deine geräte-lokale Aufgabenpräferenz werden bewertet.",
                    color = MutedMist,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            decision?.let {
                ConfidenceBadge(
                    confidence = it.confidence,
                    scoreMargin = it.scoreMargin,
                )
            }
        }
    }
}

@Composable
private fun ConfidenceBadge(
    confidence: AiHubDecisionConfidence,
    scoreMargin: Int?,
) {
    val tint = confidenceColor(confidence)
    Surface(
        color = tint.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                confidence.title,
                color = tint,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            if (scoreMargin != null) {
                Text(
                    "+$scoreMargin Punkte",
                    color = MutedMist,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun AiHubBestRouteCard(
    recommendation: AiHubRecommendation,
    decision: AiHubRouteDecision,
    shortcut: AiPublishedShortcutSurface?,
    hasPrompt: Boolean,
    canPrefer: Boolean,
    preferred: Boolean,
    onTogglePreferred: () -> Unit,
    onExecute: () -> Unit,
    onExecuteAlternative: (AiHubRecommendation) -> Unit,
) {
    val entry = recommendation.entry
    val alternative = decision.alternatives.firstOrNull()
    Surface(
        color = Violet.copy(alpha = 0.13f),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "BESTE ROUTE · ${recommendation.intent.title.uppercase()}",
                    color = Mint,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                ConfidenceBadge(decision.confidence, decision.scoreMargin)
            }
            Text(
                entry.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(recommendation.reason, color = MutedMist, style = MaterialTheme.typography.bodySmall)
            Text(
                shortcut?.let { "Direkt · ${it.label} · von der App veröffentlicht" }
                    ?: "Sicherer Android-Weg · ${actionLabel(entry, hasPrompt)}",
                color = Sky,
                style = MaterialTheme.typography.labelMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onExecute,
                    modifier = Modifier.weight(1f),
                    enabled = entry.installState != AiHubInstallState.UNAVAILABLE,
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        shortcut?.let { "Direkt · ${it.label}" } ?: "Mit ${entry.title} starten",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (canPrefer) {
                    FilterChip(
                        selected = preferred,
                        onClick = onTogglePreferred,
                        label = { Text(if (preferred) "Bevorzugt" else "Merken") },
                    )
                }
            }
            alternative?.let { second ->
                OutlinedButton(
                    onClick = { onExecuteAlternative(second) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = second.entry.installState != AiHubInstallState.UNAVAILABLE,
                ) {
                    Text(
                        if (decision.confidence == AiHubDecisionConfidence.LOW) {
                            "Gleichwertige Alternative · ${second.entry.title}"
                        } else {
                            "Schnelle Alternative · ${second.entry.title}"
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
    taskTitle: String,
    recommendationReason: String?,
    canPrefer: Boolean,
    preferred: Boolean,
    publishedShortcuts: List<AiPublishedShortcutSurface>,
    publishedWidgetCount: Int,
    onTogglePreferred: () -> Unit,
    onPublishedShortcut: (AiPublishedShortcutSurface) -> Unit,
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
                            imageVector = if (
                                entry.kind == AiHubEntryKind.BROWSER || entry.kind == AiHubEntryKind.SYSTEM_BROWSER
                            ) {
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

            if (publishedShortcuts.isNotEmpty() || publishedWidgetCount > 0) {
                Text(
                    "VON DER APP VERÖFFENTLICHT",
                    color = Mint,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    publishedShortcuts.take(4).forEach { shortcut ->
                        AssistChip(
                            onClick = { onPublishedShortcut(shortcut) },
                            label = { Text(shortcut.label, maxLines = 1) },
                            leadingIcon = {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                        )
                    }
                    if (publishedWidgetCount > 0) {
                        Surface(
                            color = Sky.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                if (publishedWidgetCount == 1) {
                                    "1 Widget veröffentlicht"
                                } else {
                                    "$publishedWidgetCount Widgets veröffentlicht"
                                },
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                color = Sky,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }

            if (canPrefer) {
                FilterChip(
                    selected = preferred,
                    onClick = onTogglePreferred,
                    label = {
                        Text(if (preferred) "Bevorzugt für $taskTitle" else "Für $taskTitle bevorzugen")
                    },
                )
            }

            Button(
                onClick = onOpen,
                enabled = entry.installState != AiHubInstallState.UNAVAILABLE,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val icon = when (entry.installState) {
                    AiHubInstallState.STORE_AVAILABLE -> Icons.Rounded.Storefront
                    else -> if (
                        hasPrompt &&
                        (entry.kind == AiHubEntryKind.LLM_APP || entry.kind == AiHubEntryKind.LOCAL_LLM_APP)
                    ) {
                        Icons.AutoMirrored.Rounded.Send
                    } else {
                        Icons.AutoMirrored.Rounded.OpenInNew
                    }
                }
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(actionLabel(entry, hasPrompt))
            }
        }
    }
}

private fun confidenceColor(confidence: AiHubDecisionConfidence): Color = when (confidence) {
    AiHubDecisionConfidence.HIGH -> Mint
    AiHubDecisionConfidence.MEDIUM -> Sky
    AiHubDecisionConfidence.LOW -> Violet
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

private const val MAX_VISIBLE_PROVIDER_RESPONSE_CHARS = 20_000
