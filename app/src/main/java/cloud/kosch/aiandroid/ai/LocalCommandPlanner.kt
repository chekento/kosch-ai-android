package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.SceneId
import java.text.Normalizer
import java.util.Locale

sealed interface LauncherCommand {
    data object Empty : LauncherCommand
    data object OpenDrawer : LauncherCommand
    data object StartVoice : LauncherCommand
    data class SwitchScene(val scene: SceneId) : LauncherCommand
    data class LaunchApp(val query: String) : LauncherCommand
    data class RoutePrompt(val prompt: String) : LauncherCommand
}

class LocalCommandPlanner {
    fun plan(input: String): LauncherCommand {
        val raw = input.trim()
        if (raw.isEmpty()) return LauncherCommand.Empty

        val normalized = raw.normalized()
        if (normalized in drawerCommands) return LauncherCommand.OpenDrawer
        if (normalized in voiceCommands) return LauncherCommand.StartVoice

        sceneFrom(normalized)?.let { return LauncherCommand.SwitchScene(it) }

        launchPrefixes.firstOrNull { normalized.startsWith(it) }?.let { prefix ->
            val query = raw.drop(prefix.length).trim()
            if (query.isNotEmpty()) return LauncherCommand.LaunchApp(query)
        }

        return LauncherCommand.RoutePrompt(raw)
    }

    private fun sceneFrom(value: String): SceneId? {
        val withoutPrefix = scenePrefixes.fold(value) { result, prefix ->
            result.removePrefix(prefix).trim()
        }
        return SceneId.entries.firstOrNull { scene ->
            withoutPrefix == scene.name.lowercase(Locale.ROOT) ||
                withoutPrefix == scene.title.normalized()
        }
    }

    private fun String.normalized(): String = Normalizer
        .normalize(lowercase(Locale.GERMAN), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .replace("\\s+".toRegex(), " ")
        .trim()

    private companion object {
        val drawerCommands = setOf(
            "apps",
            "alle apps",
            "app drawer",
            "app-drawer",
            "zeige apps",
        )
        val voiceCommands = setOf(
            "voice",
            "sprache",
            "zuhoren",
            "hor zu",
            "listen",
        )
        val scenePrefixes = listOf(
            "szene ",
            "scene ",
            "offne szene ",
            "wechsle zu ",
        )
        val launchPrefixes = listOf(
            "öffne ",
            "offne ",
            "starte ",
            "open ",
            "launch ",
        )
    }
}

