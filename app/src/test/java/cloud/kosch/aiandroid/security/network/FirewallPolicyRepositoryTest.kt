package cloud.kosch.aiandroid.security.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirewallPolicyRepositoryTest {
    private class FakeStorage(initial: String? = null) : FirewallPolicyRawStorage {
        var raw: String? = initial
        var writes = 0
        var clears = 0

        override fun readRaw(): String? = raw

        override fun writeRaw(raw: String): Boolean {
            writes += 1
            this.raw = raw
            return true
        }

        override fun clear(): Boolean {
            clears += 1
            raw = null
            return true
        }
    }

    @Test
    fun absentLoad_isSideEffectFree() {
        val storage = FakeStorage()
        val repository = FirewallPolicyRepository(storage)

        assertEquals(FirewallPolicyLoadState.Absent, repository.load())
        assertEquals(0, storage.writes)
        assertEquals(0, storage.clears)
        assertNull(storage.raw)
    }

    @Test
    fun validPolicy_loadsWithoutRewritingStorage() {
        val raw = FirewallPolicyCodec.encode(
            FirewallPolicyDocument(
                rules = listOf(FirewallRule("block", 1, FirewallVerdict.BLOCK)),
            ),
        )
        val storage = FakeStorage(raw)
        val repository = FirewallPolicyRepository(storage)

        val state = repository.load() as FirewallPolicyLoadState.Ready

        assertEquals("block", state.document.normalizedRules().single().id)
        assertEquals(raw, storage.raw)
        assertEquals(0, storage.writes)
    }

    @Test
    fun corruptPolicy_isPreservedByteForByteUntilExplicitAction() {
        val original = "broken-policy\nwith-data"
        val storage = FakeStorage(original)
        val repository = FirewallPolicyRepository(storage)

        val state = repository.load()

        assertTrue(state is FirewallPolicyLoadState.Corrupt)
        assertEquals(original, storage.raw)
        assertEquals(0, storage.writes)
        assertEquals(0, storage.clears)
    }

    @Test
    fun futurePolicy_isPreservedAndReportedWithoutDowngrade() {
        val original = "KOSCH_FIREWALL_POLICY|9\nopaque-future-fields\n"
        val storage = FakeStorage(original)
        val repository = FirewallPolicyRepository(storage)

        val state = repository.load() as FirewallPolicyLoadState.FutureSchema

        assertEquals(9, state.schemaVersion)
        assertEquals(original.toByteArray(Charsets.UTF_8).size, state.rawByteCount)
        assertEquals(original, storage.raw)
        assertEquals(0, storage.writes)
    }

    @Test
    fun replace_isAlwaysExplicitAndProducesDecodableState() {
        val storage = FakeStorage("broken")
        val repository = FirewallPolicyRepository(storage)
        val document = FirewallPolicyDocument(
            rules = listOf(
                FirewallRule(
                    id = "allow-work",
                    priority = 5,
                    verdict = FirewallVerdict.ALLOW,
                    ownerUid = 10123,
                ),
            ),
        )

        assertTrue(repository.replace(document))
        assertEquals(1, storage.writes)
        assertTrue(repository.load() is FirewallPolicyLoadState.Ready)
    }

    @Test
    fun reset_isExplicitAndDestructive() {
        val storage = FakeStorage(
            FirewallPolicyCodec.encode(FirewallPolicyDocument(rules = emptyList())),
        )
        val repository = FirewallPolicyRepository(storage)

        assertTrue(repository.reset())
        assertEquals(1, storage.clears)
        assertFalse(repository.load() is FirewallPolicyLoadState.Ready)
        assertEquals(FirewallPolicyLoadState.Absent, repository.load())
    }
}
