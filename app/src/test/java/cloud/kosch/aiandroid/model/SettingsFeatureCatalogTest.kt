package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsFeatureCatalogTest {
    @Test
    fun stableIds_areUniqueAndMachineReadable() {
        val ids = SettingsFeatureCatalog.all.map(SettingsFeatureDefinition::id)
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { id ->
            assertTrue("Invalid settings feature id: $id", id.matches(Regex("[a-z0-9]+(?:[._][a-z0-9]+)+")))
        }
    }

    @Test
    fun everySettingsSection_hasExplicitCoverage() {
        SettingsSection.entries.forEach { section ->
            val features = SettingsFeatureCatalog.forSection(section)
            assertTrue("Missing feature coverage for ${section.name}", features.size >= 4)
            assertTrue(features.all { it.scopes.isNotEmpty() })
        }
    }

    @Test
    fun deviceAndSessionSecrets_neverPretendToBePortable() {
        val forbiddenPortableIds = setOf(
            "widgets.host_id",
            "assistant.screen_session",
            "assistant.camera_session",
            "assistant.visual_context",
            "api.credential_secret",
            "voice.assignment",
            "privacy.vault",
        )
        forbiddenPortableIds.forEach { id ->
            val feature = SettingsFeatureCatalog.all.single { it.id == id }
            assertFalse("$id must not be portable", feature.portability == SettingPortability.PORTABLE)
        }

        SettingsFeatureCatalog.all
            .filter { SettingScope.SESSION in it.scopes }
            .forEach { feature ->
                assertTrue(
                    "Session feature ${feature.id} must be session-only",
                    feature.portability == SettingPortability.SESSION_ONLY,
                )
            }
    }

    @Test
    fun criticalProfessionalLauncherDomains_cannotDisappearSilently() {
        val required = setOf(
            "home.grid.orientation",
            "pages.grid_override",
            "pages.profiles",
            "apps.custom_links",
            "dock.item_actions",
            "folders.mixed_actions",
            "widgets.host_recovery",
            "widgets.stacks",
            "appearance.icon_pack",
            "themes.creator",
            "assistant.wake_word",
            "assistant.screen_awareness",
            "assistant.camera_awareness",
            "assistant.external_confirmation",
            "assistant.context_redaction",
            "ai.task_routing",
            "ai.fallback_chain",
            "ai.cost_budget",
            "api.credential_alias",
            "api.timeout_retry",
            "voice.assignment",
            "gestures.page_override",
            "gestures.item_override",
            "search.command_palette",
            "notifications.privacy_redaction",
            "pen.circle_to_ask",
            "automation.dry_run",
            "accessibility.switch_access",
            "privacy.capture_exclusions",
            "backup.preview_diff",
            "backup.exclude_device_ids",
            "system.foldables",
            "system.external_display",
            "advanced.safe_mode",
            "advanced.performance_stats",
        )
        val actual = SettingsFeatureCatalog.all.mapTo(mutableSetOf(), SettingsFeatureDefinition::id)
        assertTrue("Missing critical features: ${required - actual}", actual.containsAll(required))
    }

    @Test
    fun liveClaims_areReservedForImplementedOrRuntimeBackedCapabilities() {
        val live = SettingsFeatureCatalog.all.filter { it.maturity == SettingMaturity.LIVE }
        assertTrue(live.size >= 25)
        assertTrue(live.any { it.id == "widgets.first_class_home" })
        assertTrue(live.any { it.id == "assistant.visual_context" })
        assertTrue(live.any { it.id == "appearance.material_you" })
        assertTrue(live.none { it.id == "assistant.recorder" })
        assertTrue(live.none { it.id == "ai.cost_budget" })
        assertTrue(live.none { it.id == "assistant.wake_word" })
        assertTrue(live.none { it.id == "assistant.presence" })
    }

    @Test
    fun stagedAssistantChoices_remainCoreReadyUntilTheirRuntimeActuallyExists() {
        val byId = SettingsFeatureCatalog.all.associateBy(SettingsFeatureDefinition::id)
        assertEquals(SettingMaturity.CORE_READY, byId.getValue("assistant.wake_word").maturity)
        assertEquals(SettingMaturity.CORE_READY, byId.getValue("assistant.presence").maturity)
        assertEquals(SettingMaturity.LIVE, byId.getValue("assistant.screen_awareness").maturity)
        assertEquals(SettingMaturity.LIVE, byId.getValue("assistant.camera_awareness").maturity)
    }

    @Test
    fun catalogSearch_coversIdsTitlesSectionsAndKeywords() {
        assertTrue(SettingsFeatureCatalog.search("deep link").any { it.id == "apps.custom_links" })
        assertTrue(SettingsFeatureCatalog.search("Screen Awareness").any { it.id == "assistant.screen_awareness" })
        assertTrue(SettingsFeatureCatalog.search("Backup").any { it.section == SettingsSection.BACKUP })
        assertTrue(SettingsFeatureCatalog.search("circle").any { it.id == "pen.circle_to_ask" })
    }
}
