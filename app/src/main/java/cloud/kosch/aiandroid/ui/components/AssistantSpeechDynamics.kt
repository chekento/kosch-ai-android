package cloud.kosch.aiandroid.ui.components

import android.media.AudioFormat
import java.util.Locale
import kotlin.math.sqrt

/** The exact fifteen-viseme runtime contract from the Assistant asset matrix. */
enum class AssistantViseme(val code: String) {
    SIL("sil"),
    PP("pp"),
    FF("ff"),
    TH("th"),
    DD("dd"),
    KK("kk"),
    CH("ch"),
    SS("ss"),
    NN("nn"),
    RR("rr"),
    AA("aa"),
    E("ee"),
    IH("ih"),
    OH("oh"),
    OU("ou"),
}

/**
 * Ephemeral TTS-only visual signal. It contains no recorded microphone data and is never persisted.
 * Range timing wins when the selected Android TTS engine provides it; PCM level is the fallback.
 */
data class AssistantSpeechSignal(
    val utteranceId: String? = null,
    val rangeVisemes: List<AssistantViseme> = listOf(AssistantViseme.SIL),
    val rangeStartedAtUptimeMillis: Long = 0L,
    val amplitude: Float = 0f,
    val rangeTimed: Boolean = false,
) {
    init {
        require(amplitude.isFinite() && amplitude in 0f..1f) {
            "Assistant speech amplitude must be normalized"
        }
        require(rangeVisemes.isNotEmpty() && rangeVisemes.size <= MAX_RANGE_VISEMES) {
            "Assistant speech range must contain 1..$MAX_RANGE_VISEMES visemes"
        }
    }

    val active: Boolean get() = utteranceId != null

    fun currentViseme(
        nowUptimeMillis: Long,
        reducedMotion: Boolean,
    ): AssistantViseme {
        if (!active) return AssistantViseme.SIL
        if (rangeTimed) {
            val elapsed = (nowUptimeMillis - rangeStartedAtUptimeMillis).coerceAtLeast(0L)
            val stepMillis = if (reducedMotion) REDUCED_VISEME_STEP_MILLIS else VISEME_STEP_MILLIS
            val index = (elapsed / stepMillis).toInt().coerceAtMost(rangeVisemes.lastIndex)
            return rangeVisemes[index]
        }
        return AssistantAmplitudeViseme.fromLevel(amplitude, nowUptimeMillis, reducedMotion)
    }

    companion object {
        const val MAX_RANGE_VISEMES = 24
        private const val VISEME_STEP_MILLIS = 72L
        private const val REDUCED_VISEME_STEP_MILLIS = 140L

        val Idle = AssistantSpeechSignal()
    }
}

/** Conservative German/English grapheme mapping for Android TTS range callbacks. */
object AssistantVisemeMapper {
    fun fromRange(text: String, start: Int, end: Int): List<AssistantViseme> {
        if (text.isEmpty()) return listOf(AssistantViseme.SIL)
        val safeStart = start.coerceIn(0, text.length)
        val safeEnd = end.coerceIn(safeStart, text.length)
        val selected = if (safeEnd > safeStart) {
            text.substring(safeStart, safeEnd)
        } else {
            text.substring(safeStart.coerceAtMost(text.lastIndex), (safeStart + 1).coerceAtMost(text.length))
        }
        return fromText(selected)
    }

    fun fromText(text: String): List<AssistantViseme> {
        val normalized = text.lowercase(Locale.ROOT).replace("ß", "ss")
        val result = ArrayList<AssistantViseme>(normalized.length.coerceAtMost(AssistantSpeechSignal.MAX_RANGE_VISEMES))
        var index = 0

        fun append(viseme: AssistantViseme) {
            if (result.lastOrNull() != viseme && result.size < AssistantSpeechSignal.MAX_RANGE_VISEMES) {
                result += viseme
            }
        }

        while (index < normalized.length && result.size < AssistantSpeechSignal.MAX_RANGE_VISEMES) {
            val remaining = normalized.substring(index)
            val digraph = DIGRAPHS.firstOrNull { remaining.startsWith(it.first) }
            if (digraph != null) {
                append(digraph.second)
                index += digraph.first.length
                continue
            }

            val character = normalized[index]
            val viseme = when (character) {
                'p', 'b', 'm' -> AssistantViseme.PP
                'f', 'v', 'w' -> AssistantViseme.FF
                't', 'd' -> AssistantViseme.DD
                'k', 'g', 'q', 'c' -> AssistantViseme.KK
                'j' -> AssistantViseme.CH
                's', 'z', 'x' -> AssistantViseme.SS
                'n', 'l' -> AssistantViseme.NN
                'r' -> AssistantViseme.RR
                'a', 'á', 'à', 'â' -> AssistantViseme.AA
                'e', 'é', 'è', 'ê', 'ä' -> AssistantViseme.E
                'i', 'í', 'ì', 'î', 'y' -> AssistantViseme.IH
                'o', 'ó', 'ò', 'ô', 'ö' -> AssistantViseme.OH
                'u', 'ú', 'ù', 'û', 'ü' -> AssistantViseme.OU
                else -> AssistantViseme.SIL
            }
            append(viseme)
            index += 1
        }

        return result.ifEmpty { listOf(AssistantViseme.SIL) }
    }

    private val DIGRAPHS = listOf(
        "sch" to AssistantViseme.CH,
        "tch" to AssistantViseme.CH,
        "sh" to AssistantViseme.CH,
        "ch" to AssistantViseme.CH,
        "th" to AssistantViseme.TH,
        "ph" to AssistantViseme.FF,
        "ck" to AssistantViseme.KK,
        "ng" to AssistantViseme.NN,
        "au" to AssistantViseme.OU,
        "ou" to AssistantViseme.OU,
        "ow" to AssistantViseme.OU,
        "eu" to AssistantViseme.OU,
        "äu" to AssistantViseme.OU,
        "ee" to AssistantViseme.E,
        "oo" to AssistantViseme.OH,
    )
}

/** Decodes TTS PCM callbacks only far enough to obtain a bounded RMS level. */
object AssistantPcmAmplitude {
    fun normalizedRms(audio: ByteArray, encoding: Int): Float {
        if (audio.isEmpty()) return 0f
        val (sumSquares, sampleCount) = when (encoding) {
            AudioFormat.ENCODING_PCM_8BIT -> pcm8(audio)
            AudioFormat.ENCODING_PCM_FLOAT -> pcmFloat(audio)
            AudioFormat.ENCODING_PCM_16BIT,
            AudioFormat.ENCODING_DEFAULT,
            -> pcm16(audio)
            else -> return 0f
        }
        if (sampleCount == 0) return 0f
        return sqrt(sumSquares / sampleCount).toFloat().coerceIn(0f, 1f)
    }

    private fun pcm8(audio: ByteArray): Pair<Double, Int> {
        var sum = 0.0
        audio.forEach { byte ->
            val sample = ((byte.toInt() and 0xff) - 128) / 128.0
            sum += sample * sample
        }
        return sum to audio.size
    }

    private fun pcm16(audio: ByteArray): Pair<Double, Int> {
        var sum = 0.0
        var samples = 0
        var index = 0
        while (index + 1 < audio.size) {
            val low = audio[index].toInt() and 0xff
            val high = audio[index + 1].toInt()
            val signed = (high shl 8) or low
            val sample = signed.toShort().toInt() / 32768.0
            sum += sample * sample
            samples += 1
            index += 2
        }
        return sum to samples
    }

    private fun pcmFloat(audio: ByteArray): Pair<Double, Int> {
        var sum = 0.0
        var samples = 0
        var index = 0
        while (index + 3 < audio.size) {
            val bits = (audio[index].toInt() and 0xff) or
                ((audio[index + 1].toInt() and 0xff) shl 8) or
                ((audio[index + 2].toInt() and 0xff) shl 16) or
                (audio[index + 3].toInt() shl 24)
            val sample = Float.fromBits(bits)
            if (sample.isFinite()) {
                val bounded = sample.coerceIn(-1f, 1f).toDouble()
                sum += bounded * bounded
                samples += 1
            }
            index += 4
        }
        return sum to samples
    }
}

private object AssistantAmplitudeViseme {
    fun fromLevel(
        amplitude: Float,
        nowUptimeMillis: Long,
        reducedMotion: Boolean,
    ): AssistantViseme {
        if (amplitude < 0.045f) return AssistantViseme.SIL
        val phaseMillis = if (reducedMotion) 180L else 90L
        val phase = ((nowUptimeMillis.coerceAtLeast(0L) / phaseMillis) % 3L).toInt()
        return when {
            amplitude < 0.16f -> listOf(AssistantViseme.PP, AssistantViseme.IH, AssistantViseme.SIL)[phase]
            amplitude < 0.34f -> listOf(AssistantViseme.IH, AssistantViseme.E, AssistantViseme.PP)[phase]
            amplitude < 0.58f -> listOf(AssistantViseme.E, AssistantViseme.OH, AssistantViseme.IH)[phase]
            amplitude < 0.78f -> listOf(AssistantViseme.OH, AssistantViseme.AA, AssistantViseme.E)[phase]
            else -> listOf(AssistantViseme.AA, AssistantViseme.OU, AssistantViseme.AA)[phase]
        }
    }
}
