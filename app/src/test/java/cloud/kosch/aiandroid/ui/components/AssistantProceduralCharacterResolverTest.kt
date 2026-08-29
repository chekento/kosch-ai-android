package cloud.kosch.aiandroid.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssistantProceduralCharacterResolverTest {
    @Test
    fun builtInAnimeCharacters_resolveToDistinctVisualFallbacks() {
        assertEquals(
            AssistantProceduralCharacter.ANIME_FEMALE,
            AssistantProceduralCharacterResolver.resolve("anime_female"),
        )
        assertEquals(
            AssistantProceduralCharacter.ANIME_MALE,
            AssistantProceduralCharacterResolver.resolve("anime_male"),
        )
    }

    @Test
    fun defaultAndUnknownCharacters_stayOnDefaultAssetRuntime() {
        assertNull(AssistantProceduralCharacterResolver.resolve("default"))
        assertNull(AssistantProceduralCharacterResolver.resolve("future_pack"))
    }
}
