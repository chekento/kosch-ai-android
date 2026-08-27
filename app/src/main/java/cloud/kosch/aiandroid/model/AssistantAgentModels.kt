package cloud.kosch.aiandroid.model

/**
 * Product-level presentation modes. The character is only the visual/persona layer; the agent
 * runtime and provider selection remain independent from it.
 */
enum class AssistantPresenceMode {
    PORTAL_ONLY,
    AMBIENT,
    FLOATING,
    FULL_COMPANION,
    AGENT,
}

enum class AssistantWakeWordMode {
    OFF,
    COMPUTER,
    ASSISTANT_NAME,
    CUSTOM,
}

enum class AssistantObservationSource {
    SCREEN,
    CAMERA,
}

enum class AssistantVoiceGender {
    FEMALE,
    MALE,
    NEUTRAL,
}

enum class AssistantAgentState {
    DISABLED,
    IDLE,
    ARMED,
    LISTENING,
    THINKING,
    SPEAKING,
    OBSERVING_SCREEN,
    OBSERVING_CAMERA,
    ACTING,
    PRIVACY_BLOCKED,
    ERROR,
}

enum class AssistantActionRisk {
    LOCAL_READ_ONLY,
    LOCAL_REVERSIBLE,
    EXTERNAL_SIDE_EFFECT,
    SENSITIVE_SIDE_EFFECT,
}

data class AssistantCharacterProfile(
    val id: String,
    val displayName: String,
    val assetPackId: String,
    val personaProfileId: String,
    val voiceProfileId: String,
    val voiceGender: AssistantVoiceGender,
    val supportedPresenceModes: Set<AssistantPresenceMode> = AssistantPresenceMode.entries.toSet(),
)

/**
 * Persistent, portable preferences only. Runtime consent tokens for screen/camera capture must never
 * be written here; Android grants remain session/device owned.
 *
 * Screen and camera awareness are deliberately false by default and may only be enabled through a
 * user-initiated control path. They are never inferred from Agent mode, provider choice or character.
 */
data class AssistantAgentPreferences(
    val characterId: String = "default",
    val presenceMode: AssistantPresenceMode = AssistantPresenceMode.AMBIENT,
    val wakeWordMode: AssistantWakeWordMode = AssistantWakeWordMode.OFF,
    val customWakeWord: String = "",
    val localWakeWordOnly: Boolean = true,
    val screenObservationEnabled: Boolean = false,
    val cameraObservationEnabled: Boolean = false,
    val actionExecutionEnabled: Boolean = false,
    val confirmationRequiredForExternalActions: Boolean = true,
) {
    fun observationEnabled(source: AssistantObservationSource): Boolean = when (source) {
        AssistantObservationSource.SCREEN -> screenObservationEnabled
        AssistantObservationSource.CAMERA -> cameraObservationEnabled
    }
}
