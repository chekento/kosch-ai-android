package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.AiRoutingMode
import cloud.kosch.aiandroid.model.AiSettings
import cloud.kosch.aiandroid.model.AppearanceSettings
import cloud.kosch.aiandroid.model.GestureAction
import cloud.kosch.aiandroid.model.GestureBinding
import cloud.kosch.aiandroid.model.GestureSettings
import cloud.kosch.aiandroid.model.GestureTrigger
import cloud.kosch.aiandroid.model.HomeSettings
import cloud.kosch.aiandroid.model.LauncherAssistantSettings
import cloud.kosch.aiandroid.model.LauncherSettingsDocument
import cloud.kosch.aiandroid.model.ProviderSettings
import cloud.kosch.aiandroid.model.ProviderTransport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Base64

class LauncherSettingsCodecTest {
    @Test
    fun roundTrip_preservesPortableSettingsAcrossMajorSections() {
        val source = LauncherSettingsDocument(
            home = HomeSettings(gridColumns = 16, gridRows = 14, iconScale = 1.2f, lockLayout = true),
            appearance = AppearanceSettings(blurStrength = 0.7f, surfaceOpacity = 0.81f, iconPackPackage = "example.icons"),
            assistant = LauncherAssistantSettings(
                enabled = true,
                assistantId = "default-pro",
                scale = 1.25f,
                visemeLipSyncEnabled = true,
                liveChatEnabled = true,
            ),
            ai = AiSettings(
                routingMode = AiRoutingMode.DEFAULT_PROVIDER,
                defaultProviderId = "provider-one",
                networkProvidersEnabled = true,
                providers = listOf(
                    ProviderSettings(
                        providerId = "provider-one",
                        enabled = true,
                        transport = ProviderTransport.OPENAI_COMPATIBLE,
                        endpoint = "https://example.invalid/v1",
                        modelId = "model-one",
                        credentialAlias = "vault:provider-one",
                    ),
                ),
            ),
            gestures = GestureSettings(
                bindings = listOf(
                    GestureBinding(GestureTrigger.SWIPE_UP, GestureAction.OPEN_DRAWER),
                    GestureBinding(GestureTrigger.DOUBLE_TAP, GestureAction.OPEN_ASSISTANT),
                ),
            ),
        ).normalized()

        val restored = LauncherSettingsCodec.decode(LauncherSettingsCodec.encode(source))

        assertEquals(source, restored)
    }

    @Test
    fun encoding_isDeterministicForSameNormalizedDocument() {
        val source = LauncherSettingsDocument(
            home = HomeSettings(gridColumns = 9, gridRows = 17),
            assistant = LauncherAssistantSettings(enabled = true),
        )

        assertEquals(
            LauncherSettingsCodec.encode(source),
            LauncherSettingsCodec.encode(source.copy()),
        )
    }

    @Test
    fun unknownFutureKeys_areIgnored() {
        val source = LauncherSettingsDocument(home = HomeSettings(gridColumns = 15))
        val encoded = LauncherSettingsCodec.encode(source) + "\nfuture.new.setting=${wire("future-value")}"

        val restored = LauncherSettingsCodec.decode(encoded)

        assertEquals(15, restored.home.gridColumns)
    }

    @Test
    fun backupSecurityExclusions_cannotBeDisabledByImportedWirePayload() {
        val encoded = LauncherSettingsCodec.encode(LauncherSettingsDocument())
            .replace("backup.excludeSecretsAlways=${wire("true")}", "backup.excludeSecretsAlways=${wire("false")}")
            .replace("backup.excludeWidgetHostIdsAlways=${wire("true")}", "backup.excludeWidgetHostIdsAlways=${wire("false")}")

        val restored = LauncherSettingsCodec.decode(encoded)

        assertTrue(restored.backup.excludeSecretsAlways)
        assertTrue(restored.backup.excludeWidgetHostIdsAlways)
    }

    @Test
    fun portableEncoding_containsCredentialAliasButNoSecretFieldNames() {
        val encoded = LauncherSettingsCodec.encode(
            LauncherSettingsDocument(
                ai = AiSettings(
                    providers = listOf(
                        ProviderSettings(
                            providerId = "one",
                            credentialAlias = "vault:one",
                        ),
                    ),
                ),
            ),
        )

        val decodedLines = encoded.lineSequence().associate { line ->
            val separator = line.indexOf('=')
            line.substring(0, separator) to String(
                Base64.getUrlDecoder().decode(line.substring(separator + 1)),
                StandardCharsets.UTF_8,
            )
        }

        assertEquals("vault:one", decodedLines["ai.providers.0.credentialAlias"])
        assertFalse(decodedLines.keys.any { it.contains("apiKey", ignoreCase = true) })
        assertFalse(decodedLines.keys.any { it.contains("secretValue", ignoreCase = true) })
    }

    private fun wire(value: String): String = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
}
