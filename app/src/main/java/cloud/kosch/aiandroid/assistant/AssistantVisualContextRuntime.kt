package cloud.kosch.aiandroid.assistant

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.kosch.aiandroid.model.AssistantObservationSource

/**
 * Process-only one-shot bridge between an explicitly active observation session and later vision
 * inference. It never persists frames and intentionally keeps image bytes out of Compose state.
 *
 * A request must be claimed by exactly one capture callback before it can be published. This keeps
 * continuous Screen/Camera Awareness separate from the explicit act of handing one concrete frame
 * to an inference path.
 */
object AssistantVisualContextRuntime {
    enum class Status {
        IDLE,
        REQUESTED,
        READY,
        FAILED,
    }

    data class Metadata(
        val requestId: Long,
        val source: AssistantObservationSource,
        val width: Int,
        val height: Int,
        val rotationDegrees: Int,
        val mimeType: String,
        val byteCount: Int,
        val capturedAtEpochMillis: Long,
    )

    data class Snapshot(
        val metadata: Metadata,
        val jpegBytes: ByteArray,
    )

    data class Event(
        val status: Status,
        val metadata: Metadata?,
        val failureMessage: String?,
        val generation: Long,
    )

    var status by mutableStateOf(Status.IDLE)
        private set
    var requestedSource by mutableStateOf<AssistantObservationSource?>(null)
        private set
    var metadata by mutableStateOf<Metadata?>(null)
        private set
    var failureMessage by mutableStateOf<String?>(null)
        private set
    var generation by mutableLongStateOf(0L)
        private set

    private var nextRequestId = 1L
    private var pendingRequestId: Long? = null
    private var claimedRequestId: Long? = null
    private var jpegPayload: ByteArray? = null
    private var requestCreatedAtEpochMillis = 0L
    private var eventListener: ((Event) -> Unit)? = null

    @Synchronized
    fun setEventListener(listener: ((Event) -> Unit)?) {
        eventListener = listener
    }

    @Synchronized
    fun request(source: AssistantObservationSource): Long {
        clearLocked()
        val requestId = nextRequestId++
        pendingRequestId = requestId
        requestedSource = source
        requestCreatedAtEpochMillis = System.currentTimeMillis()
        status = Status.REQUESTED
        generation += 1L
        return requestId
    }

    /** Returns the request id once. Other live frames keep flowing but cannot satisfy it twice. */
    @Synchronized
    fun claimCapture(source: AssistantObservationSource): Long? {
        expireLocked(System.currentTimeMillis())
        val requestId = pendingRequestId ?: return null
        if (status != Status.REQUESTED || requestedSource != source || claimedRequestId != null) return null
        claimedRequestId = requestId
        return requestId
    }

    @Synchronized
    fun publishJpeg(
        requestId: Long,
        source: AssistantObservationSource,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        jpegBytes: ByteArray,
    ): Boolean {
        expireLocked(System.currentTimeMillis())
        if (
            status != Status.REQUESTED ||
            pendingRequestId != requestId ||
            claimedRequestId != requestId ||
            requestedSource != source
        ) {
            return false
        }
        if (width <= 0 || height <= 0 || jpegBytes.isEmpty() || jpegBytes.size > MAX_CONTEXT_BYTES) {
            failLocked("Kontextframe konnte nicht innerhalb des sicheren Größenlimits erzeugt werden")
            return false
        }

        val capturedAt = System.currentTimeMillis()
        jpegPayload = jpegBytes
        metadata = Metadata(
            requestId = requestId,
            source = source,
            width = width,
            height = height,
            rotationDegrees = normalizeRotation(rotationDegrees),
            mimeType = MIME_JPEG,
            byteCount = jpegBytes.size,
            capturedAtEpochMillis = capturedAt,
        )
        pendingRequestId = null
        claimedRequestId = null
        requestCreatedAtEpochMillis = 0L
        status = Status.READY
        failureMessage = null
        generation += 1L
        emitLocked()
        return true
    }

    @Synchronized
    fun fail(requestId: Long, message: String) {
        if (pendingRequestId != requestId && claimedRequestId != requestId) return
        failLocked(message)
    }

    @Synchronized
    fun cancel(source: AssistantObservationSource, message: String) {
        if (status != Status.REQUESTED || requestedSource != source) return
        failLocked(message)
    }

    /**
     * Transfers ownership of the bytes to the caller and clears the process broker immediately.
     * The caller must keep the payload transient and explicitly discard it after inference/handoff.
     */
    @Synchronized
    fun consume(): Snapshot? {
        expireLocked(System.currentTimeMillis())
        val currentMetadata = metadata ?: return null
        val bytes = jpegPayload ?: return null
        if (status != Status.READY) return null
        val snapshot = Snapshot(currentMetadata, bytes)
        clearLocked()
        generation += 1L
        return snapshot
    }

    @Synchronized
    fun discard() {
        if (status == Status.IDLE && metadata == null && jpegPayload == null) return
        clearLocked()
        generation += 1L
    }

    @Synchronized
    fun resetForTest() {
        clearLocked()
        nextRequestId = 1L
        eventListener = null
        generation += 1L
    }

    private fun failLocked(message: String) {
        jpegPayload = null
        metadata = null
        pendingRequestId = null
        claimedRequestId = null
        requestCreatedAtEpochMillis = 0L
        status = Status.FAILED
        failureMessage = message.take(240)
        generation += 1L
        emitLocked()
    }

    private fun expireLocked(nowEpochMillis: Long) {
        if (
            status == Status.REQUESTED &&
            requestCreatedAtEpochMillis > 0L &&
            nowEpochMillis - requestCreatedAtEpochMillis > REQUEST_TTL_MILLIS
        ) {
            failLocked("Kontextframe-Anfrage ist abgelaufen")
            return
        }
        val capturedAt = metadata?.capturedAtEpochMillis ?: return
        if (status == Status.READY && nowEpochMillis - capturedAt > READY_TTL_MILLIS) {
            clearLocked()
            generation += 1L
        }
    }

    private fun emitLocked() {
        val listener = eventListener ?: return
        listener(
            Event(
                status = status,
                metadata = metadata,
                failureMessage = failureMessage,
                generation = generation,
            ),
        )
    }

    private fun clearLocked() {
        jpegPayload = null
        metadata = null
        pendingRequestId = null
        claimedRequestId = null
        requestedSource = null
        requestCreatedAtEpochMillis = 0L
        status = Status.IDLE
        failureMessage = null
    }

    private fun normalizeRotation(rotationDegrees: Int): Int =
        (((rotationDegrees % 360) + 360) % 360 / 90) * 90

    const val MAX_CONTEXT_BYTES = 512 * 1024
    private const val REQUEST_TTL_MILLIS = 10_000L
    private const val READY_TTL_MILLIS = 30_000L
    private const val MIME_JPEG = "image/jpeg"
}
