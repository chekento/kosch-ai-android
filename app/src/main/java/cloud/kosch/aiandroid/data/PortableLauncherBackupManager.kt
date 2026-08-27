package cloud.kosch.aiandroid.data

import android.content.Context
import cloud.kosch.aiandroid.model.BackupPreview
import java.nio.charset.StandardCharsets

/**
 * Coordinates the portable parts of the launcher without weakening their individual trust boundaries.
 *
 * Restore is validate-first. Multi-store writes are rolled back to local snapshots when a later portable section
 * fails. Android widget host ids, URI grants, device voice ids, capture sessions and secrets never enter this manager.
 */
class PortableLauncherBackupManager(context: Context) {
    private val workspaceStore = WorkspaceStore(context.applicationContext)
    private val launcherSettingsStore = LauncherSettingsStore(context.applicationContext)
    private val scopedSettingsStore = ScopedSettingsStore(context.applicationContext)
    private val customActionsStore = CustomLauncherActionStore(context.applicationContext)
    private val assistantStore = AssistantAgentStore(context.applicationContext)

    fun createPortablePayload(nowEpochMillis: Long = System.currentTimeMillis()): ByteArray {
        val workspace = workspaceStore.createPortableSnapshot(nowEpochMillis).toString(StandardCharsets.UTF_8)
        val bundle = PortableLauncherBackupBundle(
            createdAtEpochMillis = nowEpochMillis,
            workspacePayload = workspace,
            launcherSettingsPayload = launcherSettingsStore.exportPortable(),
            scopedSettingsPayload = scopedSettingsStore.exportPortable(),
            customActionsPayload = customActionsStore.exportPortable(),
            assistantPreferencesPayload = AssistantPortablePreferencesCodec.encode(assistantStore.load()),
        )
        return PortableLauncherBackupBundleCodec.encode(bundle)
    }

    fun inspect(payload: ByteArray): PortableLauncherBackupInspection {
        val bundle = PortableLauncherBackupBundleCodec.decodeOrNull(payload)
        if (bundle == null) {
            return PortableLauncherBackupInspection(
                workspacePreview = workspaceStore.previewPortableSnapshot(payload),
                diff = PortableLauncherBackupDiff(
                    legacyWorkspaceOnly = true,
                    sections = listOf(PortableBackupSectionDiff("workspace", present = true, changed = true)),
                ),
            )
        }

        val workspaceBytes = bundle.workspacePayload.toByteArray(StandardCharsets.UTF_8)
        val workspacePreview = workspaceStore.previewPortableSnapshot(workspaceBytes)
        launcherSettingsStore.validateImport(bundle.launcherSettingsPayload).getOrThrow()
        scopedSettingsStore.validateImport(bundle.scopedSettingsPayload).getOrThrow()
        customActionsStore.validateImport(bundle.customActionsPayload).getOrThrow()
        AssistantPortablePreferencesCodec.decode(bundle.assistantPreferencesPayload)

        val currentWorkspace = workspaceStore.createPortableSnapshot(bundle.createdAtEpochMillis)
            .toString(StandardCharsets.UTF_8)
        val currentAssistant = AssistantPortablePreferencesCodec.encode(assistantStore.load())
        return PortableLauncherBackupInspection(
            workspacePreview = workspacePreview,
            diff = PortableLauncherBackupDiff(
                legacyWorkspaceOnly = false,
                sections = listOf(
                    PortableBackupSectionDiff("workspace", true, bundle.workspacePayload != currentWorkspace),
                    PortableBackupSectionDiff(
                        "launcherSettings",
                        true,
                        bundle.launcherSettingsPayload != launcherSettingsStore.exportPortable(),
                    ),
                    PortableBackupSectionDiff(
                        "scopedSettings",
                        true,
                        bundle.scopedSettingsPayload != scopedSettingsStore.exportPortable(),
                    ),
                    PortableBackupSectionDiff(
                        "customActions",
                        true,
                        bundle.customActionsPayload != customActionsStore.exportPortable(),
                    ),
                    PortableBackupSectionDiff("assistant", true, bundle.assistantPreferencesPayload != currentAssistant),
                ),
            ),
        )
    }

    fun restore(payload: ByteArray): PortableLauncherBackupInspection {
        val inspection = inspect(payload)
        val bundle = PortableLauncherBackupBundleCodec.decodeOrNull(payload)
        if (bundle == null) {
            workspaceStore.restorePortableSnapshot(payload)
            return inspection
        }

        // Local rollback snapshots are captured only after the full incoming bundle has validated.
        val oldWorkspace = workspaceStore.createPortableSnapshot()
        val oldSettings = launcherSettingsStore.exportPortable()
        val oldScoped = scopedSettingsStore.exportPortable()
        val oldActions = customActionsStore.exportPortable()

        try {
            workspaceStore.restorePortableSnapshot(bundle.workspacePayload.toByteArray(StandardCharsets.UTF_8))
            launcherSettingsStore.applyImport(bundle.launcherSettingsPayload).getOrThrow()
            scopedSettingsStore.applyImport(
                payload = bundle.scopedSettingsPayload,
                workspace = workspaceStore.loadWorkspaceDocument(),
            ).getOrThrow()
            customActionsStore.applyImport(bundle.customActionsPayload).getOrThrow()
            val assistant = AssistantPortablePreferencesCodec.decode(bundle.assistantPreferencesPayload)
            check(assistantStore.restorePortable(assistant)) { "Assistant preferences restore could not be committed" }
        } catch (failure: Throwable) {
            rollback(
                workspacePayload = oldWorkspace,
                settingsPayload = oldSettings,
                scopedPayload = oldScoped,
                actionsPayload = oldActions,
                originalFailure = failure,
            )
            throw failure
        } finally {
            oldWorkspace.fill(0)
        }
        return inspection
    }

    private fun rollback(
        workspacePayload: ByteArray,
        settingsPayload: String,
        scopedPayload: String,
        actionsPayload: String,
        originalFailure: Throwable,
    ) {
        val failures = mutableListOf<Throwable>()
        runCatching { workspaceStore.restorePortableSnapshot(workspacePayload) }.exceptionOrNull()?.let(failures::add)
        runCatching { launcherSettingsStore.applyImport(settingsPayload).getOrThrow() }.exceptionOrNull()?.let(failures::add)
        runCatching {
            scopedSettingsStore.applyImport(scopedPayload, workspaceStore.loadWorkspaceDocument()).getOrThrow()
        }.exceptionOrNull()?.let(failures::add)
        runCatching { customActionsStore.applyImport(actionsPayload).getOrThrow() }.exceptionOrNull()?.let(failures::add)
        failures.forEach(originalFailure::addSuppressed)
    }
}

data class PortableLauncherBackupInspection(
    val workspacePreview: BackupPreview,
    val diff: PortableLauncherBackupDiff,
)
