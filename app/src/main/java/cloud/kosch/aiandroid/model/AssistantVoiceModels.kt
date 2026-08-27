package cloud.kosch.aiandroid.model

/**
 * Android TTS exposes concrete voice names but no portable/reliable gender metadata. KoSch therefore
 * stores user-assigned device-local slots for FEMALE / MALE / NEUTRAL and applies the character
 * contract to those slots at runtime.
 */
data class AssistantSystemVoiceOption(
    val name: String,
    val languageTag: String,
    val quality: Int,
    val latency: Int,
    val networkRequired: Boolean,
)

data class AssistantVoiceAssignments(
    val femaleVoiceName: String? = null,
    val maleVoiceName: String? = null,
    val neutralVoiceName: String? = null,
) {
    fun forGender(gender: AssistantVoiceGender): String? = when (gender) {
        AssistantVoiceGender.FEMALE -> femaleVoiceName
        AssistantVoiceGender.MALE -> maleVoiceName
        AssistantVoiceGender.NEUTRAL -> neutralVoiceName
    }

    fun withAssignment(gender: AssistantVoiceGender, voiceName: String?): AssistantVoiceAssignments = when (gender) {
        AssistantVoiceGender.FEMALE -> copy(femaleVoiceName = voiceName)
        AssistantVoiceGender.MALE -> copy(maleVoiceName = voiceName)
        AssistantVoiceGender.NEUTRAL -> copy(neutralVoiceName = voiceName)
    }
}
