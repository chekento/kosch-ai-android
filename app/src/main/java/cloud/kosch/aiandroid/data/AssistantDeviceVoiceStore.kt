package cloud.kosch.aiandroid.data

import android.content.Context
import cloud.kosch.aiandroid.model.AssistantVoiceAssignments
import cloud.kosch.aiandroid.model.AssistantVoiceGender

/**
 * Device-local Android TTS binding. Voice names are engine/device specific and must never enter the
 * portable launcher settings document or backups.
 */
class AssistantDeviceVoiceStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): AssistantVoiceAssignments = AssistantVoiceAssignments(
        femaleVoiceName = preferences.getString(KEY_FEMALE, null)?.takeIf(::validVoiceName),
        maleVoiceName = preferences.getString(KEY_MALE, null)?.takeIf(::validVoiceName),
        neutralVoiceName = preferences.getString(KEY_NEUTRAL, null)?.takeIf(::validVoiceName),
    )

    fun assign(gender: AssistantVoiceGender, voiceName: String?) {
        val normalized = voiceName?.trim()?.takeIf { it.isNotEmpty() }
        require(normalized == null || validVoiceName(normalized)) { "Ungültiger TTS-Stimmenname" }
        val key = when (gender) {
            AssistantVoiceGender.FEMALE -> KEY_FEMALE
            AssistantVoiceGender.MALE -> KEY_MALE
            AssistantVoiceGender.NEUTRAL -> KEY_NEUTRAL
        }
        preferences.edit().apply {
            if (normalized == null) remove(key) else putString(key, normalized)
        }.apply()
    }

    private fun validVoiceName(value: String): Boolean =
        value.length in 1..MAX_VOICE_NAME_LENGTH && value.none { it.isISOControl() }

    private companion object {
        const val PREFERENCES_NAME = "kosch_assistant_device_voice"
        const val KEY_FEMALE = "female_voice_name_v1"
        const val KEY_MALE = "male_voice_name_v1"
        const val KEY_NEUTRAL = "neutral_voice_name_v1"
        const val MAX_VOICE_NAME_LENGTH = 192
    }
}
