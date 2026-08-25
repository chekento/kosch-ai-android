package cloud.kosch.aiandroid.security.network

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class NetworkGuardCoreTest {
    @Test
    fun ipv4Tcp_extractsDestinationIpAndPorts_withoutPayloadRetention() {
        val packet = ByteArray(40)
        packet[0] = 0x45
        setU16(packet, 2, 40)
        packet[9] = 6
        setIpv4(packet, 12, 10, 0, 0, 2)
        setIpv4(packet, 16, 93, 184, 216, 34)
        setU16(packet, 20, 50_000)
        setU16(packet, 22, 443)

        val parsed = PacketMetadataParser.parse(packet) as PacketParseOutcome.Parsed

        assertEquals(IpFamily.IPV4, parsed.metadata.family)
        assertEquals("93.184.216.34", parsed.metadata.destination.toString())
        assertEquals(TrafficProtocol.TCP, parsed.metadata.protocol)
        assertEquals(50_000, parsed.metadata.sourcePort)
        assertEquals(443, parsed.metadata.destinationPort)
        assertEquals(40, parsed.metadata.packetLengthBytes)
        assertFalse(parsed.metadata.nonInitialFragment)
    }

    @Test
    fun ipv4NonInitialFragment_neverInventsPorts() {
        val packet = ByteArray(28)
        packet[0] = 0x45
        setU16(packet, 2, 28)
        setU16(packet, 6, 1) // fragment offset = 1
        packet[9] = 17
        setIpv4(packet, 12, 192, 0, 2, 10)
        setIpv4(packet, 16, 198, 51, 100, 20)

        val parsed = PacketMetadataParser.parse(packet) as PacketParseOutcome.Parsed

        assertEquals(TrafficProtocol.UDP, parsed.metadata.protocol)
        assertTrue(parsed.metadata.nonInitialFragment)
        assertNull(parsed.metadata.sourcePort)
        assertNull(parsed.metadata.destinationPort)
    }

    @Test
    fun ipv6Udp_extractsDestinationAndPorts() {
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

        val parsed = PacketMetadataParser.parse(packet) as PacketParseOutcome.Parsed

        assertEquals(IpFamily.IPV6, parsed.metadata.family)
        assertEquals(TrafficProtocol.UDP, parsed.metadata.protocol)
        assertEquals(53_000, parsed.metadata.sourcePort)
        assertEquals(53, parsed.metadata.destinationPort)
    }

    @Test
    fun malformedTransportHeader_isRejectedInsteadOfReadingPastBounds() {
        val packet = ByteArray(22)
        packet[0] = 0x45
        setU16(packet, 2, 22)
        packet[9] = 6
        setIpv4(packet, 12, 10, 0, 0, 1)
        setIpv4(packet, 16, 10, 0, 0, 2)

        assertTrue(PacketMetadataParser.parse(packet) is PacketParseOutcome.Malformed)
    }

    @Test
    fun randomInput_neverEscapesParserAsException() {
        val random = Random(0x4B4F5343)
        repeat(5_000) {
            val size = random.nextInt(0, 512)
            val packet = ByteArray(size)
            random.nextBytes(packet)
            try {
                PacketMetadataParser.parse(packet)
            } catch (failure: Throwable) {
                fail("Parser escaped on random input of $size bytes: ${failure::class.java.simpleName}")
            }
        }
    }

    @Test
    fun cidrMatching_handlesIpv4AndIpv6Boundaries() {
        val v4 = CidrBlock.parse("10.24.0.0/16")
        assertTrue(v4.contains(NetworkAddress.parseNumeric("10.24.9.8")))
        assertFalse(v4.contains(NetworkAddress.parseNumeric("10.25.0.1")))

        val v6 = CidrBlock.parse("2001:db8:42::/48")
        assertTrue(v6.contains(NetworkAddress.parseNumeric("2001:db8:42::9")))
        assertFalse(v6.contains(NetworkAddress.parseNumeric("2001:db8:43::9")))
    }

    @Test
    fun numericAddressParser_rejectsHostnamesInsteadOfResolvingDns() {
        val rejected = runCatching { NetworkAddress.parseNumeric("deadbeef") }.exceptionOrNull()
        assertTrue("Hostname-like input must be rejected locally", rejected is IllegalArgumentException)
    }

    @Test
    fun firewall_firstPriorityMatchWins_deterministically() {
        val flow = NetworkFlow(
            destination = NetworkAddress.parseNumeric("203.0.113.45"),
            protocol = TrafficProtocol.TCP,
            destinationPort = 443,
            ownerUid = 10_123,
            packageName = "cloud.kosch.example",
        )
        val rules = listOf(
            FirewallRule(
                id = "allow-specific",
                priority = 20,
                verdict = FirewallVerdict.ALLOW,
                packageName = "cloud.kosch.example",
                destinationPorts = PortRange(443),
            ),
            FirewallRule(
                id = "block-range",
                priority = 10,
                verdict = FirewallVerdict.BLOCK,
                destination = CidrBlock.parse("203.0.113.0/24"),
                protocol = TrafficProtocol.TCP,
            ),
        )

        val decision = FirewallRuleEngine.evaluate(flow, rules)

        assertEquals(FirewallVerdict.BLOCK, decision.verdict)
        assertEquals("block-range", decision.matchedRuleId)
    }

    @Test
    fun firewall_unknownApp_doesNotMatchAppSpecificRule() {
        val flow = NetworkFlow(
            destination = NetworkAddress.parseNumeric("198.51.100.7"),
            protocol = TrafficProtocol.UDP,
            destinationPort = 443,
        )
        val decision = FirewallRuleEngine.evaluate(
            flow,
            listOf(
                FirewallRule(
                    id = "block-known-app",
                    priority = 1,
                    verdict = FirewallVerdict.BLOCK,
                    packageName = "cloud.kosch.example",
                ),
            ),
        )

        assertEquals(FirewallVerdict.ALLOW, decision.verdict)
        assertNull(decision.matchedRuleId)
    }

    @Test
    fun trafficLedger_isStrictlyBoundedAndClearable() {
        val ledger = BoundedTrafficLedger(capacity = 2)
        val flow = NetworkFlow(NetworkAddress.parseNumeric("192.0.2.1"), TrafficProtocol.TCP, 443)
        repeat(3) { index ->
            ledger.record(
                TrafficEvent(
                    timestampEpochMillis = 1_000L + index,
                    flow = flow,
                    packetLengthBytes = 100 + index,
                    verdict = FirewallVerdict.ALLOW,
                ),
            )
        }

        assertEquals(listOf(1_001L, 1_002L), ledger.snapshot().map(TrafficEvent::timestampEpochMillis))
        ledger.clear()
        assertTrue(ledger.snapshot().isEmpty())
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
