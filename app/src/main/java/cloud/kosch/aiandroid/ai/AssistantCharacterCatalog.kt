package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.AssistantCharacterProfile

/**
 * Built-in character descriptors. Asset packs are resolved independently by the visual runtime so
 * missing optional artwork can safely fall back to the default assistant without changing agent
 * behavior, provider, permissions or conversation state.
 */
object AssistantCharacterCatalog {
    private val defaultProfile = AssistantCharacterProfile(
        id = "default",
        displayName = "KoSch Default",
        assetPackId = "default",
        personaProfileId = "kosch_balanced",
        voiceProfileId = "system_default",
    )

    private val profiles = listOf(
        defaultProfile,
        AssistantCharacterProfile(
            id = "anime_female",
            displayName = "Anime Companion · Female",
            assetPackId = "anime_female_v1",
            personaProfileId = "kosch_balanced",
            voiceProfileId = "system_default",
        ),
        AssistantCharacterProfile(
            id = "anime_male",
            displayName = "Anime Companion · Male",
            assetPackId = "anime_male_v1",
            personaProfileId = "kosch_balanced",
            voiceProfileId = "system_default",
        ),
    )

    fun all(): List<AssistantCharacterProfile> = profiles

    fun resolve(id: String): AssistantCharacterProfile =
        profiles.firstOrNull { it.id == id } ?: defaultProfile
}
