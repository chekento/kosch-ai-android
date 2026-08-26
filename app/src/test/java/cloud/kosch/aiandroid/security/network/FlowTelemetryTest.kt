package cloud.kosch.aiandroid.security.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowTelemetryTest {
    @Test
    fun `outbound and inbound packets aggregate into one directional flow`() {
        val table = BoundedFlowTable(capacity = 8)
        val attribution = FlowAttribution(ownerUid = 10_123, packageName = "cloud.kosch.example")

        table.record(
            TrafficObservation.from(
                metadata = packet(
                    source = "10.0.0.2",
                    destination = "203.0.113.9",
                    sourcePort = 50_000,
                    destinationPort = 443,
                    bytes = 100,
                ),
                direction = TrafficDirection.OUTBOUND,
                timestampEpochMillis = 1_000,
                attribution = attribution,
            ),
        )
        table.record(
            TrafficObservation.from(
                metadata = packet(
                    source = "203.0.113.9",
                    destination = "10.0.0.2",
                    sourcePort = 443,
                    destinationPort = 50_000,
                    bytes = 120,
                ),
                direction = TrafficDirection.INBOUND,
                timestampEpochMillis = 1_100,
                attribution = attribution,
            ),
        )

        val flow = table.snapshot().single()
        assertEquals("10.0.0.2", flow.key.localAddress.toString())
        assertEquals("203.0.113.9", flow.key.remoteAddress.toString())
        assertEquals(50_000, flow.key.localPort)
        assertEquals(443, flow.key.remotePort)
        assertEquals(100L, flow.outboundBytes)
        assertEquals(120L, flow.inboundBytes)
        assertEquals(1L, flow.outboundPackets)
        assertEquals(1L, flow.inboundPackets)
        assertEquals(220L, flow.totalBytes)
        assertEquals("cloud.kosch.example", flow.packageName)
        assertFalse(flow.packageAttributionAmbiguous)
    }

    @Test
    fun `unknown ownership is never merged into a later known UID`() {
        val table = BoundedFlowTable(capacity = 8)
        val metadata = packet("10.0.0.2", "198.51.100.7", 40_000, 53, 80)

        table.record(TrafficObservation.from(metadata, TrafficDirection.OUTBOUND, 1_000))
        table.record(
            TrafficObservation.from(
                metadata,
                TrafficDirection.OUTBOUND,
                1_010,
                FlowAttribution(10_123, "cloud.kosch.example"),
            ),
        )

        assertEquals(2, table.size())
        assertTrue(table.snapshot().any { it.key.ownerUid == null })
        assertTrue(table.snapshot().any { it.key.ownerUid == 10_123 })
    }

    @Test
    fun `conflicting package names for one UID become ambiguous instead of guessed`() {
        val table = BoundedFlowTable(capacity = 8)
        val metadata = packet("10.0.0.2", "198.51.100.7", 40_000, 443, 80)

        table.record(
            TrafficObservation.from(
                metadata,
                TrafficDirection.OUTBOUND,
                1_000,
                FlowAttribution(10_123, "cloud.kosch.first"),
            ),
        )
        table.record(
            TrafficObservation.from(
                metadata,
                TrafficDirection.OUTBOUND,
                1_010,
                FlowAttribution(10_123, "cloud.kosch.second"),
            ),
        )

        val flow = table.snapshot().single()
        assertNull(flow.packageName)
        assertTrue(flow.packageAttributionAmbiguous)
    }

    @Test
    fun `flow table uses access-order eviction and hard capacity`() {
        val table = BoundedFlowTable(capacity = 2)
        val a = observation("203.0.113.1", 1_000)
        val b = observation("203.0.113.2", 1_010)
        val c = observation("203.0.113.3", 1_030)

        table.record(a)
        table.record(b)
        table.record(a.copy(timestampEpochMillis = 1_020)) // Touch A; B becomes least-recently used.
        table.record(c)

        val remoteAddresses = table.snapshot().map { it.key.remoteAddress.toString() }.toSet()
        assertEquals(2, table.size())
        assertTrue("203.0.113.1" in remoteAddresses)
        assertTrue("203.0.113.3" in remoteAddresses)
        assertFalse("203.0.113.2" in remoteAddresses)
    }

    @Test
    fun `traffic ledger is bounded clearable and contains no firewall verdict`() {
        val ledger = BoundedTrafficLedger(capacity = 2)
        ledger.record(observation("192.0.2.1", 1_000))
        ledger.record(observation("192.0.2.2", 1_010))
        ledger.record(observation("192.0.2.3", 1_020))

        assertEquals(2, ledger.size())
        assertEquals(listOf(1_010L, 1_020L), ledger.snapshot().map { it.timestampEpochMillis })
        assertFalse(TrafficObservation::class.java.declaredFields.any { it.name.contains("verdict", ignoreCase = true) })

        ledger.clear()
        assertTrue(ledger.snapshot().isEmpty())
    }

    @Test
    fun `package attribution without UID is rejected`() {
        assertTrue(runCatching { FlowAttribution(ownerUid = null, packageName = "cloud.kosch.example") }.isFailure)
    }

    private fun observation(remote: String, timestamp: Long) = TrafficObservation.from(
        metadata = packet("10.0.0.2", remote, 40_000, 443, 80),
        direction = TrafficDirection.OUTBOUND,
        timestampEpochMillis = timestamp,
    )

    private fun packet(
        source: String,
        destination: String,
        sourcePort: Int,
        destinationPort: Int,
        bytes: Int,
    ) = PacketMetadata(
        family = IpFamily.IPV4,
        source = NetworkAddress.parseNumeric(source),
        destination = NetworkAddress.parseNumeric(destination),
        protocol = TrafficProtocol.TCP,
        ipProtocolNumber = 6,
        sourcePort = sourcePort,
        destinationPort = destinationPort,
        packetLengthBytes = bytes,
        nonInitialFragment = false,
    )
}
