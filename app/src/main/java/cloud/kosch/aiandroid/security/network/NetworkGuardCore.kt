package cloud.kosch.aiandroid.security.network

import java.net.InetAddress

/** Pure, local-only packet metadata model. Payload bytes are deliberately never retained. */
enum class IpFamily { IPV4, IPV6 }

enum class TrafficProtocol {
    TCP,
    UDP,
    ICMP,
    ICMPV6,
    OTHER,
}

data class NetworkAddress private constructor(
    val family: IpFamily,
    private val octets: List<Int>,
) {
    init {
        require(octets.size == if (family == IpFamily.IPV4) 4 else 16)
        require(octets.all { it in 0..255 })
    }

    fun asByteArray(): ByteArray = ByteArray(octets.size) { octets[it].toByte() }

    override fun toString(): String = InetAddress.getByAddress(asByteArray()).hostAddress.orEmpty()

    companion object {
        fun fromBytes(bytes: ByteArray): NetworkAddress {
            val family = when (bytes.size) {
                4 -> IpFamily.IPV4
                16 -> IpFamily.IPV6
                else -> throw IllegalArgumentException("IP address must be 4 or 16 bytes")
            }
            return NetworkAddress(family, bytes.map { it.toInt() and 0xff })
        }

        fun parseNumeric(value: String): NetworkAddress {
            val trimmed = value.trim()
            require(trimmed.isNotEmpty()) { "IP address is empty" }
            require(trimmed.all { it.isDigit() || it == '.' || it == ':' || it.lowercaseChar() in 'a'..'f' }) {
                "Only numeric IPv4/IPv6 addresses are accepted"
            }
            return fromBytes(InetAddress.getByName(trimmed).address)
        }
    }
}

data class CidrBlock(
    val network: NetworkAddress,
    val prefixLength: Int,
) {
    init {
        val max = if (network.family == IpFamily.IPV4) 32 else 128
        require(prefixLength in 0..max) { "CIDR prefix is out of range" }
    }

    fun contains(address: NetworkAddress): Boolean {
        if (address.family != network.family) return false
        val left = network.asByteArray()
        val right = address.asByteArray()
        val wholeBytes = prefixLength / 8
        val remainingBits = prefixLength % 8
        for (index in 0 until wholeBytes) {
            if (left[index] != right[index]) return false
        }
        if (remainingBits == 0) return true
        val mask = (0xff shl (8 - remainingBits)) and 0xff
        return (left[wholeBytes].toInt() and mask) == (right[wholeBytes].toInt() and mask)
    }

    override fun toString(): String = "$network/$prefixLength"

    companion object {
        fun parse(value: String): CidrBlock {
            val parts = value.trim().split('/')
            require(parts.size == 2) { "CIDR must contain one prefix separator" }
            val address = NetworkAddress.parseNumeric(parts[0])
            val prefix = parts[1].toIntOrNull() ?: throw IllegalArgumentException("CIDR prefix is invalid")
            return CidrBlock(address, prefix)
        }
    }
}

data class PortRange(val first: Int, val last: Int = first) {
    init {
        require(first in 0..65535 && last in 0..65535 && first <= last) { "Port range is invalid" }
    }

    operator fun contains(port: Int): Boolean = port in first..last
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
 * Bounds-checked IPv4/IPv6 metadata parser for the future VpnService TUN reader.
 * It never exposes or stores packet payload contents.
 */
object PacketMetadataParser {
    private const val IPV4_MIN_HEADER = 20
    private const val IPV6_HEADER = 40
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
        if (packet.size < IPV4_MIN_HEADER) return PacketParseOutcome.Malformed("truncated IPv4 header")
        val ihl = (packet[0].toInt() and 0x0f) * 4
        if (ihl < IPV4_MIN_HEADER || ihl > packet.size) return PacketParseOutcome.Malformed("invalid IPv4 IHL")
        val totalLength = u16(packet, 2)
        if (totalLength < ihl || totalLength > packet.size) return PacketParseOutcome.Malformed("invalid IPv4 length")

        val fragmentField = u16(packet, 6)
        val fragmentOffset = fragmentField and 0x1fff
        val nonInitialFragment = fragmentOffset != 0
        val protocolNumber = u8(packet, 9)
        val source = NetworkAddress.fromBytes(packet.copyOfRange(12, 16))
        val destination = NetworkAddress.fromBytes(packet.copyOfRange(16, 20))
        val protocol = protocolFor(protocolNumber, ipv6 = false)
        val ports = if (nonInitialFragment) null else readPorts(packet, ihl, totalLength, protocol)

        if ((protocol == TrafficProtocol.TCP || protocol == TrafficProtocol.UDP) && !nonInitialFragment && ports == null) {
            return PacketParseOutcome.Malformed("truncated transport header")
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
        if (packet.size < IPV6_HEADER) return PacketParseOutcome.Malformed("truncated IPv6 header")
        val payloadLength = u16(packet, 4)
        val totalLength = if (payloadLength == 0) packet.size else IPV6_HEADER + payloadLength
        if (totalLength > packet.size) return PacketParseOutcome.Malformed("invalid IPv6 length")

        val source = NetworkAddress.fromBytes(packet.copyOfRange(8, 24))
        val destination = NetworkAddress.fromBytes(packet.copyOfRange(24, 40))
        var nextHeader = u8(packet, 6)
        var offset = IPV6_HEADER
        var nonInitialFragment = false
        var extensionCount = 0

        while (nextHeader in setOf(0, 43, 44, 51, 60)) {
            extensionCount += 1
            if (extensionCount > MAX_EXTENSION_HEADERS) {
                return PacketParseOutcome.Unsupported("too many IPv6 extension headers")
            }
            when (nextHeader) {
                0, 43, 60 -> {
                    if (offset + 2 > totalLength) return PacketParseOutcome.Malformed("truncated IPv6 extension header")
                    val following = u8(packet, offset)
                    val length = (u8(packet, offset + 1) + 1) * 8
                    if (length < 8 || offset + length > totalLength) {
                        return PacketParseOutcome.Malformed("invalid IPv6 extension length")
                    }
                    nextHeader = following
                    offset += length
                }

                44 -> {
                    if (offset + 8 > totalLength) return PacketParseOutcome.Malformed("truncated IPv6 fragment header")
                    val following = u8(packet, offset)
                    val fragmentField = u16(packet, offset + 2)
                    nonInitialFragment = ((fragmentField ushr 3) and 0x1fff) != 0
                    nextHeader = following
                    offset += 8
                }

                51 -> {
                    if (offset + 2 > totalLength) return PacketParseOutcome.Malformed("truncated IPv6 AH header")
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
        val ports = if (nonInitialFragment) null else readPorts(packet, offset, totalLength, protocol)
        if ((protocol == TrafficProtocol.TCP || protocol == TrafficProtocol.UDP) && !nonInitialFragment && ports == null) {
            return PacketParseOutcome.Malformed("truncated transport header")
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

    private fun readPorts(
        packet: ByteArray,
        offset: Int,
        totalLength: Int,
        protocol: TrafficProtocol,
    ): Pair<Int, Int>? {
        if (protocol != TrafficProtocol.TCP && protocol != TrafficProtocol.UDP) return null
        if (offset < 0 || offset + 4 > totalLength || offset + 4 > packet.size) return null
        return u16(packet, offset) to u16(packet, offset + 2)
    }

    private fun protocolFor(number: Int, ipv6: Boolean): TrafficProtocol = when (number) {
        6 -> TrafficProtocol.TCP
        17 -> TrafficProtocol.UDP
        1 -> if (ipv6) TrafficProtocol.OTHER else TrafficProtocol.ICMP
        58 -> if (ipv6) TrafficProtocol.ICMPV6 else TrafficProtocol.OTHER
        else -> TrafficProtocol.OTHER
    }

    private fun u8(bytes: ByteArray, offset: Int): Int = bytes[offset].toInt() and 0xff
    private fun u16(bytes: ByteArray, offset: Int): Int = (u8(bytes, offset) shl 8) or u8(bytes, offset + 1)
}

enum class FirewallVerdict { ALLOW, BLOCK }

data class NetworkFlow(
    val destination: NetworkAddress,
    val protocol: TrafficProtocol,
    val destinationPort: Int? = null,
    val ownerUid: Int? = null,
    val packageName: String? = null,
)

data class FirewallRule(
    val id: String,
    val priority: Int,
    val verdict: FirewallVerdict,
    val enabled: Boolean = true,
    val packageName: String? = null,
    val ownerUid: Int? = null,
    val destination: CidrBlock? = null,
    val protocol: TrafficProtocol? = null,
    val destinationPorts: PortRange? = null,
) {
    init {
        require(id.isNotBlank() && id.length <= 160) { "Firewall rule id is invalid" }
        require(priority in 0..1_000_000) { "Firewall rule priority is out of range" }
        require(packageName == null || (packageName.isNotBlank() && packageName.length <= 255)) {
            "Firewall package filter is invalid"
        }
        require(ownerUid == null || ownerUid >= 0) { "Firewall UID filter is invalid" }
    }

    fun matches(flow: NetworkFlow): Boolean {
        if (!enabled) return false
        if (packageName != null && packageName != flow.packageName) return false
        if (ownerUid != null && ownerUid != flow.ownerUid) return false
        if (destination != null && !destination.contains(flow.destination)) return false
        if (protocol != null && protocol != flow.protocol) return false
        if (destinationPorts != null) {
            val port = flow.destinationPort ?: return false
            if (port !in destinationPorts) return false
        }
        return true
    }
}

data class FirewallDecision(
    val verdict: FirewallVerdict,
    val matchedRuleId: String?,
)

object FirewallRuleEngine {
    const val MAX_RULES = 4_096

    fun evaluate(
        flow: NetworkFlow,
        rules: List<FirewallRule>,
        defaultVerdict: FirewallVerdict = FirewallVerdict.ALLOW,
    ): FirewallDecision {
        require(rules.size <= MAX_RULES) { "Too many firewall rules" }
        val matched = rules.asSequence()
            .filter(FirewallRule::enabled)
            .sortedWith(compareBy<FirewallRule> { it.priority }.thenBy { it.id })
            .firstOrNull { it.matches(flow) }
        return FirewallDecision(matched?.verdict ?: defaultVerdict, matched?.id)
    }
}

data class TrafficEvent(
    val timestampEpochMillis: Long,
    val flow: NetworkFlow,
    val packetLengthBytes: Int,
    val verdict: FirewallVerdict,
) {
    init {
        require(timestampEpochMillis > 0)
        require(packetLengthBytes >= 0)
    }
}

/** Small in-memory ring buffer. It stores metadata only and applies hard memory bounds. */
class BoundedTrafficLedger(private val capacity: Int = 2_000) {
    init {
        require(capacity in 1..20_000) { "Traffic ledger capacity is out of range" }
    }

    private val events = ArrayDeque<TrafficEvent>(capacity.coerceAtMost(256))

    @Synchronized
    fun record(event: TrafficEvent) {
        while (events.size >= capacity) events.removeFirst()
        events.addLast(event)
    }

    @Synchronized
    fun snapshot(): List<TrafficEvent> = events.toList()

    @Synchronized
    fun clear() = events.clear()
}
