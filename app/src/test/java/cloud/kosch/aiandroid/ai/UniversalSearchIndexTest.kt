package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.CustomLauncherAction
import cloud.kosch.aiandroid.model.CustomLauncherTarget
import cloud.kosch.aiandroid.model.FolderKind
import cloud.kosch.aiandroid.model.LauncherFolder
import cloud.kosch.aiandroid.model.SettingMaturity
import cloud.kosch.aiandroid.model.SettingPortability
import cloud.kosch.aiandroid.model.SettingScope
import cloud.kosch.aiandroid.model.SettingsFeatureDefinition
import cloud.kosch.aiandroid.model.SettingsSection
import cloud.kosch.aiandroid.model.WorkspacePage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalSearchIndexTest {
    @Test
    fun `one index contains folders pages settings actions and ai routes`() {
        val entries = UniversalSearchIndex.build(
            UniversalSearchSources(
                folders = listOf(LauncherFolder("folder:work", "Arbeit", FolderKind.WORK, emptyList())),
                pages = listOf(WorkspacePage("page:studio", "Studio", 0)),
                settings = listOf(
                    SettingsFeatureDefinition(
                        id = "privacy.local",
                        section = SettingsSection.PRIVACY,
                        title = "Lokaler Datenschutz",
                        scopes = setOf(SettingScope.GLOBAL),
                        portability = SettingPortability.PORTABLE,
                        maturity = SettingMaturity.LIVE,
                        keywords = setOf("privacy", "datenschutz"),
                    ),
                ),
                customActions = listOf(
                    CustomLauncherAction(
                        id = "open.dashboard",
                        name = "Dashboard öffnen",
                        target = CustomLauncherTarget.WebUrl("https://example.com"),
                    ),
                ),
                aiRoutes = listOf(
                    UniversalAiRouteSource("local", "Lokale KI", keywords = listOf("offline", "privat")),
                ),
            ),
        )

        assertEquals(
            setOf(
                UniversalSearchKind.FOLDER,
                UniversalSearchKind.PAGE,
                UniversalSearchKind.SETTING,
                UniversalSearchKind.CUSTOM_ACTION,
                UniversalSearchKind.AI_ROUTE,
            ),
            entries.map(UniversalSearchEntry::kind).toSet(),
        )
        assertEquals(UniversalSearchKind.SETTING, UniversalSearchIndex.rank("datenschutz", entries).first().entry.kind)
        assertEquals(UniversalSearchKind.AI_ROUTE, UniversalSearchIndex.rank("offline", entries).first().entry.kind)
    }

    @Test
    fun `native script exact match outranks romanized discovery`() {
        val entries = listOf(
            UniversalSearchEntry(
                id = "native",
                kind = UniversalSearchKind.APP,
                title = "Телеграм",
                target = UniversalSearchTarget.App("native"),
            ),
            UniversalSearchEntry(
                id = "latin",
                kind = UniversalSearchKind.APP,
                title = "Telegram Tools",
                target = UniversalSearchTarget.App("latin"),
            ),
        )

        val native = UniversalSearchIndex.rank("Телеграм", entries)
        assertEquals("native", native.first().entry.id)
        assertEquals(SearchMatchReason.EXACT, native.first().reason)

        val romanized = UniversalSearchIndex.rank("telegram", entries)
        assertTrue(romanized.any { it.entry.id == "native" && it.reason == SearchMatchReason.TRANSLITERATED })
    }

    @Test
    fun `bounded local priority cannot turn prefix match into exact winner`() {
        val entries = listOf(
            UniversalSearchEntry(
                id = "exact",
                kind = UniversalSearchKind.APP,
                title = "Mail",
                target = UniversalSearchTarget.App("exact"),
            ),
            UniversalSearchEntry(
                id = "prefix",
                kind = UniversalSearchKind.APP,
                title = "Mailbox",
                target = UniversalSearchTarget.App("prefix"),
                localPriorityBoost = Int.MAX_VALUE,
            ),
        )

        val ranked = UniversalSearchIndex.rank("mail", entries)
        assertEquals("exact", ranked.first().entry.id)
        assertEquals(SearchMatchReason.EXACT, ranked.first().reason)
    }
}
