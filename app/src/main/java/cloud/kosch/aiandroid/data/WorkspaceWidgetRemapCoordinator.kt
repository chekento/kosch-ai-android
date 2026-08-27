package cloud.kosch.aiandroid.data

import android.content.Context
import cloud.kosch.aiandroid.model.DeviceWidgetBinding
import cloud.kosch.aiandroid.model.WorkspaceWidgetEditor

/** Result of committing a replacement provider + device-local host binding for one existing V7 widget. */
data class WorkspaceWidgetRemapCommit(
    val committed: Boolean,
    val releasedAppWidgetId: Int? = null,
)

/**
 * Small transaction coordinator for restoring/remapping an existing V7 widget.
 *
 * Portable provider identity is committed first. The device-local binding is then atomically replaced. If the
 * second commit fails, the safe failure state is a provider-known but unbound placeholder; no wrong host id is
 * accepted as valid. The caller owns releasing the returned old host id only after a successful binding commit.
 */
class WorkspaceWidgetRemapCoordinator(context: Context) {
    private val workspaceStore = WorkspaceStore(context.applicationContext)
    private val bindingStore = WorkspaceWidgetBindingStore(context.applicationContext)

    fun remap(
        workspaceItemId: String,
        appWidgetId: Int,
        providerComponent: String,
    ): WorkspaceWidgetRemapCommit {
        if (appWidgetId <= 0) return WorkspaceWidgetRemapCommit(committed = false)
        val current = workspaceStore.loadWorkspaceDocument().normalized()
        val updated = runCatching {
            WorkspaceWidgetEditor.remapProvider(
                document = current,
                itemId = workspaceItemId,
                providerComponent = providerComponent,
            )
        }.getOrElse { return WorkspaceWidgetRemapCommit(committed = false) }

        if (!workspaceStore.saveWorkspaceDocument(updated)) {
            return WorkspaceWidgetRemapCommit(committed = false)
        }

        val bindingResult = bindingStore.replace(DeviceWidgetBinding(workspaceItemId, appWidgetId))
        return WorkspaceWidgetRemapCommit(
            committed = bindingResult.committed,
            releasedAppWidgetId = bindingResult.releasedAppWidgetId,
        )
    }
}
