package cloud.kosch.aiandroid.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PendingDocumentStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `payload survives store recreation and is consumed exactly once`() {
        val directory = temporaryFolder.newFolder("pending")
        val original = "private payload".encodeToByteArray()
        val token = PendingDocumentStore(directory)
            .stage(PendingDocumentKind.AUDIT, original)
            .getOrThrow()

        assertTrue(original.all { it == 0.toByte() })
        val recreated = PendingDocumentStore(directory)
        assertTrue(recreated.contains(PendingDocumentKind.AUDIT, token))
        assertArrayEquals("private payload".encodeToByteArray(), recreated.consume(PendingDocumentKind.AUDIT, token))
        assertNull(recreated.consume(PendingDocumentKind.AUDIT, token))
    }

    @Test
    fun `token kind cannot be swapped and expired files are removed`() {
        val directory = temporaryFolder.newFolder("expiry")
        val store = PendingDocumentStore(directory)
        val token = store.stage(PendingDocumentKind.BACKUP, byteArrayOf(1, 2, 3)).getOrThrow()

        assertFalse(store.contains(PendingDocumentKind.INK_SVG, token))
        directory.resolve(token).setLastModified(1L)
        store.cleanupExpired(nowEpochMillis = 2L * 24L * 60L * 60L * 1_000L)
        assertFalse(store.contains(PendingDocumentKind.BACKUP, token))
    }
}
