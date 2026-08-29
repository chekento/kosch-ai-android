package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiHubShortcutRoutePolicyTest {
    @Test
    fun voiceUsesOnlyExplicitVoiceShortcut() {
        val selected = AiHubShortcutRoutePolicy.preferredKind(
            AiHubTaskIntent.VOICE,
            listOf(
                AiPublishedShortcutKind.NEW_CHAT,
                AiPublishedShortcutKind.AI_ASSISTANT,
                AiPublishedShortcutKind.VOICE,
            ),
        )

        assertEquals(AiPublishedShortcutKind.VOICE, selected)
    }

    @Test
    fun researchDoesNotTreatGenericAssistantAsResearch() {
        val selected = AiHubShortcutRoutePolicy.preferredKind(
            AiHubTaskIntent.RESEARCH,
            listOf(AiPublishedShortcutKind.NEW_CHAT, AiPublishedShortcutKind.AI_ASSISTANT),
        )

        assertNull(selected)
    }

    @Test
    fun generalChatPrefersExplicitNewChatOverAssistantEntry() {
        val selected = AiHubShortcutRoutePolicy.preferredKind(
            AiHubTaskIntent.GENERAL_CHAT,
            listOf(AiPublishedShortcutKind.AI_ASSISTANT, AiPublishedShortcutKind.NEW_CHAT),
        )

        assertEquals(AiPublishedShortcutKind.NEW_CHAT, selected)
    }

    @Test
    fun sourceNotebookHasNoInventedShortcutFallback() {
        val selected = AiHubShortcutRoutePolicy.preferredKind(
            AiHubTaskIntent.SOURCE_NOTEBOOK,
            AiPublishedShortcutKind.entries,
        )

        assertNull(selected)
    }
}
