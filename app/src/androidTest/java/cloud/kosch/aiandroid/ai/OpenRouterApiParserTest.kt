package cloud.kosch.aiandroid.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenRouterApiParserTest {
    @Test
    fun modelCatalogReadsLiveCapabilityShapeWithoutHardcodedModelNames() {
        val models = OpenRouterModelCatalogParser.parse(
            """
            {
              "data": [
                {
                  "id": "provider/model-a",
                  "name": "Model A",
                  "context_length": 1000000,
                  "architecture": {
                    "input_modalities": ["text", "image"],
                    "output_modalities": ["text"]
                  },
                  "supported_parameters": ["tools", "reasoning"],
                  "pricing": {"prompt": "0.000001", "completion": "0.000002"}
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(1, models.size)
        val model = models.single()
        assertEquals("provider/model-a", model.id)
        assertEquals(1_000_000, model.contextLength)
        assertTrue(model.supportsTextInput)
        assertTrue(model.supportsImageInput)
        assertTrue(model.supportsTextOutput)
        assertTrue("tools" in model.supportedParameters)
        assertEquals("0.000001", model.promptPrice)
    }

    @Test
    fun modelCatalogAlsoAcceptsTypedParameterObjects() {
        val models = OpenRouterModelCatalogParser.parse(
            """
            {
              "data": [
                {
                  "id": "provider/image-model",
                  "architecture": {
                    "input_modalities": ["text"],
                    "output_modalities": ["image"]
                  },
                  "supported_parameters": {
                    "resolution": {"type": "enum"},
                    "n": {"type": "range"}
                  }
                }
              ]
            }
            """.trimIndent(),
        )

        val model = models.single()
        assertTrue("resolution" in model.supportedParameters)
        assertTrue("n" in model.supportedParameters)
        assertFalse(model.supportsTextOutput)
    }

    @Test
    fun chatParserAcceptsStringContentAndUsageCost() {
        val response = OpenRouterChatResponseParser.parse(
            """
            {
              "id": "gen-123",
              "model": "provider/model-a",
              "choices": [
                {"message": {"role": "assistant", "content": "Hallo von OpenRouter"}}
              ],
              "usage": {"cost": 0.00123}
            }
            """.trimIndent(),
        )

        assertEquals("provider/model-a", response.modelId)
        assertEquals("Hallo von OpenRouter", response.text)
        assertEquals("gen-123", response.requestId)
        assertEquals(0.00123, response.costUsd!!, 0.0000001)
    }

    @Test
    fun chatParserAcceptsStructuredTextParts() {
        val response = OpenRouterChatResponseParser.parse(
            """
            {
              "model": "provider/model-b",
              "choices": [
                {"message": {"content": [
                  {"type": "text", "text": "Teil 1 "},
                  {"type": "text", "text": "Teil 2"}
                ]}}
              ]
            }
            """.trimIndent(),
        )

        assertEquals("Teil 1 Teil 2", response.text)
    }

    @Test(expected = IllegalArgumentException::class)
    fun chatParserFailsClosedWithoutText() {
        OpenRouterChatResponseParser.parse(
            "{\"choices\":[{\"message\":{\"content\":[]}}]}",
        )
    }

    @Test
    fun errorParserBoundsProviderMessage() {
        val result = OpenRouterErrorParser.safeMessage(
            "{\"error\":{\"message\":\"Invalid model\"}}",
            400,
        )

        assertEquals("OpenRouter: Invalid model", result)
    }
}
