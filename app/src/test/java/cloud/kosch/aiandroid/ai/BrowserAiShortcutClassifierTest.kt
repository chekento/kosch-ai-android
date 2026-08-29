package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrowserAiShortcutClassifierTest {
    @Test
    fun knownPublishedLabels_mapToVendorAiEntrypoints() {
        assertEquals(
            BrowserAiEntrypointKind.COPILOT,
            BrowserAiShortcutClassifier.classify("edge", "copilot", "Copilot")?.kind,
        )
        assertEquals(
            BrowserAiEntrypointKind.ARIA,
            BrowserAiShortcutClassifier.classify("opera", "assistant", "Ask Aria")?.kind,
        )
        assertEquals(
            BrowserAiEntrypointKind.LEO,
            BrowserAiShortcutClassifier.classify("brave", "leo", "Leo AI")?.kind,
        )
        assertEquals(
            BrowserAiEntrypointKind.DUCK_AI,
            BrowserAiShortcutClassifier.classify("duckduckgo", "duck_ai", "Duck.ai")?.kind,
        )
    }

    @Test
    fun staticBrowserAiStatus_doesNotInventShortcut() {
        assertNull(BrowserAiShortcutClassifier.classify("edge", "new_tab", "New tab"))
        assertNull(BrowserAiShortcutClassifier.classify("chrome", "search", "Search"))
    }
}
