package cloud.kosch.aiandroid.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Device-local encrypted storage for KAL provider credentials.
 *
 * The app currently declares android:allowBackup="false", so these encrypted blobs are not portable. The AES key
 * itself is non-exportable and lives in Android Keystore. This vault intentionally stores only credentials/tokens;
 * normal provider preferences and model choices belong in non-secret state.
 */
class KalProviderCredentialVault(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun put(providerId: String, type: KalCredentialType, value: String) {
        require(value.isNotEmpty()) { "Credential must not be empty" }
        val storageKey = storageKey(providerId, type)
        preferences.edit().putString(storageKey, encrypt(value)).apply()
    }

    fun get(providerId: String, type: KalCredentialType): String? {
        val stored = preferences.getString(storageKey(providerId, type), null) ?: return null
        return runCatching { decrypt(stored) }.getOrNull()
    }

    fun remove(providerId: String, type: KalCredentialType) {
        preferences.edit().remove(storageKey(providerId, type)).apply()
    }

    fun removeProvider(providerId: String) {
        val normalized = normalizeProviderId(providerId)
        val prefix = "$normalized:"
        val matching = preferences.all.keys.filter { it.startsWith(prefix) }
        if (matching.isEmpty()) return
        preferences.edit().also { editor ->
            matching.forEach(editor::remove)
        }.apply()
    }

    fun clearAll() {
        preferences.edit().clear().apply()
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return listOf(
            FORMAT_VERSION,
            base64(cipher.iv),
            base64(cipherText),
        ).joinToString(SEPARATOR)
    }

    private fun decrypt(encoded: String): String {
        val parts = encoded.split(SEPARATOR)
        require(parts.size == 3 && parts[0] == FORMAT_VERSION) { "Unsupported credential format" }
        val iv = decodeBase64(parts[1])
        val cipherText = decodeBase64(parts[2])
        require(iv.size == GCM_IV_BYTES) { "Invalid credential IV" }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(GCM_TAG_BITS, iv),
        )
        return cipher.doFinal(cipherText).toString(Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(false)
                .build(),
        )
        return generator.generateKey()
    }

    private fun storageKey(providerId: String, type: KalCredentialType): String =
        "${normalizeProviderId(providerId)}:${type.storageId}"

    private fun normalizeProviderId(providerId: String): String {
        val normalized = providerId.trim().lowercase()
        require(normalized.matches(PROVIDER_ID_PATTERN)) { "Invalid provider id" }
        return normalized
    }

    private fun base64(bytes: ByteArray): String = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(bytes)

    private fun decodeBase64(value: String): ByteArray = Base64.getUrlDecoder().decode(value)

    companion object {
        private const val PREFERENCES_NAME = "kal_provider_credentials_v1"
        private const val KEY_ALIAS = "kal.provider.credentials.v1"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val FORMAT_VERSION = "v1"
        private const val SEPARATOR = "."
        private const val GCM_TAG_BITS = 128
        private const val GCM_IV_BYTES = 12
        private val PROVIDER_ID_PATTERN = Regex("^[a-z0-9][a-z0-9._-]{0,63}$")
    }
}

enum class KalCredentialType(val storageId: String) {
    API_KEY("api_key"),
    ACCESS_TOKEN("access_token"),
    REFRESH_TOKEN("refresh_token"),
    OAUTH_GENERATED_KEY("oauth_generated_key"),
    ID_TOKEN("id_token"),
}
