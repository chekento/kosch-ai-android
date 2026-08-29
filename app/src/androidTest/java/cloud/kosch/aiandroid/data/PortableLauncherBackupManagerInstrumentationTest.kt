package cloud.kosch.aiandroid.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cloud.kosch.aiandroid.model.AssistantAgentPreferences
import cloud.kosch.aiandroid.model.AssistantPresenceMode
import cloud.kosch.aiandroid.model.AssistantWakeWordMode
import cloud.kosch.aiandroid.model.CustomLauncherAction
import cloud.kosch.aiandroid.model.CustomLauncherTarget
import cloud.kosch.aiandroid.model.HomeSettings
import cloud.kosch.aiandroid.model.LauncherSettingsDocument
import cloud.kosch.aiandroid.model.PortableSettingValue
import cloud.kosch.aiandroid.model.ScopedSettingsDocument
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PortableLauncherBackupManagerInstrumentationTest {
    private lateinit var context: Context
    private lateinit var workspaceStore: WorkspaceStore
    private lateinit var settingsStore: LauncherSettingsStore
    private lateinit var scopedStore: ScopedSettingsStore
    private lateinit var actionsStore: CustomLauncherActionStore
    private lateinit var assistantStore: AssistantAgentStore
    private lateinit var manager: PortableLauncherBackupManager

    private lateinit var originalWorkspace: ByteArray
    private lateinit var originalSettings: String
    private lateinit var originalScoped: String
    private lateinit var originalActions: String
    private lateinit var originalAssistant: AssistantAgentPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        workspaceStore = WorkspaceStore(context)
        settingsStore = LauncherSettingsStore(context)
        scopedStore = ScopedSettingsStore(context)
        actionsStore = CustomLauncherActionStore(context)
        assistantStore = AssistantAgentStore(context)
        manager = PortableLauncherBackupManager(context)

        originalWorkspace = workspaceStore.createPortableSnapshot()
        originalSettings = settingsStore.exportPortable()
        originalScoped = scopedStore.exportPortable()
        originalActions = actionsStore.exportPortable()
        originalAssistant = assistantStore.load()
    }

    @After
    fun tearDown() {
        runCatching { workspaceStore.restorePortableSnapshot(originalWorkspace) }
        originalWorkspace.fill(0)
        runCatching { settingsStore.applyImport(originalSettings).getOrThrow() }
        runCatching { scopedStore.applyImport(originalScoped, workspaceStore.loadWorkspaceDocument()).getOrThrow() }
        runCatching { actionsStore.applyImport(originalActions).getOrThrow() }
        runCatching { assistantStore.saveUserObservationOptIn(originalAssistant) }
    }

    @Test
    fun bundle_restoresPortableDomainsButNeverObservationOrActionOptIns() {
        val workspace = workspaceStore.loadWorkspaceDocument()
        val pageId = workspace.activePageId

        assertTrue(
            settingsStore.save(
                LauncherSettingsDocument(
                    home = HomeSettings(gridColumns = 17, gridRows = 15, iconScale = 1.2f),
                ),
            ),
        )
        assertTrue(
            scopedStore.save(
                ScopedSettingsDocument().withPageOverride(
                    pageId,
                    "home.icon.scale",
                    PortableSettingValue.Decimal(1.35),
                ),
            ),
        )
        assertTrue(
            actionsStore.save(
                listOf(
                    CustomLauncherAction(
                        id = "work.docs",
                        name = "Docs",
                        target = CustomLauncherTarget.WebUrl("https://example.com/docs"),
                    ),
                ),
            ),
        )
        assistantStore.saveUserObservationOptIn(
            AssistantAgentPreferences(
                characterId = "anime_female",
                assistantName = "Aira",
                presenceMode = AssistantPresenceMode.FULL_COMPANION,
                wakeWordMode = AssistantWakeWordMode.ASSISTANT_NAME,
                customWakeWord = "Aira",
                screenObservationEnabled = true,
                cameraObservationEnabled = true,
                actionExecutionEnabled = true,
                confirmationRequiredForExternalActions = true,
            ),
        )

        // WorkspaceStore deliberately rejects snapshots with implausible future creation times. Use the device clock
        // so this end-to-end test exercises a payload that could actually have been produced on the device.
        val payload = manager.createPortablePayload(nowEpochMillis = System.currentTimeMillis())

        assertTrue(settingsStore.save(LauncherSettingsDocument()))
        assertTrue(scopedStore.reset())
        assertTrue(actionsStore.reset())
        assistantStore.save(AssistantAgentPreferences())

        val inspection = manager.inspect(payload)
        assertFalse(inspection.diff.legacyWorkspaceOnly)
        assertTrue(inspection.diff.changedSectionCount >= 4)

        manager.restore(payload)

        assertEquals(17, settingsStore.load().home.gridColumns)
        assertEquals(15, settingsStore.load().home.gridRows)
        assertEquals(
            PortableSettingValue.Decimal(1.35),
            scopedStore.load().pageOverride(pageId, "home.icon.scale"),
        )
        assertEquals("work.docs", actionsStore.load().single().id)

        val restoredAssistant = assistantStore.load()
        assertEquals("anime_female", restoredAssistant.characterId)
        assertEquals("Aira", restoredAssistant.assistantName)
        assertEquals(AssistantPresenceMode.FULL_COMPANION, restoredAssistant.presenceMode)
        assertFalse(restoredAssistant.screenObservationEnabled)
        assertFalse(restoredAssistant.cameraObservationEnabled)
        assertFalse(restoredAssistant.actionExecutionEnabled)
        assertTrue(restoredAssistant.confirmationRequiredForExternalActions)
    }
}
