package cloud.kosch.aiandroid.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.AssistantAgentController
import cloud.kosch.aiandroid.AssistantSessionController
import cloud.kosch.aiandroid.AssistantVoiceController
import cloud.kosch.aiandroid.ai.AssistantCharacterCatalog
import cloud.kosch.aiandroid.ai.AssistantWakeWordResolver
import cloud.kosch.aiandroid.assistant.AssistantObservationRuntime
import cloud.kosch.aiandroid.model.AssistantAgentState
import cloud.kosch.aiandroid.model.AssistantObservationSource
import cloud.kosch.aiandroid.model.AssistantPresenceMode
import cloud.kosch.aiandroid.model.AssistantSystemVoiceOption
import cloud.kosch.aiandroid.model.AssistantVoiceGender
import cloud.kosch.aiandroid.model.AssistantVisualState
import cloud.kosch.aiandroid.model.AssistantWakeWordMode
import cloud.kosch.aiandroid.ui.components.AssistantCameraSessionPreview
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Warm
import java.util.Locale

@Composable
fun AssistantControlCenter(
    assistant: AssistantSessionController,
    agent: AssistantAgentController,
    voice: AssistantVoiceController,
    requestVoicePreview: (voiceName: String) -> Boolean,
    stopSpeech: () -> Unit,
    requestScreenSession: () -> Unit,
    stopScreenSession: () -> Unit,
    cameraSessionRequested: Boolean,
    requestCameraSession: () -> Unit,
    stopCameraSession: () -> Unit,
    onCameraSessionStarted: () -> Unit,
    onCameraSessionFailure: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val character = agent.character
    val requiredGender = character.voiceGender
    val assignedVoice = voice.assignedVoice(requiredGender)
    val locale = Locale.getDefault()
    val reservedOppositeVoice = when (requiredGender) {
        AssistantVoiceGender.FEMALE -> voice.assignments.maleVoiceName
        AssistantVoiceGender.MALE -> voice.assignments.femaleVoiceName
        AssistantVoiceGender.NEUTRAL -> null
    }
    val voiceOptions = remember(voice.availableVoices, locale, requiredGender, reservedOppositeVoice) {
        prioritizeVoices(
            voice.availableVoices.filterNot { it.name == reservedOppositeVoice },
            locale,
        ).take(MAX_VISIBLE_VOICES)
    }
    val effectiveWakeWord = AssistantWakeWordResolver.resolve(agent.preferences, character)
    val screenActive = AssistantObservationRuntime.screenActive
    val cameraActive = AssistantObservationRuntime.cameraActive

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag(ASSISTANT_CONTROL_CENTER_SCROLL_TAG),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ControlSection(
                title = "Privacy Live",
                body = "Aktive Sensor-/Agent-Zustände sind immer sichtbar. Freigabe bedeutet nicht automatisch Aufnahme.",
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        PrivacyStatusChip(
                            label = "MIC",
                            active = assistant.awaitingVoice || assistant.visualState == AssistantVisualState.LISTENING,
                        )
                    }
                    item { PrivacyStatusChip(label = "SCREEN", active = screenActive) }
                    item { PrivacyStatusChip(label = "CAM", active = cameraActive) }
                    item {
                        PrivacyStatusChip(
                            label = "ACTING",
                            active = agent.state == AssistantAgentState.ACTING,
                        )
                    }
                }
                if (screenActive) {
                    Text(
                        "Screen Share: ${AssistantObservationRuntime.screenWidth}×${AssistantObservationRuntime.screenHeight} · ${AssistantObservationRuntime.screenFrameCount} flüchtige Frames beobachtet",
                        color = MutedMist,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (cameraActive) {
                    Text(
                        "Camera Live: ${AssistantObservationRuntime.cameraFrameCount} flüchtige Frames beobachtet",
                        color = MutedMist,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        item {
            ControlSection(
                title = "Charakter & Name",
                body = "Darstellung, Rufname und Stimme wechseln; Provider, Rechte und Chat werden nicht mit dem Avatar gekoppelt.",
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AssistantCharacterCatalog.all(), key = { it.id }) { profile ->
                        FilterChip(
                            selected = character.id == profile.id,
                            onClick = {
                                stopSpeech()
                                agent.selectCharacter(profile.id)
                            },
                            label = { Text(profile.displayName) },
                        )
                    }
                }
                OutlinedTextField(
                    value = agent.preferences.assistantName,
                    onValueChange = agent::setAssistantName,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Name des Assistenten") },
                    placeholder = { Text(character.displayName) },
                    supportingText = {
                        Text("Dieser Rufname wird bei Wake Word = Assistentenname verwendet.")
                    },
                )
                Text(
                    "Aktiv: ${character.displayName} · Stimme ${voiceGenderLabel(requiredGender)}",
                    color = MutedMist,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        item {
            ControlSection(
                title = "Stimme",
                body = voiceSectionBody(requiredGender),
            ) {
                val assignedName = assignedVoice?.name ?: voice.assignments.forGender(requiredGender)
                Text(
                    when {
                        assignedVoice != null -> "Zugeordnet: ${voiceDisplayName(assignedVoice)}"
                        requiredGender == AssistantVoiceGender.NEUTRAL -> "Keine feste Zuordnung · Systemstimme erlaubt"
                        assignedName != null -> "Gespeicherte Stimme ist aktuell nicht verfügbar"
                        else -> "Noch keine ${voiceGenderLabel(requiredGender)}e Stimme zugeordnet"
                    },
                    color = if (requiredGender != AssistantVoiceGender.NEUTRAL && assignedVoice == null) Warm else MutedMist,
                    style = MaterialTheme.typography.bodySmall,
                )

                voice.statusMessage?.let { message ->
                    Text(message, color = Warm, style = MaterialTheme.typography.bodySmall)
                }

                if (voiceOptions.isEmpty()) {
                    Text(
                        "Android TTS meldet noch keine passenden auswählbaren Stimmen.",
                        color = MutedMist,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(voiceOptions, key = { it.name }) { option ->
                            FilterChip(
                                selected = assignedVoice?.name == option.name,
                                onClick = {
                                    stopSpeech()
                                    voice.assignFromUser(requiredGender, option.name)
                                },
                                label = { Text(voiceDisplayName(option)) },
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = assignedVoice != null,
                        onClick = { assignedVoice?.let { requestVoicePreview(it.name) } },
                    ) {
                        Text("Probehören")
                    }
                    TextButton(
                        enabled = assignedName != null,
                        onClick = {
                            stopSpeech()
                            voice.assignFromUser(requiredGender, null)
                        },
                    ) {
                        Text("Zuordnung löschen")
                    }
                }
                assignedVoice?.takeIf { it.networkRequired }?.let {
                    Text(
                        "Diese TTS-Stimme meldet Netzwerkbedarf. Eine lokale Stimme ist für Privacy/Offline vorzuziehen.",
                        color = Warm,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (requiredGender != AssistantVoiceGender.NEUTRAL) {
                    Text(
                        "Die Zuordnung erfolgt bewusst nach Probehören, weil Android TTS kein verlässliches plattformweites Geschlechtsmerkmal für Stimmen liefert.",
                        color = MutedMist,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        item {
            ControlSection(
                title = "Presence Mode · vorbereitet",
                body = "Die Auswahl wird bereits gespeichert. Die vollständige visuelle/runtime-seitige Differenzierung von Portal, Ambient, Floating, Full und Agent ist in diesem APK noch nicht komplett implementiert; Rechte bleiben davon unabhängig.",
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AssistantPresenceMode.entries, key = { it.name }) { mode ->
                        FilterChip(
                            selected = agent.preferences.presenceMode == mode,
                            onClick = { agent.setPresenceMode(mode) },
                            label = { Text(presenceLabel(mode)) },
                        )
                    }
                }
            }
        }

        item {
            ControlSection(
                title = "Wake Word · vorbereitet",
                body = "Standardmäßig aus. „Computer“, Assistentenname oder ein eigenes Wake Word können als lokale Policy vorbereitet werden. Dieses APK enthält noch keinen kontinuierlich lauschenden Wake-Word-Detektor; die Auswahl startet keine Mikrofonaufnahme.",
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AssistantWakeWordMode.entries, key = { it.name }) { mode ->
                        FilterChip(
                            selected = agent.preferences.wakeWordMode == mode,
                            onClick = { agent.setWakeWord(mode) },
                            label = { Text(wakeWordLabel(mode)) },
                        )
                    }
                }
                if (agent.preferences.wakeWordMode == AssistantWakeWordMode.CUSTOM) {
                    OutlinedTextField(
                        value = agent.preferences.customWakeWord,
                        onValueChange = { agent.setWakeWord(AssistantWakeWordMode.CUSTOM, it.take(32)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Eigenes Wake Word") },
                        supportingText = { Text("Mindestens 2 Zeichen; Konfiguration bleibt speicherbar, Detektor noch nicht aktiv.") },
                    )
                }
                Text(
                    if (effectiveWakeWord == null) {
                        "Konfiguriert: AUS / noch ungültig · Detektor nicht aktiv"
                    } else {
                        "Konfiguriert: „$effectiveWakeWord“ · Detektor noch nicht aktiv"
                    },
                    color = if (agent.preferences.wakeWordMode != AssistantWakeWordMode.OFF && effectiveWakeWord == null) Warm else MutedMist,
                    style = MaterialTheme.typography.bodySmall,
                )
                ControlToggleRow(
                    title = "Lokaler Wake-Word-Modus · vorbereitet",
                    body = "Policy für eine künftige lokale Erkennung. Dieser Schalter startet heute keine Audioaufnahme und sendet kein Aktivierungs-Audio an einen Netzwerkprovider.",
                    checked = agent.preferences.localWakeWordOnly,
                    onCheckedChange = agent::setLocalWakeWordOnly,
                )
            }
        }

        item {
            ControlSection(
                title = "Screen & Camera Awareness",
                body = "Capability-Freigabe und laufende Session sind getrennt. Eine Live-Quelle gleichzeitig; Start und Stop bleiben sichtbar.",
            ) {
                ControlToggleRow(
                    title = "Screen Awareness",
                    body = "Erlaubt die Fähigkeit. Jede echte Session braucht zusätzlich Androids sichtbaren Screen-Share-Consent.",
                    checked = agent.preferences.screenObservationEnabled,
                    onCheckedChange = { enabled ->
                        if (!enabled && screenActive) stopScreenSession()
                        agent.setObservationEnabledFromUser(AssistantObservationSource.SCREEN, enabled)
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = assistant.settings.enabled && agent.preferences.screenObservationEnabled,
                        onClick = if (screenActive) stopScreenSession else requestScreenSession,
                    ) {
                        Text(if (screenActive) "Screen Share stoppen" else "Screen Share starten")
                    }
                }
                Text(
                    if (screenActive) {
                        "Android MediaProjection läuft als sichtbarer Foreground Service. Stoppen geht hier oder über die Systembenachrichtigung."
                    } else {
                        "Kein Bildschirm wird erfasst. Die Capability allein startet keine Aufnahme."
                    },
                    color = MutedMist,
                    style = MaterialTheme.typography.bodySmall,
                )

                ControlToggleRow(
                    title = "Camera Awareness",
                    body = "Erlaubt die Fähigkeit. Live-Kamera läuft nur mit Runtime-Permission und sichtbarer Vorschau in diesem Control Center.",
                    checked = agent.preferences.cameraObservationEnabled,
                    onCheckedChange = { enabled ->
                        if (!enabled && cameraSessionRequested) stopCameraSession()
                        agent.setObservationEnabledFromUser(AssistantObservationSource.CAMERA, enabled)
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = assistant.settings.enabled && agent.preferences.cameraObservationEnabled,
                        onClick = if (cameraSessionRequested) stopCameraSession else requestCameraSession,
                    ) {
                        Text(if (cameraSessionRequested) "Kamera stoppen" else "Kamera starten")
                    }
                }
                if (cameraSessionRequested) {
                    AssistantCameraSessionPreview(
                        onStarted = onCameraSessionStarted,
                        onStopped = stopCameraSession,
                        onFailure = onCameraSessionFailure,
                    )
                    Text(
                        "Die Vorschau ist der Privacy-Indikator: verlässt du diese Fläche, wird CameraX sofort ungebunden.",
                        color = MutedMist,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text(
                        "Keine Kamera-Session aktiv. KoSch hält keine Kamera im Hintergrund offen.",
                        color = MutedMist,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        item {
            ControlSection(
                title = "Agent-Rechte",
                body = "Lokales Lesen bleibt getrennt von Aktionen mit Seiteneffekten.",
            ) {
                ControlToggleRow(
                    title = "Aktionen ausführen",
                    body = "Erlaubt reversible lokale Agent-Aktionen. Externe/sensitive Aktionen bleiben separat geschützt.",
                    checked = agent.preferences.actionExecutionEnabled,
                    onCheckedChange = agent::setActionExecutionEnabled,
                )
                ControlToggleRow(
                    title = "Externe Aktionen bestätigen",
                    body = "Vor Nachrichten, externen Änderungen oder sensitiven Seiteneffekten explizit nachfragen.",
                    checked = agent.preferences.confirmationRequiredForExternalActions,
                    onCheckedChange = agent::setConfirmationRequiredForExternalActions,
                )
            }
        }
    }
}

@Composable
private fun ControlSection(
    title: String,
    body: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(color = RaisedSurface, shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, color = MutedMist, style = MaterialTheme.typography.bodySmall)
            content()
        }
    }
}

@Composable
private fun ControlToggleRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(body, color = MutedMist, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PrivacyStatusChip(label: String, active: Boolean) {
    AssistChip(
        onClick = {},
        label = { Text("$label ${if (active) "AKTIV" else "AUS"}") },
        enabled = false,
    )
}

private fun prioritizeVoices(
    voices: List<AssistantSystemVoiceOption>,
    locale: Locale,
): List<AssistantSystemVoiceOption> {
    val exact = locale.toLanguageTag()
    val language = locale.language
    return voices.sortedWith(
        compareBy<AssistantSystemVoiceOption>(
            { it.networkRequired },
            { if (it.languageTag.equals(exact, ignoreCase = true)) 0 else 1 },
            { if (Locale.forLanguageTag(it.languageTag).language == language) 0 else 1 },
            { -it.quality },
            { it.name },
        ),
    )
}

private fun voiceDisplayName(voice: AssistantSystemVoiceOption): String {
    val compactName = voice.name.takeLast(28)
    val locale = voice.languageTag.ifBlank { "?" }
    return "$compactName · $locale"
}

private fun voiceGenderLabel(gender: AssistantVoiceGender): String = when (gender) {
    AssistantVoiceGender.FEMALE -> "weiblich"
    AssistantVoiceGender.MALE -> "männlich"
    AssistantVoiceGender.NEUTRAL -> "neutral"
}

private fun voiceSectionBody(gender: AssistantVoiceGender): String = when (gender) {
    AssistantVoiceGender.FEMALE -> "Dieser Charakter darf ausschließlich den weiblichen Voice-Slot verwenden."
    AssistantVoiceGender.MALE -> "Dieser Charakter darf ausschließlich den männlichen Voice-Slot verwenden."
    AssistantVoiceGender.NEUTRAL -> "Der neutrale Default darf die Systemstimme verwenden oder optional fest zugeordnet werden."
}

private fun presenceLabel(mode: AssistantPresenceMode): String = when (mode) {
    AssistantPresenceMode.PORTAL_ONLY -> "Portal"
    AssistantPresenceMode.AMBIENT -> "Ambient"
    AssistantPresenceMode.FLOATING -> "Floating"
    AssistantPresenceMode.FULL_COMPANION -> "Full"
    AssistantPresenceMode.AGENT -> "Agent"
}

private fun wakeWordLabel(mode: AssistantWakeWordMode): String = when (mode) {
    AssistantWakeWordMode.OFF -> "Aus"
    AssistantWakeWordMode.COMPUTER -> "Computer"
    AssistantWakeWordMode.ASSISTANT_NAME -> "Assistentenname"
    AssistantWakeWordMode.CUSTOM -> "Eigenes"
}

internal const val ASSISTANT_CONTROL_CENTER_SCROLL_TAG = "assistant-control-center-scroll"
private const val MAX_VISIBLE_VOICES = 14