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

/**
 * Dormant credential boundary for a future optional API module. M2 has no INTERNET permission and
 * never calls this from the UI, but secrets can later be stored without ever persisting raw keys.
 */
class SecureCredentialVault(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun contains(providerId: String): Boolean = preferences.contains(storageKey(providerId))

    fun put(providerId: String, secret: CharArray) {
        requireProviderId(providerId)
        require(secret.isNotEmpty()) { "Secret must not be empty" }
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            cipher.updateAAD(providerId.toByteArray(Charsets.UTF_8))
            val plaintext = String(secret).toByteArray(Charsets.UTF_8)
            try {
                val ciphertext = cipher.doFinal(plaintext)
                val payload = listOf(
                    VERSION,
                    Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
                    Base64.encodeToString(ciphertext, Base64.NO_WRAP),
                ).joinToString(SEPARATOR)
                preferences.edit().putString(storageKey(providerId), payload).apply()
            } finally {
                plaintext.fill(0)
            }
        } finally {
            secret.fill('\u0000')
        }
    }

    fun read(providerId: String): CharArray? {
        requireProviderId(providerId)
        val parts = preferences.getString(storageKey(providerId), null)?.split(SEPARATOR) ?: return null
        if (parts.size != 3 || parts[0] != VERSION) return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = Base64.decode(parts[1], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[2], Base64.NO_WRAP)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(providerId.toByteArray(Charsets.UTF_8))
            val plaintext = cipher.doFinal(ciphertext)
            try {
                plaintext.toString(Charsets.UTF_8).toCharArray()
            } finally {
                plaintext.fill(0)
            }
        }.getOrNull()
    }

    fun delete(providerId: String) {
        requireProviderId(providerId)
        preferences.edit().remove(storageKey(providerId)).apply()
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
                    .build(),
            )
            generateKey()
        }
    }

    private fun storageKey(providerId: String) = "provider_${providerId.lowercase()}"

    private fun requireProviderId(providerId: String) {
        require(PROVIDER_ID.matches(providerId)) { "Invalid provider ID" }
    }

    private companion object {
        const val PREFERENCES_NAME = "kosch_api_credentials_no_backup"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "kosch-provider-vault-v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val VERSION = "v1"
        const val SEPARATOR = ":"
        val PROVIDER_ID = "[a-z0-9][a-z0-9-]{1,48}".toRegex()
    }
}
