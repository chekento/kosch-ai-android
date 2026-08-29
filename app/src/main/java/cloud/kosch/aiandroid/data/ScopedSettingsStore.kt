package cloud.kosch.aiandroid.data

import android.content.Context
import cloud.kosch.aiandroid.model.ScopedSettingsDocument
import cloud.kosch.aiandroid.model.WorkspaceDocument

/** Atomic persistence for portable page/object overrides, independent from layout and device-local grants. */
class ScopedSettingsStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): ScopedSettingsDocument {
        val payload = preferences.getString(KEY_DOCUMENT, null) ?: return ScopedSettingsDocument()
        return runCatching { ScopedSettingsCodec.decode(payload) }.getOrDefault(ScopedSettingsDocument())
    }

    fun save(document: ScopedSettingsDocument): Boolean {
        val payload = ScopedSettingsCodec.encode(document)
        require(payload.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) {
            "Scoped settings document exceeds the local size budget"
        }
        return preferences.edit().putString(KEY_DOCUMENT, payload).commit()
    }

    fun reconcile(workspace: WorkspaceDocument): ScopedSettingsDocument {
        val current = load()
        val pruned = current.prunedTo(workspace)
        if (pruned != current) check(save(pruned)) { "Scoped settings reconciliation could not be committed" }
        return pruned
    }

    fun validateImport(payload: String, workspace: WorkspaceDocument? = null): Result<ScopedSettingsDocument> = runCatching {
        require(payload.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Scoped settings import exceeds size budget" }
        val decoded = ScopedSettingsCodec.decode(payload)
        workspace?.let(decoded::prunedTo) ?: decoded
    }

    fun applyImport(payload: String, workspace: WorkspaceDocument? = null): Result<ScopedSettingsDocument> =
        validateImport(payload, workspace).mapCatching { document ->
            check(save(document)) { "Scoped settings import could not be committed" }
            document
        }

    fun exportPortable(): String = ScopedSettingsCodec.encode(load())

    fun reset(): Boolean = preferences.edit().remove(KEY_DOCUMENT).commit()

    private companion object {
        const val PREFS_NAME = "scoped-settings-v1"
        const val KEY_DOCUMENT = "document"
        const val MAX_BYTES = 512 * 1024
    }
}
