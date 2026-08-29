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
                    connectedProviderAvailable = true,
                    userAllowsConnectedProvider = true,
                    appHandoffAvailable = true,
                    userAllowsExternalHandoff = true,
                ),
            ),
        )
    }

    @Test
    fun onDeviceGenAiWinsBeforeConnectedOrExternalRoutes() {
        assertEquals(
            AiExecutionLane.ANDROID_ON_DEVICE_GENAI,
            AiExecutionRouter.route(
                AiExecutionContext(
                    deterministicLocalSupport = false,
                    onDeviceGenAiAvailable = true,
                    connectedProviderAvailable = true,
                    userAllowsConnectedProvider = true,
                    appHandoffAvailable = true,
                    userAllowsExternalHandoff = true,
                ),
            ),
        )
    }

    @Test
    fun connectedProviderWinsBeforeInstalledAppOnlyAfterExplicitCloudGate() {
        assertEquals(
            AiExecutionLane.CONNECTED_PROVIDER,
            AiExecutionRouter.route(
                AiExecutionContext(
                    deterministicLocalSupport = false,
                    onDeviceGenAiAvailable = false,
                    connectedProviderAvailable = true,
                    userAllowsConnectedProvider = true,
                    appHandoffAvailable = true,
                    userAllowsExternalHandoff = true,
                ),
            ),
        )
    }

    @Test
    fun connectedCredentialDoesNotBypassCloudPermission() {
        assertEquals(
            AiExecutionLane.INSTALLED_APP_HANDOFF,
            AiExecutionRouter.route(
                AiExecutionContext(
                    deterministicLocalSupport = false,
                    onDeviceGenAiAvailable = false,
                    connectedProviderAvailable = true,
                    userAllowsConnectedProvider = false,
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
