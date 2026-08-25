package cloud.kosch.aiandroid.ui.components

/**
 * Machine-checkable runtime mirror of KoSch_AI_Launcher_Assistant_Asset_Matrix_v1.xlsx.
 *
 * The spreadsheet remains the external authoring/source specification. This manifest deliberately
 * mirrors the exact v1 filenames needed by the APK so incomplete or drifting sprite exports can be
 * detected before they are activated at runtime.
 */
data class AssistantAssetManifest(
    val assistantId: String,
    val displayName: String,
    val assetVersion: String,
    val themeId: String,
    val portalThemeId: String,
    val bodyPoseFiles: List<String>,
    val spawnFiles: List<String>,
    val turnYFiles: List<String>,
    val eyeOverlayFiles: List<String>,
    val mouthVisemeFiles: List<String>,
    val mouthEmotionFiles: List<String>,
    val portalFiles: List<String>,
    val faceCalibration: AssistantFaceCalibration,
) {
    val bodyPaths: Set<String> by lazy {
        (bodyPoseFiles + spawnFiles + turnYFiles)
            .map { AssistantAssetPaths.body(assistantId, it) }
            .toSet()
    }

    val overlayPaths: Set<String> by lazy {
        (eyeOverlayFiles + mouthVisemeFiles + mouthEmotionFiles)
            .map { AssistantAssetPaths.overlay(assistantId, it) }
            .toSet()
    }

    val portalPaths: Set<String> by lazy {
        portalFiles.map(AssistantAssetPaths::commonFx).toSet()
    }

    val requiredPaths: Set<String> by lazy { bodyPaths + overlayPaths + portalPaths }

    fun audit(presentWebpPaths: Set<String>): AssistantAssetPackAudit {
        val assistantPrefix = "assistant/$assistantId/"
        val portalPrefix = "assistant/common/fx/portal_${portalThemeId}_"
        val relevantPresent = presentWebpPaths.filterTo(linkedSetOf()) {
            it.startsWith(assistantPrefix) || it.startsWith(portalPrefix)
        }
        return AssistantAssetPackAudit(
            expectedPaths = requiredPaths,
            presentPaths = relevantPresent,
            bodyMissing = bodyPaths - relevantPresent,
            overlayMissing = overlayPaths - relevantPresent,
            portalMissing = portalPaths - relevantPresent,
            unexpectedPaths = relevantPresent - requiredPaths,
            faceCalibrated = faceCalibration.isCalibrated,
        )
    }
}

data class AssistantAssetPackAudit(
    val expectedPaths: Set<String>,
    val presentPaths: Set<String>,
    val bodyMissing: Set<String>,
    val overlayMissing: Set<String>,
    val portalMissing: Set<String>,
    val unexpectedPaths: Set<String>,
    val faceCalibrated: Boolean,
) {
    val bodyComplete: Boolean get() = bodyMissing.isEmpty()
    val overlayComplete: Boolean get() = overlayMissing.isEmpty()
    val portalComplete: Boolean get() = portalMissing.isEmpty()
    val exportComplete: Boolean
        get() = bodyComplete && overlayComplete && portalComplete && unexpectedPaths.isEmpty()

    /**
     * Full visual activation is stricter than file completeness: eye/mouth placement must have been
     * measured against the exported front body rather than guessed from the reference sheet.
     */
    val activationReady: Boolean get() = exportComplete && faceCalibrated
}

/** Normalized placement rectangle relative to the 384x384 body canvas. */
data class AssistantNormalizedRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    init {
        require(left.isFinite() && top.isFinite() && width.isFinite() && height.isFinite()) {
            "Assistant overlay anchor must contain finite values"
        }
        require(left in 0f..1f && top in 0f..1f) { "Assistant overlay anchor origin must be normalized" }
        require(width > 0f && height > 0f) { "Assistant overlay anchor must have positive size" }
        require(left + width <= 1f && top + height <= 1f) {
            "Assistant overlay anchor must stay inside the body canvas"
        }
    }
}

data class AssistantFaceCalibration(
    val sourceAsset: String,
    val sourceBodyFile: String,
    val bodyPixelSize: Int,
    val overlayPixelSize: Int,
    val eyeAnchor: AssistantNormalizedRect? = null,
    val mouthAnchor: AssistantNormalizedRect? = null,
) {
    init {
        require(bodyPixelSize == AssistantAssetContract.BODY.pixelSize) {
            "Face calibration must use the runtime body pixel size"
        }
        require(overlayPixelSize == AssistantAssetContract.OVERLAY.pixelSize) {
            "Face calibration must use the runtime overlay pixel size"
        }
    }

    val isCalibrated: Boolean get() = eyeAnchor != null && mouthAnchor != null
}

object DefaultAssistantAssetManifest {
    val bodyPoseFiles = listOf(
        "asst_default_body_idle_neutral.webp",
        "asst_default_body_idle_happy.webp",
        "asst_default_body_idle_think.webp",
        "asst_default_body_idle_read.webp",
        "asst_default_body_idle_listen.webp",
        "asst_default_body_peace.webp",
        "asst_default_body_victory.webp",
        "asst_default_body_thumbs_up.webp",
        "asst_default_body_wave.webp",
        "asst_default_body_welcome.webp",
        "asst_default_body_point_left.webp",
        "asst_default_body_point_right.webp",
        "asst_default_body_point_up.webp",
        "asst_default_body_point_down.webp",
        "asst_default_body_present_left.webp",
        "asst_default_body_present_right.webp",
        "asst_default_body_shrug.webp",
        "asst_default_body_clap.webp",
        "asst_default_body_celebrate.webp",
        "asst_default_body_dance.webp",
        "asst_default_body_emote_happy.webp",
        "asst_default_body_emote_excited.webp",
        "asst_default_body_emote_sad.webp",
        "asst_default_body_emote_surprised.webp",
        "asst_default_body_emote_confused.webp",
        "asst_default_body_emote_angry.webp",
        "asst_default_body_emote_love.webp",
        "asst_default_body_emote_laugh.webp",
        "asst_default_body_sleep_yawn.webp",
        "asst_default_body_sleep_sleepy.webp",
        "asst_default_body_sleep.webp",
        "asst_default_body_sleep_wake.webp",
        "asst_default_body_launcher_listening.webp",
        "asst_default_body_launcher_thinking.webp",
        "asst_default_body_launcher_searching.webp",
        "asst_default_body_launcher_working.webp",
        "asst_default_body_launcher_typing.webp",
        "asst_default_body_launcher_notification.webp",
        "asst_default_body_launcher_alert.webp",
        "asst_default_body_launcher_settings.webp",
        "asst_default_body_launcher_calendar.webp",
        "asst_default_body_launcher_download.webp",
        "asst_default_body_launcher_upload.webp",
        "asst_default_body_launcher_sync.webp",
        "asst_default_body_launcher_locked.webp",
        "asst_default_body_launcher_unlocked.webp",
        "asst_default_body_launcher_charging.webp",
        "asst_default_body_launcher_battery_low.webp",
        "asst_default_body_launcher_connecting.webp",
        "asst_default_body_launcher_connected.webp",
        "asst_default_body_launcher_offline.webp",
        "asst_default_body_launcher_error.webp",
        "asst_default_body_view_front.webp",
        "asst_default_body_view_back.webp",
    )

    val eyeOverlayFiles = listOf(
        "asst_default_eye_center.webp",
        "asst_default_eye_left.webp",
        "asst_default_eye_right.webp",
        "asst_default_eye_up.webp",
        "asst_default_eye_down.webp",
        "asst_default_eye_up_left.webp",
        "asst_default_eye_up_right.webp",
        "asst_default_eye_down_left.webp",
        "asst_default_eye_down_right.webp",
        "asst_default_eye_blink_open.webp",
        "asst_default_eye_blink_half_1.webp",
        "asst_default_eye_blink_closed.webp",
        "asst_default_eye_blink_half_2.webp",
        "asst_default_eye_happy.webp",
        "asst_default_eye_sad.webp",
        "asst_default_eye_surprised.webp",
        "asst_default_eye_angry.webp",
        "asst_default_eye_confused.webp",
        "asst_default_eye_love.webp",
        "asst_default_eye_sleepy.webp",
        "asst_default_eye_excited.webp",
        "asst_default_eye_worried.webp",
        "asst_default_eye_focus.webp",
        "asst_default_eye_wink_left.webp",
        "asst_default_eye_wink_right.webp",
    )

    val mouthEmotionFiles = listOf(
        "asst_default_mouth_neutral.webp",
        "asst_default_mouth_smile.webp",
        "asst_default_mouth_grin.webp",
        "asst_default_mouth_laugh.webp",
        "asst_default_mouth_sad.webp",
        "asst_default_mouth_frown.webp",
        "asst_default_mouth_surprised.webp",
        "asst_default_mouth_yawn.webp",
    )

    val manifest = AssistantAssetManifest(
        assistantId = "default",
        displayName = "Default Assistant",
        assetVersion = "v1",
        themeId = "default_neon",
        portalThemeId = "default",
        bodyPoseFiles = bodyPoseFiles,
        spawnFiles = AssistantAssetCatalog.spawnFiles("default"),
        turnYFiles = AssistantAssetCatalog.turnYFiles("default"),
        eyeOverlayFiles = eyeOverlayFiles,
        mouthVisemeFiles = AssistantAssetCatalog.visemes.map {
            AssistantAssetCatalog.mouthVisemeFile(it, "default")
        },
        mouthEmotionFiles = mouthEmotionFiles,
        portalFiles = AssistantAssetCatalog.portalFiles("default"),
        faceCalibration = AssistantFaceCalibration(
            sourceAsset = "Futuristische Roboter-Asset-Übersicht.png",
            sourceBodyFile = "asst_default_body_idle_neutral.webp",
            bodyPixelSize = 384,
            overlayPixelSize = 128,
            eyeAnchor = null,
            mouthAnchor = null,
        ),
    )
}
