package cloud.kosch.aiandroid.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cloud.kosch.aiandroid.model.ThemeMode

val Ink = Color(0xFF071018)
val DeepSurface = Color(0xFF0E1A24)
val RaisedSurface = Color(0xFF162733)
val Mist = Color(0xFFF4FBFF)
val MutedMist = Color(0xFFC7D7DE)
val Mint = Color(0xFF69E6D7)
val Sky = Color(0xFF80BFFF)
val Violet = Color(0xFFB7A7FF)
val Warm = Color(0xFFFFC979)

private val LauncherDarkColorScheme = darkColorScheme(
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
    outline = Color(0xFF718994),
    error = Color(0xFFFFB4AB),
)

private val LauncherLightColorScheme = lightColorScheme(
    primary = Color(0xFF006B61),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9EF2E7),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF17618E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCBE6FF),
    onSecondaryContainer = Color(0xFF001E30),
    tertiary = Color(0xFF66558E),
    onTertiary = Color.White,
    background = Color(0xFFF6FAFC),
    onBackground = Color(0xFF172126),
    surface = Color(0xFFFDFEFF),
    onSurface = Color(0xFF172126),
    surfaceVariant = Color(0xFFE1EAEE),
    onSurfaceVariant = Color(0xFF3F494D),
    outline = Color(0xFF6F797D),
    error = Color(0xFFBA1A1A),
)

/**
 * KAL is primarily read at launcher distance. The baseline therefore deliberately uses larger labels, body text and
 * line heights than stock Material defaults. Compact expert surfaces can still opt into smaller styles explicitly,
 * while the normal Home, Drawer, Assistant and Settings remain readable without a magnifier.
 */
private val KalTypography = Typography(
    displayLarge = TextStyle(fontSize = 50.sp, lineHeight = 56.sp, fontWeight = FontWeight.Light),
    displayMedium = TextStyle(fontSize = 42.sp, lineHeight = 48.sp, fontWeight = FontWeight.Light),
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Normal),
    headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 25.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun KoSchLauncherTheme(
    dynamicColor: Boolean = false,
    mode: ThemeMode = ThemeMode.THEME_DEFINED,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK,
        ThemeMode.THEME_DEFINED -> true
    }
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> LauncherDarkColorScheme
        else -> LauncherLightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = KalTypography,
        content = content,
    )
}
