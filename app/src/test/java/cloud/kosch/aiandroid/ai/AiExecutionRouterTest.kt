package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AiExecutionRouterTest {
    @Test
    fun localDeterministicLaneWinsEvenWhenOtherAiIsAvailable() {
        assertEquals(
            AiExecutionLane.LOCAL_DETERMINISTIC,
            AiExecutionRouter.route(
                AiExecutionContext(
                    deterministicLocalSupport = true,
                    onDeviceGenAiAvailable = true,
                    appHandoffAvailable = true,
                    userAllowsExternalHandoff = true,
                ),
            ),
        )
    }

    @Test
    fun onDeviceGenAiWinsBeforeExternalAppHandoff() {
        assertEquals(
            AiExecutionLane.ANDROID_ON_DEVICE_GENAI,
            AiExecutionRouter.route(
                AiExecutionContext(
                    deterministicLocalSupport = false,
                    onDeviceGenAiAvailable = true,
                    appHandoffAvailable = true,
                    userAllowsExternalHandoff = true,
                ),
            ),
        )
    }

    @Test
    fun externalAppRequiresUserPermission() {
        assertEquals(
            AiExecutionLane.UNAVAILABLE,
            AiExecutionRouter.route(
                AiExecutionContext(
                    deterministicLocalSupport = false,
                    onDeviceGenAiAvailable = false,
                    appHandoffAvailable = true,
                    userAllowsExternalHandoff = false,
                ),
            ),
        )
    }

    @Test
    fun webIsNeverAutomatic() {
        assertEquals(
            AiExecutionLane.UNAVAILABLE,
            AiExecutionRouter.route(
                AiExecutionContext(
                    deterministicLocalSupport = false,
                    onDeviceGenAiAvailable = false,
                    appHandoffAvailable = false,
                    userAllowsExternalHandoff = true,
                    userExplicitlyRequestedWeb = false,
                ),
            ),
        )
        assertEquals(
            AiExecutionLane.EXPLICIT_WEB_HANDOFF,
            AiExecutionRouter.route(
                AiExecutionContext(
                    deterministicLocalSupport = false,
                    onDeviceGenAiAvailable = false,
                    appHandoffAvailable = false,
                    userAllowsExternalHandoff = true,
                    userExplicitlyRequestedWeb = true,
                ),
            ),
        )
    }
}
