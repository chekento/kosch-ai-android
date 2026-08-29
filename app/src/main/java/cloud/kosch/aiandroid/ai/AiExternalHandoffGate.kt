package cloud.kosch.aiandroid.ai

/** Exact process-local payload identity used by the two-step external text handoff gate. */
data class AiExternalHandoffCandidate(
    val stableTargetId: String,
    val packageName: String,
    val prompt: String,
) {
    init {
        require(stableTargetId.isNotBlank())
        require(packageName.isNotBlank())
        require(prompt.isNotBlank())
        require(prompt.length <= MAX_PROMPT_CHARS)
    }

    private companion object {
        const val MAX_PROMPT_CHARS = 32_000
    }
}

enum class AiExternalHandoffDecision {
    STAGED,
    CONFIRMED,
}

/**
 * Fail-closed, memory-only confirmation gate for text leaving KoSch through an Android Share handoff.
 *
 * The first evaluation only stages the exact target + package + prompt tuple. Only an immediately repeated,
 * byte-for-byte identical candidate confirms. Any prompt or destination change replaces the pending tuple and therefore
 * requires another explicit user gesture. `clear()` is used whenever the Hub closes, the prompt changes or another
 * action is selected.
 */
class AiExternalHandoffGate {
    private var pending: AiExternalHandoffCandidate? = null

    fun evaluate(candidate: AiExternalHandoffCandidate): AiExternalHandoffDecision {
        if (pending == candidate) {
            pending = null
            return AiExternalHandoffDecision.CONFIRMED
        }
        pending = candidate
        return AiExternalHandoffDecision.STAGED
    }

    fun clear() {
        pending = null
    }

    internal fun hasPending(): Boolean = pending != null
}
