package cloud.kosch.aiandroid.data

import android.content.Context
import cloud.kosch.aiandroid.model.WorkspaceItemContent
import cloud.kosch.aiandroid.model.WorkspacePage
import cloud.kosch.aiandroid.system.WidgetHostController
import cloud.kosch.aiandroid.system.WidgetHostOwnership

/**
 * Startup reconciliation for V7 AppWidgetHost ids that can outlive the launcher process.
 *
 * This runs only from the real launcher startup path, never from the temporary picker Activity, so a picker id
 * being restored by Android is not reclaimed underneath the picker. Portable workspace content remains the source
 * of expected provider identity; the device-local binding store remains the only ownership record for host ids.
 */
class WorkspaceWidgetHostRecovery(context: Context) {
    private val appContext = context.applicationContext
    private val workspaceStore = WorkspaceStore(appContext)
    private val bindingStore = WorkspaceWidgetBindingStore(appContext)

    fun reconcile(): Set<Int> {
        val document = workspaceStore.loadWorkspaceDocument().normalized()
        val expectedProviders = document.pages
            .flatMap(WorkspacePage::items)
            .mapNotNull { item ->
                val widget = item.content as? WorkspaceItemContent.Widget ?: return@mapNotNull null
                val provider = widget.providerComponent ?: return@mapNotNull null
                item.id to provider
            }
            .toMap()
        val storedBindings = bindingStore.load()
        val host = WidgetHostController(appContext, WidgetHostController.WORKSPACE_HOST_ID)
        val hostedProviders = host.hostedProviderComponents()
        val plan = plan(expectedProviders, storedBindings, hostedProviders)

        val releasedBindings = bindingStore.prune(plan.validBindings)
        val released = releasedBindings + plan.orphanedHostedIds
        released.forEach(host::deleteId)
        return released
    }

    companion object {
        internal fun plan(
            expectedProviders: Map<String, String>,
            storedBindings: Map<String, Int>,
            hostedProviders: Map<Int, String?>,
        ): RecoveryPlan {
            val validBindings = storedBindings.filter { (itemId, appWidgetId) ->
                val expected = expectedProviders[itemId] ?: return@filter false
                val actual = hostedProviders[appWidgetId] ?: return@filter false
                expected == actual
            }
            val orphanedHostedIds = WidgetHostOwnership.orphanedIds(
                hostedIds = hostedProviders.keys,
                ownedIds = validBindings.values.toSet(),
            )
            return RecoveryPlan(validBindings, orphanedHostedIds)
        }
    }

    internal data class RecoveryPlan(
        val validBindings: Map<String, Int>,
        val orphanedHostedIds: Set<Int>,
    )
}
