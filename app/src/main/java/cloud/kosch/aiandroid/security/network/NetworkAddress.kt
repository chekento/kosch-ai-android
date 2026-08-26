package cloud.kosch.aiandroid.security.network

import java.net.InetAddress

enum class IpFamily { IPV4, IPV6 }

/** Immutable numeric IP address. Parsing never performs DNS resolution. */
class NetworkAddress private constructor(
    val family: IpFamily,
    bytes: ByteArray,
) {
    private val octets = bytes.copyOf()

    init {
        require(octets.size == if (family == IpFamily.IPV4) 4 else 16)
    }

    fun asByteArray(): ByteArray = octets.copyOf()

    override fun equals(other: Any?): Boolean =
        other is NetworkAddress && family == other.family && octets.contentEquals(other.octets)

    override fun hashCode(): Int = 31 * family.hashCode() + octets.contentHashCode()

    override fun toString(): String = InetAddress.getByAddress(octets).hostAddress.orEmpty()

    companion object {
        fun fromBytes(bytes: ByteArray): NetworkAddress {
            val family = when (bytes.size) {
                4 -> IpFamily.IPV4
                16 -> IpFamily.IPV6
                else -> throw IllegalArgumentException("IP address must be 4 or 16 bytes")
            }
            return NetworkAddress(family, bytes)
        }

        fun parseNumeric(value: String): NetworkAddress {
            val trimmed = value.trim()
            require(trimmed.isNotEmpty()) { "IP address is empty" }
            require('%' !in trimmed) { "IPv6 zone identifiers are not supported" }
            return if (':' in trimmed) {
                fromBytes(parseIpv6Bytes(trimmed))
            } else {
                fromBytes(parseIpv4Bytes(trimmed))
            }
        }

        private fun parseIpv4Bytes(value: String): ByteArray {
            val parts = value.split('.')
            require(parts.size == 4) { "IPv4 address must contain four octets" }
            return ByteArray(4) { index ->
                val part = parts[index]
                require(part.isNotEmpty() && part.length <= 3 && part.all(Char::isDigit)) {
                    "IPv4 octet is invalid"
                }
                val number = part.toIntOrNull()
                    ?: throw IllegalArgumentException("IPv4 octet is invalid")
                require(number in 0..255) { "IPv4 octet is out of range" }
                number.toByte()
            }
        }

        private fun parseIpv6Bytes(rawValue: String): ByteArray {
            require(rawValue.isNotEmpty() && ':' in rawValue) { "IPv6 address is invalid" }
            require(!rawValue.contains(":::")) { "IPv6 compression is invalid" }

            val value = normalizeEmbeddedIpv4(rawValue)
            val compressionIndex = value.indexOf("::")
            if (compressionIndex >= 0) {
                require(value.indexOf("::", compressionIndex + 2) < 0) {
                    "IPv6 address contains multiple compression markers"
                }
            }

            val groups = if (compressionIndex >= 0) {
                val left = parseIpv6Groups(value.substring(0, compressionIndex))
                val right = parseIpv6Groups(value.substring(compressionIndex + 2))
                require(left.size + right.size < 8) { "IPv6 compression must replace at least one group" }
                left + List(8 - left.size - right.size) { 0 } + right
            } else {
                parseIpv6Groups(value).also {
                    require(it.size == 8) { "IPv6 address must contain eight groups without compression" }
                }
            }

            require(groups.size == 8) { "IPv6 address has an invalid group count" }
            return ByteArray(16).also { bytes ->
                groups.forEachIndexed { index, group ->
                    bytes[index * 2] = ((group ushr 8) and 0xff).toByte()
                    bytes[index * 2 + 1] = (group and 0xff).toByte()
                }
            }
        }

        private fun normalizeEmbeddedIpv4(value: String): String {
            if ('.' !in value) return value
            val lastColon = value.lastIndexOf(':')
            require(lastColon >= 0 && lastColon < value.lastIndex) { "Embedded IPv4 address is invalid" }
            val ipv4 = parseIpv4Bytes(value.substring(lastColon + 1))
            val high = ((ipv4[0].toInt() and 0xff) shl 8) or (ipv4[1].toInt() and 0xff)
            val low = ((ipv4[2].toInt() and 0xff) shl 8) or (ipv4[3].toInt() and 0xff)
            return buildString {
                append(value.substring(0, lastColon + 1))
                append(high.toString(16))
                append(':')
                append(low.toString(16))
            }
        }

        private fun parseIpv6Groups(value: String): List<Int> {
            if (value.isEmpty()) return emptyList()
            return value.split(':').map { group ->
                require(group.length in 1..4 && group.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                    "IPv6 group is invalid"
                }
                group.toInt(16)
            }
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
