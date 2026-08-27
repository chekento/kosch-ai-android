package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.AssistantActionRisk
import cloud.kosch.aiandroid.model.AssistantAgentPreferences
import cloud.kosch.aiandroid.model.AssistantObservationSource
import cloud.kosch.aiandroid.model.AssistantVoiceGender
import cloud.kosch.aiandroid.model.AssistantWakeWordMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AssistantAgentPolicyTest {
    @Test
    fun observation_isOffByDefaultAndRequiresCapabilityAndVisibleConsentedSession() {
        val disabled = AssistantAgentPreferences()
        assertFalse(disabled.screenObservationEnabled)
        assertFalse(disabled.cameraObservationEnabled)
        assertEquals(
            AssistantPolicyDecision.BLOCK_CAPABILITY_DISABLED,
            AssistantAgentPolicy.observationDecision(
                assistantEnabled = true,
                preferences = disabled,
                source = AssistantObservationSource.SCREEN,
                platformConsentGranted = true,
                sessionVisible = true,
            ),
        )

        val enabled = disabled.copy(screenObservationEnabled = true)
        assertEquals(
            AssistantPolicyDecision.REQUIRE_USER_CONSENT,
            AssistantAgentPolicy.observationDecision(
                assistantEnabled = true,
                preferences = enabled,
                source = AssistantObservationSource.SCREEN,
                platformConsentGranted = false,
                sessionVisible = true,
            ),
        )
        assertEquals(
            AssistantPolicyDecision.BLOCK_SESSION_NOT_VISIBLE,
            AssistantAgentPolicy.observationDecision(
                assistantEnabled = true,
                preferences = enabled,
                source = AssistantObservationSource.SCREEN,
                platformConsentGranted = true,
                sessionVisible = false,
            ),
        )
        assertEquals(
            AssistantPolicyDecision.ALLOW,
            AssistantAgentPolicy.observationDecision(
                assistantEnabled = true,
                preferences = enabled,
                source = AssistantObservationSource.SCREEN,
                platformConsentGranted = true,
                sessionVisible = true,
            ),
        )
    }

    @Test
    fun externalActions_defaultToExplicitConfirmation() {
        val preferences = AssistantAgentPreferences(actionExecutionEnabled = true)
        assertEquals(
            AssistantPolicyDecision.REQUIRE_ACTION_CONFIRMATION,
            AssistantAgentPolicy.actionDecision(
                assistantEnabled = true,
                preferences = preferences,
                risk = AssistantActionRisk.EXTERNAL_SIDE_EFFECT,
                explicitUserConfirmation = false,
            ),
        )
        assertEquals(
            AssistantPolicyDecision.ALLOW,
            AssistantAgentPolicy.actionDecision(
                assistantEnabled = true,
                preferences = preferences,
                risk = AssistantActionRisk.EXTERNAL_SIDE_EFFECT,
                explicitUserConfirmation = true,
            ),
        )
    }

    @Test
    fun localReadOnlyDoesNotRequireAgentExecutionPrivilege() {
        assertEquals(
            AssistantPolicyDecision.ALLOW,
            AssistantAgentPolicy.actionDecision(
                assistantEnabled = true,
                preferences = AssistantAgentPreferences(actionExecutionEnabled = false),
                risk = AssistantActionRisk.LOCAL_READ_ONLY,
                explicitUserConfirmation = false,
            ),
        )
    }

    @Test
    fun wakeWordResolver_supportsComputerAssistantNameAndSafeCustomDrafts() {
        val character = AssistantCharacterCatalog.resolve("anime_female")
        assertNull(
            AssistantWakeWordResolver.resolve(
                preferences = AssistantAgentPreferences(wakeWordMode = AssistantWakeWordMode.OFF),
                character = character,
            ),
        )
        assertEquals(
            "Computer",
            AssistantWakeWordResolver.resolve(
                preferences = AssistantAgentPreferences(wakeWordMode = AssistantWakeWordMode.COMPUTER),
                character = character,
            ),
        )
        assertEquals(
            character.displayName,
            AssistantWakeWordResolver.resolve(
                preferences = AssistantAgentPreferences(wakeWordMode = AssistantWakeWordMode.ASSISTANT_NAME),
                character = character,
            ),
        )
        assertEquals(
            "Aira",
            AssistantWakeWordResolver.resolve(
                preferences = AssistantAgentPreferences(
                    wakeWordMode = AssistantWakeWordMode.ASSISTANT_NAME,
                    assistantName = "Aira",
                ),
                character = character,
            ),
        )
        assertNull(
            AssistantWakeWordResolver.resolve(
                preferences = AssistantAgentPreferences(
                    wakeWordMode = AssistantWakeWordMode.CUSTOM,
                    customWakeWord = "A",
                ),
                character = character,
            ),
        )
        assertEquals(
            "AI",
            AssistantWakeWordResolver.resolve(
                preferences = AssistantAgentPreferences(
                    wakeWordMode = AssistantWakeWordMode.CUSTOM,
                    customWakeWord = "AI",
                ),
                character = character,
            ),
        )
    }

    @Test
    fun builtInCharactersHaveGenderMatchedVoiceProfiles() {
        val profiles = AssistantCharacterCatalog.all()
        assertEquals(profiles.size, profiles.map { it.id }.toSet().size)
        assertEquals("default", AssistantCharacterCatalog.resolve("missing").id)

        val female = AssistantCharacterCatalog.resolve("anime_female")
        val femaleVoice = requireNotNull(AssistantBuiltInVoiceCatalog.resolve(female.voiceProfileId))
        assertEquals(AssistantVoiceGender.FEMALE, female.voiceGender)
        assertEquals(AssistantVoiceGender.FEMALE, femaleVoice.gender)
        assertEquals(AssistantVoiceDecision.ALLOW, AssistantVoicePolicy.decision(female, femaleVoice))
        assertEquals(
            AssistantVoiceDecision.REJECT_GENDER_MISMATCH,
            AssistantVoicePolicy.decision(
                female,
                requireNotNull(AssistantBuiltInVoiceCatalog.resolve("male_default")),
            ),
        )

        val male = AssistantCharacterCatalog.resolve("anime_male")
        val maleVoice = requireNotNull(AssistantBuiltInVoiceCatalog.resolve(male.voiceProfileId))
        assertEquals(AssistantVoiceGender.MALE, male.voiceGender)
        assertEquals(AssistantVoiceGender.MALE, maleVoice.gender)
        assertEquals(AssistantVoiceDecision.ALLOW, AssistantVoicePolicy.decision(male, maleVoice))
        assertEquals(
            AssistantVoiceDecision.REJECT_GENDER_MISMATCH,
            AssistantVoicePolicy.decision(
                male,
                requireNotNull(AssistantBuiltInVoiceCatalog.resolve("female_default")),
            ),
        )
    }
}
