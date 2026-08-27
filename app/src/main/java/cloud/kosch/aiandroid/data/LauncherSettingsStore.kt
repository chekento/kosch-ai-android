package cloud.kosch.aiandroid.data

import android.content.Context
import cloud.kosch.aiandroid.model.LauncherSettingsDocument

/**
 * Single-blob settings store: one SharedPreferences commit replaces the complete normalized document atomically.
 * This intentionally stays separate from Android capabilities and the secret vault.
 */
class LauncherSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): LauncherSettingsDocument {
        val payload = preferences.getString(KEY_DOCUMENT, null) ?: return LauncherSettingsDocument()
        return runCatching { LauncherSettingsCodec.decode(payload) }
            .getOrDefault(LauncherSettingsDocument())
    }

    fun save(document: LauncherSettingsDocument): Boolean {
        val payload = LauncherSettingsCodec.encode(document)
        require(payload.toByteArray(Charsets.UTF_8).size <= MAX_SETTINGS_BYTES) {
            "Launcher settings document exceeds the local size budget"
        }
        return preferences.edit()
            .putString(KEY_DOCUMENT, payload)
            .commit()
    }

    fun validateImport(payload: String): Result<LauncherSettingsDocument> = runCatching {
        require(payload.toByteArray(Charsets.UTF_8).size <= MAX_SETTINGS_BYTES) {
            "Launcher settings import exceeds the size budget"
        }
        LauncherSettingsCodec.decode(payload).normalized()
    }

    fun applyImport(payload: String): Result<LauncherSettingsDocument> = validateImport(payload).mapCatching { document ->
        check(save(document)) { "Launcher settings import could not be committed" }
        document
    }

    fun exportPortable(): String = LauncherSettingsCodec.encode(load())

    fun reset(): Boolean = preferences.edit().remove(KEY_DOCUMENT).commit()

    private companion object {
        const val PREFS_NAME = "launcher-settings-v1"
        const val KEY_DOCUMENT = "document"
        const val MAX_SETTINGS_BYTES = 512 * 1024
    }
}
