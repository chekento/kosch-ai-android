package cloud.kosch.aiandroid.assistant

import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantProviderReadinessTest {
    @Test
    fun disconnected_explainsLocalFallbackWithoutPretendingToConnect() {
        val text = AssistantProviderReadiness.describe(
            connected = false,
            cloudExecutionEnabled = false,
            selectedModelId = "",
        )

        assertTrue(text.contains("nicht verbunden", ignoreCase = true))
        assertTrue(text.contains("lokal", ignoreCase = true))
        assertTrue(text.contains("AI Hub", ignoreCase = true))
    }

    @Test
    fun connectedButCloudDisabled_explainsThatNothingIsSent() {
        val text = AssistantProviderReadiness.describe(
            connected = true,
            cloudExecutionEnabled = false,
            selectedModelId = "openai/gpt-test",
        )

        assertTrue(text.contains("Cloud Access", ignoreCase = true))
        assertTrue(text.contains("keine", ignoreCase = true))
        assertTrue(text.contains("extern", ignoreCase = true))
    }

    @Test
    fun missingModel_isExplicit() {
        val text = AssistantProviderReadiness.describe(
            connected = true,
            cloudExecutionEnabled = true,
            selectedModelId = " ",
        )

        assertTrue(text.contains("kein Modell", ignoreCase = true))
        assertTrue(text.contains("AI Hub", ignoreCase = true))
    }

    @Test
    fun ready_showsProviderAndSelectedModel() {
        val text = AssistantProviderReadiness.describe(
            connected = true,
            cloudExecutionEnabled = true,
            selectedModelId = "openai/gpt-test",
        )

        assertTrue(text.contains("bereit", ignoreCase = true))
        assertTrue(text.contains("OpenRouter"))
        assertTrue(text.contains("openai/gpt-test"))
    }
}
