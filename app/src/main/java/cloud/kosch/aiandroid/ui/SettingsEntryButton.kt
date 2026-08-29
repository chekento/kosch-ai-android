package cloud.kosch.aiandroid.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Mint

/** Dedicated launcher-level entry; Settings is intentionally separate from the System/Security control center. */
@Composable
fun SettingsEntryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(48.dp),
        color = DeepSurface.copy(alpha = 0.94f),
        shape = CircleShape,
        tonalElevation = 8.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            IconButton(onClick = onClick) {
                Icon(Icons.Rounded.Settings, contentDescription = "Launcher-Einstellungen", tint = Mint)
            }
        }
    }
}
