package cloud.kosch.aiandroid.ui.components

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.AudioFormat
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import cloud.kosch.aiandroid.LauncherViewModel
import cloud.kosch.aiandroid.model.AssistantVisualState
import cloud.kosch.aiandroid.system.DocumentGrantManager
import cloud.kosch.aiandroid.ui.AssistantHost
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Existing Ask-Dock companion, now upgraded into the optional Assistant entry point.
 *
 * The original voice callback remains the fallback if this composable is ever rendered outside the
 * launcher Activity. Inside KoSch the Assistant reuses the Activity-owned LauncherViewModel, so no
 * second launcher runtime or background service is created.
 */
@Composable
fun CompanionFace(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findComponentActivity() }
    val viewModel = remember(activity) {
        activity?.let { ViewModelProvider(it)[LauncherViewModel::class.java] }
    }
    val assistant = viewModel?.assistant
    val launcherController = viewModel?.controller
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val documentGrantManager = remember(context) { DocumentGrantManager(context.applicationContext) }

    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    val speechRuntimeEnabled = assistant?.settings?.enabled == true && assistant.settings.speechOutputEnabled
    val effectiveReducedMotion = assistant?.settings?.reducedMotion == true || !ValueAnimator.areAnimatorsEnabled()

    DisposableEffect(context, assistant, speechRuntimeEnabled) {
        if (assistant == null || !speechRuntimeEnabled) {
            ttsEngine = null
            ttsReady = false
            onDispose { }
        } else {
            var engineReference: TextToSpeech? = null
            val audioFormats = ConcurrentHashMap<String, Int>()
            val engine = TextToSpeech(context.applicationContext) { status ->
                val current = engineReference
                if (current != null) {
                    val ready = if (status == TextToSpeech.SUCCESS) {
                        val language = current.setLanguage(Locale.getDefault())
                        language != TextToSpeech.LANG_MISSING_DATA && language != TextToSpeech.LANG_NOT_SUPPORTED
                    } else {
                        false
                    }
                    mainHandler.post { ttsReady = ready }
                }
            }
            engineReference = engine
            engine.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        mainHandler.post { assistant.speechStarted(utteranceId) }
                    }

                    override fun onDone(utteranceId: String?) {
                        utteranceId?.let(audioFormats::remove)
                        mainHandler.post { assistant.speechFinished(utteranceId) }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        utteranceId?.let(audioFormats::remove)
                        mainHandler.post { assistant.speechFailed(utteranceId) }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        utteranceId?.let(audioFormats::remove)
                        mainHandler.post { assistant.speechFailed(utteranceId) }
                    }

                    override fun onStop(utteranceId: String?, interrupted: Boolean) {
                        utteranceId?.let(audioFormats::remove)
                        mainHandler.post { assistant.speechInterrupted(utteranceId) }
                    }

                    override fun onBeginSynthesis(
                        utteranceId: String?,
                        sampleRateInHz: Int,
                        audioFormat: Int,
                        channelCount: Int,
                    ) {
                        if (utteranceId != null) audioFormats[utteranceId] = audioFormat
                    }

                    override fun onAudioAvailable(utteranceId: String?, audio: ByteArray?) {
                        val samples = audio ?: return
                        val encoding = utteranceId?.let(audioFormats::get) ?: AudioFormat.ENCODING_PCM_16BIT
                        val level = AssistantPcmAmplitude.normalizedRms(samples, encoding)
                        mainHandler.post { assistant.speechAudioLevel(utteranceId, level) }
                    }

                    override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                        mainHandler.post { assistant.speechRange(utteranceId, start, end) }
                    }
                },
            )
            ttsEngine = engine
            onDispose {
                engine.stop()
                engine.shutdown()
                assistant.speechInterrupted()
                if (ttsEngine === engine) ttsEngine = null
                ttsReady = false
            }
        }
    }

    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null || launcherController == null) {
            launcherController?.postNotice("Keine Datei ausgewählt")
        } else {
            documentGrantManager.adopt(uri)
                .onFailure {
                    launcherController.postNotice(
                        "Datei wird geprüft; dauerhafter Lesezugriff wurde nicht gespeichert",
                    )
                }
            launcherController.inspectDocument(uri)
        }
    }
    val requestDocument: () -> Unit = {
        runCatching { documentLauncher.launch(arrayOf("*/*")) }
            .onFailure { launcherController?.postNotice("Die Android-Dateiauswahl ist nicht verfügbar") }
    }

    val contactLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null && launcherController != null) {
            launcherController.consumePickedContact(uri)
        } else {
            launcherController?.postNotice("Keine Kontaktdaten übernommen")
        }
    }
    val requestContact: () -> Unit = {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { contactLauncher.launch(intent) }
            .onFailure { launcherController?.postNotice("Die Android-Kontaktauswahl ist nicht verfügbar") }
    }

    val requestSpeech: (String) -> Boolean = { text ->
        val engine = ttsEngine
        if (!ttsReady || engine == null) {
            launcherController?.postNotice("Android Text-to-Speech ist auf diesem Gerät nicht bereit")
            false
        } else {
            val utteranceId = "kosch-assistant-${System.nanoTime()}"
            assistant?.speechQueued(utteranceId, text)
            val result = runCatching {
                engine.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    utteranceId,
                )
            }.getOrDefault(TextToSpeech.ERROR)
            if (result == TextToSpeech.ERROR) assistant?.speechFailed(utteranceId)
            result != TextToSpeech.ERROR
        }
    }
    val stopSpeech: () -> Unit = {
        ttsEngine?.stop()
        assistant?.speechInterrupted()
    }

    lateinit var requestAssistantVoice: () -> Unit
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (assistant != null && launcherController != null) {
            assistant.consumeVoiceResult(
                spoken = spoken,
                launcherController = launcherController,
                requestVoiceInput = requestAssistantVoice,
                requestDocument = requestDocument,
                requestContact = requestContact,
                requestSpeech = requestSpeech,
            )
        }
    }
    requestAssistantVoice = {
        if (assistant == null || launcherController == null) {
            onClick()
        } else {
            ttsEngine?.stop()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Was möchtest du den KoSch Assistant fragen?")
            }
            runCatching { voiceLauncher.launch(intent) }
                .onFailure {
                    assistant.voiceCancelled()
                    launcherController.postNotice("Auf diesem Gerät ist keine Spracheingabe verfügbar")
                }
        }
    }

    val visualState = assistant?.visualState ?: AssistantVisualState.IDLE
    AssistantInteractiveAvatar(
        state = visualState,
        speechSignal = assistant?.speechSignal ?: AssistantSpeechSignal.Idle,
        reducedMotion = effectiveReducedMotion,
        attentionSignal = assistant?.attentionSignal ?: AssistantAttentionSignal.Idle,
        contentDescription = if (assistant?.settings?.enabled == true) {
            "KoSch Assistant öffnen"
        } else {
            "KoSch Assistant einrichten"
        },
        onPointerAttention = { x, y, pressed -> assistant?.pointerAttention(x, y, pressed) },
        onActivate = { assistant?.attentionActivated() },
        onClick = { assistant?.open() ?: onClick() },
        modifier = modifier,
    )

    if (assistant != null && launcherController != null) {
        AssistantHost(
            assistant = assistant,
            launcherController = launcherController,
            requestVoiceInput = requestAssistantVoice,
            requestDocument = requestDocument,
            requestContact = requestContact,
            requestSpeech = requestSpeech,
            stopSpeech = stopSpeech,
            showFloatingTrigger = false,
        )
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}
