package cloud.kosch.aiandroid

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cloud.kosch.aiandroid.ai.AiConfirmedContextHandoff
import cloud.kosch.aiandroid.ai.AiContextHandoffDraft
import cloud.kosch.aiandroid.ai.AiContextHandoffPolicy
import cloud.kosch.aiandroid.model.FileInsight

/**
 * Ephemeral user-consent boundary between local launcher context and an AI prompt.
 *
 * Drafts are memory-only and never persisted. Preparing a draft does not modify/open the AI Hub and does not transfer
 * content. Only confirm(..., userConfirmed=true) returns a payload that another controller may hand to an AI route.
 */
class AiContextHandoffController {
    var draft by mutableStateOf<AiContextHandoffDraft?>(null)
        private set

    fun prepareFile(insight: FileInsight): AiContextHandoffDraft {
        val prepared = AiContextHandoffPolicy.fromFile(insight)
        draft = prepared
        return prepared
    }

    fun cancel() {
        draft = null
    }

    fun confirm(
        userPrompt: String,
        userConfirmed: Boolean,
    ): AiConfirmedContextHandoff? {
        val current = draft ?: return null
        val confirmed = AiContextHandoffPolicy.confirm(
            draft = current,
            userPrompt = userPrompt,
            userConfirmed = userConfirmed,
        )
        draft = null
        return confirmed
    }
}
