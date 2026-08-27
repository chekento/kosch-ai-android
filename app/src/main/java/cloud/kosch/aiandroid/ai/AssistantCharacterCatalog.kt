package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.AssistantCharacterProfile
import cloud.kosch.aiandroid.model.AssistantVoiceGender

/**
 * Built-in character descriptors. Asset packs are resolved independently by the visual runtime so
 * missing optional artwork can safely fall back to the default assistant without changing agent
 * behavior, provider, permissions or conversation state.
 *
 * Voice gender is part of the character contract. A female character may resolve only to a female
 * voice profile and a male character only to a male voice profile; runtimes must fail closed rather
 * than silently fall back to the opposite gender.
 */
object AssistantCharacterCatalog {
    private val defaultProfile = AssistantCharacterProfile(
        id = "default",
        displayName = "KoSch Default",
        assetPackId = "default",
        personaProfileId = "kosch_balanced",
        voiceProfileId = "neutral_default",
        voiceGender = AssistantVoiceGender.NEUTRAL,
    )

    private val profiles = listOf(
        defaultProfile,
        AssistantCharacterProfile(
            id = "anime_female",
            displayName = "Anime Companion · Female",
            assetPackId = "anime_female_v1",
            personaProfileId = "kosch_balanced",
            voiceProfileId = "female_default",
            voiceGender = AssistantVoiceGender.FEMALE,
        ),
        AssistantCharacterProfile(
            id = "anime_male",
            displayName = "Anime Companion · Male",
            assetPackId = "anime_male_v1",
            personaProfileId = "kosch_balanced",
            voiceProfileId = "male_default",
            voiceGender = AssistantVoiceGender.MALE,
        ),
    )

    fun all(): List<AssistantCharacterProfile> = profiles

    fun resolve(id: String): AssistantCharacterProfile =
        profiles.firstOrNull { it.id == id } ?: defaultProfile
}
