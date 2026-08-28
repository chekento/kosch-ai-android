package cloud.kosch.aiandroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.model.AdaptiveHomePresentation
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.Sky

/**
 * Presentation-only quick-action rail for expanded/tall Unified Home windows.
 *
 * The parent decides when the rail is allowed from the adaptive policy. This surface never changes workspace
 * content or device state on its own; every action is an explicit user click routed through an existing controller.
 */
@Composable
fun AdaptiveEdgePowerRail(
    presentation: AdaptiveHomePresentation,
    onOpenApps: () -> Unit,
    onAsk: () -> Unit,
    onControls: () -> Unit,
    onPen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!presentation.showEdgePowerRail) return

    Surface(
        modifier = modifier,
        color = DeepSurface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onOpenApps) {
                Icon(Icons.Rounded.Apps, contentDescription = "Power Rail · Alle Apps")
            }
            IconButton(
                onClick = onAsk,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Mint.copy(alpha = 0.14f)),
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = "Power Rail · Ask", tint = Mint)
            }
            IconButton(onClick = onControls) {
                Icon(Icons.Rounded.Tune, contentDescription = "Power Rail · Kontrollzentrum", tint = Sky)
            }
            if (presentation.showPenShortcut) {
                IconButton(
                    onClick = onPen,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (presentation.emphasizePenShortcut) Mint.copy(alpha = 0.16f) else Color.Transparent,
                        ),
                ) {
                    Icon(
                        Icons.Rounded.Draw,
                        contentDescription = if (presentation.emphasizePenShortcut) {
                            "Power Rail · Pen Space · für Stift priorisiert"
                        } else {
                            "Power Rail · Pen Space"
                        },
                        tint = if (presentation.emphasizePenShortcut) Mint else Sky,
                    )
                }
            }
        }
    }
}
