package cloud.kosch.aiandroid.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Base64

class PortableBackupCodecTest {
    private val codec = PortableBackupCodec()

    @Test
    fun roundTripPreservesPayload() {
        val original = "{\"format\":\"cloud.kosch.workspace\",\"version\":1}".encodeToByteArray()
        val encrypted = codec.encrypt(original, "correct horse battery staple".toCharArray())

        assertFalse(encrypted.contains("cloud.kosch.workspace"))
        assertArrayEquals(
            original,
            codec.decrypt(encrypted, "correct horse battery staple".toCharArray()),
        )
    }

    @Test
    fun wrongPassphraseIsRejected() {
        val encrypted = codec.encrypt("workspace".encodeToByteArray(), "this is the right phrase".toCharArray())

        assertThrows(BackupDecryptionException::class.java) {
            codec.decrypt(encrypted, "this is a wrong phrase".toCharArray())
        }
    }

    @Test
    fun modifiedCiphertextIsRejected() {
        val encrypted = codec.encrypt("workspace".encodeToByteArray(), "this is the right phrase".toCharArray())
        val parts = encrypted.split(':').toMutableList()
        val ciphertext = Base64.getUrlDecoder().decode(parts.last())
        ciphertext[0] = (ciphertext[0].toInt() xor 0x01).toByte()
        parts[parts.lastIndex] = Base64.getUrlEncoder().withoutPadding().encodeToString(ciphertext)
        val modified = parts.joinToString(":")

        assertThrows(BackupDecryptionException::class.java) {
            codec.decrypt(modified, "this is the right phrase".toCharArray())
        }
        ciphertext.fill(0)
    }

    @Test
    fun nonCanonicalBase64IsRejected() {
        val encrypted = codec.encrypt("workspace".encodeToByteArray(), "this is the right phrase".toCharArray())
        val modified = "$encrypted=="

        assertThrows(BackupDecryptionException::class.java) {
            codec.decrypt(modified, "this is the right phrase".toCharArray())
        }
    }

    @Test
    fun shortPassphraseIsRejectedBeforeEncryption() {
        assertThrows(IllegalArgumentException::class.java) {
            codec.encrypt("workspace".encodeToByteArray(), "too-short".toCharArray())
        }
    }
}
