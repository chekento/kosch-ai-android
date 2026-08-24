package cloud.kosch.aiandroid.system

import cloud.kosch.aiandroid.model.AuditOutcome

/**
 * Keeps the durable result of a file mutation separate from the best-effort UI refresh that
 * follows it. A provider can accept a rename/delete/create request and then fail while listing
 * the directory; reporting the whole operation as failed would invite a dangerous retry.
 */
internal sealed interface FileMutationCompletion<out Effect, out Snapshot> {
    val auditOutcome: AuditOutcome

    data class Failed(
        val error: Throwable,
    ) : FileMutationCompletion<Nothing, Nothing> {
        override val auditOutcome = AuditOutcome.FAILED
    }

    data class Applied<Effect, Snapshot>(
        val effect: Effect,
        val refresh: Result<Snapshot>,
    ) : FileMutationCompletion<Effect, Snapshot> {
        override val auditOutcome = AuditOutcome.SUCCESS
    }
}

internal object FileMutationSemantics {
    inline fun <Effect, Snapshot> execute(
        mutation: () -> Effect,
        refresh: () -> Snapshot,
    ): FileMutationCompletion<Effect, Snapshot> {
        val mutationResult = runCatching(mutation)
        return mutationResult.fold(
            onSuccess = { effect ->
                FileMutationCompletion.Applied(effect, runCatching(refresh))
            },
            onFailure = { error -> FileMutationCompletion.Failed(error) },
        )
    }
}
