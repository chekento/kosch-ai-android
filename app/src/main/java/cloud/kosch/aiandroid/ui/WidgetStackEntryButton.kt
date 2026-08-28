package cloud.kosch.aiandroid.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Visible bridge from the legacy Widget Board into the device-local stack manager. */
@Composable
fun WidgetStackEntryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        icon = { Icon(Icons.Rounded.Layers, contentDescription = null) },
        text = { Text("Stacks") },
    )
}
