package cloud.kosch.aiandroid.data

import android.content.Context
import cloud.kosch.aiandroid.model.AssistantAgentPreferences
import cloud.kosch.aiandroid.model.AssistantPresenceMode
import cloud.kosch.aiandroid.model.AssistantWakeWordMode

/**
 * Stores portable assistant-agent preferences only. Screen/camera consent grants, MediaProjection
 * tokens, URI grants and any other device/session-bound capabilities are intentionally excluded.
 *
 * Generic preference writes are not allowed to escalate screen/camera awareness from off to on.
 * That transition has a dedicated user-opt-in write path for Settings/UI controls.
 */
class AssistantAgentStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): AssistantAgentPreferences = AssistantAgentPreferences(
        characterId = preferences.getString(KEY_CHARACTER_ID, DEFAULT_CHARACTER_ID)
            ?.takeIf(::validIdentifier)
            ?: DEFAULT_CHARACTER_ID,
        presenceMode = enumValueOrDefault(
            value = preferences.getString(KEY_PRESENCE_MODE, null),
            fallback = AssistantPresenceMode.AMBIENT,
        ),
        wakeWordMode = enumValueOrDefault(
            value = preferences.getString(KEY_WAKE_WORD_MODE, null),
            fallback = AssistantWakeWordMode.OFF,
        ),
        customWakeWord = preferences.getString(KEY_CUSTOM_WAKE_WORD, "")
            ?.takeIf(::validWakeWord)
            .orEmpty(),
        localWakeWordOnly = preferences.getBoolean(KEY_LOCAL_WAKE_WORD_ONLY, true),
        screenObservationEnabled = preferences.getBoolean(KEY_SCREEN_OBSERVATION, false),
        cameraObservationEnabled = preferences.getBoolean(KEY_CAMERA_OBSERVATION, false),
        actionExecutionEnabled = preferences.getBoolean(KEY_ACTION_EXECUTION, false),
        confirmationRequiredForExternalActions = preferences.getBoolean(KEY_CONFIRM_EXTERNAL, true),
    )

    fun save(settings: AssistantAgentPreferences) {
        saveInternal(settings = settings, allowObservationOptIn = false)
    }

    /** Dedicated persistence path for a direct Settings/UI user gesture. */
    fun saveUserObservationOptIn(settings: AssistantAgentPreferences) {
        saveInternal(settings = settings, allowObservationOptIn = true)
    }

    private fun saveInternal(settings: AssistantAgentPreferences, allowObservationOptIn: Boolean) {
        require(validIdentifier(settings.characterId)) { "Ungültige Charakter-ID" }
        require(settings.wakeWordMode != AssistantWakeWordMode.CUSTOM || validWakeWord(settings.customWakeWord)) {
            "Ungültiges Wake Word"
        }
        val currentScreen = preferences.getBoolean(KEY_SCREEN_OBSERVATION, false)
        val currentCamera = preferences.getBoolean(KEY_CAMERA_OBSERVATION, false)
        require(allowObservationOptIn || currentScreen || !settings.screenObservationEnabled) {
            "Screen-Awareness darf nur manuell aktiviert werden"
        }
        require(allowObservationOptIn || currentCamera || !settings.cameraObservationEnabled) {
            "Camera-Awareness darf nur manuell aktiviert werden"
        }
        preferences.edit()
            .putString(KEY_CHARACTER_ID, settings.characterId)
            .putString(KEY_PRESENCE_MODE, settings.presenceMode.name)
            .putString(KEY_WAKE_WORD_MODE, settings.wakeWordMode.name)
            .putString(KEY_CUSTOM_WAKE_WORD, settings.customWakeWord.trim())
            .putBoolean(KEY_LOCAL_WAKE_WORD_ONLY, settings.localWakeWordOnly)
            .putBoolean(KEY_SCREEN_OBSERVATION, settings.screenObservationEnabled)
            .putBoolean(KEY_CAMERA_OBSERVATION, settings.cameraObservationEnabled)
            .putBoolean(KEY_ACTION_EXECUTION, settings.actionExecutionEnabled)
            .putBoolean(KEY_CONFIRM_EXTERNAL, settings.confirmationRequiredForExternalActions)
            .apply()
    }

    private fun validIdentifier(value: String): Boolean =
        value.length in 1..MAX_IDENTIFIER_LENGTH && value.all { it.isLowerCase() || it.isDigit() || it == '_' }

    private fun validWakeWord(value: String): Boolean {
        val normalized = value.trim()
        return normalized.length in MIN_WAKE_WORD_LENGTH..MAX_WAKE_WORD_LENGTH &&
            normalized.all { it.isLetterOrDigit() || it == ' ' || it == '-' || it == '_' }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, fallback: T): T =
        runCatching { enumValueOf<T>(value.orEmpty()) }.getOrDefault(fallback)

    private companion object {
        const val PREFERENCES_NAME = "kosch_assistant_agent_preferences"
        const val KEY_CHARACTER_ID = "character_id_v1"
        const val KEY_PRESENCE_MODE = "presence_mode_v1"
        const val KEY_WAKE_WORD_MODE = "wake_word_mode_v1"
        const val KEY_CUSTOM_WAKE_WORD = "custom_wake_word_v1"
        const val KEY_LOCAL_WAKE_WORD_ONLY = "local_wake_word_only_v1"
        const val KEY_SCREEN_OBSERVATION = "screen_observation_enabled_v1"
        const val KEY_CAMERA_OBSERVATION = "camera_observation_enabled_v1"
        const val KEY_ACTION_EXECUTION = "action_execution_enabled_v1"
        const val KEY_CONFIRM_EXTERNAL = "confirm_external_actions_v1"
        const val DEFAULT_CHARACTER_ID = "default"
        const val MAX_IDENTIFIER_LENGTH = 48
        const val MIN_WAKE_WORD_LENGTH = 2
        const val MAX_WAKE_WORD_LENGTH = 32
    }
}
