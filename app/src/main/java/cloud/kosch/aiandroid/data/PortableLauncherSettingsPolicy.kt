package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.LauncherSettingsDocument

/**
 * Hard portability boundary for LauncherSettingsDocument.
 *
 * Stage E-H made Assistant session/agent/device-voice state first-class and device/session owned. Some early
 * Settings-Center schema fields still exist for additive codec compatibility, but they are shadow fields only:
 * imports, local launcher-settings persistence and portable exports are projected back to neutral defaults.
 * Presentation choices remain portable.
 *
 * Backup security invariants are projected here as well. Legacy schema booleans that describe whether secrets or
 * device ids may be exported are retained for wire compatibility, but they are not permissions: both are forced on
 * for save/import/export so a tampered settings document can never weaken the actual backup trust boundary.
 */
object PortableLauncherSettingsPolicy {
    fun project(document: LauncherSettingsDocument): LauncherSettingsDocument {
        val normalized = document.normalized()
        val defaults = LauncherSettingsDocument().assistant
        return normalized.copy(
            assistant = normalized.assistant.copy(
                enabled = defaults.enabled,
                assistantId = defaults.assistantId,
                wakeMode = defaults.wakeMode,
                liveChatEnabled = defaults.liveChatEnabled,
                voiceInputEnabled = defaults.voiceInputEnabled,
                speechOutputEnabled = defaults.speechOutputEnabled,
            ),
            backup = normalized.backup.copy(
                excludeSecretsAlways = true,
                excludeWidgetHostIdsAlways = true,
            ),
        ).normalized()
    }
}
