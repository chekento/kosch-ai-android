package cloud.kosch.aiandroid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF071018)
val DeepSurface = Color(0xFF0E1A24)
val RaisedSurface = Color(0xFF162733)
val Mist = Color(0xFFD8F3F0)
val MutedMist = Color(0xFF9DB7B5)
val Mint = Color(0xFF69E6D7)
val Sky = Color(0xFF80BFFF)
val Violet = Color(0xFFB7A7FF)
val Warm = Color(0xFFFFC979)

private val LauncherColorScheme = darkColorScheme(
    primary = Mint,
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF155248),
    onPrimaryContainer = Color(0xFFB6FFF5),
    secondary = Sky,
    onSecondary = Color(0xFF003258),
    secondaryContainer = Color(0xFF164B73),
    onSecondaryContainer = Color(0xFFD0E9FF),
    tertiary = Violet,
    onTertiary = Color(0xFF30245F),
    background = Ink,
    onBackground = Mist,
    surface = DeepSurface,
    onSurface = Mist,
    surfaceVariant = RaisedSurface,
    onSurfaceVariant = MutedMist,
    outline = Color(0xFF667B7A),
    error = Color(0xFFFFB4AB),
)

@Composable
fun KoSchLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LauncherColorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}

