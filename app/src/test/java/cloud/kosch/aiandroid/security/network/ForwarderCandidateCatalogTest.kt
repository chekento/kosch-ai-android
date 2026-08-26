package cloud.kosch.aiandroid.security.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForwarderCandidateCatalogTest {
    @Test
    fun catalogEntries_areUniquelyPinned() {
        val keys = ForwarderCandidateCatalog.all.map { "${it.id}@${it.pinnedVersion}" }

        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.all { '@' in it })
    }

    @Test
    fun noCatalogCandidate_isActivationEligibleByDeclarationAlone() {
        ForwarderCandidateCatalog.all.forEach { candidate ->
            val result = ForwarderCandidateEvaluation.evaluate(candidate, candidate.intendedUseCase)
            assertFalse(
                "Pinned source facts must not masquerade as KoSch runtime evidence: ${candidate.id}",
                result.activationEligible,
            )
        }
    }

    @Test
    fun onlyTun2socks_isCurrentN2PrototypeFit() {
        val n2Fits = ForwarderCandidateCatalog.all.filter {
            ForwarderCandidateEvaluation.evaluate(it, ForwarderUseCase.N2_DIRECT).prototypeEligible
        }

        assertEquals(listOf(ForwarderCandidateCatalog.TUN2SOCKS_2_7_0), n2Fits)
    }
}
