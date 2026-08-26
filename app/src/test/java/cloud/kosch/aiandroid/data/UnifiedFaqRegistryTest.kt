package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.FaqCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedFaqRegistryTest {
    @Test
    fun `central FAQ contains the Security Network N1 contract`() {
        val ids = UnifiedFaqRegistry.entries.map { it.id }.toSet()

        assertTrue("security-network-n1" in ids)
        assertTrue("security-network-consent" in ids)
        assertTrue("security-network-privacy" in ids)
        assertTrue("security-network-play" in ids)
        assertTrue("security-network-n2" in ids)
    }

    @Test
    fun `security privacy answer excludes traffic runtime data from persistence`() {
        val answer = UnifiedFaqRegistry.entries
            .first { it.id == "security-network-privacy" }
            .answer

        assertTrue(answer.contains("Keine."))
        assertTrue(answer.contains("IPs"))
        assertTrue(answer.contains("Ports"))
        assertTrue(answer.contains("Paketdaten"))
        assertTrue(answer.contains("nicht Bestandteil des portablen Workspace-Backups"))
    }

    @Test
    fun `security N1 answer never claims an active VPN`() {
        val answer = UnifiedFaqRegistry.entries
            .first { it.id == "security-network-n1" }
            .answer

        assertTrue(answer.contains("keinen VPN-Tunnel"))
        assertTrue(answer.contains("keine Pakete"))
        assertTrue(answer.contains("Traffic-Zähler"))
    }

    @Test
    fun `security entries participate in normal search and Android category filtering`() {
        val result = UnifiedFaqRegistry.search("VPN", FaqCategory.ANDROID)

        assertTrue(result.any { it.id == "security-network-consent" })
        assertEquals(setOf(FaqCategory.ANDROID), result.map { it.category }.toSet())
    }
}
