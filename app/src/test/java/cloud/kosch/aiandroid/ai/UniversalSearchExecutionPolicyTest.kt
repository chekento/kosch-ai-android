package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class UniversalSearchExecutionPolicyTest {
    @Test
    fun `every indexed target becomes stable typed launcher route`() {
        val cases = listOf(
            UniversalSearchTarget.App("app:1") to UniversalSearchExecutionPlan.LaunchApp("app:1"),
            UniversalSearchTarget.AppShortcut("app:1", "shortcut:compose") to
                UniversalSearchExecutionPlan.LaunchShortcut("app:1", "shortcut:compose"),
            UniversalSearchTarget.Folder("folder:work") to UniversalSearchExecutionPlan.OpenFolder("folder:work"),
            UniversalSearchTarget.Page("page:studio") to UniversalSearchExecutionPlan.ActivatePage("page:studio"),
            UniversalSearchTarget.Setting("privacy.local") to UniversalSearchExecutionPlan.OpenSetting("privacy.local"),
            UniversalSearchTarget.CustomAction("action.dashboard") to
                UniversalSearchExecutionPlan.ExecuteCustomAction("action.dashboard"),
            UniversalSearchTarget.AiRoute("research") to UniversalSearchExecutionPlan.OpenAiRoute("research"),
        )

        cases.forEachIndexed { index, (target, expected) ->
            val result = entity(target, "entry:$index")
            assertEquals(expected, UniversalSearchExecutionPolicy.plan(result))
        }
    }

    @Test
    fun `execution plans contain no intent or uri payload`() {
        val plans = listOf(
            UniversalSearchExecutionPolicy.plan(entity(UniversalSearchTarget.App("stable-app"), "app")),
            UniversalSearchExecutionPolicy.plan(entity(UniversalSearchTarget.CustomAction("stable-action"), "action")),
            UniversalSearchExecutionPolicy.plan(entity(UniversalSearchTarget.AiRoute("smart"), "ai")),
        )

        plans.forEach { plan ->
            val serializedShape = plan.toString().lowercase()
            assertFalse(serializedShape.contains("intent:"))
            assertFalse(serializedShape.contains("https://"))
            assertFalse(serializedShape.contains("content://"))
            assertFalse(serializedShape.contains("file://"))
        }
    }

    private fun entity(target: UniversalSearchTarget, id: String): UniversalQueryResult.Entity =
        UniversalQueryResult.Entity(
            RankedUniversalSearchEntry(
                entry = UniversalSearchEntry(
                    id = id,
                    kind = when (target) {
                        is UniversalSearchTarget.App -> UniversalSearchKind.APP
                        is UniversalSearchTarget.AppShortcut -> UniversalSearchKind.APP_SHORTCUT
                        is UniversalSearchTarget.Folder -> UniversalSearchKind.FOLDER
                        is UniversalSearchTarget.Page -> UniversalSearchKind.PAGE
                        is UniversalSearchTarget.Setting -> UniversalSearchKind.SETTING
                        is UniversalSearchTarget.CustomAction -> UniversalSearchKind.CUSTOM_ACTION
                        is UniversalSearchTarget.AiRoute -> UniversalSearchKind.AI_ROUTE
                    },
                    title = id,
                    target = target,
                ),
                score = 1_000,
                reason = SearchMatchReason.EXACT,
            ),
        )
}
