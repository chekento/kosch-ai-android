package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.LauncherSettingsDocument

/**
 * Hard portability boundary for LauncherSettingsDocument.
 *
 * Stage E-H made Assistant session/agent/device-voice state first-class and device/session owned. Some early
 * Settings-Center schema fields still exist for additive codec compatibility, but they are shadow fields only:
 * imports, local launcher-settings persistence and portable exports are projected back to neutral defaults.
 * Presentation choices remain portable.
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
        ).normalized()
    }
}
