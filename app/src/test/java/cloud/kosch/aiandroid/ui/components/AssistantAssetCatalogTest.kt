package cloud.kosch.aiandroid.ui.components

import cloud.kosch.aiandroid.model.AssistantVisualState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssistantAssetCatalogTest {
    @Test
    fun spawnAndTurnFrames_matchMatrixContract() {
        val spawn = AssistantAssetCatalog.spawnFiles()
        val turn = AssistantAssetCatalog.turnYFiles()

        assertEquals(16, spawn.size)
        assertEquals("asst_default_spawn_000.webp", spawn.first())
        assertEquals("asst_default_spawn_015.webp", spawn.last())
        assertEquals(24, turn.size)
        assertEquals("asst_default_turn_y_000.webp", turn.first())
        assertEquals("asst_default_turn_y_180.webp", turn[12])
        assertEquals("asst_default_turn_y_345.webp", turn.last())
    }

    @Test
    fun despawn_isExactReverseOfSpawn() {
        assertEquals(AssistantAssetCatalog.spawnFiles().asReversed(), AssistantAssetCatalog.despawnFiles())
    }

    @Test
    fun visemeContract_containsExactlyFifteenCodes() {
        assertEquals(
            listOf("sil", "pp", "ff", "th", "dd", "kk", "ch", "ss", "nn", "rr", "aa", "ee", "ih", "oh", "ou"),
            AssistantAssetCatalog.visemes,
        )
        assertEquals("asst_default_mouth_viseme_aa.webp", AssistantAssetCatalog.mouthVisemeFile("aa"))
        assertEquals("asst_default_mouth_viseme_ee.webp", AssistantAssetCatalog.mouthVisemeFile(AssistantViseme.E))
        assertEquals(
            "asst_default_mouth_viseme_oh.webp",
            AssistantAssetCatalog.mouthVisemeFile(AssistantViseme.OH),
        )
    }

    @Test
    fun independentEyeAndMouthShapes_resolveToMatrixNames() {
        assertEquals(
            "asst_default_eye_blink_half_1.webp",
            AssistantAssetCatalog.eyeFile(AssistantEyeShape.BLINK_HALF_1),
        )
        assertEquals(
            "asst_default_mouth_frown.webp",
            AssistantAssetCatalog.mouthEmotionFile(AssistantMouthEmotion.FROWN),
        )
        assertEquals(
            "asst_default_mouth_viseme_th.webp",
            AssistantAssetCatalog.mouthFile(AssistantMouthShape.Viseme(AssistantViseme.TH)),
        )
    }

    @Test
    fun disabledAssistant_hasNoBodyAsset() {
        assertNull(AssistantAssetCatalog.bodyFile(AssistantVisualState.DISABLED))
        assertEquals("asst_default_body_launcher_listening.webp", AssistantAssetCatalog.bodyFile(AssistantVisualState.LISTENING))
    }

    @Test
    fun portalContract_hasEightSharedFrames() {
        val frames = AssistantAssetCatalog.portalFiles()
        assertEquals(8, frames.size)
        assertEquals("portal_default_000.webp", frames.first())
        assertEquals("portal_default_007.webp", frames.last())
    }
}
