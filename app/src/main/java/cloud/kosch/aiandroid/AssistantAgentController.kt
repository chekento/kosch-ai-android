package cloud.kosch.aiandroid

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.kosch.aiandroid.ai.AssistantAgentPolicy
import cloud.kosch.aiandroid.ai.AssistantCharacterCatalog
import cloud.kosch.aiandroid.ai.AssistantPolicyDecision
import cloud.kosch.aiandroid.data.AssistantAgentStore
import cloud.kosch.aiandroid.model.AssistantActionRisk
import cloud.kosch.aiandroid.model.AssistantAgentPreferences
import cloud.kosch.aiandroid.model.AssistantAgentState
import cloud.kosch.aiandroid.model.AssistantObservationSource
import cloud.kosch.aiandroid.model.AssistantPresenceMode
import cloud.kosch.aiandroid.model.AssistantWakeWordMode

/**
 * Session owner for the next-generation agent layer. Character/persona selection is deliberately
 * independent from the existing AssistantSessionController conversation runtime.
 *
 * This controller never acquires Android capture permissions itself and never persists capture
 * grants. Platform consent must be supplied by a visible Activity boundary for each observation
 * session. Screen/camera capability opt-in may only be enabled through an explicit user action.
 */
class AssistantAgentController(context: Context) {
    private val store = AssistantAgentStore(context.applicationContext)

    var preferences by mutableStateOf(store.load())
        private set

    var state by mutableStateOf(AssistantAgentState.DISABLED)
        private set

    var activeObservation by mutableStateOf<AssistantObservationSource?>(null)
        private set

    val character get() = AssistantCharacterCatalog.resolve(preferences.characterId)

    fun setAssistantEnabled(enabled: Boolean) {
        if (!enabled) {
            activeObservation = null
            state = AssistantAgentState.DISABLED
        } else if (state == AssistantAgentState.DISABLED) {
            state = AssistantAgentState.IDLE
        }
    }

    fun selectCharacter(characterId: String) {
        updatePreferences(
            preferences.copy(characterId = AssistantCharacterCatalog.resolve(characterId).id),
        )
    }

    fun setAssistantName(name: String) {
        updatePreferences(preferences.copy(assistantName = name.take(32)))
    }

    fun setPresenceMode(mode: AssistantPresenceMode) {
        val supported = character.supportedPresenceModes
        updatePreferences(preferences.copy(presenceMode = mode.takeIf(supported::contains) ?: AssistantPresenceMode.AMBIENT))
    }

    fun setWakeWord(mode: AssistantWakeWordMode, customWakeWord: String = preferences.customWakeWord) {
        updatePreferences(
            preferences.copy(
                wakeWordMode = mode,
                customWakeWord = customWakeWord.take(32),
            ),
        )
    }

    fun setLocalWakeWordOnly(enabled: Boolean) {
        updatePreferences(preferences.copy(localWakeWordOnly = enabled))
    }

    /**
     * This is the only capability-enabling path for screen/camera awareness. UI code must call it
     * directly from a user gesture. Agent plans, providers and automations must not call it.
     * Disabling remains allowed from any safety/lifecycle path.
     */
    fun setObservationEnabledFromUser(source: AssistantObservationSource, enabled: Boolean) {
        val updated = observationPreference(source, enabled)
        if (enabled) {
            store.saveUserObservationOptIn(updated)
            preferences = updated
        } else {
            updatePreferences(updated)
            if (activeObservation == source) stopObservation()
        }
    }

    fun disableObservation(source: AssistantObservationSource) {
        val updated = observationPreference(source, false)
        updatePreferences(updated)
        if (activeObservation == source) stopObservation()
    }

    fun setActionExecutionEnabled(enabled: Boolean) {
        updatePreferences(preferences.copy(actionExecutionEnabled = enabled))
    }

    fun setConfirmationRequiredForExternalActions(enabled: Boolean) {
        updatePreferences(preferences.copy(confirmationRequiredForExternalActions = enabled))
    }

    fun armWakeWord() {
        if (state != AssistantAgentState.DISABLED && preferences.wakeWordMode != AssistantWakeWordMode.OFF) {
            state = AssistantAgentState.ARMED
        }
    }

    fun listening() = transitionActive(AssistantAgentState.LISTENING)

    fun thinking() = transitionActive(AssistantAgentState.THINKING)

    fun speaking() = transitionActive(AssistantAgentState.SPEAKING)

    fun acting() = transitionActive(AssistantAgentState.ACTING)

    fun idle() {
        if (state != AssistantAgentState.DISABLED) {
            activeObservation = null
            state = AssistantAgentState.IDLE
        }
    }

    fun privacyBlocked() {
        if (state != AssistantAgentState.DISABLED) {
            activeObservation = null
            state = AssistantAgentState.PRIVACY_BLOCKED
        }
    }

    fun failed() {
        if (state != AssistantAgentState.DISABLED) {
            activeObservation = null
            state = AssistantAgentState.ERROR
        }
    }

    fun beginObservation(
        assistantEnabled: Boolean,
        source: AssistantObservationSource,
        platformConsentGranted: Boolean,
        sessionVisible: Boolean,
    ): AssistantPolicyDecision {
        val decision = AssistantAgentPolicy.observationDecision(
            assistantEnabled = assistantEnabled,
            preferences = preferences,
            source = source,
            platformConsentGranted = platformConsentGranted,
            sessionVisible = sessionVisible,
        )
        if (decision == AssistantPolicyDecision.ALLOW) {
            activeObservation = source
            state = when (source) {
                AssistantObservationSource.SCREEN -> AssistantAgentState.OBSERVING_SCREEN
                AssistantObservationSource.CAMERA -> AssistantAgentState.OBSERVING_CAMERA
            }
        } else if (decision == AssistantPolicyDecision.BLOCK_SESSION_NOT_VISIBLE) {
            privacyBlocked()
        }
        return decision
    }

    fun stopObservation() {
        activeObservation = null
        if (state == AssistantAgentState.OBSERVING_SCREEN || state == AssistantAgentState.OBSERVING_CAMERA) {
            state = AssistantAgentState.IDLE
        }
    }

    fun actionDecision(
        assistantEnabled: Boolean,
        risk: AssistantActionRisk,
        explicitUserConfirmation: Boolean,
    ): AssistantPolicyDecision = AssistantAgentPolicy.actionDecision(
        assistantEnabled = assistantEnabled,
        preferences = preferences,
        risk = risk,
        explicitUserConfirmation = explicitUserConfirmation,
    )

    private fun observationPreference(source: AssistantObservationSource, enabled: Boolean): AssistantAgentPreferences =
        when (source) {
            AssistantObservationSource.SCREEN -> preferences.copy(screenObservationEnabled = enabled)
            AssistantObservationSource.CAMERA -> preferences.copy(cameraObservationEnabled = enabled)
        }

    private fun transitionActive(next: AssistantAgentState) {
        if (state != AssistantAgentState.DISABLED) {
            activeObservation = null
            state = next
        }
    }

    private fun updatePreferences(updated: AssistantAgentPreferences) {
        store.save(updated)
        preferences = updated
    }
}
