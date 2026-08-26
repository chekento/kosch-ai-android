package cloud.kosch.aiandroid.security.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkAddressTest {
    @Test
    fun `numeric IPv4 and IPv6 parse without hostname fallback`() {
        val ipv4 = NetworkAddress.parseNumeric("93.184.216.34")
        val ipv6 = NetworkAddress.parseNumeric("2001:db8::53")
        val embedded = NetworkAddress.parseNumeric("::ffff:192.0.2.1")

        assertEquals(IpFamily.IPV4, ipv4.family)
        assertEquals("93.184.216.34", ipv4.toString())
        assertEquals(IpFamily.IPV6, ipv6.family)
        assertEquals(16, ipv6.asByteArray().size)
        assertEquals(IpFamily.IPV6, embedded.family)
    }

    @Test
    fun `hostname-like text is rejected locally`() {
        val failure = runCatching { NetworkAddress.parseNumeric("example.com") }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `invalid and zoned addresses are rejected`() {
        assertTrue(runCatching { NetworkAddress.parseNumeric("999.1.2.3") }.isFailure)
        assertTrue(runCatching { NetworkAddress.parseNumeric("2001:db8:::1") }.isFailure)
        assertTrue(runCatching { NetworkAddress.parseNumeric("fe80::1%wlan0") }.isFailure)
    }

    @Test
    fun `cidr boundaries work for IPv4 and IPv6`() {
        val v4 = CidrBlock.parse("10.24.0.0/16")
        assertTrue(v4.contains(NetworkAddress.parseNumeric("10.24.9.8")))
        assertFalse(v4.contains(NetworkAddress.parseNumeric("10.25.0.1")))

        val v6 = CidrBlock.parse("2001:db8:42::/48")
        assertTrue(v6.contains(NetworkAddress.parseNumeric("2001:db8:42::9")))
        assertFalse(v6.contains(NetworkAddress.parseNumeric("2001:db8:43::9")))
    }

    @Test
    fun `address bytes are defensively copied`() {
        val bytes = byteArrayOf(192.toByte(), 0, 2, 1)
        val address = NetworkAddress.fromBytes(bytes)
        bytes[0] = 10
        val exported = address.asByteArray()
        exported[1] = 99

        assertEquals("192.0.2.1", address.toString())
    }
}
