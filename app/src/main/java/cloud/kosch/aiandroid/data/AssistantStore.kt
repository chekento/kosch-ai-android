package cloud.kosch.aiandroid.data

import android.content.Context
import cloud.kosch.aiandroid.model.AssistantSettings

/**
 * Persists only assistant preferences. Chat transcripts intentionally remain session-only.
 */
class AssistantStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): AssistantSettings = AssistantSettings(
        enabled = preferences.getBoolean(KEY_ENABLED, false),
        voiceInputEnabled = preferences.getBoolean(KEY_VOICE_INPUT, true),
        speechOutputEnabled = preferences.getBoolean(KEY_SPEECH_OUTPUT, false),
        reducedMotion = preferences.getBoolean(KEY_REDUCED_MOTION, false),
        assistantId = preferences.getString(KEY_ASSISTANT_ID, DEFAULT_ASSISTANT_ID)
            ?.takeIf(::validAssistantId)
            ?: DEFAULT_ASSISTANT_ID,
    )

    fun save(settings: AssistantSettings) {
        require(validAssistantId(settings.assistantId)) { "Ungültige Assistenten-ID" }
        preferences.edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putBoolean(KEY_VOICE_INPUT, settings.voiceInputEnabled)
            .putBoolean(KEY_SPEECH_OUTPUT, settings.speechOutputEnabled)
            .putBoolean(KEY_REDUCED_MOTION, settings.reducedMotion)
            .putString(KEY_ASSISTANT_ID, settings.assistantId)
            .apply()
    }

    private fun validAssistantId(value: String): Boolean =
        value.length in 1..MAX_ASSISTANT_ID_LENGTH && value.all { it.isLowerCase() || it.isDigit() || it == '_' }

    private companion object {
        const val PREFERENCES_NAME = "kosch_assistant_settings"
        const val KEY_ENABLED = "assistant_enabled_v1"
        const val KEY_VOICE_INPUT = "voice_input_enabled_v1"
        const val KEY_SPEECH_OUTPUT = "speech_output_enabled_v1"
        const val KEY_REDUCED_MOTION = "reduced_motion_enabled_v1"
        const val KEY_ASSISTANT_ID = "assistant_id_v1"
        const val DEFAULT_ASSISTANT_ID = "default"
        const val MAX_ASSISTANT_ID_LENGTH = 48
    }
}
