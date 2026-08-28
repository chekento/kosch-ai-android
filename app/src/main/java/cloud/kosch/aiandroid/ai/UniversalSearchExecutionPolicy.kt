package cloud.kosch.aiandroid.ai

/**
 * Pure execution boundary for Universal Search.
 *
 * Search results never carry Android Intents, raw URIs or permission decisions. This policy converts an indexed target
 * into a stable launcher-owned route. The runtime must resolve the id against current state again before acting.
 */
sealed interface UniversalSearchExecutionPlan {
    data class LaunchApp(val appKey: String) : UniversalSearchExecutionPlan
    data class LaunchShortcut(val appKey: String, val shortcutId: String) : UniversalSearchExecutionPlan
    data class OpenFolder(val folderId: String) : UniversalSearchExecutionPlan
    data class ActivatePage(val pageId: String) : UniversalSearchExecutionPlan
    data class OpenSetting(val featureId: String) : UniversalSearchExecutionPlan
    data class ExecuteCustomAction(val actionId: String) : UniversalSearchExecutionPlan
    data class OpenAiRoute(val routeId: String) : UniversalSearchExecutionPlan
}

object UniversalSearchExecutionPolicy {
    fun plan(result: UniversalQueryResult.Entity): UniversalSearchExecutionPlan = when (val target = result.ranked.entry.target) {
        is UniversalSearchTarget.App -> UniversalSearchExecutionPlan.LaunchApp(target.appKey)
        is UniversalSearchTarget.AppShortcut -> UniversalSearchExecutionPlan.LaunchShortcut(
            appKey = target.appKey,
            shortcutId = target.shortcutId,
        )
        is UniversalSearchTarget.Folder -> UniversalSearchExecutionPlan.OpenFolder(target.folderId)
        is UniversalSearchTarget.Page -> UniversalSearchExecutionPlan.ActivatePage(target.pageId)
        is UniversalSearchTarget.Setting -> UniversalSearchExecutionPlan.OpenSetting(target.featureId)
        is UniversalSearchTarget.CustomAction -> UniversalSearchExecutionPlan.ExecuteCustomAction(target.actionId)
        is UniversalSearchTarget.AiRoute -> UniversalSearchExecutionPlan.OpenAiRoute(target.routeId)
    }
}
