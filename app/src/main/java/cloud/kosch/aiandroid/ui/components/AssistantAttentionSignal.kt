package cloud.kosch.aiandroid.ui.components

/**
 * Ephemeral, permission-free attention cue produced only by direct interaction with the avatar.
 * Coordinates are normalized around the avatar center (`-1..1`) and are never persisted.
 */
data class AssistantAttentionSignal(
    val targetX: Float = 0f,
    val targetY: Float = 0f,
    val pressed: Boolean = false,
    val updatedAtUptimeMillis: Long? = null,
    val activatedAtUptimeMillis: Long? = null,
) {
    fun pointer(
        normalizedX: Float,
        normalizedY: Float,
        isPressed: Boolean,
        nowUptimeMillis: Long,
    ): AssistantAttentionSignal = copy(
        targetX = normalizedX.finiteNormalized(),
        targetY = normalizedY.finiteNormalized(),
        pressed = isPressed,
        updatedAtUptimeMillis = nowUptimeMillis.coerceAtLeast(0L),
    )

    fun activate(nowUptimeMillis: Long): AssistantAttentionSignal = copy(
        pressed = false,
        updatedAtUptimeMillis = nowUptimeMillis.coerceAtLeast(0L),
        activatedAtUptimeMillis = nowUptimeMillis.coerceAtLeast(0L),
    )

    fun trackingWeight(nowUptimeMillis: Long): Float {
        val updatedAt = updatedAtUptimeMillis ?: return 0f
        if (pressed) return 1f
        val progress = ((nowUptimeMillis - updatedAt).coerceAtLeast(0L).toFloat() / GAZE_LINGER_MILLIS)
            .coerceIn(0f, 1f)
        val remaining = 1f - progress
        return remaining * remaining * (3f - 2f * remaining)
    }

    fun reactionWeight(nowUptimeMillis: Long): Float {
        val activatedAt = activatedAtUptimeMillis ?: return 0f
        val progress = ((nowUptimeMillis - activatedAt).coerceAtLeast(0L).toFloat() / REACTION_MILLIS)
            .coerceIn(0f, 1f)
        return 1f - progress
    }

    companion object {
        const val GAZE_LINGER_MILLIS = 1_150f
        const val REACTION_MILLIS = 680f

        val Idle = AssistantAttentionSignal()
    }
}

private fun Float.finiteNormalized(): Float = if (isFinite()) coerceIn(-1f, 1f) else 0f
