package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherSettingsModelsTest {
    @Test
    fun normalized_clampsVisualAndGridRanges() {
        val normalized = LauncherSettingsDocument(
            home = HomeSettings(gridColumns = 99, gridRows = 1, iconScale = 9f),
            dock = DockSettings(maxItems = 50, backgroundOpacity = -3f),
            appearance = AppearanceSettings(blurStrength = 5f, contentScale = 0.1f),
            assistant = LauncherAssistantSettings(scale = 9f, opacity = 0.01f),
        ).normalized()

        assertEquals(MAX_GRID_COLUMNS, normalized.home.gridColumns)
        assertEquals(MIN_GRID_ROWS, normalized.home.gridRows)
        assertEquals(1.75f, normalized.home.iconScale)
        assertEquals(12, normalized.dock.maxItems)
        assertEquals(0f, normalized.dock.backgroundOpacity)
        assertEquals(1f, normalized.appearance.blurStrength)
        assertEquals(0.75f, normalized.appearance.contentScale)
        assertEquals(2.5f, normalized.assistant.scale)
        assertEquals(0.2f, normalized.assistant.opacity)
    }

    @Test
    fun providerSettings_storeOnlyCredentialAliasNotSecretMaterial() {
        val provider = ProviderSettings(
            providerId = " private-provider ",
            enabled = true,
            transport = ProviderTransport.OPENAI_COMPATIBLE,
            endpoint = " https://example.invalid/v1 ",
            modelId = " model-x ",
            credentialAlias = " vault:provider:private ",
        ).normalized()

        assertEquals("private-provider", provider.providerId)
        assertEquals("https://example.invalid/v1", provider.endpoint)
        assertEquals("model-x", provider.modelId)
        assertEquals("vault:provider:private", provider.credentialAlias)
        assertTrue(ProviderSettings::class.members.none { it.name.contains("key", ignoreCase = true) || it.name.contains("secret", ignoreCase = true) })
    }

    @Test
    fun invalidDefaultProvider_fallsBackToAskEveryTime() {
        val normalized = AiSettings(
            routingMode = AiRoutingMode.DEFAULT_PROVIDER,
            defaultProviderId = "missing",
            providers = listOf(ProviderSettings(providerId = "local")),
        ).normalized()

        assertNull(normalized.defaultProviderId)
        assertEquals(AiRoutingMode.ASK_EVERY_TIME, normalized.routingMode)
    }

    @Test
    fun duplicateProviderAndGestureDefinitions_areDeterministicallyReduced() {
        val normalized = LauncherSettingsDocument(
            ai = AiSettings(
                providers = listOf(
                    ProviderSettings("one", enabled = false),
                    ProviderSettings("one", enabled = true),
                    ProviderSettings("two", enabled = true),
                ),
            ),
            gestures = GestureSettings(
                bindings = listOf(
                    GestureBinding(GestureTrigger.SWIPE_UP, GestureAction.OPEN_DRAWER),
                    GestureBinding(GestureTrigger.SWIPE_UP, GestureAction.OPEN_SETTINGS),
                    GestureBinding(GestureTrigger.DOUBLE_TAP, GestureAction.OPEN_COMMAND_PALETTE),
                ),
            ),
        ).normalized()

        assertEquals(listOf("one", "two"), normalized.ai.providers.map { it.providerId })
        assertFalse(normalized.ai.providers.first().enabled)
        assertEquals(2, normalized.gestures.bindings.size)
        assertEquals(GestureAction.OPEN_DRAWER, normalized.gestures.bindings.first().action)
    }

    @Test
    fun backupDefaults_explicitlyExcludeSecretsAndWidgetHostIds() {
        val settings = LauncherSettingsDocument().backup
        assertTrue(settings.excludeSecretsAlways)
        assertTrue(settings.excludeWidgetHostIdsAlways)
        assertFalse(settings.includeUsageLearning)
    }

    @Test
    fun settingsCenter_hasBroadDedicatedSubtabs() {
        assertTrue(SettingsSection.entries.size >= 20)
        assertTrue(SettingsSection.ASSISTANT in SettingsSection.entries)
        assertTrue(SettingsSection.THEMES in SettingsSection.entries)
        assertTrue(SettingsSection.API in SettingsSection.entries)
        assertTrue(SettingsSection.ACCESSIBILITY in SettingsSection.entries)
    }
}
