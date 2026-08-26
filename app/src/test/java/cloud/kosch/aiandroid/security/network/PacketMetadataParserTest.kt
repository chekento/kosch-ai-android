package cloud.kosch.aiandroid.security.network

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PacketMetadataParserTest {
    @Test
    fun `IPv4 TCP extracts addresses and ports with a valid data offset`() {
        val packet = ByteArray(40)
        packet[0] = 0x45
        setU16(packet, 2, 40)
        packet[9] = 6
        setIpv4(packet, 12, 10, 0, 0, 2)
        setIpv4(packet, 16, 93, 184, 216, 34)
        setU16(packet, 20, 50_000)
        setU16(packet, 22, 443)
        packet[32] = 0x50 // TCP data offset = 5 * 4 = 20-byte header.

        val metadata = requireParsed(PacketMetadataParser.parse(packet))

        assertEquals(IpFamily.IPV4, metadata.family)
        assertEquals("93.184.216.34", metadata.destination.toString())
        assertEquals(TrafficProtocol.TCP, metadata.protocol)
        assertEquals(50_000, metadata.sourcePort)
        assertEquals(443, metadata.destinationPort)
        assertEquals(40, metadata.packetLengthBytes)
        assertFalse(metadata.nonInitialFragment)
    }

    @Test
    fun `IPv4 non-initial fragment never invents ports`() {
        val packet = ByteArray(28)
        packet[0] = 0x45
        setU16(packet, 2, 28)
        setU16(packet, 6, 1) // fragment offset = 1
        packet[9] = 17
        setIpv4(packet, 12, 192, 0, 2, 10)
        setIpv4(packet, 16, 198, 51, 100, 20)

        val metadata = requireParsed(PacketMetadataParser.parse(packet))

        assertEquals(TrafficProtocol.UDP, metadata.protocol)
        assertTrue(metadata.nonInitialFragment)
        assertNull(metadata.sourcePort)
        assertNull(metadata.destinationPort)
    }

    @Test
    fun `IPv6 UDP extracts addresses and ports`() {
        val packet = ByteArray(48)
        packet[0] = 0x60
        setU16(packet, 4, 8)
        packet[6] = 17
        packet[7] = 64
        NetworkAddress.parseNumeric("2001:db8::1").asByteArray().copyInto(packet, destinationOffset = 8)
        NetworkAddress.parseNumeric("2001:db8::53").asByteArray().copyInto(packet, destinationOffset = 24)
        setU16(packet, 40, 53_000)
        setU16(packet, 42, 53)
        setU16(packet, 44, 8)

        val metadata = requireParsed(PacketMetadataParser.parse(packet))

        assertEquals(IpFamily.IPV6, metadata.family)
        assertEquals(TrafficProtocol.UDP, metadata.protocol)
        assertEquals(53_000, metadata.sourcePort)
        assertEquals(53, metadata.destinationPort)
    }

    @Test
    fun `IPv6 hop-by-hop extension reaches validated UDP header`() {
        val packet = ByteArray(56)
        packet[0] = 0x60
        setU16(packet, 4, 16)
        packet[6] = 0 // Hop-by-Hop Options
        NetworkAddress.parseNumeric("2001:db8::1").asByteArray().copyInto(packet, destinationOffset = 8)
        NetworkAddress.parseNumeric("2001:db8::2").asByteArray().copyInto(packet, destinationOffset = 24)
        packet[40] = 17 // UDP follows
        packet[41] = 0 // 8-byte extension header
        setU16(packet, 48, 40_000)
        setU16(packet, 50, 443)
        setU16(packet, 52, 8)

        val metadata = requireParsed(PacketMetadataParser.parse(packet))

        assertEquals(TrafficProtocol.UDP, metadata.protocol)
        assertEquals(40_000, metadata.sourcePort)
        assertEquals(443, metadata.destinationPort)
    }

    @Test
    fun `invalid TCP data offset is malformed instead of cast-crashing`() {
        val packet = ByteArray(40)
        packet[0] = 0x45
        setU16(packet, 2, 40)
        packet[9] = 6
        setIpv4(packet, 12, 10, 0, 0, 1)
        setIpv4(packet, 16, 10, 0, 0, 2)
        setU16(packet, 20, 1234)
        setU16(packet, 22, 443)
        // Data-offset nibble intentionally remains 0.

        assertTrue(PacketMetadataParser.parse(packet) is PacketParseOutcome.Malformed)
    }

    @Test
    fun `truncated transport header is rejected before reading past bounds`() {
        val packet = ByteArray(22)
        packet[0] = 0x45
        setU16(packet, 2, 22)
        packet[9] = 6
        setIpv4(packet, 12, 10, 0, 0, 1)
        setIpv4(packet, 16, 10, 0, 0, 2)

        assertTrue(PacketMetadataParser.parse(packet) is PacketParseOutcome.Malformed)
    }

    @Test
    fun `IPv6 payload length zero with extra bytes is explicitly unsupported`() {
        val packet = ByteArray(48)
        packet[0] = 0x60
        setU16(packet, 4, 0)
        packet[6] = 17

        assertTrue(PacketMetadataParser.parse(packet) is PacketParseOutcome.Unsupported)
    }

    @Test
    fun `packet metadata cannot structurally retain payload bytes`() {
        val byteArrayFields = PacketMetadata::class.java.declaredFields.filter { field ->
            field.type == ByteArray::class.java
        }

        assertTrue(byteArrayFields.isEmpty())
    }

    @Test
    fun `ten thousand random inputs never escape parser as exceptions`() {
        val random = Random(0x4B4F5343)
        repeat(10_000) {
            val size = random.nextInt(0, 1_024)
            val packet = ByteArray(size)
            random.nextBytes(packet)
            runCatching { PacketMetadataParser.parse(packet) }
                .exceptionOrNull()
                ?.let { failure ->
                    throw AssertionError(
                        "Parser escaped on random input of $size bytes: ${failure::class.java.simpleName}",
                        failure,
                    )
                }
        }
    }

    private fun requireParsed(outcome: PacketParseOutcome): PacketMetadata = when (outcome) {
        is PacketParseOutcome.Parsed -> outcome.metadata
        is PacketParseOutcome.Malformed -> throw AssertionError("Expected parsed packet, got malformed: ${outcome.reason}")
        is PacketParseOutcome.Unsupported -> throw AssertionError("Expected parsed packet, got unsupported: ${outcome.reason}")
    }

    private fun setIpv4(packet: ByteArray, offset: Int, a: Int, b: Int, c: Int, d: Int) {
        packet[offset] = a.toByte()
        packet[offset + 1] = b.toByte()
        packet[offset + 2] = c.toByte()
        packet[offset + 3] = d.toByte()
    }

    private fun setU16(packet: ByteArray, offset: Int, value: Int) {
        packet[offset] = ((value ushr 8) and 0xff).toByte()
        packet[offset + 1] = (value and 0xff).toByte()
    }
}
