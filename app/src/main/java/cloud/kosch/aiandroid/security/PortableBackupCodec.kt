package cloud.kosch.aiandroid.security

import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Password-based, portable encryption for user-initiated Workspace exports.
 *
 * The format is deliberately small, versioned and authenticated. The header is used as GCM AAD,
 * so its version and work factor cannot be changed without invalidating the backup.
 */
class PortableBackupCodec(
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    fun encrypt(payload: ByteArray, passphrase: CharArray): String {
        require(payload.isNotEmpty()) { "Backup payload is empty" }
        require(payload.size <= MAX_PAYLOAD_BYTES) { "Backup payload is too large" }
        require(passphrase.size >= MIN_PASSPHRASE_LENGTH) {
            "Passphrase must contain at least $MIN_PASSPHRASE_LENGTH characters"
        }

        val salt = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(secureRandom::nextBytes)
        val key = deriveKey(passphrase, salt, PBKDF2_ITERATIONS)
        return try {
            val aad = aad(VERSION, PBKDF2_ITERATIONS)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
                updateAAD(aad)
            }
            val encrypted = cipher.doFinal(payload)
            listOf(
                MAGIC,
                VERSION.toString(),
                PBKDF2_ITERATIONS.toString(),
                encode(salt),
                encode(nonce),
                encode(encrypted),
            ).joinToString(SEPARATOR)
        } finally {
            key.fill(0)
            salt.fill(0)
            nonce.fill(0)
        }
    }

    @Throws(BackupDecryptionException::class)
    fun decrypt(envelope: String, passphrase: CharArray): ByteArray {
        if (envelope.length > MAX_ENVELOPE_CHARS) {
            throw BackupDecryptionException("Backup is too large")
        }
        val parts = envelope.trim().split(SEPARATOR)
        if (parts.size != PART_COUNT || parts[0] != MAGIC) {
            throw BackupDecryptionException("Unknown backup format")
        }
        val version = parts[1].toIntOrNull()
            ?: throw BackupDecryptionException("Invalid backup version")
        if (version != VERSION) throw BackupDecryptionException("Unsupported backup version: $version")
        val iterations = parts[2].toIntOrNull()
            ?.takeIf { it in MIN_ACCEPTED_ITERATIONS..MAX_ACCEPTED_ITERATIONS }
            ?: throw BackupDecryptionException("Invalid key derivation settings")

        val salt = decode(parts[3], SALT_BYTES, "salt")
        val nonce = decode(parts[4], NONCE_BYTES, "nonce")
        val encrypted = decode(parts[5], null, "payload")
        if (encrypted.size !in (GCM_TAG_BITS / 8 + 1)..MAX_ENCRYPTED_BYTES) {
            throw BackupDecryptionException("Invalid encrypted payload size")
        }

        val key = deriveKey(passphrase, salt, iterations)
        return try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
                updateAAD(aad(version, iterations))
            }
            cipher.doFinal(encrypted).also { payload ->
                if (payload.size > MAX_PAYLOAD_BYTES) {
                    payload.fill(0)
                    throw BackupDecryptionException("Decrypted payload is too large")
                }
            }
        } catch (_: AEADBadTagException) {
            throw BackupDecryptionException("Passphrase is wrong or backup was modified")
        } catch (exception: GeneralSecurityException) {
            throw BackupDecryptionException("Backup could not be decrypted", exception)
        } finally {
            key.fill(0)
            salt.fill(0)
            nonce.fill(0)
            encrypted.fill(0)
        }
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(KEY_DERIVATION).generateSecret(spec).encoded
        } catch (exception: GeneralSecurityException) {
            throw BackupDecryptionException("Key derivation is unavailable", exception)
        } finally {
            spec.clearPassword()
        }
    }

    private fun decode(value: String, exactSize: Int?, label: String): ByteArray = try {
        Base64.getUrlDecoder().decode(value).also { decoded ->
            if (encode(decoded) != value) {
                decoded.fill(0)
                throw BackupDecryptionException("Invalid $label encoding")
            }
            if (exactSize != null && decoded.size != exactSize) {
                decoded.fill(0)
                throw BackupDecryptionException("Invalid $label size")
            }
        }
    } catch (exception: IllegalArgumentException) {
        throw BackupDecryptionException("Invalid $label encoding", exception)
    }

    private fun encode(value: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(value)

    private fun aad(version: Int, iterations: Int): ByteArray =
        "$MAGIC$SEPARATOR$version$SEPARATOR$iterations".encodeToByteArray()

    companion object {
        const val MIN_PASSPHRASE_LENGTH = 12
        const val MAX_PAYLOAD_BYTES = 5 * 1024 * 1024
        private const val MAX_ENCRYPTED_BYTES = MAX_PAYLOAD_BYTES + 32
        private const val MAX_ENVELOPE_CHARS = 8 * 1024 * 1024
        private const val MAGIC = "KOSCH-BACKUP"
        private const val VERSION = 1
        private const val PART_COUNT = 6
        private const val SEPARATOR = ":"
        private const val PBKDF2_ITERATIONS = 210_000
        private const val MIN_ACCEPTED_ITERATIONS = 150_000
        private const val MAX_ACCEPTED_ITERATIONS = 2_000_000
        private const val KEY_BITS = 256
        private const val SALT_BYTES = 16
        private const val NONCE_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private const val KEY_DERIVATION = "PBKDF2WithHmacSHA256"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

class BackupDecryptionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
