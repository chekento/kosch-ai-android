package cloud.kosch.aiandroid

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.kosch.aiandroid.data.CustomLauncherActionStore
import cloud.kosch.aiandroid.model.CustomLauncherAction
import cloud.kosch.aiandroid.model.CustomLauncherActionValidation
import cloud.kosch.aiandroid.model.CustomLauncherActionValidator

/** Activity-recreation-safe CRUD facade for portable custom actions. */
class CustomLauncherActionController(context: Context) {
    private val store = CustomLauncherActionStore(context.applicationContext)

    var actions by mutableStateOf(store.load())
        private set
    var notice by mutableStateOf<String?>(null)
        private set

    fun upsert(action: CustomLauncherAction): Boolean {
        val normalized = when (val validation = CustomLauncherActionValidator.validate(action)) {
            is CustomLauncherActionValidation.Valid -> validation.normalized
            is CustomLauncherActionValidation.Invalid -> {
                notice = validation.reason
                return false
            }
        }
        val next = actions.filterNot { it.id == normalized.id } + normalized
        if (!store.save(next)) {
            notice = "Eigene Verknüpfung konnte nicht gespeichert werden"
            return false
        }
        actions = store.load()
        notice = "Eigene Verknüpfung gespeichert"
        return true
    }

    fun remove(actionId: String): Boolean {
        val next = actions.filterNot { it.id == actionId }
        if (next.size == actions.size) return false
        if (!store.save(next)) {
            notice = "Eigene Verknüpfung konnte nicht entfernt werden"
            return false
        }
        actions = next
        notice = "Eigene Verknüpfung entfernt"
        return true
    }

    fun find(actionId: String): CustomLauncherAction? = actions.firstOrNull { it.id == actionId }

    fun exportPortable(): String = store.exportPortable()

    fun importPortable(payload: String): Result<List<CustomLauncherAction>> = store.applyImport(payload).onSuccess {
        actions = it
        notice = "Eigene Verknüpfungen importiert"
    }.onFailure {
        notice = it.message ?: "Eigene Verknüpfungen konnten nicht importiert werden"
    }

    fun reset(): Boolean {
        if (!store.reset()) return false
        actions = emptyList()
        notice = "Eigene Verknüpfungen zurückgesetzt"
        return true
    }

    fun consumeNotice() {
        notice = null
    }
}
