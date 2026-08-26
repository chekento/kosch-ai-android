package cloud.kosch.aiandroid.security.network

import java.util.ArrayDeque
import java.util.LinkedHashMap

enum class TrafficDirection { OUTBOUND, INBOUND }

data class FlowAttribution(
    val ownerUid: Int? = null,
    val packageName: String? = null,
) {
    init {
        require(ownerUid == null || ownerUid >= 0) { "Flow owner UID is invalid" }
        require(packageName == null || (packageName.isNotBlank() && packageName.length <= 255)) {
            "Flow package attribution is invalid"
        }
        require(packageName == null || ownerUid != null) {
            "Package attribution requires a known UID"
        }
    }

    companion object {
        val UNKNOWN = FlowAttribution()
    }
}

data class FlowKey(
    val localAddress: NetworkAddress,
    val remoteAddress: NetworkAddress,
    val protocol: TrafficProtocol,
    val localPort: Int?,
    val remotePort: Int?,
    val ownerUid: Int?,
) {
    init {
        require(localAddress.family == remoteAddress.family) { "Flow address families differ" }
        require(localPort == null || localPort in 0..65535) { "Local port is invalid" }
        require(remotePort == null || remotePort in 0..65535) { "Remote port is invalid" }
        require(ownerUid == null || ownerUid >= 0) { "Flow owner UID is invalid" }
    }

    companion object {
        fun from(
            metadata: PacketMetadata,
            direction: TrafficDirection,
            attribution: FlowAttribution = FlowAttribution.UNKNOWN,
        ): FlowKey = when (direction) {
            TrafficDirection.OUTBOUND -> FlowKey(
                localAddress = metadata.source,
                remoteAddress = metadata.destination,
                protocol = metadata.protocol,
                localPort = metadata.sourcePort,
                remotePort = metadata.destinationPort,
                ownerUid = attribution.ownerUid,
            )

            TrafficDirection.INBOUND -> FlowKey(
                localAddress = metadata.destination,
                remoteAddress = metadata.source,
                protocol = metadata.protocol,
                localPort = metadata.destinationPort,
                remotePort = metadata.sourcePort,
                ownerUid = attribution.ownerUid,
            )
        }
    }
}

data class FlowSnapshot(
    val key: FlowKey,
    val firstSeenEpochMillis: Long,
    val lastSeenEpochMillis: Long,
    val outboundBytes: Long,
    val inboundBytes: Long,
    val outboundPackets: Long,
    val inboundPackets: Long,
    val packageName: String?,
    val packageAttributionAmbiguous: Boolean,
) {
    val totalBytes: Long get() = saturatedSum(outboundBytes, inboundBytes)
    val totalPackets: Long get() = saturatedSum(outboundPackets, inboundPackets)
}

data class TrafficObservation(
    val timestampEpochMillis: Long,
    val direction: TrafficDirection,
    val key: FlowKey,
    val packetLengthBytes: Int,
    val packageName: String?,
) {
    init {
        require(timestampEpochMillis >= 0) { "Traffic timestamp is invalid" }
        require(packetLengthBytes > 0) { "Traffic packet length is invalid" }
        require(packageName == null || (packageName.isNotBlank() && packageName.length <= 255)) {
            "Traffic package attribution is invalid"
        }
        require(packageName == null || key.ownerUid != null) {
            "Traffic package attribution requires a known UID"
        }
    }

    companion object {
        fun from(
            metadata: PacketMetadata,
            direction: TrafficDirection,
            timestampEpochMillis: Long,
            attribution: FlowAttribution = FlowAttribution.UNKNOWN,
        ) = TrafficObservation(
            timestampEpochMillis = timestampEpochMillis,
            direction = direction,
            key = FlowKey.from(metadata, direction, attribution),
            packetLengthBytes = metadata.packetLengthBytes,
            packageName = attribution.packageName,
        )
    }
}

/**
 * Session-local bounded flow table.
 *
 * Owner identity is part of the flow key. Unknown ownership is never merged into a later known UID,
 * and conflicting package names for a shared UID are surfaced as ambiguous instead of guessed.
 */
class BoundedFlowTable(private val capacity: Int = 2_048) {
    init {
        require(capacity in 1..20_000) { "Flow-table capacity is out of range" }
    }

    private data class MutableFlow(
        val key: FlowKey,
        var firstSeenEpochMillis: Long,
        var lastSeenEpochMillis: Long,
        var outboundBytes: Long = 0,
        var inboundBytes: Long = 0,
        var outboundPackets: Long = 0,
        var inboundPackets: Long = 0,
        var packageName: String? = null,
        var packageAttributionAmbiguous: Boolean = false,
    )

    private val flows = LinkedHashMap<FlowKey, MutableFlow>(capacity.coerceAtMost(256), 0.75f, true)

    @Synchronized
    fun record(observation: TrafficObservation): FlowSnapshot {
        val flow = flows[observation.key] ?: MutableFlow(
            key = observation.key,
            firstSeenEpochMillis = observation.timestampEpochMillis,
            lastSeenEpochMillis = observation.timestampEpochMillis,
        ).also { flows[observation.key] = it }

        flow.firstSeenEpochMillis = minOf(flow.firstSeenEpochMillis, observation.timestampEpochMillis)
        flow.lastSeenEpochMillis = maxOf(flow.lastSeenEpochMillis, observation.timestampEpochMillis)
        when (observation.direction) {
            TrafficDirection.OUTBOUND -> {
                flow.outboundBytes = saturatingAdd(flow.outboundBytes, observation.packetLengthBytes.toLong())
                flow.outboundPackets = saturatingAdd(flow.outboundPackets, 1)
            }

            TrafficDirection.INBOUND -> {
                flow.inboundBytes = saturatingAdd(flow.inboundBytes, observation.packetLengthBytes.toLong())
                flow.inboundPackets = saturatingAdd(flow.inboundPackets, 1)
            }
        }
        updatePackageAttribution(flow, observation.packageName)
        trimToCapacity()
        return flow.snapshot()
    }

    @Synchronized
    fun snapshot(): List<FlowSnapshot> = flows.values
        .map(MutableFlow::snapshot)
        .sortedByDescending(FlowSnapshot::lastSeenEpochMillis)

    @Synchronized
    fun clear() = flows.clear()

    @Synchronized
    fun size(): Int = flows.size

    private fun updatePackageAttribution(flow: MutableFlow, candidate: String?) {
        if (candidate == null || flow.packageAttributionAmbiguous) return
        when (val current = flow.packageName) {
            null -> flow.packageName = candidate
            candidate -> Unit
            else -> {
                flow.packageName = null
                flow.packageAttributionAmbiguous = true
            }
        }
    }

    private fun trimToCapacity() {
        while (flows.size > capacity) {
            val iterator = flows.entries.iterator()
            if (!iterator.hasNext()) return
            iterator.next()
            iterator.remove()
        }
    }

    private fun MutableFlow.snapshot() = FlowSnapshot(
        key = key,
        firstSeenEpochMillis = firstSeenEpochMillis,
        lastSeenEpochMillis = lastSeenEpochMillis,
        outboundBytes = outboundBytes,
        inboundBytes = inboundBytes,
        outboundPackets = outboundPackets,
        inboundPackets = inboundPackets,
        packageName = packageName,
        packageAttributionAmbiguous = packageAttributionAmbiguous,
    )
}

/** Metadata-only ring buffer with a hard record cap. No packet payload or DNS name is retained. */
class BoundedTrafficLedger(private val capacity: Int = 2_000) {
    init {
        require(capacity in 1..20_000) { "Traffic-ledger capacity is out of range" }
    }

    private val events = ArrayDeque<TrafficObservation>(capacity.coerceAtMost(256))

    @Synchronized
    fun record(observation: TrafficObservation) {
        while (events.size >= capacity) events.removeFirst()
        events.addLast(observation)
    }

    @Synchronized
    fun snapshot(): List<TrafficObservation> = events.toList()

    @Synchronized
    fun clear() = events.clear()

    @Synchronized
    fun size(): Int = events.size
}

private fun saturatingAdd(left: Long, right: Long): Long {
    require(left >= 0 && right >= 0)
    return if (Long.MAX_VALUE - left < right) Long.MAX_VALUE else left + right
}

private fun saturatedSum(left: Long, right: Long): Long =
    if (Long.MAX_VALUE - left < right) Long.MAX_VALUE else left + right
