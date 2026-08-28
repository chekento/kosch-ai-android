package cloud.kosch.aiandroid.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/** Visible bridge from the legacy Widget Board into the device-local stack manager. */
@Composable
fun WidgetStackEntryButton(onClick: () -> Unit) {
    val density = LocalDensity.current
    val offset = with(density) {
        IntOffset(
            x = (-18).dp.roundToPx(),
            y = (-94).dp.roundToPx(),
        )
    }
    Popup(
        alignment = Alignment.BottomEnd,
        offset = offset,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            clippingEnabled = true,
        ),
    ) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            icon = { Icon(Icons.Rounded.Layers, contentDescription = null) },
            text = { Text("Stacks") },
        )
    }
}
