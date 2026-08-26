package cloud.kosch.aiandroid.security.network

/**
 * Platform-neutral seam for the future userspace packet forwarder.
 *
 * Android/TUN descriptors and socket objects intentionally do not cross this contract yet. The Android
 * adapter will own those resources in the later activation slice and must translate them into an
 * implementation-specific session only after the activation gate is Ready.
 */
interface N2PacketForwarder {
    val implementationId: String

    /** Returns null until review/test evidence for this exact implementation is available. */
    fun readinessEvidence(): ForwarderReadinessEvidence?

    /** Starts one generation. Implementations must reject duplicate or stale generations. */
    fun start(request: N2ForwarderStartRequest): N2ForwarderStartResult

    /** Must be idempotent; cleanup is required even after partial start failures. */
    fun stop(reason: N2StopReason): N2ForwarderStopResult
}

data class N2ForwarderStartRequest(
    val activationGeneration: Long,
    val mtu: Int,
    val ipv4Enabled: Boolean = true,
    val ipv6Enabled: Boolean = true,
) {
    init {
        require(activationGeneration > 0) { "Forwarder generation must be positive" }
        require(mtu in 576..9_000) { "Forwarder MTU is outside the supported safety range" }
        require(ipv4Enabled || ipv6Enabled) { "At least one IP family must be enabled" }
    }
}

sealed interface N2ForwarderStartResult {
    data class Started(val activationGeneration: Long) : N2ForwarderStartResult {
        init {
            require(activationGeneration > 0) { "Started generation must be positive" }
        }
    }

    data class Rejected(val reason: N2ForwarderRejectReason) : N2ForwarderStartResult
    data class Failed(val reason: String) : N2ForwarderStartResult {
        init {
            require(reason.isNotBlank() && reason.length <= 256) { "Forwarder failure reason is invalid" }
        }
    }
}

enum class N2ForwarderRejectReason {
    NOT_READY,
    STALE_GENERATION,
    ALREADY_RUNNING,
    UNSUPPORTED_CONFIGURATION,
}

sealed interface N2ForwarderStopResult {
    data object Stopped : N2ForwarderStopResult
    data class Failed(val reason: String) : N2ForwarderStopResult {
        init {
            require(reason.isNotBlank() && reason.length <= 256) { "Forwarder stop failure reason is invalid" }
        }
    }
}
