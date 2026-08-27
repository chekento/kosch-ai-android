package cloud.kosch.aiandroid

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.kosch.aiandroid.data.AssistantDeviceVoiceStore
import cloud.kosch.aiandroid.model.AssistantSystemVoiceOption
import cloud.kosch.aiandroid.model.AssistantVoiceAssignments
import cloud.kosch.aiandroid.model.AssistantVoiceGender

/**
 * Activity-recreation-safe device voice runtime. Available voices are ephemeral; assignments are
 * device-local and intentionally excluded from portable launcher configuration and backup.
 */
class AssistantVoiceController(context: Context) {
    private val store = AssistantDeviceVoiceStore(context.applicationContext)

    var assignments by mutableStateOf(store.load())
        private set

    var availableVoices by mutableStateOf<List<AssistantSystemVoiceOption>>(emptyList())
        private set

    var statusMessage by mutableStateOf<String?>(null)
        private set

    fun updateAvailableVoices(voices: Collection<AssistantSystemVoiceOption>) {
        availableVoices = voices
            .distinctBy { it.name }
            .sortedWith(compareBy<AssistantSystemVoiceOption>({ it.networkRequired }, { it.languageTag }, { it.name }))
    }

    fun assignFromUser(gender: AssistantVoiceGender, voiceName: String?): Boolean {
        val normalized = voiceName?.trim()?.takeIf { it.isNotEmpty() }
        if (normalized != null && availableVoices.none { it.name == normalized }) {
            statusMessage = "TTS-Stimme ist auf diesem Gerät nicht verfügbar"
            return false
        }
        if (normalized != null) {
            val conflict = when (gender) {
                AssistantVoiceGender.FEMALE -> assignments.maleVoiceName == normalized
                AssistantVoiceGender.MALE -> assignments.femaleVoiceName == normalized
                AssistantVoiceGender.NEUTRAL -> false
            }
            if (conflict) {
                statusMessage = "Weiblicher und männlicher Voice-Slot müssen unterschiedliche Stimmen verwenden"
                return false
            }
        }
        return runCatching {
            store.assign(gender, normalized)
            assignments = assignments.withAssignment(gender, normalized)
            statusMessage = null
            true
        }.getOrElse {
            statusMessage = "Stimmenzuordnung konnte nicht gespeichert werden"
            false
        }
    }

    fun assignedVoiceName(gender: AssistantVoiceGender): String? = assignments.forGender(gender)

    fun assignedVoice(gender: AssistantVoiceGender): AssistantSystemVoiceOption? {
        val name = assignedVoiceName(gender) ?: return null
        return availableVoices.firstOrNull { it.name == name }
    }

    fun isReady(gender: AssistantVoiceGender): Boolean = when (gender) {
        AssistantVoiceGender.NEUTRAL -> true
        AssistantVoiceGender.FEMALE,
        AssistantVoiceGender.MALE -> assignedVoice(gender) != null
    }
}
