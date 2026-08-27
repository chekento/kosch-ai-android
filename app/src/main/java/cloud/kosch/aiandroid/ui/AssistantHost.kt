package cloud.kosch.aiandroid.ui

import android.animation.ValueAnimator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.AssistantAgentController
import cloud.kosch.aiandroid.AssistantSessionController
import cloud.kosch.aiandroid.AssistantVoiceController
import cloud.kosch.aiandroid.LauncherController
import cloud.kosch.aiandroid.model.AssistantMessageRole
import cloud.kosch.aiandroid.model.AssistantVisualState
import cloud.kosch.aiandroid.ui.components.AssistantInteractiveAvatar
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Warm

@Composable
fun AssistantHost(
    assistant: AssistantSessionController,
    agent: AssistantAgentController,
    voice: AssistantVoiceController,
    launcherController: LauncherController,
    requestVoiceInput: () -> Unit,
    requestDocument: () -> Unit,
    requestContact: () -> Unit,
    requestSpeech: (String) -> Boolean,
    requestVoicePreview: (voiceName: String) -> Boolean,
    stopSpeech: () -> Unit,
    showFloatingTrigger: Boolean = true,
) {
    val effectiveReducedMotion = assistant.settings.reducedMotion || !ValueAnimator.areAnimatorsEnabled()
    if (showFloatingTrigger) {
        Box(Modifier.fillMaxSize()) {
            if (assistant.settings.enabled) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 104.dp)
                        .size(width = 76.dp, height = 68.dp),
                    color = DeepSurface.copy(alpha = 0.97f),
                    shape = RoundedCornerShape(23.dp),
                    tonalElevation = 10.dp,
                ) {
                    AssistantInteractiveAvatar(
                        state = assistant.visualState,
                        speechSignal = assistant.speechSignal,
                        reducedMotion = effectiveReducedMotion,
                        attentionSignal = assistant.attentionSignal,
                        contentDescription = "KoSch Assistent öffnen",
                        onPointerAttention = assistant::pointerAttention,
                        onActivate = assistant::attentionActivated,
                        onClick = assistant::open,
                        assistantId = agent.character.assetPackId,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                AssistChip(
                    onClick = assistant::open,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 104.dp),
                    label = { Text("Assistant aus") },
                    leadingIcon = {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                )
            }
        }
    }

    if (assistant.sheetVisible) {
        AssistantSheet(
            assistant = assistant,
            agent = agent,
            voice = voice,
            launcherController = launcherController,
            requestVoiceInput = requestVoiceInput,
            requestDocument = requestDocument,
            requestContact = requestContact,
            requestSpeech = requestSpeech,
            requestVoicePreview = requestVoicePreview,
            stopSpeech = stopSpeech,
            effectiveReducedMotion = effectiveReducedMotion,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssistantSheet(
    assistant: AssistantSessionController,
    agent: AssistantAgentController,
    voice: AssistantVoiceController,
    launcherController: LauncherController,
    requestVoiceInput: () -> Unit,
    requestDocument: () -> Unit,
    requestContact: () -> Unit,
    requestSpeech: (String) -> Boolean,
    requestVoicePreview: (voiceName: String) -> Boolean,
    stopSpeech: () -> Unit,
    effectiveReducedMotion: Boolean,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var input by remember { mutableStateOf("") }
    var controlsVisible by remember { mutableStateOf(false) }
    val messages = assistant.messages

    ModalBottomSheet(
        onDismissRequest = assistant::closeSheet,
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(width = 86.dp, height = 72.dp),
                    color = RaisedSurface,
                    shape = RoundedCornerShape(22.dp),
                ) {
                    AssistantInteractiveAvatar(
                        state = assistant.visualState,
                        speechSignal = assistant.speechSignal,
                        reducedMotion = effectiveReducedMotion,
                        attentionSignal = assistant.attentionSignal,
                        contentDescription = "KoSch Assistent begrüßen",
                        onPointerAttention = assistant::pointerAttention,
                        onActivate = assistant::attentionActivated,
                        onClick = {},
                        assistantId = agent.character.assetPackId,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("KoSch Assistant", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        assistantStatus(assistant.visualState),
                        color = assistantStatusColor(assistant.visualState),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        "${agent.character.displayName} · lokaler Shell-Core",
                        color = MutedMist,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = { controlsVisible = !controlsVisible }) {
                    Text(if (controlsVisible) "Chat" else "Steuerung")
                }
            }

            if (controlsVisible) {
                AssistantControlCenter(
                    assistant = assistant,
                    agent = agent,
                    voice = voice,
                    requestVoicePreview = requestVoicePreview,
                    stopSpeech = stopSpeech,
                    modifier = Modifier.weight(1f),
                )
            } else {
                AssistantToggleRow(
                    title = "Assistant aktiv",
                    body = "Aus bedeutet: kein Assistant-Voice und kein laufender Chat. Der Ask-Dock bleibt als Launcher-Steuerung verfügbar.",
                    checked = assistant.settings.enabled,
                    onCheckedChange = { enabled ->
                        if (!enabled) stopSpeech()
                        assistant.setEnabled(enabled)
                        agent.setAssistantEnabled(enabled)
                    },
                )
                AssistantToggleRow(
                    title = "Voice Input",
                    body = "Nur nach Tippen; Androids Speech-UI übernimmt die Aufnahme.",
                    checked = assistant.settings.voiceInputEnabled,
                    enabled = assistant.settings.enabled,
                    onCheckedChange = assistant::setVoiceInputEnabled,
                )
                AssistantToggleRow(
                    title = "Antworten vorlesen",
                    body = "Android Text-to-Speech; Mundbewegung folgt Textbereichen und lokalem Audiopegel.",
                    checked = assistant.settings.speechOutputEnabled,
                    enabled = assistant.settings.enabled,
                    onCheckedChange = { enabled ->
                        if (!enabled) stopSpeech()
                        assistant.setSpeechOutputEnabled(enabled)
                    },
                )
                AssistantToggleRow(
                    title = "Bewegung reduzieren",
                    body = "Stoppt Schweben, Blickwanderung und Effektloops. Zustände und vereinfachte Sprachbewegung bleiben erkennbar; Androids Systemeinstellung gilt zusätzlich.",
                    checked = assistant.settings.reducedMotion,
                    enabled = assistant.settings.enabled,
                    onCheckedChange = assistant::setReducedMotion,
                )

                Surface(color = Mint.copy(alpha = 0.09f), shape = RoundedCornerShape(16.dp)) {
                    Text(
                        "Chatverlauf bleibt nur in dieser laufenden Assistant-Sitzung. Der Offline-Build enthält noch kein generatives LLM. Freie KI-Anfragen werden erst nach deiner ausdrücklichen Anbieterwahl weitergegeben.",
                        modifier = Modifier.padding(12.dp),
                        color = MutedMist,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(messages, key = { it.id }) { message ->
                        val user = message.role == AssistantMessageRole.USER
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (user) Arrangement.End else Arrangement.Start,
                        ) {
                            Surface(
                                color = if (user) Sky.copy(alpha = 0.16f) else RaisedSurface,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(if (user) 0.82f else 0.9f),
                            ) {
                                Text(
                                    message.text,
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }

                assistant.handoffPrompt?.let {
                    Button(
                        onClick = { assistant.handoffToProvider(launcherController) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("KI-Anbieter auswählen und übergeben")
                    }
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(4_096) },
                    enabled = assistant.settings.enabled,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mit Assistant schreiben") },
                    placeholder = { Text("Öffne Kamera … oder freie KI-Frage") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            submitAssistantInput(
                                input = input,
                                assistant = assistant,
                                launcherController = launcherController,
                                requestVoiceInput = requestVoiceInput,
                                requestDocument = requestDocument,
                                requestContact = requestContact,
                                requestSpeech = requestSpeech,
                                onConsumed = { input = "" },
                            )
                        },
                    ),
                    trailingIcon = {
                        Row {
                            if (assistant.speechSignal.active) {
                                IconButton(
                                    onClick = stopSpeech,
                                ) {
                                    Icon(Icons.Rounded.Stop, contentDescription = "Vorlesen stoppen")
                                }
                            }
                            IconButton(
                                enabled = assistant.settings.enabled && assistant.settings.voiceInputEnabled,
                                onClick = {
                                    stopSpeech()
                                    assistant.requestVoice(requestVoiceInput)
                                },
                            ) {
                                Icon(Icons.Rounded.KeyboardVoice, contentDescription = "Spracheingabe")
                            }
                        }
                    },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        enabled = assistant.settings.enabled && input.isNotBlank(),
                        onClick = {
                            submitAssistantInput(
                                input = input,
                                assistant = assistant,
                                launcherController = launcherController,
                                requestVoiceInput = requestVoiceInput,
                                requestDocument = requestDocument,
                                requestContact = requestContact,
                                requestSpeech = requestSpeech,
                                onConsumed = { input = "" },
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Senden")
                    }
                    TextButton(
                        onClick = {
                            stopSpeech()
                            assistant.clearSession()
                        },
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Chat löschen")
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun AssistantToggleRow(
    title: String,
    body: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(color = RaisedSurface, shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(body, color = MutedMist, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}

private fun submitAssistantInput(
    input: String,
    assistant: AssistantSessionController,
    launcherController: LauncherController,
    requestVoiceInput: () -> Unit,
    requestDocument: () -> Unit,
    requestContact: () -> Unit,
    requestSpeech: (String) -> Boolean,
    onConsumed: () -> Unit,
) {
    if (input.isBlank()) return
    assistant.submit(
        text = input,
        launcherController = launcherController,
        requestVoiceInput = requestVoiceInput,
        requestDocument = requestDocument,
        requestContact = requestContact,
        requestSpeech = requestSpeech,
    )
    onConsumed()
}

private fun assistantStatus(state: AssistantVisualState): String = when (state) {
    AssistantVisualState.DISABLED -> "AUS"
    AssistantVisualState.IDLE -> "BEREIT"
    AssistantVisualState.LISTENING -> "HÖRT ZU"
    AssistantVisualState.THINKING -> "PRÜFT LOKAL"
    AssistantVisualState.SPEAKING -> "SPRICHT"
    AssistantVisualState.WORKING -> "FÜHRT AUS"
    AssistantVisualState.OFFLINE -> "KI-HANDOFF BEREIT"
    AssistantVisualState.ERROR -> "FEHLER"
}

private fun assistantStatusColor(state: AssistantVisualState) = when (state) {
    AssistantVisualState.ERROR -> Warm
    AssistantVisualState.OFFLINE -> Sky
    AssistantVisualState.DISABLED -> MutedMist
    else -> Mint
}
