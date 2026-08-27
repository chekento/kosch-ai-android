package cloud.kosch.aiandroid.security.network

import android.content.Context
import android.content.SharedPreferences

/**
 * Device-private N3 policy storage.
 *
 * KoSch's application manifest disables Android Auto Backup and this key is not part of the portable
 * workspace-backup schema. A future security-policy export must be a separate, explicit feature.
 */
class SharedPreferencesFirewallPolicyStorage(
    private val preferences: SharedPreferences,
) : FirewallPolicyRawStorage {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
    )

    override fun readRaw(): String? = preferences.getString(KEY_POLICY, null)

    override fun writeRaw(raw: String): Boolean {
        require(raw.toByteArray(Charsets.UTF_8).size <= FIREWALL_POLICY_MAX_SERIALIZED_BYTES) {
            "Firewall policy exceeds storage limit"
        }
        return preferences.edit().putString(KEY_POLICY, raw).commit()
    }

    override fun clear(): Boolean = preferences.edit().remove(KEY_POLICY).commit()

    companion object {
        internal const val PREFERENCES_NAME = "kosch_firewall_policy_v1"
        internal const val KEY_POLICY = "policy_raw"
    }
}
