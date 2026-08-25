package cloud.kosch.aiandroid.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AssistantAssetRuntimeTest {
    @Test
    fun matrixBudgets_areLockedToRuntimeContract() {
        assertEquals(384, AssistantAssetContract.BODY.pixelSize)
        assertEquals(35 * 1024, AssistantAssetContract.BODY.maxBytes)
        assertEquals(128, AssistantAssetContract.OVERLAY.pixelSize)
        assertEquals(12 * 1024, AssistantAssetContract.OVERLAY.maxBytes)
        assertEquals(256, AssistantAssetContract.PORTAL.pixelSize)
        assertEquals(20 * 1024, AssistantAssetContract.PORTAL.maxBytes)
    }

    @Test
    fun paths_followMatrixFoldersWithoutLeadingAssetsSegment() {
        assertEquals(
            "assistant/default/body/asst_default_body_idle_neutral.webp",
            AssistantAssetPaths.body("default", "asst_default_body_idle_neutral.webp"),
        )
        assertEquals(
            "assistant/default/overlay/asst_default_eye_center.webp",
            AssistantAssetPaths.overlay("default", "asst_default_eye_center.webp"),
        )
        assertEquals(
            "assistant/common/fx/portal_default_003.webp",
            AssistantAssetPaths.commonFx("portal_default_003.webp"),
        )
    }

    @Test
    fun paths_rejectTraversalAndNonAndroidNames() {
        assertThrows(IllegalArgumentException::class.java) {
            AssistantAssetPaths.body("../default", "asst_default_body_idle_neutral.webp")
        }
        assertThrows(IllegalArgumentException::class.java) {
            AssistantAssetPaths.body("default", "../secret.webp")
        }
        assertThrows(IllegalArgumentException::class.java) {
            AssistantAssetPaths.body("default", "Assistant Default.webp")
        }
        assertThrows(IllegalArgumentException::class.java) {
            AssistantAssetPaths.body("default", "asst-default.webp")
        }
    }

    @Test
    fun catalogKeepsExactlyEightPortalAndTwentyFourTurnFrames() {
        assertEquals(8, AssistantAssetCatalog.portalFiles().size)
        assertEquals(24, AssistantAssetCatalog.turnYFiles().size)
        assertEquals("asst_default_turn_y_180.webp", AssistantAssetCatalog.turnYFiles()[12])
    }
}
