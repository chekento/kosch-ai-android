package cloud.kosch.aiandroid.security.network

/** Minimal storage contract so policy-state semantics remain JVM-testable and Android-storage agnostic. */
interface FirewallPolicyRawStorage {
    fun readRaw(): String?
    fun writeRaw(raw: String): Boolean
    fun clear(): Boolean
}

sealed interface FirewallPolicyLoadState {
    data object Absent : FirewallPolicyLoadState
    data class Ready(val document: FirewallPolicyDocument) : FirewallPolicyLoadState
    data class FutureSchema(val schemaVersion: Int, val rawByteCount: Int) : FirewallPolicyLoadState
    data class Corrupt(val reason: String, val rawByteCount: Int) : FirewallPolicyLoadState
}

/**
 * N3 persistence boundary.
 *
 * Reading is side-effect free. Corrupt or future data remains byte-for-byte in storage until the user
 * explicitly replaces or resets it. A valid policy document is still not an activation signal: packet
 * enforcement remains blocked by the separate N2/N3 runtime gates.
 */
class FirewallPolicyRepository(
    private val storage: FirewallPolicyRawStorage,
) {
    fun load(): FirewallPolicyLoadState {
        val raw = storage.readRaw() ?: return FirewallPolicyLoadState.Absent
        return when (val decoded = FirewallPolicyCodec.decode(raw)) {
            is FirewallPolicyDecodeResult.Valid -> FirewallPolicyLoadState.Ready(decoded.document)
            is FirewallPolicyDecodeResult.FutureSchema -> FirewallPolicyLoadState.FutureSchema(
                schemaVersion = decoded.schemaVersion,
                rawByteCount = raw.toByteArray(Charsets.UTF_8).size,
            )
            is FirewallPolicyDecodeResult.Corrupt -> FirewallPolicyLoadState.Corrupt(
                reason = decoded.reason,
                rawByteCount = raw.toByteArray(Charsets.UTF_8).size,
            )
        }
    }

    /** Explicit replacement only. No caller should invoke this as a read-time recovery path. */
    fun replace(document: FirewallPolicyDocument): Boolean = storage.writeRaw(FirewallPolicyCodec.encode(document))

    /** Explicit destructive reset. UI must place confirmation in front of this operation. */
    fun reset(): Boolean = storage.clear()
}
