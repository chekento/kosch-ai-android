package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.SelectedContact

sealed interface ContactPickResolution {
    data class Accepted(val contact: SelectedContact) : ContactPickResolution
    data object Rejected : ContactPickResolution
    data class Failed(val cause: Throwable) : ContactPickResolution
}

object ContactPickSemantics {
    fun resolve(result: Result<SelectedContact?>): ContactPickResolution = result.fold(
        onSuccess = { contact ->
            contact?.let(ContactPickResolution::Accepted) ?: ContactPickResolution.Rejected
        },
        onFailure = ContactPickResolution::Failed,
    )
}
