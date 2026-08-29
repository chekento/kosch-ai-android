package cloud.kosch.aiandroid

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.kosch.aiandroid.ai.KalCloudAccessMode
import cloud.kosch.aiandroid.ai.KalCloudAccessPolicy
import cloud.kosch.aiandroid.ai.OpenRouterApiClient
import cloud.kosch.aiandroid.ai.OpenRouterApiResult
import cloud.kosch.aiandroid.ai.OpenRouterChatResponse
import cloud.kosch.aiandroid.ai.OpenRouterModelDescriptor
import cloud.kosch.aiandroid.model.AiSettings
import cloud.kosch.aiandroid.model.PrivacySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Lifecycle-owned bridge between KAL UI surfaces and the direct OpenRouter connector.
 *
 * It performs no work on its own: model discovery and chat are triggered only by explicit UI actions. Network I/O
 * runs off the main thread, while every request still passes through [OpenRouterApiClient] and therefore through
 * [KalCloudAccessPolicy]. Credentials never enter this controller or Compose state.
 */
class OpenRouterDirectController(
    context: Context,
    private val scope: CoroutineScope,
    private val settingsProvider: () -> Pair<AiSettings, PrivacySettings>,
    private val api: OpenRouterApiClient = OpenRouterApiClient(context.applicationContext),
) {
    var connected by mutableStateOf(false)
        private set
    var cloudExecutionEnabled by mutableStateOf(false)
        private set
    var loadingModels by mutableStateOf(false)
        private set
    var sending by mutableStateOf(false)
        private set
    var models by mutableStateOf<List<OpenRouterModelDescriptor>>(emptyList())
        private set
    var selectedModelId by mutableStateOf("")
        private set
    var response by mutableStateOf<OpenRouterChatResponse?>(null)
        private set
    var notice by mutableStateOf<String?>(null)
        private set

    init {
        refreshState()
    }

    fun refreshState() {
        connected = api.isConnected()
        val (ai, privacy) = settingsProvider()
        cloudExecutionEnabled = KalCloudAccessPolicy.effectiveMode(ai, privacy) ==
            KalCloudAccessMode.CONNECTED_PROVIDERS_ONLY
        if (!connected) {
            models = emptyList()
            selectedModelId = ""
            response = null
        }
    }

    fun updateModelId(value: String) {
        selectedModelId = value.trim().take(MAX_MODEL_ID_CHARS)
        response = null
    }

    fun chooseModel(modelId: String) {
        val normalized = modelId.trim().take(MAX_MODEL_ID_CHARS)
        if (normalized.isNotBlank()) {
            selectedModelId = normalized
            response = null
        }
    }

    fun loadModels() {
        if (loadingModels || sending) return
        refreshState()
        if (!connected) {
            notice = "OpenRouter ist nicht verbunden"
            return
        }
        val (ai, privacy) = settingsProvider()
        loadingModels = true
        notice = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                api.listModels(ai = ai, privacy = privacy)
            }
            loadingModels = false
            when (result) {
                is OpenRouterApiResult.Success -> {
                    val compatible = result.value
                        .asSequence()
                        .filter { it.supportsTextInput && it.supportsTextOutput }
                        .sortedBy { it.name.lowercase() }
                        .take(MAX_UI_MODELS)
                        .toList()
                    models = compatible
                    if (selectedModelId.isBlank() || compatible.none { it.id == selectedModelId }) {
                        selectedModelId = compatible.firstOrNull()?.id.orEmpty()
                    }
                    notice = if (compatible.isEmpty()) {
                        "OpenRouter hat aktuell keine kompatiblen Textmodelle geliefert"
                    } else {
                        "${compatible.size} kompatible OpenRouter-Modelle geladen"
                    }
                }
                is OpenRouterApiResult.Blocked -> notice = result.reason
                is OpenRouterApiResult.Failed -> notice = result.reason
            }
            refreshState()
        }
    }

    fun send(prompt: String) {
        sendInternal(
            prompt = prompt,
            onSuccess = { response = it },
            onFailure = {},
        )
    }

    /**
     * Starts one Assistant reply only when the same explicit provider gates used by the AI Hub are already satisfied.
     * Returning false means no network request was started and the Assistant must retain its safe provider-handoff UI.
     */
    fun sendForAssistant(
        prompt: String,
        onSuccess: (OpenRouterChatResponse) -> Unit,
        onFailure: (String) -> Unit,
    ): Boolean = sendInternal(
        prompt = prompt,
        onSuccess = onSuccess,
        onFailure = onFailure,
    )

    private fun sendInternal(
        prompt: String,
        onSuccess: (OpenRouterChatResponse) -> Unit,
        onFailure: (String) -> Unit,
    ): Boolean {
        if (sending || loadingModels) return false
        val normalizedPrompt = prompt.trim()
        if (normalizedPrompt.isBlank()) {
            notice = "Bitte zuerst einen Prompt eingeben"
            return false
        }

        refreshState()
        if (!connected) {
            notice = "OpenRouter ist nicht verbunden"
            return false
        }
        if (!cloudExecutionEnabled) {
            notice = "Cloud Access ist für verbundene Provider nicht freigegeben"
            return false
        }
        val modelId = selectedModelId.trim()
        if (modelId.isBlank()) {
            notice = "Bitte zuerst ein OpenRouter-Modell auswählen oder eine Modell-ID eingeben"
            return false
        }

        val (ai, privacy) = settingsProvider()
        sending = true
        response = null
        notice = "Bestätigte Anfrage wird direkt an OpenRouter gesendet …"
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                api.chat(
                    ai = ai,
                    privacy = privacy,
                    modelId = modelId,
                    prompt = normalizedPrompt,
                )
            }
            sending = false
            when (result) {
                is OpenRouterApiResult.Success -> {
                    response = result.value
                    notice = result.value.costUsd?.let { cost ->
                        "OpenRouter-Antwort erhalten · gemeldete Kosten: $cost USD"
                    } ?: "OpenRouter-Antwort erhalten"
                    onSuccess(result.value)
                }
                is OpenRouterApiResult.Blocked -> {
                    notice = result.reason
                    onFailure(result.reason)
                }
                is OpenRouterApiResult.Failed -> {
                    notice = result.reason
                    onFailure(result.reason)
                }
            }
            refreshState()
        }
        return true
    }

    fun clearResponse() {
        response = null
    }

    fun consumeNotice() {
        notice = null
    }

    private companion object {
        const val MAX_MODEL_ID_CHARS = 300
        const val MAX_UI_MODELS = 200
    }
}
