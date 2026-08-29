package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.FolderKind
import cloud.kosch.aiandroid.model.LauncherFolder
import cloud.kosch.aiandroid.model.SceneId
import java.util.Locale

data class SmartAppDescriptor(
    val key: String,
    val label: String,
    val packageName: String,
)

enum class SmartDockReason(val title: String) {
    PINNED("Angeheftet"),
    RECENT("Kürzlich genutzt"),
    LEARNED("Lokal gelernt"),
    SCENE("Passt zur Szene"),
    AVAILABLE("Verfügbar"),
}

data class SmartDockSuggestion(
    val app: SmartAppDescriptor,
    val score: Int,
    val primaryReason: SmartDockReason,
    val reasons: Set<SmartDockReason>,
)

/**
 * A deterministic, offline organizer used before an optional LLM is installed.
 * It only sees launcher labels, package names and local recency/usage ordering supplied by the caller.
 */
object LocalSmartOrganizer {
    fun proposeFolders(apps: List<SmartAppDescriptor>): List<LauncherFolder> {
        val grouped = apps.distinctBy(SmartAppDescriptor::key).groupBy(::kindFor)
        val preferredOrder = listOf(
            FolderKind.COMMUNICATION,
            FolderKind.WORK,
            FolderKind.MEDIA,
            FolderKind.TOOLS,
            FolderKind.AI,
            FolderKind.OTHER,
        )
        return preferredOrder.mapNotNull { kind ->
            val matches = grouped[kind].orEmpty()
                .sortedBy { it.label.lowercase(Locale.ROOT) }
                .take(MAX_FOLDER_APPS)
            if (matches.isEmpty()) return@mapNotNull null
            LauncherFolder(
                id = "local-${kind.name.lowercase(Locale.ROOT)}",
                title = kind.title,
                kind = kind,
                appKeys = matches.map(SmartAppDescriptor::key),
            )
        }
    }

    fun bestFolderKind(app: SmartAppDescriptor): FolderKind = kindFor(app)

    fun smartDockKeys(
        apps: List<SmartAppDescriptor>,
        pinnedKeys: List<String>,
        recentPackages: List<String>,
        usageKeys: List<String> = emptyList(),
        scene: SceneId,
        limit: Int = DEFAULT_DOCK_SIZE,
    ): List<String> = smartDockSuggestions(
        apps = apps,
        pinnedKeys = pinnedKeys,
        recentPackages = recentPackages,
        usageKeys = usageKeys,
        scene = scene,
        limit = limit,
    ).map { it.app.key }

    /**
     * Explainable Smart Dock ranking. Pinned order is absolute; all other signals are bounded additive hints.
     * No app content, notification text, contacts, prompts or network activity is inspected here.
     */
    fun smartDockSuggestions(
        apps: List<SmartAppDescriptor>,
        pinnedKeys: List<String>,
        recentPackages: List<String>,
        usageKeys: List<String> = emptyList(),
        scene: SceneId,
        limit: Int = DEFAULT_DOCK_SIZE,
    ): List<SmartDockSuggestion> {
        if (limit <= 0) return emptyList()
        val uniqueApps = apps.distinctBy(SmartAppDescriptor::key)
        val byKey = uniqueApps.associateBy(SmartAppDescriptor::key)
        val pinIndex = pinnedKeys
            .filter(byKey::containsKey)
            .distinct()
            .withIndex()
            .associate { it.value to it.index }
        val recentIndex = recentPackages.distinct().withIndex().associate { it.value to it.index }
        val usageIndex = usageKeys
            .filter(byKey::containsKey)
            .distinct()
            .withIndex()
            .associate { it.value to it.index }
        val preferredKinds = preferredKinds(scene)

        return uniqueApps.map { app ->
            val reasons = linkedSetOf<SmartDockReason>()
            var score = BASE_AVAILABLE_SCORE
            val pinnedAt = pinIndex[app.key]
            val recentAt = recentIndex[app.packageName]
            val learnedAt = usageIndex[app.key]
            val sceneAt = preferredKinds.indexOf(kindFor(app)).takeIf { it >= 0 }

            if (pinnedAt != null) {
                score += PINNED_SCORE - pinnedAt.coerceAtMost(99) * PINNED_POSITION_STEP
                reasons += SmartDockReason.PINNED
            }
            if (recentAt != null) {
                score += RECENT_SCORE - recentAt.coerceAtMost(99) * RECENT_POSITION_STEP
                reasons += SmartDockReason.RECENT
            }
            if (learnedAt != null) {
                score += LEARNED_SCORE - learnedAt.coerceAtMost(99) * LEARNED_POSITION_STEP
                reasons += SmartDockReason.LEARNED
            }
            if (sceneAt != null) {
                score += SCENE_SCORE - sceneAt * SCENE_POSITION_STEP
                reasons += SmartDockReason.SCENE
            }
            if (reasons.isEmpty()) reasons += SmartDockReason.AVAILABLE

            SmartDockSuggestion(
                app = app,
                score = score,
                primaryReason = primaryReason(reasons),
                reasons = reasons,
            )
        }.sortedWith(
            compareByDescending<SmartDockSuggestion> { it.score }
                .thenBy { pinIndex[it.app.key] ?: Int.MAX_VALUE }
                .thenBy { recentIndex[it.app.packageName] ?: Int.MAX_VALUE }
                .thenBy { usageIndex[it.app.key] ?: Int.MAX_VALUE }
                .thenBy { it.app.label.lowercase(Locale.ROOT) },
        ).take(limit)
    }

    private fun primaryReason(reasons: Set<SmartDockReason>): SmartDockReason = when {
        SmartDockReason.PINNED in reasons -> SmartDockReason.PINNED
        SmartDockReason.RECENT in reasons -> SmartDockReason.RECENT
        SmartDockReason.LEARNED in reasons -> SmartDockReason.LEARNED
        SmartDockReason.SCENE in reasons -> SmartDockReason.SCENE
        else -> SmartDockReason.AVAILABLE
    }

    private fun preferredKinds(scene: SceneId): List<FolderKind> = when (scene) {
        SceneId.WORK -> listOf(FolderKind.WORK, FolderKind.TOOLS, FolderKind.COMMUNICATION)
        SceneId.STUDIO -> listOf(FolderKind.MEDIA, FolderKind.AI, FolderKind.TOOLS)
        SceneId.SOCIAL -> listOf(FolderKind.COMMUNICATION, FolderKind.MEDIA, FolderKind.AI)
        SceneId.EVENING -> listOf(FolderKind.MEDIA, FolderKind.COMMUNICATION, FolderKind.OTHER)
        SceneId.AI -> listOf(FolderKind.AI, FolderKind.TOOLS, FolderKind.COMMUNICATION)
    }

    private fun kindFor(app: SmartAppDescriptor): FolderKind {
        val haystack = "${app.label} ${app.packageName}".lowercase(Locale.ROOT)
        return when {
            aiTokens.any(haystack::contains) -> FolderKind.AI
            communicationTokens.any(haystack::contains) -> FolderKind.COMMUNICATION
            workTokens.any(haystack::contains) -> FolderKind.WORK
            mediaTokens.any(haystack::contains) -> FolderKind.MEDIA
            toolTokens.any(haystack::contains) -> FolderKind.TOOLS
            else -> FolderKind.OTHER
        }
    }

    private val aiTokens = setOf(
        "anthropic", "chatgpt", "chatterui", "claude", "gemini", "grok",
        "llama", "maid", "notebooklm", "ollama", "openai", "perplexity", "pocketpal",
        "ki", "artificial intelligence", "local llm",
    )
    private val communicationTokens = setOf(
        "chat", "contact", "kontakt", "discord", "mail", "message", "nachricht", "phone", "telefon",
        "signal", "telegram", "whatsapp", "messenger",
    )
    private val workTokens = setOf(
        "calendar", "kalender", "docs", "drive", "office", "notion", "outlook", "sheets", "slack", "teams",
        "trello", "work", "arbeit", "projekt", "project", "meet",
    )
    private val mediaTokens = setOf(
        "audio", "camera", "kamera", "gallery", "galerie", "music", "musik", "photo", "foto", "spotify",
        "video", "youtube", "podcast",
    )
    private val toolTokens = setOf(
        "authenticator", "calculator", "rechner", "clock", "uhr", "file", "datei", "scanner", "settings",
        "einstellungen", "tool", "werkzeug", "terminal", "vpn",
    )

    private const val BASE_AVAILABLE_SCORE = 100
    private const val PINNED_SCORE = 100_000
    private const val PINNED_POSITION_STEP = 1_000
    private const val RECENT_SCORE = 10_000
    private const val RECENT_POSITION_STEP = 100
    private const val LEARNED_SCORE = 6_000
    private const val LEARNED_POSITION_STEP = 50
    private const val SCENE_SCORE = 3_000
    private const val SCENE_POSITION_STEP = 500
    private const val MAX_FOLDER_APPS = 16
    private const val DEFAULT_DOCK_SIZE = 5
}
