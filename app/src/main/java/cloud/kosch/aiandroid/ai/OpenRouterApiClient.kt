package cloud.kosch.aiandroid.ai

import android.content.Context
import cloud.kosch.aiandroid.model.AiSettings
import cloud.kosch.aiandroid.model.PrivacySettings
import cloud.kosch.aiandroid.security.ProviderEndpointPolicy
import cloud.kosch.aiandroid.security.SecureCredentialType
import cloud.kosch.aiandroid.security.SecureCredentialVault
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection

data class OpenRouterModelDescriptor(
    val id: String,
    val name: String,
    val contextLength: Int?,
    val inputModalities: Set<String>,
    val outputModalities: Set<String>,
    val supportedParameters: Set<String>,
    val promptPrice: String?,
    val completionPrice: String?,
) {
    val supportsTextInput: Boolean get() = "text" in inputModalities
    val supportsImageInput: Boolean get() = "image" in inputModalities
    val supportsTextOutput: Boolean get() = "text" in outputModalities
}

data class OpenRouterChatResponse(
    val modelId: String,
    val text: String,
    val requestId: String?,
    val costUsd: Double?,
)

sealed interface OpenRouterApiResult<out T> {
    data class Success<T>(val value: T) : OpenRouterApiResult<T>
    data class Blocked(val reason: String) : OpenRouterApiResult<Nothing>
    data class Failed(val reason: String, val httpStatus: Int? = null) : OpenRouterApiResult<Nothing>
}

/**
 * First direct inference implementation behind KAL Provider Connections.
 *
 * Every call is foreground/user initiated, checked against KAL's two existing network opt-ins and requires an
 * explicitly connected OpenRouter credential. There is no periodic refresh, analytics traffic or background model
 * call. Model capabilities are read from OpenRouter's live catalog instead of hard-coding model names.
 */
class OpenRouterApiClient(
    context: Context,
    private val vault: SecureCredentialVault = SecureCredentialVault(context),
) {
    fun isConnected(): Boolean =
        vault.contains(PROVIDER_ID, SecureCredentialType.OAUTH_GENERATED_KEY) ||
            vault.contains(PROVIDER_ID, SecureCredentialType.API_KEY)

    fun listModels(
        ai: AiSettings,
        privacy: PrivacySettings,
    ): OpenRouterApiResult<List<OpenRouterModelDescriptor>> {
        val gate = gate(ai, privacy, containsUserContent = false)
        if (!gate.allowed) return OpenRouterApiResult.Blocked(gate.reason)

        return withCredential { credential ->
            requestJson(
                url = MODELS_URL,
                method = "GET",
                credential = credential,
            ).mapSuccess { body -> OpenRouterModelCatalogParser.parse(body) }
        }
    }

    fun chat(
        ai: AiSettings,
        privacy: PrivacySettings,
        modelId: String,
        prompt: String,
    ): OpenRouterApiResult<OpenRouterChatResponse> {
        val normalizedModelId = modelId.trim().take(MAX_MODEL_ID_LENGTH)
        val normalizedPrompt = prompt.trim().take(MAX_PROMPT_CHARS)
        if (normalizedModelId.isBlank()) return OpenRouterApiResult.Failed("Kein OpenRouter-Modell ausgewählt")
        if (normalizedPrompt.isBlank()) return OpenRouterApiResult.Failed("Leere Anfrage wird nicht gesendet")

        val gate = gate(ai, privacy, containsUserContent = true)
        if (!gate.allowed) return OpenRouterApiResult.Blocked(gate.reason)

        val requestBody = JSONObject()
            .put("model", normalizedModelId)
            .put(
                "messages",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", normalizedPrompt),
                ),
            )
            .put(
                "provider",
                JSONObject()
                    .put("data_collection", "deny")
                    .put("allow_fallbacks", true),
            )
            .put("stream", false)
            .toString()

        return withCredential { credential ->
            requestJson(
                url = CHAT_URL,
                method = "POST",
                credential = credential,
                body = requestBody,
            ).mapSuccess(OpenRouterChatResponseParser::parse)
        }
    }

    private fun gate(
        ai: AiSettings,
        privacy: PrivacySettings,
        containsUserContent: Boolean,
    ): KalCloudAccessDecision = KalCloudAccessPolicy.evaluate(
        ai = ai,
        privacy = privacy,
        request = KalCloudRequest(
            providerId = PROVIDER_ID,
            origin = KalCloudRequestOrigin.USER_ACTION,
            providerConnected = isConnected(),
            containsUserContent = containsUserContent,
        ),
    )

    private inline fun <T> withCredential(
        block: (CharArray) -> OpenRouterApiResult<T>,
    ): OpenRouterApiResult<T> {
        val credential = vault.read(PROVIDER_ID, SecureCredentialType.OAUTH_GENERATED_KEY)
            ?: vault.read(PROVIDER_ID, SecureCredentialType.API_KEY)
            ?: return OpenRouterApiResult.Blocked("OpenRouter ist nicht verbunden")
        return try {
            block(credential)
        } finally {
            credential.fill('\u0000')
        }
    }

    private fun requestJson(
        url: String,
        method: String,
        credential: CharArray,
        body: String? = null,
    ): OpenRouterApiResult<String> {
        val endpointDecision = ProviderEndpointPolicy.validate(url)
        if (!endpointDecision.allowed) return OpenRouterApiResult.Failed(endpointDecision.reason)

        val connection = (URL(url).openConnection() as HttpsURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer ${String(credential)}")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }

        return try {
            if (body != null) {
                OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                    writer.write(body)
                }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { reader ->
                val text = reader.readText()
                if (text.length > MAX_RESPONSE_CHARS) return OpenRouterApiResult.Failed("Provider-Antwort überschreitet KALs Sicherheitslimit", status)
                text
            }.orEmpty()
            if (status !in 200..299) {
                OpenRouterApiResult.Failed(
                    reason = OpenRouterErrorParser.safeMessage(response, status),
                    httpStatus = status,
                )
            } else if (response.isBlank()) {
                OpenRouterApiResult.Failed("OpenRouter hat eine leere Antwort geliefert", status)
            } else {
                OpenRouterApiResult.Success(response)
            }
        } catch (_: java.net.SocketTimeoutException) {
            OpenRouterApiResult.Failed("OpenRouter-Anfrage hat das Zeitlimit überschritten")
        } catch (_: java.net.UnknownHostException) {
            OpenRouterApiResult.Failed("OpenRouter ist derzeit nicht erreichbar")
        } catch (_: javax.net.ssl.SSLException) {
            OpenRouterApiResult.Failed("Sichere OpenRouter-Verbindung konnte nicht aufgebaut werden")
        } catch (_: Exception) {
            OpenRouterApiResult.Failed("OpenRouter-Anfrage konnte nicht abgeschlossen werden")
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val PROVIDER_ID = "openrouter"
        const val BASE_URL = "https://openrouter.ai/api/v1"
        const val MODELS_URL = "$BASE_URL/models"
        const val CHAT_URL = "$BASE_URL/chat/completions"
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 60_000
        const val MAX_RESPONSE_CHARS = 2_000_000
        const val MAX_PROMPT_CHARS = 200_000
        const val MAX_MODEL_ID_LENGTH = 300
    }
}

object OpenRouterModelCatalogParser {
    fun parse(json: String): List<OpenRouterModelDescriptor> {
        val root = JSONObject(json)
        val data = root.optJSONArray("data") ?: return emptyList()
        return buildList {
            for (index in 0 until data.length()) {
                val model = data.optJSONObject(index) ?: continue
                val id = model.optString("id").trim()
                if (id.isBlank()) continue
                val architecture = model.optJSONObject("architecture")
                val pricing = model.optJSONObject("pricing")
                add(
                    OpenRouterModelDescriptor(
                        id = id,
                        name = model.optString("name").trim().ifBlank { id },
                        contextLength = model.optInt("context_length").takeIf { it > 0 },
                        inputModalities = architecture?.optJSONArray("input_modalities").toStringSet(),
                        outputModalities = architecture?.optJSONArray("output_modalities").toStringSet(),
                        supportedParameters = supportedParameters(model.opt("supported_parameters")),
                        promptPrice = pricing?.optString("prompt")?.takeIf(String::isNotBlank),
                        completionPrice = pricing?.optString("completion")?.takeIf(String::isNotBlank),
                    ),
                )
            }
        }.distinctBy(OpenRouterModelDescriptor::id)
    }

    private fun supportedParameters(value: Any?): Set<String> = when (value) {
        is JSONArray -> value.toStringSet()
        is JSONObject -> value.keys().asSequence().toCollection(linkedSetOf())
        else -> emptySet()
    }

    private fun JSONArray?.toStringSet(): Set<String> {
        if (this == null) return emptySet()
        return buildSet {
            for (index in 0 until length()) {
                optString(index).trim().takeIf(String::isNotBlank)?.let(::add)
            }
        }
    }
}

object OpenRouterChatResponseParser {
    fun parse(json: String): OpenRouterChatResponse {
        val root = JSONObject(json)
        val modelId = root.optString("model").trim().ifBlank { "openrouter" }
        val choices = root.optJSONArray("choices")
            ?: throw IllegalArgumentException("OpenRouter response has no choices")
        val message = choices.optJSONObject(0)?.optJSONObject("message")
            ?: throw IllegalArgumentException("OpenRouter response has no message")
        val text = messageContent(message.opt("content")).trim()
        require(text.isNotBlank()) { "OpenRouter response has no text content" }
        val usage = root.optJSONObject("usage")
        return OpenRouterChatResponse(
            modelId = modelId,
            text = text,
            requestId = root.optString("id").takeIf(String::isNotBlank),
            costUsd = usage?.optDouble("cost")?.takeUnless(Double::isNaN),
        )
    }

    private fun messageContent(content: Any?): String = when (content) {
        is String -> content
        is JSONArray -> buildString {
            for (index in 0 until content.length()) {
                val part = content.optJSONObject(index) ?: continue
                if (part.optString("type") == "text") append(part.optString("text"))
            }
        }
        else -> ""
    }
}

object OpenRouterErrorParser {
    fun safeMessage(json: String, status: Int): String {
        val providerMessage = runCatching {
            val root = JSONObject(json)
            val error = root.optJSONObject("error")
            error?.optString("message")?.trim()?.take(240)
        }.getOrNull().orEmpty()
        return if (providerMessage.isBlank()) {
            "OpenRouter-Anfrage fehlgeschlagen (HTTP $status)"
        } else {
            "OpenRouter: $providerMessage"
        }
    }
}

private inline fun <T, R> OpenRouterApiResult<T>.mapSuccess(transform: (T) -> R): OpenRouterApiResult<R> = when (this) {
    is OpenRouterApiResult.Success -> runCatching { transform(value) }
        .fold(
            onSuccess = { OpenRouterApiResult.Success(it) },
            onFailure = { OpenRouterApiResult.Failed("OpenRouter-Antwort konnte nicht sicher verarbeitet werden") },
        )
    is OpenRouterApiResult.Blocked -> this
    is OpenRouterApiResult.Failed -> this
}
