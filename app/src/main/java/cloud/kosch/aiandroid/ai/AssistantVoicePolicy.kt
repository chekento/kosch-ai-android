package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.AssistantCharacterProfile
import cloud.kosch.aiandroid.model.AssistantVoiceGender

data class AssistantVoiceProfile(
    val id: String,
    val gender: AssistantVoiceGender,
)

enum class AssistantVoiceDecision {
    ALLOW,
    REJECT_GENDER_MISMATCH,
}

/**
 * Hard policy: gendered characters may never silently fall back to the opposite voice gender.
 * A provider/runtime that cannot supply the requested voice must surface an unavailable-voice state
 * instead of speaking with a mismatched voice.
 */
object AssistantVoicePolicy {
    fun decision(
        character: AssistantCharacterProfile,
        voice: AssistantVoiceProfile,
    ): AssistantVoiceDecision {
        val required = character.voiceGender
        if (required == AssistantVoiceGender.NEUTRAL) return AssistantVoiceDecision.ALLOW
        return if (voice.gender == required) {
            AssistantVoiceDecision.ALLOW
        } else {
            AssistantVoiceDecision.REJECT_GENDER_MISMATCH
        }
    }
}

object AssistantBuiltInVoiceCatalog {
    private val profiles = listOf(
        AssistantVoiceProfile("neutral_default", AssistantVoiceGender.NEUTRAL),
        AssistantVoiceProfile("female_default", AssistantVoiceGender.FEMALE),
        AssistantVoiceProfile("male_default", AssistantVoiceGender.MALE),
    )

    fun resolve(id: String): AssistantVoiceProfile? = profiles.firstOrNull { it.id == id }
}
