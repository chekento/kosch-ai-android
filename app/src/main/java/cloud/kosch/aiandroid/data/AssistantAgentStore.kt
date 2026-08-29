package cloud.kosch.aiandroid.data

import android.content.Context
import android.content.SharedPreferences
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
        assistantName = preferences.getString(KEY_ASSISTANT_NAME, "")
            ?.takeIf(::validAssistantName)
            .orEmpty(),
        presenceMode = enumValueOrDefault(
            value = preferences.getString(KEY_PRESENCE_MODE, null),
            fallback = AssistantPresenceMode.AMBIENT,
        ),
        wakeWordMode = enumValueOrDefault(
            value = preferences.getString(KEY_WAKE_WORD_MODE, null),
            fallback = AssistantWakeWordMode.OFF,
        ),
        customWakeWord = preferences.getString(KEY_CUSTOM_WAKE_WORD, "")
            ?.takeIf(::validWakeWordDraft)
            .orEmpty(),
        localWakeWordOnly = preferences.getBoolean(KEY_LOCAL_WAKE_WORD_ONLY, true),
        screenObservationEnabled = preferences.getBoolean(KEY_SCREEN_OBSERVATION, false),
        cameraObservationEnabled = preferences.getBoolean(KEY_CAMERA_OBSERVATION, false),
        actionExecutionEnabled = preferences.getBoolean(KEY_ACTION_EXECUTION, false),
        confirmationRequiredForExternalActions = preferences.getBoolean(KEY_CONFIRM_EXTERNAL, true),
    )

    fun save(settings: AssistantAgentPreferences) {
        saveInternal(settings = settings, allowObservationOptIn = false, synchronous = false)
    }

    /** Dedicated persistence path for a direct Settings/UI user gesture. */
    fun saveUserObservationOptIn(settings: AssistantAgentPreferences) {
        saveInternal(settings = settings, allowObservationOptIn = true, synchronous = false)
    }

    /**
     * Transaction-friendly restore boundary for an already sanitized portable backup.
     * Observation and action execution must be false and therefore can never be escalated by restore.
     */
    fun restorePortable(settings: AssistantAgentPreferences): Boolean {
        require(!settings.screenObservationEnabled) { "Portable restore cannot enable Screen-Awareness" }
        require(!settings.cameraObservationEnabled) { "Portable restore cannot enable Camera-Awareness" }
        require(!settings.actionExecutionEnabled) { "Portable restore cannot enable action execution" }
        require(settings.confirmationRequiredForExternalActions) {
            "Portable restore must keep external-action confirmation enabled"
        }
        return saveInternal(settings = settings, allowObservationOptIn = false, synchronous = true)
    }

    private fun saveInternal(
        settings: AssistantAgentPreferences,
        allowObservationOptIn: Boolean,
        synchronous: Boolean,
    ): Boolean {
        validate(settings)
        val currentScreen = preferences.getBoolean(KEY_SCREEN_OBSERVATION, false)
        val currentCamera = preferences.getBoolean(KEY_CAMERA_OBSERVATION, false)
        require(allowObservationOptIn || currentScreen || !settings.screenObservationEnabled) {
            "Screen-Awareness darf nur manuell aktiviert werden"
        }
        require(allowObservationOptIn || currentCamera || !settings.cameraObservationEnabled) {
            "Camera-Awareness darf nur manuell aktiviert werden"
        }
        val editor = preferences.edit().write(settings)
        return if (synchronous) editor.commit() else true.also { editor.apply() }
    }

    private fun SharedPreferences.Editor.write(settings: AssistantAgentPreferences): SharedPreferences.Editor =
        putString(KEY_CHARACTER_ID, settings.characterId)
            .putString(KEY_ASSISTANT_NAME, settings.assistantName.trim())
            .putString(KEY_PRESENCE_MODE, settings.presenceMode.name)
            .putString(KEY_WAKE_WORD_MODE, settings.wakeWordMode.name)
            .putString(KEY_CUSTOM_WAKE_WORD, settings.customWakeWord.trim())
            .putBoolean(KEY_LOCAL_WAKE_WORD_ONLY, settings.localWakeWordOnly)
            .putBoolean(KEY_SCREEN_OBSERVATION, settings.screenObservationEnabled)
            .putBoolean(KEY_CAMERA_OBSERVATION, settings.cameraObservationEnabled)
            .putBoolean(KEY_ACTION_EXECUTION, settings.actionExecutionEnabled)
            .putBoolean(KEY_CONFIRM_EXTERNAL, settings.confirmationRequiredForExternalActions)

    private fun validate(settings: AssistantAgentPreferences) {
        require(validIdentifier(settings.characterId)) { "Ungültige Charakter-ID" }
        require(validAssistantName(settings.assistantName)) { "Ungültiger Assistentenname" }
        require(validWakeWordDraft(settings.customWakeWord)) { "Ungültiges Wake Word" }
    }

    private fun validIdentifier(value: String): Boolean =
        value.length in 1..MAX_IDENTIFIER_LENGTH && value.all { it.isLowerCase() || it.isDigit() || it == '_' }

    private fun validAssistantName(value: String): Boolean {
        val normalized = value.trim()
        return normalized.length <= MAX_ASSISTANT_NAME_LENGTH && normalized.none { it.isISOControl() }
    }

    private fun validWakeWordDraft(value: String): Boolean {
        val normalized = value.trim()
        return normalized.length <= MAX_WAKE_WORD_LENGTH &&
            normalized.all { it.isLetterOrDigit() || it == ' ' || it == '-' || it == '_' }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, fallback: T): T =
        runCatching { enumValueOf<T>(value.orEmpty()) }.getOrDefault(fallback)

    private companion object {
        const val PREFERENCES_NAME = "kosch_assistant_agent_preferences"
        const val KEY_CHARACTER_ID = "character_id_v1"
        const val KEY_ASSISTANT_NAME = "assistant_name_v1"
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
        const val MAX_ASSISTANT_NAME_LENGTH = 32
        const val MAX_WAKE_WORD_LENGTH = 32
    }
}
