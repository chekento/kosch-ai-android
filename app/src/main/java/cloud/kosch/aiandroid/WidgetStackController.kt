package cloud.kosch.aiandroid

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.kosch.aiandroid.ai.WidgetStackPolicy
import cloud.kosch.aiandroid.data.WidgetStackStore
import cloud.kosch.aiandroid.model.WidgetStack
import cloud.kosch.aiandroid.model.WidgetStackMode
import java.util.UUID

/** Activity-recreation-safe, device-local state holder for already-bound Android widget stacks. */
class WidgetStackController(context: Context) {
    private val store = WidgetStackStore(context.applicationContext)

    var stacks by mutableStateOf(store.load())
        private set
    var notice by mutableStateOf<String?>(null)
        private set

    fun repair(validWidgetIds: Collection<Int>): Boolean = persist(
        WidgetStackPolicy.repair(stacks, validWidgetIds.filter { it > 0 }.toSet()),
        "Widget-Stacks mit aktuellen Android-Bindings abgeglichen",
    )

    fun createStack(
        appWidgetIds: List<Int>,
        title: String = "Widget Stack",
    ): Boolean {
        if (stacks.size >= WidgetStackPolicy.MAX_STACKS) {
            notice = "Maximal ${WidgetStackPolicy.MAX_STACKS} Widget-Stacks"
            return false
        }
        val candidate = WidgetStackPolicy.normalize(
            WidgetStack(
                id = UUID.randomUUID().toString(),
                title = title,
                appWidgetIds = appWidgetIds,
            ),
        ) ?: run {
            notice = "Wähle mindestens ein gebundenes Widget"
            return false
        }
        return persist(stacks + candidate, "Widget-Stack erstellt")
    }

    fun deleteStack(stackId: String): Boolean {
        val updated = stacks.filterNot { it.id == stackId }
        if (updated.size == stacks.size) return false
        return persist(updated, "Widget-Stack entfernt")
    }

    fun renameStack(stackId: String, title: String): Boolean = mutate(
        stackId,
        { WidgetStackPolicy.rename(it, title) },
        "Widget-Stack umbenannt",
    )

    fun next(stackId: String): Boolean = mutate(stackId, WidgetStackPolicy::next, "Nächstes Widget")

    fun previous(stackId: String): Boolean = mutate(stackId, WidgetStackPolicy::previous, "Vorheriges Widget")

    fun select(stackId: String, appWidgetId: Int): Boolean = mutate(
        stackId,
        { WidgetStackPolicy.select(it, appWidgetId) },
        "Widget ausgewählt",
    )

    fun addWidget(stackId: String, appWidgetId: Int): Boolean = mutate(
        stackId,
        { WidgetStackPolicy.addWidget(it, appWidgetId) },
        "Widget zum Stack hinzugefügt",
    )

    fun moveWidget(stackId: String, appWidgetId: Int, delta: Int): Boolean = mutate(
        stackId,
        { WidgetStackPolicy.moveWidget(it, appWidgetId, delta) },
        "Widget-Reihenfolge geändert",
    )

    fun removeWidget(stackId: String, appWidgetId: Int): Boolean {
        val stack = stacks.firstOrNull { it.id == stackId } ?: return false
        val changed = WidgetStackPolicy.removeWidget(stack, appWidgetId)
        val updated = if (changed == null) {
            stacks.filterNot { it.id == stackId }
        } else {
            stacks.map { if (it.id == stackId) changed else it }
        }
        return persist(updated, if (changed == null) "Leerer Widget-Stack entfernt" else "Widget aus Stack entfernt")
    }

    fun setMode(
        stackId: String,
        mode: WidgetStackMode,
        autoCycleSeconds: Int = 0,
    ): Boolean = mutate(
        stackId,
        { stack -> WidgetStackPolicy.normalize(stack.copy(mode = mode, autoCycleSeconds = autoCycleSeconds)) },
        "Stack-Modus: ${mode.title}",
    )

    fun consumeNotice() {
        notice = null
    }

    private fun mutate(
        stackId: String,
        transform: (WidgetStack) -> WidgetStack?,
        successNotice: String,
    ): Boolean {
        val index = stacks.indexOfFirst { it.id == stackId }
        if (index < 0) return false
        val updatedStack = transform(stacks[index]) ?: return false
        val updated = stacks.toMutableList().apply { this[index] = updatedStack }
        return persist(updated, successNotice)
    }

    private fun persist(updated: List<WidgetStack>, successNotice: String): Boolean {
        val normalized = WidgetStackPolicy.normalizeAll(updated)
        if (normalized == stacks) return true
        if (!store.save(normalized)) {
            notice = "Widget-Stacks konnten nicht lokal gespeichert werden"
            return false
        }
        stacks = normalized
        notice = successNotice
        return true
    }
}
