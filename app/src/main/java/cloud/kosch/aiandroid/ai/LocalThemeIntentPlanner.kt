package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.AppearanceSettings
import cloud.kosch.aiandroid.model.HomeSettings
import cloud.kosch.aiandroid.model.MotionProfile
import cloud.kosch.aiandroid.model.ThemeMode
import cloud.kosch.aiandroid.model.ThemeSettings
import cloud.kosch.aiandroid.model.WallpaperMode
import java.util.Locale

/**
 * API-free semantic theme copilot.
 *
 * This planner intentionally handles a useful, deterministic vocabulary locally instead of pretending to be a
 * generative cloud model. It can be used for instant previews; richer requests can later be handed to an installed
 * LLM app with an explicit context preview.
 */
data class LocalLauncherStyleProposal(
    val appearance: AppearanceSettings,
    val home: HomeSettings,
    val theme: ThemeSettings,
    val matchedSignals: Int,
    val rationale: List<String>,
) {
    val actionable: Boolean get() = matchedSignals > 0
}

object LocalThemeIntentPlanner {
    fun propose(
        prompt: String,
        appearance: AppearanceSettings = AppearanceSettings(),
        home: HomeSettings = HomeSettings(),
        theme: ThemeSettings = ThemeSettings(),
    ): LocalLauncherStyleProposal {
        val text = prompt.trim().lowercase(Locale.ROOT)
        var nextAppearance = appearance
        var nextHome = home
        var nextTheme = theme
        val reasons = mutableListOf<String>()
        var signals = 0

        fun signal(reason: String) {
            signals += 1
            reasons += reason
        }

        when {
            containsAny(text, "dunkel", "dark", "nacht", "night") -> {
                nextAppearance = nextAppearance.copy(mode = ThemeMode.DARK)
                signal("Dunkle Darstellung")
            }
            containsAny(text, "hell", "light", "bright") -> {
                nextAppearance = nextAppearance.copy(mode = ThemeMode.LIGHT)
                signal("Helle Darstellung")
            }
            containsAny(text, "system theme", "systemdesign", "systemmodus") -> {
                nextAppearance = nextAppearance.copy(mode = ThemeMode.SYSTEM)
                signal("Systemdarstellung folgen")
            }
        }

        if (containsAny(text, "neural glass", "glass", "glas", "gläsern")) {
            nextTheme = nextTheme.copy(activeThemeId = "neural-glass")
            nextAppearance = nextAppearance.copy(blurStrength = 0.68f, surfaceOpacity = 0.86f, cornerScale = 1.12f)
            signal("Neural-Glass-Tiefe")
        }

        if (containsAny(text, "minimal", "clean", "sauber", "schlicht")) {
            nextAppearance = nextAppearance.copy(blurStrength = 0.18f, surfaceOpacity = 0.98f, cornerScale = 0.86f)
            nextHome = nextHome.copy(horizontalGapDp = 5, verticalGapDp = 5)
            signal("Reduzierte, klare Oberflächen")
        }

        if (containsAny(text, "kinoreif", "cinematic", "lebendig", "expressive", "dynamisch")) {
            nextAppearance = nextAppearance.copy(
                motionProfile = MotionProfile.EXPRESSIVE,
                blurStrength = maxOf(nextAppearance.blurStrength, 0.58f),
                cornerScale = maxOf(nextAppearance.cornerScale, 1.08f),
            )
            signal("Ausdrucksstarke Bewegung und Tiefe")
        }

        if (containsAny(text, "ruhig", "calm", "weniger bewegung", "reduced motion")) {
            nextAppearance = nextAppearance.copy(motionProfile = MotionProfile.REDUCED)
            signal("Reduzierte Bewegung")
        }

        if (containsAny(text, "keine animation", "ohne animation", "no animation", "motion off")) {
            nextAppearance = nextAppearance.copy(motionProfile = MotionProfile.OFF)
            signal("Animationen deaktiviert")
        }

        if (containsAny(text, "kompakt", "compact", "dicht")) {
            nextAppearance = nextAppearance.copy(contentScale = 0.92f)
            nextHome = nextHome.copy(horizontalGapDp = 3, verticalGapDp = 3, iconScale = 0.92f)
            signal("Kompakter Informationsraum")
        }

        if (containsAny(text, "groß", "large", "größer", "spacious", "luftig")) {
            nextAppearance = nextAppearance.copy(contentScale = 1.12f)
            nextHome = nextHome.copy(horizontalGapDp = 9, verticalGapDp = 9, iconScale = 1.08f)
            signal("Größere, luftigere Bedienelemente")
        }

        if (containsAny(text, "hoher kontrast", "high contrast", "maximaler kontrast")) {
            nextAppearance = nextAppearance.copy(surfaceOpacity = 1f, blurStrength = minOf(nextAppearance.blurStrength, 0.2f))
            signal("Kontrastpriorisierte Oberflächen")
        }

        if (containsAny(text, "material you", "systemfarben", "dynamic color", "dynamische farben")) {
            nextAppearance = nextAppearance.copy(useMaterialYouAccents = true)
            signal("Material-You-Akzente")
        }

        if (containsAny(text, "ohne material you", "keine systemfarben", "no dynamic color")) {
            nextAppearance = nextAppearance.copy(useMaterialYouAccents = false)
            signal("Theme-eigene Akzente")
        }

        when {
            containsAny(text, "system wallpaper", "systemhintergrund", "system hintergrund") -> {
                nextAppearance = nextAppearance.copy(wallpaperMode = WallpaperMode.SYSTEM)
                signal("System-Wallpaper")
            }
            containsAny(text, "theme wallpaper", "theme-hintergrund", "theme hintergrund") -> {
                nextAppearance = nextAppearance.copy(wallpaperMode = WallpaperMode.THEME)
                signal("Theme-Hintergrund")
            }
            containsAny(text, "seitenhintergrund", "page wallpaper", "pro seite") -> {
                nextAppearance = nextAppearance.copy(wallpaperMode = WallpaperMode.PAGE_SPECIFIC)
                signal("Seitenspezifischer Hintergrund")
            }
            containsAny(text, "einfarbig", "solid background", "solid wallpaper") -> {
                nextAppearance = nextAppearance.copy(wallpaperMode = WallpaperMode.SOLID)
                signal("Einfarbiger Hintergrundmodus")
            }
        }

        return LocalLauncherStyleProposal(
            appearance = nextAppearance.normalized(),
            home = nextHome.normalized(),
            theme = nextTheme,
            matchedSignals = signals,
            rationale = reasons.distinct(),
        )
    }

    private fun containsAny(text: String, vararg needles: String): Boolean = needles.any(text::contains)
}
