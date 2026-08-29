package cloud.kosch.aiandroid.ui.components

import android.media.AudioFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantSpeechDynamicsTest {
    @Test
    fun visemeEnum_matchesExactMatrixOrder() {
        assertEquals(
            listOf("sil", "pp", "ff", "th", "dd", "kk", "ch", "ss", "nn", "rr", "aa", "ee", "ih", "oh", "ou"),
            AssistantViseme.entries.map(AssistantViseme::code),
        )
    }

    @Test
    fun germanAndEnglishGraphemes_mapToBoundedVisemeSequences() {
        assertEquals(
            listOf(
                AssistantViseme.PP,
                AssistantViseme.E,
                AssistantViseme.NN,
                AssistantViseme.CH,
            ),
            AssistantVisemeMapper.fromText("Mensch"),
        )
        assertEquals(
            listOf(AssistantViseme.FF, AssistantViseme.OH, AssistantViseme.NN, AssistantViseme.E),
            AssistantVisemeMapper.fromText("phone"),
        )
        assertTrue(
            AssistantVisemeMapper.fromText("a".repeat(200)).size <= AssistantSpeechSignal.MAX_RANGE_VISEMES,
        )
    }

    @Test
    fun matrixEeSlot_isUsedForEAndEeGraphemes() {
        assertEquals("ee", AssistantViseme.E.code)
        assertEquals(listOf(AssistantViseme.E), AssistantVisemeMapper.fromText("e"))
        assertEquals(listOf(AssistantViseme.E), AssistantVisemeMapper.fromText("ee"))
        assertEquals(
            "asst_default_mouth_viseme_ee.webp",
            AssistantAssetCatalog.mouthVisemeFile(AssistantViseme.E),
        )
    }

    @Test
    fun rangeMapping_clampsUntrustedTtsOffsets() {
        assertEquals(
            listOf(AssistantViseme.OH),
            AssistantVisemeMapper.fromRange("Hallo", start = 99, end = 120),
        )
        assertEquals(listOf(AssistantViseme.SIL), AssistantVisemeMapper.fromRange("", 0, 1))
    }

    @Test
    fun timedSignal_advancesDeterministicallyAndReducedMotionUsesSlowerSteps() {
        val signal = AssistantSpeechSignal(
            utteranceId = "u1",
            rangeVisemes = listOf(AssistantViseme.AA, AssistantViseme.E, AssistantViseme.OH),
            rangeStartedAtUptimeMillis = 1_000L,
            amplitude = 0.5f,
            rangeTimed = true,
        )

        assertEquals(AssistantViseme.AA, signal.currentViseme(1_000L, reducedMotion = false))
        assertEquals(AssistantViseme.E, signal.currentViseme(1_072L, reducedMotion = false))
        assertEquals(AssistantViseme.OH, signal.currentViseme(1_144L, reducedMotion = false))
        assertEquals(AssistantViseme.AA, signal.currentViseme(1_072L, reducedMotion = true))
    }

    @Test
    fun pcmRms_decodesSilenceAndFullScaleWithoutLeavingNormalizedRange() {
        assertEquals(
            0f,
            AssistantPcmAmplitude.normalizedRms(ByteArray(12), AudioFormat.ENCODING_PCM_16BIT),
            0.0001f,
        )
        val fullScale16 = byteArrayOf(-1, 127, -1, 127, -1, 127, -1, 127)
        val loud = AssistantPcmAmplitude.normalizedRms(fullScale16, AudioFormat.ENCODING_PCM_16BIT)
        assertTrue(loud in 0.99f..1f)
        assertEquals(
            0f,
            AssistantPcmAmplitude.normalizedRms(byteArrayOf(-128, -128), AudioFormat.ENCODING_PCM_8BIT),
            0.0001f,
        )
        assertEquals(0f, AssistantPcmAmplitude.normalizedRms(fullScale16, encoding = -99), 0.0001f)
    }

    @Test
    fun amplitudeFallback_keepsSilenceClosedAndMovesForSpeechEnergy() {
        val silent = AssistantSpeechSignal(utteranceId = "u", amplitude = 0f)
        val loud = AssistantSpeechSignal(utteranceId = "u", amplitude = 0.9f)

        assertEquals(AssistantViseme.SIL, silent.currentViseme(1_000L, reducedMotion = false))
        assertNotEquals(AssistantViseme.SIL, loud.currentViseme(1_000L, reducedMotion = false))
    }
}
