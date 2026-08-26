package cloud.kosch.aiandroid.ui.components

import cloud.kosch.aiandroid.model.AssistantVisualState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantAssetManifestTest {
    private val manifest = DefaultAssistantAssetManifest.manifest

    @Test
    fun v1Manifest_mirrorsEveryMatrixAssetExactlyOnce() {
        assertEquals(54, manifest.bodyPoseFiles.size)
        assertEquals(16, manifest.spawnFiles.size)
        assertEquals(24, manifest.turnYFiles.size)
        assertEquals(25, manifest.eyeOverlayFiles.size)
        assertEquals(15, manifest.mouthVisemeFiles.size)
        assertEquals(8, manifest.mouthEmotionFiles.size)
        assertEquals(8, manifest.portalFiles.size)

        assertEquals(94, manifest.bodyPaths.size)
        assertEquals(48, manifest.overlayPaths.size)
        assertEquals(8, manifest.portalPaths.size)
        assertEquals(150, manifest.requiredPaths.size)
    }

    @Test
    fun currentVisualStates_areContainedInFullMatrixManifest() {
        AssistantVisualState.entries
            .filterNot { it == AssistantVisualState.DISABLED }
            .forEach { state ->
                val body = requireNotNull(AssistantAssetCatalog.bodyFile(state)) {
                    "Non-disabled state has no body mapping: $state"
                }
                assertTrue("Missing body mapping for $state: $body", body in manifest.bodyPoseFiles)
                assertTrue(
                    "Missing eye mapping for $state",
                    AssistantAssetCatalog.eyeFile(state) in manifest.eyeOverlayFiles,
                )
            }

        AssistantAssetCatalog.visemes.forEach { viseme ->
            assertTrue(
                AssistantAssetCatalog.mouthVisemeFile(viseme) in manifest.mouthVisemeFiles,
            )
        }
        AssistantEyeShape.entries.forEach { eye ->
            assertTrue(AssistantAssetCatalog.eyeFile(eye) in manifest.eyeOverlayFiles)
        }
        AssistantMouthEmotion.entries.forEach { mouth ->
            assertTrue(AssistantAssetCatalog.mouthEmotionFile(mouth) in manifest.mouthEmotionFiles)
        }
    }

    @Test
    fun manifest_keepsTrueBackViewAndReverseCompatibleRotationSet() {
        assertTrue("asst_default_body_view_back.webp" in manifest.bodyPoseFiles)
        assertTrue("asst_default_turn_y_180.webp" in manifest.turnYFiles)
        assertEquals("asst_default_spawn_015.webp", manifest.spawnFiles.last())
    }

    @Test
    fun emptyOrCompleteButUncalibratedExport_cannotActivateSprites() {
        val empty = manifest.audit(emptySet())
        assertFalse(empty.exportComplete)
        assertFalse(empty.activationReady)
        assertEquals(150, empty.bodyMissing.size + empty.overlayMissing.size + empty.portalMissing.size)

        val completeFiles = manifest.audit(manifest.requiredPaths)
        assertTrue(completeFiles.exportComplete)
        assertFalse(completeFiles.activationReady)
        assertFalse(manifest.faceCalibration.isCalibrated)
    }

    @Test
    fun measuredFaceAnchors_areRequiredAndMustStayInsideBodyCanvas() {
        val calibrated = manifest.copy(
            faceCalibration = manifest.faceCalibration.copy(
                eyeAnchor = AssistantNormalizedRect(left = 0.20f, top = 0.20f, width = 0.60f, height = 0.24f),
                mouthAnchor = AssistantNormalizedRect(left = 0.32f, top = 0.48f, width = 0.36f, height = 0.18f),
            ),
        )
        assertTrue(calibrated.audit(calibrated.requiredPaths).activationReady)

        assertThrows(IllegalArgumentException::class.java) {
            AssistantNormalizedRect(left = 0.9f, top = 0.2f, width = 0.2f, height = 0.2f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AssistantNormalizedRect(left = Float.NaN, top = 0.2f, width = 0.2f, height = 0.2f)
        }
    }

    @Test
    fun anotherPortalTheme_canCoexistWithoutBlockingDefaultManifest() {
        val anotherTheme = "assistant/common/fx/portal_professional_000.webp"
        val audit = manifest.audit(manifest.requiredPaths + anotherTheme)

        assertTrue(audit.exportComplete)
        assertTrue(audit.unexpectedPaths.isEmpty())
        assertFalse(audit.activationReady) // Default v1 is still intentionally uncalibrated.
    }
}
