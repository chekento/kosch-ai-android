package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiPublishedShortcutClassifierTest {
    @Test
    fun knownBrowserAiLabels_areAssistantEntrypoints() {
        assertEquals(AiPublishedShortcutKind.AI_ASSISTANT, AiPublishedShortcutClassifier.classify("Copilot"))
        assertEquals(AiPublishedShortcutKind.AI_ASSISTANT, AiPublishedShortcutClassifier.classify("Aria"))
        assertEquals(AiPublishedShortcutKind.AI_ASSISTANT, AiPublishedShortcutClassifier.classify("Brave Leo"))
        assertEquals(AiPublishedShortcutKind.AI_ASSISTANT, AiPublishedShortcutClassifier.classify("Duck.ai"))
        assertEquals(AiPublishedShortcutKind.AI_ASSISTANT, AiPublishedShortcutClassifier.classify("Gemini"))
    }

    @Test
    fun aiAppActions_areClassifiedWithoutInventingGenericActions() {
        assertEquals(AiPublishedShortcutKind.NEW_CHAT, AiPublishedShortcutClassifier.classify("New Chat"))
        assertEquals(AiPublishedShortcutKind.VOICE, AiPublishedShortcutClassifier.classify("Voice Chat"))
        assertEquals(AiPublishedShortcutKind.RESEARCH, AiPublishedShortcutClassifier.classify("Deep Research"))
        assertEquals(AiPublishedShortcutKind.IMAGE, AiPublishedShortcutClassifier.classify("Create Image"))
        assertNull(AiPublishedShortcutClassifier.classify("New private tab"))
        assertNull(AiPublishedShortcutClassifier.classify("Downloads"))
        assertNull(AiPublishedShortcutClassifier.classify("Settings"))
    }
}
