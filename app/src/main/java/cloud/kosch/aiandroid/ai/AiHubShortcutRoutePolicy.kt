package cloud.kosch.aiandroid.ai

/**
 * Selects only semantically explicit Android shortcuts for a task family.
 *
 * This policy never upgrades a generic shortcut into a capability. SOURCE_NOTEBOOK deliberately has no shortcut
 * fallback until another app publishes a surface that KoSch can classify that specifically.
 */
object AiHubShortcutRoutePolicy {
    fun priority(intent: AiHubTaskIntent, kind: AiPublishedShortcutKind): Int = when (intent) {
        AiHubTaskIntent.GENERAL_CHAT -> when (kind) {
            AiPublishedShortcutKind.NEW_CHAT -> 100
            AiPublishedShortcutKind.AI_ASSISTANT -> 90
            else -> 0
        }

        AiHubTaskIntent.RESEARCH -> when (kind) {
            AiPublishedShortcutKind.RESEARCH -> 100
            else -> 0
        }

        AiHubTaskIntent.BROWSER_PAGE -> when (kind) {
            AiPublishedShortcutKind.AI_ASSISTANT -> 100
            else -> 0
        }

        AiHubTaskIntent.LOCAL_PRIVATE -> when (kind) {
            AiPublishedShortcutKind.NEW_CHAT -> 100
            else -> 0
        }

        AiHubTaskIntent.VOICE -> when (kind) {
            AiPublishedShortcutKind.VOICE -> 100
            else -> 0
        }

        AiHubTaskIntent.IMAGE -> when (kind) {
            AiPublishedShortcutKind.IMAGE -> 100
            else -> 0
        }

        AiHubTaskIntent.SOURCE_NOTEBOOK -> 0
    }

    fun preferredKind(
        intent: AiHubTaskIntent,
        availableKinds: Collection<AiPublishedShortcutKind>,
    ): AiPublishedShortcutKind? = availableKinds
        .distinct()
        .map { it to priority(intent, it) }
        .filter { (_, priority) -> priority > 0 }
        .maxWithOrNull(
            compareBy<Pair<AiPublishedShortcutKind, Int>> { it.second }
                .thenByDescending { it.first.ordinal },
        )
        ?.first
}
