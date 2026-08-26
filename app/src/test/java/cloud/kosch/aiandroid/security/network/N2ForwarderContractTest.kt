package cloud.kosch.aiandroid.security.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class N2ForwarderContractTest {
    @Test
    fun validRequest_preservesGenerationAndFamilies() {
        val request = N2ForwarderStartRequest(
            activationGeneration = 4,
            mtu = 1_500,
            ipv4Enabled = true,
            ipv6Enabled = true,
        )

        assertEquals(4L, request.activationGeneration)
        assertTrue(request.ipv4Enabled)
        assertTrue(request.ipv6Enabled)
    }

    @Test
    fun zeroGeneration_isRejected() {
        val failure = runCatching {
            N2ForwarderStartRequest(activationGeneration = 0, mtu = 1_500)
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun unsafeMtu_isRejected() {
        val tooSmall = runCatching {
            N2ForwarderStartRequest(activationGeneration = 1, mtu = 575)
        }.exceptionOrNull()
        val tooLarge = runCatching {
            N2ForwarderStartRequest(activationGeneration = 1, mtu = 9_001)
        }.exceptionOrNull()

        assertTrue(tooSmall is IllegalArgumentException)
        assertTrue(tooLarge is IllegalArgumentException)
    }

    @Test
    fun disablingBothIpFamilies_isRejected() {
        val failure = runCatching {
            N2ForwarderStartRequest(
                activationGeneration = 1,
                mtu = 1_500,
                ipv4Enabled = false,
                ipv6Enabled = false,
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun failedResults_rejectBlankDiagnostics() {
        val startFailure = runCatching { N2ForwarderStartResult.Failed(" ") }.exceptionOrNull()
        val stopFailure = runCatching { N2ForwarderStopResult.Failed("") }.exceptionOrNull()

        assertTrue(startFailure is IllegalArgumentException)
        assertTrue(stopFailure is IllegalArgumentException)
    }
}
