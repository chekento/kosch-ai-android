package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.AssistantAgentPreferences
import cloud.kosch.aiandroid.model.AssistantPresenceMode
import cloud.kosch.aiandroid.model.AssistantWakeWordMode
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Portable subset of AssistantAgentPreferences.
 *
 * Screen/camera awareness and action execution are intentionally not representable in this format. Decoding always
 * returns those capabilities disabled, so a backup or imported payload can never opt a new device into observation
 * or agent side effects. Device-local voice assignments live in AssistantDeviceVoiceStore and are excluded as well.
 */
object AssistantPortablePreferencesCodec {
    fun encode(preferences: AssistantAgentPreferences): String {
        validatePortable(preferences)
        return buildString {
            append("schema=").append(SCHEMA_VERSION).append('\n')
            append("character=").append(b64(preferences.characterId)).append('\n')
            append("name=").append(b64(preferences.assistantName.trim())).append('\n')
            append("presence=").append(preferences.presenceMode.name).append('\n')
            append("wake=").append(preferences.wakeWordMode.name).append('\n')
            append("customWake=").append(b64(preferences.customWakeWord.trim())).append('\n')
            append("localWakeOnly=").append(if (preferences.localWakeWordOnly) "1" else "0").append('\n')
        }
    }

    fun decode(payload: String): AssistantAgentPreferences {
        require(payload.toByteArray(StandardCharsets.UTF_8).size <= MAX_BYTES) {
            "Assistant preferences payload too large"
        }
        val lines = payload.lineSequence().filter(String::isNotBlank).toList()
        require(lines.size == ALLOWED_KEYS.size) { "Assistant preferences payload is incomplete" }
        val values = linkedMapOf<String, String>()
        lines.forEach { line ->
            val separator = line.indexOf('=')
            require(separator > 0) { "Malformed assistant preferences record" }
            val key = line.substring(0, separator)
            require(key in ALLOWED_KEYS) { "Unknown assistant preferences field: $key" }
            require(key !in values) { "Duplicate assistant preferences field: $key" }
            values[key] = line.substring(separator + 1)
        }
        require(values.keys == ALLOWED_KEYS) { "Assistant preferences payload is incomplete" }
        require(values["schema"] == SCHEMA_VERSION.toString()) { "Unsupported assistant preferences schema" }
        val character = unb64(values.getValue("character"))
        val name = unb64(values.getValue("name"))
        val presence = enumValue<AssistantPresenceMode>(values.getValue("presence"), "presence mode")
        val wake = enumValue<AssistantWakeWordMode>(values.getValue("wake"), "wake-word mode")
        val customWake = unb64(values.getValue("customWake"))
        val localWakeOnly = when (values.getValue("localWakeOnly")) {
            "1" -> true
            "0" -> false
            else -> throw IllegalArgumentException("Invalid local-wake policy")
        }
        return AssistantAgentPreferences(
            characterId = character,
            assistantName = name,
            presenceMode = presence,
            wakeWordMode = wake,
            customWakeWord = customWake,
            localWakeWordOnly = localWakeOnly,
            screenObservationEnabled = false,
            cameraObservationEnabled = false,
            actionExecutionEnabled = false,
            confirmationRequiredForExternalActions = true,
        ).also(::validatePortable)
    }

    private fun validatePortable(preferences: AssistantAgentPreferences) {
        require(preferences.characterId.length in 1..MAX_IDENTIFIER_LENGTH &&
            preferences.characterId.all { it.isLowerCase() || it.isDigit() || it == '_' }) {
            "Invalid assistant character id"
        }
        val name = preferences.assistantName.trim()
        require(name.length <= MAX_ASSISTANT_NAME_LENGTH && name.none { it.isISOControl() }) {
            "Invalid assistant name"
        }
        val wake = preferences.customWakeWord.trim()
        require(wake.length <= MAX_WAKE_WORD_LENGTH &&
            wake.all { it.isLetterOrDigit() || it == ' ' || it == '-' || it == '_' }) {
            "Invalid custom wake word"
        }
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String, label: String): T =
        runCatching { enumValueOf<T>(value) }.getOrElse { throw IllegalArgumentException("Invalid $label") }

    private fun b64(value: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun unb64(value: String): String = try {
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    } catch (exception: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid assistant preferences encoding", exception)
    }

    private val ALLOWED_KEYS = linkedSetOf(
        "schema",
        "character",
        "name",
        "presence",
        "wake",
        "customWake",
        "localWakeOnly",
    )
    private const val SCHEMA_VERSION = 1
    private const val MAX_BYTES = 16 * 1024
    private const val MAX_IDENTIFIER_LENGTH = 48
    private const val MAX_ASSISTANT_NAME_LENGTH = 32
    private const val MAX_WAKE_WORD_LENGTH = 32
}
