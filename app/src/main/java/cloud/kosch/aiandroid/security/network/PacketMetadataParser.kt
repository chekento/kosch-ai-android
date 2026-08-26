package cloud.kosch.aiandroid.security.network

enum class TrafficProtocol {
    TCP,
    UDP,
    ICMP,
    ICMPV6,
    OTHER,
}

data class PacketMetadata(
    val family: IpFamily,
    val source: NetworkAddress,
    val destination: NetworkAddress,
    val protocol: TrafficProtocol,
    val ipProtocolNumber: Int,
    val sourcePort: Int?,
    val destinationPort: Int?,
    val packetLengthBytes: Int,
    val nonInitialFragment: Boolean,
)

sealed interface PacketParseOutcome {
    data class Parsed(val metadata: PacketMetadata) : PacketParseOutcome
    data class Malformed(val reason: String) : PacketParseOutcome
    data class Unsupported(val reason: String) : PacketParseOutcome
}

/**
 * Bounds-checked metadata parser for future TUN packets.
 *
 * It extracts only IP/transport metadata required for N2 telemetry. Packet payload bytes are never
 * exposed through the result and no hostname/DNS inference is attempted.
 */
object PacketMetadataParser {
    private const val IPV4_MIN_HEADER = 20
    private const val IPV6_HEADER = 40
    private const val TCP_MIN_HEADER = 20
    private const val UDP_HEADER = 8
    private const val MAX_EXTENSION_HEADERS = 8

    fun parse(packet: ByteArray): PacketParseOutcome {
        if (packet.isEmpty()) return PacketParseOutcome.Malformed("empty packet")
        return when ((packet[0].toInt() ushr 4) and 0x0f) {
            4 -> parseIpv4(packet)
            6 -> parseIpv6(packet)
            else -> PacketParseOutcome.Unsupported("unsupported IP version")
        }
    }

    private fun parseIpv4(packet: ByteArray): PacketParseOutcome {
        if (packet.size < IPV4_MIN_HEADER) {
            return PacketParseOutcome.Malformed("truncated IPv4 header")
        }

        val ihl = (u8(packet, 0) and 0x0f) * 4
        if (ihl < IPV4_MIN_HEADER || ihl > packet.size) {
            return PacketParseOutcome.Malformed("invalid IPv4 IHL")
        }
        val totalLength = u16(packet, 2)
        if (totalLength < ihl || totalLength > packet.size) {
            return PacketParseOutcome.Malformed("invalid IPv4 length")
        }

        val fragmentField = u16(packet, 6)
        val nonInitialFragment = (fragmentField and 0x1fff) != 0
        val protocolNumber = u8(packet, 9)
        val protocol = protocolFor(protocolNumber, ipv6 = false)
        val source = NetworkAddress.fromBytes(packet.copyOfRange(12, 16))
        val destination = NetworkAddress.fromBytes(packet.copyOfRange(16, 20))
        val ports = if (nonInitialFragment) {
            null
        } else {
            readValidatedPorts(packet, ihl, totalLength, protocol)
        }

        if (protocol.requiresPorts() && !nonInitialFragment && ports == null) {
            return PacketParseOutcome.Malformed("invalid or truncated transport header")
        }

        return PacketParseOutcome.Parsed(
            PacketMetadata(
                family = IpFamily.IPV4,
                source = source,
                destination = destination,
                protocol = protocol,
                ipProtocolNumber = protocolNumber,
                sourcePort = ports?.first,
                destinationPort = ports?.second,
                packetLengthBytes = totalLength,
                nonInitialFragment = nonInitialFragment,
            ),
        )
    }

    private fun parseIpv6(packet: ByteArray): PacketParseOutcome {
        if (packet.size < IPV6_HEADER) {
            return PacketParseOutcome.Malformed("truncated IPv6 header")
        }

        val payloadLength = u16(packet, 4)
        val totalLength = when {
            payloadLength > 0 -> IPV6_HEADER + payloadLength
            packet.size == IPV6_HEADER -> IPV6_HEADER
            else -> return PacketParseOutcome.Unsupported("IPv6 jumbograms are not supported")
        }
        if (totalLength > packet.size) {
            return PacketParseOutcome.Malformed("invalid IPv6 length")
        }

        val source = NetworkAddress.fromBytes(packet.copyOfRange(8, 24))
        val destination = NetworkAddress.fromBytes(packet.copyOfRange(24, 40))
        var nextHeader = u8(packet, 6)
        var offset = IPV6_HEADER
        var nonInitialFragment = false
        var extensionCount = 0

        while (nextHeader in EXTENSION_HEADERS) {
            extensionCount += 1
            if (extensionCount > MAX_EXTENSION_HEADERS) {
                return PacketParseOutcome.Unsupported("too many IPv6 extension headers")
            }

            when (nextHeader) {
                0, 43, 60 -> {
                    if (offset + 2 > totalLength) {
                        return PacketParseOutcome.Malformed("truncated IPv6 extension header")
                    }
                    val following = u8(packet, offset)
                    val length = (u8(packet, offset + 1) + 1) * 8
                    if (length < 8 || offset + length > totalLength) {
                        return PacketParseOutcome.Malformed("invalid IPv6 extension length")
                    }
                    nextHeader = following
                    offset += length
                }

                44 -> {
                    if (offset + 8 > totalLength) {
                        return PacketParseOutcome.Malformed("truncated IPv6 fragment header")
                    }
                    val following = u8(packet, offset)
                    val fragmentField = u16(packet, offset + 2)
                    nonInitialFragment = ((fragmentField ushr 3) and 0x1fff) != 0
                    nextHeader = following
                    offset += 8
                }

                51 -> {
                    if (offset + 2 > totalLength) {
                        return PacketParseOutcome.Malformed("truncated IPv6 AH header")
                    }
                    val following = u8(packet, offset)
                    val length = (u8(packet, offset + 1) + 2) * 4
                    if (length < 8 || offset + length > totalLength) {
                        return PacketParseOutcome.Malformed("invalid IPv6 AH length")
                    }
                    nextHeader = following
                    offset += length
                }
            }
        }

        val protocol = protocolFor(nextHeader, ipv6 = true)
        val ports = if (nonInitialFragment) {
            null
        } else {
            readValidatedPorts(packet, offset, totalLength, protocol)
        }
        if (protocol.requiresPorts() && !nonInitialFragment && ports == null) {
            return PacketParseOutcome.Malformed("invalid or truncated transport header")
        }

        return PacketParseOutcome.Parsed(
            PacketMetadata(
                family = IpFamily.IPV6,
                source = source,
                destination = destination,
                protocol = protocol,
                ipProtocolNumber = nextHeader,
                sourcePort = ports?.first,
                destinationPort = ports?.second,
                packetLengthBytes = totalLength,
                nonInitialFragment = nonInitialFragment,
            ),
        )
    }

    private fun readValidatedPorts(
        packet: ByteArray,
        offset: Int,
        totalLength: Int,
        protocol: TrafficProtocol,
    ): Pair<Int, Int>? {
        if (offset < 0 || offset > totalLength || totalLength > packet.size) return null
        val available = totalLength - offset
        return when (protocol) {
            TrafficProtocol.TCP -> {
                if (available < TCP_MIN_HEADER || offset + TCP_MIN_HEADER > packet.size) return null
                val dataOffsetBytes = ((u8(packet, offset + 12) ushr 4) and 0x0f) * 4
                if (dataOffsetBytes < TCP_MIN_HEADER || dataOffsetBytes > available) return null
                u16(packet, offset) to u16(packet, offset + 2)
            }

            TrafficProtocol.UDP -> {
                if (available < UDP_HEADER || offset + UDP_HEADER > packet.size) return null
                val udpLength = u16(packet, offset + 4)
                if (udpLength < UDP_HEADER || udpLength > available) return null
                u16(packet, offset) to u16(packet, offset + 2)
            }

            else -> null
        }
    }

    private fun TrafficProtocol.requiresPorts(): Boolean =
        this == TrafficProtocol.TCP || this == TrafficProtocol.UDP

    private fun protocolFor(number: Int, ipv6: Boolean): TrafficProtocol = when (number) {
        6 -> TrafficProtocol.TCP
        17 -> TrafficProtocol.UDP
        1 -> if (ipv6) TrafficProtocol.OTHER else TrafficProtocol.ICMP
        58 -> if (ipv6) TrafficProtocol.ICMPV6 else TrafficProtocol.OTHER
        else -> TrafficProtocol.OTHER
    }

    private fun u8(bytes: ByteArray, offset: Int): Int = bytes[offset].toInt() and 0xff

    private fun u16(bytes: ByteArray, offset: Int): Int =
        (u8(bytes, offset) shl 8) or u8(bytes, offset + 1)

    private val EXTENSION_HEADERS = setOf(0, 43, 44, 51, 60)
}
