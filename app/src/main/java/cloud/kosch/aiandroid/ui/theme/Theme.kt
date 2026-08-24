package cloud.kosch.aiandroid.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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
fun KoSchLauncherTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(LocalContext.current)
    } else {
        LauncherColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
