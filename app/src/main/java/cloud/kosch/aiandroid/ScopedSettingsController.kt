package cloud.kosch.aiandroid

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.kosch.aiandroid.data.ScopedSettingsStore
import cloud.kosch.aiandroid.model.PortableSettingValue
import cloud.kosch.aiandroid.model.ResolvedSettingValue
import cloud.kosch.aiandroid.model.ScopedSettingValue
import cloud.kosch.aiandroid.model.ScopedSettingsDocument
import cloud.kosch.aiandroid.model.SettingOverride
import cloud.kosch.aiandroid.model.SettingsScopeResolver
import cloud.kosch.aiandroid.model.WorkspaceDocument

/** Runtime facade for persistent Global → Page → Object inheritance. */
class ScopedSettingsController(context: Context) {
    private val store = ScopedSettingsStore(context.applicationContext)

    var document by mutableStateOf(store.load())
        private set
    var notice by mutableStateOf<String?>(null)
        private set

    fun reconcile(workspace: WorkspaceDocument) {
        runCatching { store.reconcile(workspace) }
            .onSuccess { document = it }
            .onFailure { notice = it.message ?: "Scoped Settings konnten nicht bereinigt werden" }
    }

    fun setPageOverride(pageId: String, featureId: String, value: PortableSettingValue?): Boolean =
        persist(runCatching { document.withPageOverride(pageId, featureId, value) }, "Seiten-Override gespeichert")

    fun setObjectOverride(itemId: String, featureId: String, value: PortableSettingValue?): Boolean =
        persist(runCatching { document.withObjectOverride(itemId, featureId, value) }, "Objekt-Override gespeichert")

    /** Applies a group of object-level changes in one store commit, useful for presets and full style reset. */
    fun setObjectOverrides(
        itemId: String,
        values: Map<String, PortableSettingValue?>,
    ): Boolean = persist(
        runCatching {
            values.entries.fold(document) { current, (featureId, value) ->
                current.withObjectOverride(itemId, featureId, value)
            }
        },
        "Objekt-Stil gespeichert",
    )

    fun resolve(
        featureId: String,
        global: PortableSettingValue,
        pageId: String? = null,
        itemId: String? = null,
    ): ResolvedSettingValue<PortableSettingValue> {
        val page = pageId?.let { document.pageOverride(it, featureId) }
        val objectValue = itemId?.let { document.objectOverride(it, featureId) }
        return SettingsScopeResolver.resolve(
            ScopedSettingValue(
                featureId = featureId,
                global = global,
                page = page?.let { SettingOverride.Value(it) } ?: SettingOverride.Inherit,
                objectValue = objectValue?.let { SettingOverride.Value(it) } ?: SettingOverride.Inherit,
            ),
        )
    }

    fun exportPortable(): String = store.exportPortable()

    fun importPortable(payload: String, workspace: WorkspaceDocument): Result<ScopedSettingsDocument> =
        store.applyImport(payload, workspace).onSuccess {
            document = it
            notice = "Scoped Settings importiert"
        }.onFailure {
            notice = it.message ?: "Scoped Settings konnten nicht importiert werden"
        }

    fun consumeNotice() {
        notice = null
    }

    private fun persist(candidate: Result<ScopedSettingsDocument>, successMessage: String): Boolean {
        val next = candidate.getOrElse {
            notice = it.message ?: "Ungültiger Scoped-Setting-Wert"
            return false
        }
        if (next == document) return true
        if (!store.save(next)) {
            notice = "Scoped Settings konnten nicht gespeichert werden"
            return false
        }
        document = next
        notice = successMessage
        return true
    }
}
