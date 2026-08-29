package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.AssistantActionRisk
import cloud.kosch.aiandroid.model.AssistantAgentPreferences
import cloud.kosch.aiandroid.model.AssistantCharacterProfile
import cloud.kosch.aiandroid.model.AssistantObservationSource
import cloud.kosch.aiandroid.model.AssistantWakeWordMode

enum class AssistantPolicyDecision {
    ALLOW,
    REQUIRE_USER_CONSENT,
    REQUIRE_ACTION_CONFIRMATION,
    BLOCK_ASSISTANT_DISABLED,
    BLOCK_CAPABILITY_DISABLED,
    BLOCK_SESSION_NOT_VISIBLE,
}

/**
 * Pure policy boundary for assistant observation and action execution. Android permission/grant
 * acquisition belongs outside this class; this class decides whether a requested transition is
 * allowed to reach that platform boundary at all.
 */
object AssistantAgentPolicy {
    fun observationDecision(
        assistantEnabled: Boolean,
        preferences: AssistantAgentPreferences,
        source: AssistantObservationSource,
        platformConsentGranted: Boolean,
        sessionVisible: Boolean,
    ): AssistantPolicyDecision {
        if (!assistantEnabled) return AssistantPolicyDecision.BLOCK_ASSISTANT_DISABLED
        if (!preferences.observationEnabled(source)) {
            return AssistantPolicyDecision.BLOCK_CAPABILITY_DISABLED
        }
        if (!sessionVisible) return AssistantPolicyDecision.BLOCK_SESSION_NOT_VISIBLE
        if (!platformConsentGranted) return AssistantPolicyDecision.REQUIRE_USER_CONSENT
        return AssistantPolicyDecision.ALLOW
    }

    fun actionDecision(
        assistantEnabled: Boolean,
        preferences: AssistantAgentPreferences,
        risk: AssistantActionRisk,
        explicitUserConfirmation: Boolean,
    ): AssistantPolicyDecision {
        if (!assistantEnabled) return AssistantPolicyDecision.BLOCK_ASSISTANT_DISABLED
        if (risk == AssistantActionRisk.LOCAL_READ_ONLY) return AssistantPolicyDecision.ALLOW
        if (!preferences.actionExecutionEnabled) {
            return AssistantPolicyDecision.BLOCK_CAPABILITY_DISABLED
        }
        val confirmationRequired = when (risk) {
            AssistantActionRisk.LOCAL_READ_ONLY -> false
            AssistantActionRisk.LOCAL_REVERSIBLE -> false
            AssistantActionRisk.EXTERNAL_SIDE_EFFECT -> preferences.confirmationRequiredForExternalActions
            // Sensitive actions can never be downgraded to silent execution by a preference.
            AssistantActionRisk.SENSITIVE_SIDE_EFFECT -> true
        }
        if (confirmationRequired && !explicitUserConfirmation) {
            return AssistantPolicyDecision.REQUIRE_ACTION_CONFIRMATION
        }
        return AssistantPolicyDecision.ALLOW
    }
}

/**
 * Central risk vocabulary for assistant-triggered launcher commands.
 *
 * This classifier describes the next action boundary, not the user's intent. Opening a dialer or composer is therefore
 * an external navigation side effect even though KoSch itself does not place a call or send a message. Starting voice
 * capture is sensitive because it crosses into an audio-observation session. Free prompts are also external because a
 * later provider handoff may leave the launcher; the existing handoff gate remains an additional independent barrier.
 */
object AssistantCommandRiskClassifier {
    fun risk(command: LauncherCommand): AssistantActionRisk = when (command) {
        LauncherCommand.Empty,
        LauncherCommand.OpenDrawer,
        LauncherCommand.OpenFaq,
        LauncherCommand.OpenAudit -> AssistantActionRisk.LOCAL_READ_ONLY

        LauncherCommand.OpenControls,
        LauncherCommand.OpenWidgets,
        LauncherCommand.OpenPenSpace,
        LauncherCommand.OpenBackup,
        LauncherCommand.OpenProDesk,
        LauncherCommand.OpenFiles,
        LauncherCommand.OpenFileWorkspace,
        is LauncherCommand.SwitchScene -> AssistantActionRisk.LOCAL_REVERSIBLE

        LauncherCommand.StartVoice -> AssistantActionRisk.SENSITIVE_SIDE_EFFECT

        LauncherCommand.PickContact,
        is LauncherCommand.OpenPhone,
        is LauncherCommand.OpenMessage,
        LauncherCommand.OpenCalendar,
        LauncherCommand.OpenAlarms,
        LauncherCommand.OpenCamera,
        LauncherCommand.CreateSystemNote,
        is LauncherCommand.OpenSystemPanel,
        is LauncherCommand.LaunchApp,
        is LauncherCommand.RoutePrompt -> AssistantActionRisk.EXTERNAL_SIDE_EFFECT
    }
}

object AssistantWakeWordResolver {
    fun resolve(
        preferences: AssistantAgentPreferences,
        character: AssistantCharacterProfile,
    ): String? = when (preferences.wakeWordMode) {
        AssistantWakeWordMode.OFF -> null
        AssistantWakeWordMode.COMPUTER -> "Computer"
        AssistantWakeWordMode.ASSISTANT_NAME -> preferences.assistantName.trim().ifEmpty { character.displayName }
        AssistantWakeWordMode.CUSTOM -> preferences.customWakeWord.trim().takeIf { it.length >= MIN_CUSTOM_WAKE_WORD_LENGTH }
    }

    private const val MIN_CUSTOM_WAKE_WORD_LENGTH = 2
}
