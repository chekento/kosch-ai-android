package cloud.kosch.aiandroid.model

enum class AssistantVisualState {
    DISABLED,
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    WORKING,
    OFFLINE,
    ERROR,
}

enum class AssistantMessageRole {
    USER,
    ASSISTANT,
}

data class AssistantMessage(
    val id: Long,
    val role: AssistantMessageRole,
    val text: String,
    val createdAtEpochMillis: Long,
)

data class AssistantSettings(
    val enabled: Boolean = false,
    val voiceInputEnabled: Boolean = true,
    val speechOutputEnabled: Boolean = false,
    val assistantId: String = "default",
)
