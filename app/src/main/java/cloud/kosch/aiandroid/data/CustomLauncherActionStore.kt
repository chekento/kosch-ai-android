package cloud.kosch.aiandroid.data

import android.content.Context
import cloud.kosch.aiandroid.model.CustomLauncherAction

/**
 * Atomic launcher-owned persistence for validated custom actions.
 *
 * The wire format contains only typed, allow-listed targets. Android Intent extras, component overrides,
 * device grants and secrets cannot be represented by [CustomLauncherActionCodec].
 */
class CustomLauncherActionStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<CustomLauncherAction> {
        val payload = preferences.getString(KEY_DOCUMENT, null) ?: return emptyList()
        return runCatching { CustomLauncherActionCodec.decode(payload) }.getOrDefault(emptyList())
    }

    fun save(actions: List<CustomLauncherAction>): Boolean {
        val payload = CustomLauncherActionCodec.encode(actions)
        require(payload.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) {
            "Custom launcher actions exceed the local size budget"
        }
        return preferences.edit().putString(KEY_DOCUMENT, payload).commit()
    }

    fun validateImport(payload: String): Result<List<CustomLauncherAction>> = runCatching {
        require(payload.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) {
            "Custom action import exceeds the size budget"
        }
        CustomLauncherActionCodec.decode(payload)
    }

    fun applyImport(payload: String): Result<List<CustomLauncherAction>> = validateImport(payload).mapCatching { actions ->
        check(save(actions)) { "Custom action import could not be committed" }
        actions
    }

    fun exportPortable(): String = CustomLauncherActionCodec.encode(load())

    fun reset(): Boolean = preferences.edit().remove(KEY_DOCUMENT).commit()

    private companion object {
        const val PREFS_NAME = "custom-launcher-actions-v1"
        const val KEY_DOCUMENT = "document"
        const val MAX_BYTES = 512 * 1024
    }
}
