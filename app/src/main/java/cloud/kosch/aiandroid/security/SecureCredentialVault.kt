package cloud.kosch.aiandroid.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class SecureCredentialType(val storageId: String) {
    API_KEY("api_key"),
    ACCESS_TOKEN("access_token"),
    REFRESH_TOKEN("refresh_token"),
    OAUTH_GENERATED_KEY("oauth_generated_key"),
    ID_TOKEN("id_token"),
}

/**
 * Device-local credential boundary for optional KAL provider connections.
 *
 * Credentials are encrypted with a non-exportable Android Keystore AES key. The caller passes mutable [CharArray]
 * material so raw secrets can be cleared after encryption. The application currently disables Android backup, and
 * portable KAL backup formats must never include values stored here.
 */
class SecureCredentialVault(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** Backward-compatible API-key lookup used by the earlier dormant provider boundary. */
    fun contains(providerId: String): Boolean = contains(providerId, SecureCredentialType.API_KEY)

    fun contains(providerId: String, type: SecureCredentialType): Boolean =
        preferences.contains(storageKey(providerId, type)) ||
            (type == SecureCredentialType.API_KEY && preferences.contains(legacyStorageKey(providerId)))

    /** Backward-compatible API-key write. */
    fun put(providerId: String, secret: CharArray) = put(providerId, SecureCredentialType.API_KEY, secret)

    fun put(providerId: String, type: SecureCredentialType, secret: CharArray) {
        val normalizedProviderId = normalizeProviderId(providerId)
        require(secret.isNotEmpty()) { "Secret must not be empty" }
        val aad = aad(normalizedProviderId, type)
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            cipher.updateAAD(aad)
            val plaintext = String(secret).toByteArray(Charsets.UTF_8)
            try {
                val ciphertext = cipher.doFinal(plaintext)
                val payload = listOf(
                    VERSION,
                    Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
                    Base64.encodeToString(ciphertext, Base64.NO_WRAP),
                ).joinToString(SEPARATOR)
                preferences.edit()
                    .putString(storageKey(normalizedProviderId, type), payload)
                    .apply()
            } finally {
                plaintext.fill(0)
            }
        } finally {
            secret.fill('\u0000')
        }
    }

    /** Backward-compatible API-key read. */
    fun read(providerId: String): CharArray? = read(providerId, SecureCredentialType.API_KEY)

    fun read(providerId: String, type: SecureCredentialType): CharArray? {
        val normalizedProviderId = normalizeProviderId(providerId)
        val newKey = storageKey(normalizedProviderId, type)
        val payload = preferences.getString(newKey, null)
        if (payload != null) {
            return decrypt(payload, aad(normalizedProviderId, type))
        }

        // One-time compatibility path for API keys written before credential typing existed.
        if (type != SecureCredentialType.API_KEY) return null
        val legacyKey = legacyStorageKey(normalizedProviderId)
        val legacyPayload = preferences.getString(legacyKey, null) ?: return null
        val secret = decrypt(legacyPayload, normalizedProviderId.toByteArray(Charsets.UTF_8)) ?: return null
        put(normalizedProviderId, type, secret.copyOf())
        preferences.edit().remove(legacyKey).apply()
        return secret
    }

    /** Backward-compatible API-key delete. */
    fun delete(providerId: String) = delete(providerId, SecureCredentialType.API_KEY)

    fun delete(providerId: String, type: SecureCredentialType) {
        val normalizedProviderId = normalizeProviderId(providerId)
        preferences.edit()
            .remove(storageKey(normalizedProviderId, type))
            .also { editor ->
                if (type == SecureCredentialType.API_KEY) editor.remove(legacyStorageKey(normalizedProviderId))
            }
            .apply()
    }

    fun deleteProvider(providerId: String) {
        val normalizedProviderId = normalizeProviderId(providerId)
        val prefix = "provider_${normalizedProviderId}_"
        val editor = preferences.edit()
        preferences.all.keys
            .filter { it.startsWith(prefix) || it == legacyStorageKey(normalizedProviderId) }
            .forEach(editor::remove)
        editor.apply()
    }

    private fun decrypt(payload: String, aad: ByteArray): CharArray? {
        val parts = payload.split(SEPARATOR)
        if (parts.size != 3 || parts[0] != VERSION) return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = Base64.decode(parts[1], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[2], Base64.NO_WRAP)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(aad)
            val plaintext = cipher.doFinal(ciphertext)
            try {
                plaintext.toString(Charsets.UTF_8).toCharArray()
            } finally {
                plaintext.fill(0)
            }
        }.getOrNull()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private fun aad(providerId: String, type: SecureCredentialType): ByteArray =
        "$providerId:${type.storageId}".toByteArray(Charsets.UTF_8)

    private fun storageKey(providerId: String, type: SecureCredentialType) =
        "provider_${providerId}_${type.storageId}"

    private fun legacyStorageKey(providerId: String) = "provider_$providerId"

    private fun normalizeProviderId(providerId: String): String {
        val normalized = providerId.trim().lowercase()
        require(PROVIDER_ID.matches(normalized)) { "Invalid provider ID" }
        return normalized
    }

    private companion object {
        const val PREFERENCES_NAME = "kosch_api_credentials_no_backup"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "kosch-provider-vault-v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val VERSION = "v1"
        const val SEPARATOR = ":"
        val PROVIDER_ID = "[a-z0-9][a-z0-9._-]{0,63}".toRegex()
    }
}
