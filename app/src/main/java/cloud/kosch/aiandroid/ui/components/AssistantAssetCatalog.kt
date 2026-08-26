package cloud.kosch.aiandroid.ui.components

import cloud.kosch.aiandroid.model.AssistantVisualState

/** Runtime naming contract derived from the KoSch assistant asset matrix. */
object AssistantAssetCatalog {
    const val DEFAULT_ASSISTANT_ID = "default"
    const val DEFAULT_THEME_ID = "default"

    val visemes = AssistantViseme.entries.map(AssistantViseme::code)

    fun bodyFile(
        state: AssistantVisualState,
        assistantId: String = DEFAULT_ASSISTANT_ID,
    ): String? = when (state) {
        AssistantVisualState.DISABLED -> null
        AssistantVisualState.IDLE -> "asst_${assistantId}_body_idle_neutral.webp"
        AssistantVisualState.LISTENING -> "asst_${assistantId}_body_launcher_listening.webp"
        AssistantVisualState.THINKING -> "asst_${assistantId}_body_launcher_thinking.webp"
        AssistantVisualState.SPEAKING -> "asst_${assistantId}_body_idle_happy.webp"
        AssistantVisualState.WORKING -> "asst_${assistantId}_body_launcher_working.webp"
        AssistantVisualState.OFFLINE -> "asst_${assistantId}_body_launcher_offline.webp"
        AssistantVisualState.ERROR -> "asst_${assistantId}_body_launcher_error.webp"
    }

    fun eyeFile(
        state: AssistantVisualState,
        assistantId: String = DEFAULT_ASSISTANT_ID,
    ): String = eyeFile(
        eye = when (state) {
            AssistantVisualState.DISABLED,
            AssistantVisualState.IDLE,
            AssistantVisualState.WORKING,
            -> AssistantEyeShape.CENTER
            AssistantVisualState.LISTENING -> AssistantEyeShape.FOCUS
            AssistantVisualState.THINKING -> AssistantEyeShape.UP_RIGHT
            AssistantVisualState.SPEAKING -> AssistantEyeShape.HAPPY
            AssistantVisualState.OFFLINE -> AssistantEyeShape.WORRIED
            AssistantVisualState.ERROR -> AssistantEyeShape.CONFUSED
        },
        assistantId = assistantId,
    )

    fun eyeFile(
        eye: AssistantEyeShape,
        assistantId: String = DEFAULT_ASSISTANT_ID,
    ): String = "asst_${assistantId}_eye_${eye.assetSuffix}.webp"

    fun mouthVisemeFile(
        viseme: String,
        assistantId: String = DEFAULT_ASSISTANT_ID,
    ): String {
        require(viseme in visemes) { "Unbekanntes Visem: $viseme" }
        return "asst_${assistantId}_mouth_viseme_${viseme}.webp"
    }

    fun mouthVisemeFile(
        viseme: AssistantViseme,
        assistantId: String = DEFAULT_ASSISTANT_ID,
    ): String = mouthVisemeFile(viseme.code, assistantId)

    fun mouthEmotionFile(
        emotion: AssistantMouthEmotion,
        assistantId: String = DEFAULT_ASSISTANT_ID,
    ): String = "asst_${assistantId}_mouth_${emotion.assetSuffix}.webp"

    fun mouthFile(
        mouth: AssistantMouthShape,
        assistantId: String = DEFAULT_ASSISTANT_ID,
    ): String = when (mouth) {
        is AssistantMouthShape.Viseme -> mouthVisemeFile(mouth.value, assistantId)
        is AssistantMouthShape.Emotion -> mouthEmotionFile(mouth.value, assistantId)
    }

    fun spawnFiles(assistantId: String = DEFAULT_ASSISTANT_ID): List<String> =
        (0..15).map { frame -> "asst_${assistantId}_spawn_${frame.toString().padStart(3, '0')}.webp" }

    fun despawnFiles(assistantId: String = DEFAULT_ASSISTANT_ID): List<String> =
        spawnFiles(assistantId).asReversed()

    fun turnYFiles(assistantId: String = DEFAULT_ASSISTANT_ID): List<String> =
        (0 until 360 step 15).map { degrees ->
            "asst_${assistantId}_turn_y_${degrees.toString().padStart(3, '0')}.webp"
        }

    fun portalFiles(themeId: String = DEFAULT_THEME_ID): List<String> =
        (0..7).map { frame -> "portal_${themeId}_${frame.toString().padStart(3, '0')}.webp" }
}
