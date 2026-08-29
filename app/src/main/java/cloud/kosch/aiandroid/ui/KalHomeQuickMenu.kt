package cloud.kosch.aiandroid.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.ui.theme.DeepSurface

/** One quiet launcher entry replaces the former vertical stack of four permanent Home buttons. */
@Composable
fun KalHomeQuickMenu(
    onSearch: () -> Unit,
    onAiHub: () -> Unit,
    onPersonalize: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 8.dp, end = 10.dp),
    ) {
        Surface(
            color = DeepSurface.copy(alpha = 0.74f),
            shape = RoundedCornerShape(18.dp),
            shadowElevation = 8.dp,
        ) {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    Icons.Rounded.MoreHoriz,
                    contentDescription = "KAL Menü",
                    tint = Color.White,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = DeepSurface.copy(alpha = 0.98f),
        ) {
            DropdownMenuItem(
                text = { Text("Suche", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                onClick = {
                    expanded = false
                    onSearch()
                },
            )
            DropdownMenuItem(
                text = { Text("AI Hub", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                onClick = {
                    expanded = false
                    onAiHub()
                },
            )
            DropdownMenuItem(
                text = { Text("Anpassen", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(Icons.Rounded.Palette, contentDescription = null) },
                onClick = {
                    expanded = false
                    onPersonalize()
                },
            )
            DropdownMenuItem(
                text = { Text("Einstellungen", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                onClick = {
                    expanded = false
                    onSettings()
                },
            )
        }
    }
}
