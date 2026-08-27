package cloud.kosch.aiandroid

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.kosch.aiandroid.ai.AssistantLocalCore
import cloud.kosch.aiandroid.ai.AssistantVisualContextRequestParser
import cloud.kosch.aiandroid.ai.LauncherCommand
import cloud.kosch.aiandroid.assistant.AssistantObservationRuntime
import cloud.kosch.aiandroid.assistant.AssistantVisualContextRuntime
import cloud.kosch.aiandroid.data.AssistantStore
import cloud.kosch.aiandroid.model.AssistantMessage
import cloud.kosch.aiandroid.model.AssistantMessageRole
import cloud.kosch.aiandroid.model.AssistantObservationSource
import cloud.kosch.aiandroid.model.AssistantSettings
import cloud.kosch.aiandroid.model.AssistantVisualState
import cloud.kosch.aiandroid.ui.components.AssistantAttentionSignal
import cloud.kosch.aiandroid.ui.components.AssistantSpeechSignal
import cloud.kosch.aiandroid.ui.components.AssistantVisemeMapper
import kotlin.math.sqrt

/**
 * Activity-recreation-safe assistant session. Only settings are persisted; messages remain in memory.
 */
class AssistantSessionController(context: Context) {
    private val store = AssistantStore(context.applicationContext)
    private val localCore = AssistantLocalCore()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var nextMessageId = 1L
    private var activeSpeechText = ""
    private var visualStateAfterSpeech = AssistantVisualState.IDLE
    private var lastVisualReadyRequestId = -1L
    private var lastVisualFailureGeneration = -1L

    var settings by mutableStateOf(store.load())
        private set
    var sheetVisible by mutableStateOf(false)
        private set
    var visualState by mutableStateOf(
        if (settings.enabled) AssistantVisualState.IDLE else AssistantVisualState.DISABLED,
    )
        private set
    var messages by mutableStateOf<List<AssistantMessage>>(emptyList())
        private set
    var awaitingVoice by mutableStateOf(false)
        private set
    var handoffPrompt by mutableStateOf<String?>(null)
        private set
    var speechSignal by mutableStateOf(AssistantSpeechSignal.Idle)
        private set
    var attentionSignal by mutableStateOf(AssistantAttentionSignal.Idle)
        private set

    init {
        AssistantVisualContextRuntime.setEventListener { event ->
            mainHandler.post { handleVisualContextEvent(event) }
        }
    }

    fun open() {
        sheetVisible = true
    }

    fun closeSheet() {
        sheetVisible = false
        if (!awaitingVoice && visualState != AssistantVisualState.SPEAKING) {
            visualState = if (settings.enabled) AssistantVisualState.IDLE else AssistantVisualState.DISABLED
        }
    }

    fun setEnabled(enabled: Boolean) {
        updateSettings(settings.copy(enabled = enabled))
        awaitingVoice = false
        clearSpeechSignal()
        if (!enabled) {
            attentionSignal = AssistantAttentionSignal.Idle
            AssistantVisualContextRuntime.discard()
        }
        visualState = if (enabled) AssistantVisualState.IDLE else AssistantVisualState.DISABLED
        if (enabled && messages.isEmpty()) {
            append(
                AssistantMessageRole.ASSISTANT,
                "Bereit. Launcher-Befehle laufen lokal; freie KI-Anfragen gebe ich nur nach deiner Auswahl weiter.",
            )
        }
    }

    fun setVoiceInputEnabled(enabled: Boolean) {
        updateSettings(settings.copy(voiceInputEnabled = enabled))
        if (!enabled && awaitingVoice) voiceCancelled()
    }

    fun setSpeechOutputEnabled(enabled: Boolean) {
        updateSettings(settings.copy(speechOutputEnabled = enabled))
        if (!enabled && speechSignal.active) speechInterrupted()
    }

    fun setReducedMotion(enabled: Boolean) {
        updateSettings(settings.copy(reducedMotion = enabled))
    }

    fun pointerAttention(normalizedX: Float, normalizedY: Float, pressed: Boolean) {
        if (!settings.enabled) return
        attentionSignal = attentionSignal.pointer(
            normalizedX = normalizedX,
            normalizedY = normalizedY,
            isPressed = pressed,
            nowUptimeMillis = SystemClock.uptimeMillis(),
        )
    }

    fun attentionActivated() {
        if (!settings.enabled) return
        attentionSignal = attentionSignal.activate(SystemClock.uptimeMillis())
    }

    fun clearSession() {
        messages = emptyList()
        handoffPrompt = null
        clearSpeechSignal()
        attentionSignal = AssistantAttentionSignal.Idle
        AssistantVisualContextRuntime.discard()
        visualState = if (settings.enabled) AssistantVisualState.IDLE else AssistantVisualState.DISABLED
    }

    fun close() {
        AssistantVisualContextRuntime.setEventListener(null)
        AssistantVisualContextRuntime.discard()
        mainHandler.removeCallbacksAndMessages(null)
    }

    fun requestVoice(requestVoiceInput: () -> Unit) {
        if (!settings.enabled || !settings.voiceInputEnabled) {
            sheetVisible = true
            append(
                AssistantMessageRole.ASSISTANT,
                if (!settings.enabled) "Aktiviere mich zuerst, damit ich Spracheingaben annehmen kann."
                else "Spracheingabe ist für den Assistenten ausgeschaltet.",
            )
            return
        }
        clearSpeechSignal()
        awaitingVoice = true
        visualState = AssistantVisualState.LISTENING
        requestVoiceInput()
    }

    fun consumeVoiceResult(
        spoken: String?,
        launcherController: LauncherController,
        requestVoiceInput: () -> Unit,
        requestDocument: () -> Unit,
        requestContact: () -> Unit,
        requestSpeech: (String) -> Boolean,
        requestVisualContext: (AssistantObservationSource?) -> Boolean = { false },
    ) {
        if (!awaitingVoice) return
        awaitingVoice = false
        if (spoken.isNullOrBlank()) {
            voiceCancelled()
            return
        }
        sheetVisible = true
        submit(
            text = spoken,
            launcherController = launcherController,
            requestVoiceInput = requestVoiceInput,
            requestDocument = requestDocument,
            requestContact = requestContact,
            requestSpeech = requestSpeech,
            requestVisualContext = requestVisualContext,
        )
    }

    fun voiceCancelled() {
        awaitingVoice = false
        visualState = if (settings.enabled) AssistantVisualState.IDLE else AssistantVisualState.DISABLED
    }

    fun submit(
        text: String,
        launcherController: LauncherController,
        requestVoiceInput: () -> Unit,
        requestDocument: () -> Unit,
        requestContact: () -> Unit,
        requestSpeech: (String) -> Boolean,
        requestVisualContext: (AssistantObservationSource?) -> Boolean = { false },
    ) {
        if (!settings.enabled) {
            sheetVisible = true
            append(AssistantMessageRole.ASSISTANT, "Der Assistent ist ausgeschaltet. Aktiviere ihn zuerst.")
            return
        }
        val input = text.trim().take(MAX_MESSAGE_LENGTH)
        if (input.isBlank()) return

        append(AssistantMessageRole.USER, input)
        handoffPrompt = null
        visualState = AssistantVisualState.THINKING

        val visualRequest = AssistantVisualContextRequestParser.parseRequest(input)
        if (visualRequest != null) {
            val accepted = requestVisualContext(visualRequest.source) ||
                requestActiveVisualContext(visualRequest.source)
            val sourceText = when (visualRequest.source) {
                AssistantObservationSource.SCREEN -> "Bildschirm"
                AssistantObservationSource.CAMERA -> "Kamera"
                null -> "aktiven visuellen"
            }
            val replyText = if (accepted) {
                "Ich fordere genau einen aktuellen $sourceText-Kontextframe an. Die Capture-Session bleibt sichtbar; dieser Frame wird noch an kein KI-Modell übertragen."
            } else {
                "Dafür ist noch keine passende sichtbare Screen- oder Kamera-Session freigegeben. Aktiviere die gewünschte Awareness-Funktion und bestätige Androids Consent."
            }
            append(AssistantMessageRole.ASSISTANT, replyText)
            visualState = if (accepted) AssistantVisualState.WORKING else AssistantVisualState.IDLE
            if (settings.speechOutputEnabled && replyText.isNotBlank()) requestSpeech(replyText)
            return
        }

        val reply = localCore.reply(input)
        append(AssistantMessageRole.ASSISTANT, reply.text)
        handoffPrompt = reply.handoffPrompt

        when (reply.command) {
            null -> visualState = reply.visualState
            LauncherCommand.StartVoice -> requestVoice(requestVoiceInput)
            else -> {
                visualState = AssistantVisualState.WORKING
                sheetVisible = false
                launcherController.submitCommand(
                    text = input,
                    requestVoice = { requestVoice(requestVoiceInput) },
                    requestDocument = requestDocument,
                    requestContact = requestContact,
                )
                visualState = AssistantVisualState.IDLE
            }
        }

        if (
            settings.speechOutputEnabled &&
            !awaitingVoice &&
            reply.text.isNotBlank()
        ) {
            requestSpeech(reply.text)
            // The TTS listener switches to SPEAKING only when Android reports actual playback.
        }
    }

    fun visualContextReady(metadata: AssistantVisualContextRuntime.Metadata) {
        if (!settings.enabled || metadata.requestId == lastVisualReadyRequestId) return
        lastVisualReadyRequestId = metadata.requestId
        val source = when (metadata.source) {
            AssistantObservationSource.SCREEN -> "Bildschirm"
            AssistantObservationSource.CAMERA -> "Kamera"
        }
        val kib = (metadata.byteCount + 1023) / 1024
        append(
            AssistantMessageRole.ASSISTANT,
            "$source-Kontextframe bereit: ${metadata.width}×${metadata.height}, ca. $kib KiB. Er liegt nur kurz im Arbeitsspeicher und wurde noch an kein KI-Modell übertragen.",
        )
        visualState = AssistantVisualState.IDLE
    }

    fun visualContextFailed(message: String, eventGeneration: Long = -1L) {
        if (!settings.enabled || eventGeneration == lastVisualFailureGeneration) return
        lastVisualFailureGeneration = eventGeneration
        append(
            AssistantMessageRole.ASSISTANT,
            "Der visuelle Kontextframe ist fehlgeschlagen: ${message.take(240)}",
        )
        visualState = AssistantVisualState.ERROR
    }

    fun handoffToProvider(launcherController: LauncherController) {
        val prompt = handoffPrompt?.takeIf(String::isNotBlank) ?: return
        sheetVisible = false
        visualState = AssistantVisualState.WORKING
        launcherController.openProviderChooser(prompt)
        visualState = AssistantVisualState.IDLE
    }

    fun speechQueued(utteranceId: String, text: String) {
        if (!settings.enabled || utteranceId.isBlank() || text.isBlank()) return
        if (visualState != AssistantVisualState.SPEAKING) {
            visualStateAfterSpeech = visualState
        }
        activeSpeechText = text.take(MAX_MESSAGE_LENGTH)
        speechSignal = AssistantSpeechSignal(
            utteranceId = utteranceId,
            rangeVisemes = AssistantVisemeMapper.fromText(activeSpeechText),
            rangeStartedAtUptimeMillis = SystemClock.uptimeMillis(),
            amplitude = 0f,
            rangeTimed = false,
        )
    }

    fun speechStarted(utteranceId: String?) {
        if (matchesActiveSpeech(utteranceId) && settings.enabled) {
            visualState = AssistantVisualState.SPEAKING
        }
    }

    fun speechRange(utteranceId: String?, start: Int, end: Int) {
        if (!matchesActiveSpeech(utteranceId)) return
        speechSignal = speechSignal.copy(
            rangeVisemes = AssistantVisemeMapper.fromRange(activeSpeechText, start, end),
            rangeStartedAtUptimeMillis = SystemClock.uptimeMillis(),
            rangeTimed = true,
        )
    }

    fun speechAudioLevel(utteranceId: String?, normalizedRms: Float) {
        if (!matchesActiveSpeech(utteranceId) || !normalizedRms.isFinite()) return
        val perceptualLevel = sqrt(normalizedRms.coerceIn(0f, 1f))
        val smoothed = (speechSignal.amplitude * 0.52f + perceptualLevel * 0.48f).coerceIn(0f, 1f)
        speechSignal = speechSignal.copy(amplitude = smoothed)
    }

    fun speechFinished(utteranceId: String? = speechSignal.utteranceId) {
        if (!matchesActiveSpeech(utteranceId)) return
        clearSpeechSignal()
        if (settings.enabled && !awaitingVoice) visualState = visualStateAfterSpeech
    }

    fun speechInterrupted(utteranceId: String? = speechSignal.utteranceId) {
        if (!matchesActiveSpeech(utteranceId)) return
        clearSpeechSignal()
        visualState = when {
            !settings.enabled -> AssistantVisualState.DISABLED
            awaitingVoice -> AssistantVisualState.LISTENING
            else -> visualStateAfterSpeech
        }
    }

    fun speechFailed(utteranceId: String? = speechSignal.utteranceId) {
        if (!matchesActiveSpeech(utteranceId)) return
        clearSpeechSignal()
        if (settings.enabled) visualState = AssistantVisualState.ERROR
    }

    private fun handleVisualContextEvent(event: AssistantVisualContextRuntime.Event) {
        when (event.status) {
            AssistantVisualContextRuntime.Status.READY -> event.metadata?.let(::visualContextReady)
            AssistantVisualContextRuntime.Status.FAILED -> event.failureMessage?.let { message ->
                visualContextFailed(message, event.generation)
            }
            else -> Unit
        }
    }

    private fun requestActiveVisualContext(requestedSource: AssistantObservationSource?): Boolean {
        val source = requestedSource ?: when {
            AssistantObservationRuntime.screenActive -> AssistantObservationSource.SCREEN
            AssistantObservationRuntime.cameraActive -> AssistantObservationSource.CAMERA
            else -> return false
        }
        val active = when (source) {
            AssistantObservationSource.SCREEN -> AssistantObservationRuntime.screenActive
            AssistantObservationSource.CAMERA -> AssistantObservationRuntime.cameraActive
        }
        if (!active) return false
        AssistantVisualContextRuntime.request(source)
        return true
    }

    private fun updateSettings(updated: AssistantSettings) {
        store.save(updated)
        settings = updated
    }

    private fun matchesActiveSpeech(utteranceId: String?): Boolean =
        utteranceId != null && utteranceId == speechSignal.utteranceId

    private fun clearSpeechSignal() {
        activeSpeechText = ""
        speechSignal = AssistantSpeechSignal.Idle
    }

    private fun append(role: AssistantMessageRole, text: String) {
        val message = AssistantMessage(
            id = nextMessageId++,
            role = role,
            text = text.take(MAX_MESSAGE_LENGTH),
            createdAtEpochMillis = System.currentTimeMillis(),
        )
        messages = (messages + message).takeLast(MAX_SESSION_MESSAGES)
    }

    private companion object {
        const val MAX_MESSAGE_LENGTH = 4_096
        const val MAX_SESSION_MESSAGES = 80
    }
}
