package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.AssistantActionRisk
import cloud.kosch.aiandroid.model.AssistantAgentPreferences
import cloud.kosch.aiandroid.model.AssistantObservationSource
import cloud.kosch.aiandroid.model.AssistantWakeWordMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssistantAgentPolicyTest {
    @Test
    fun observation_requiresCapabilityAndVisibleConsentedSession() {
        val disabled = AssistantAgentPreferences()
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
    fun wakeWordResolver_keepsCharacterIndependentFromRuntime() {
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
    }

    @Test
    fun builtInCharacterIdsAreUniqueAndResolvable() {
        val profiles = AssistantCharacterCatalog.all()
        assertEquals(profiles.size, profiles.map { it.id }.toSet().size)
        assertEquals("default", AssistantCharacterCatalog.resolve("missing").id)
        assertEquals("anime_female", AssistantCharacterCatalog.resolve("anime_female").id)
        assertEquals("anime_male", AssistantCharacterCatalog.resolve("anime_male").id)
    }
}
