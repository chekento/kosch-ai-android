package cloud.kosch.aiandroid.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cloud.kosch.aiandroid.model.WorkspaceObjectStyle
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.RaisedSurface

/** Current resolved object style for child renderers that need icon/label-specific behavior. */
val LocalWorkspaceObjectStyle = staticCompositionLocalOf { WorkspaceObjectStyle() }

/**
 * Shared visual frame for normal Home and Home Studio previews.
 *
 * Hidden objects disappear on normal Home but remain faintly visible in Home Studio so the user can always recover
 * them. Layout bounds stay untouched: offsets, scale and rotation are presentation-only and never mutate the v7 grid.
 */
@Composable
fun WorkspaceObjectStyleFrame(
    style: WorkspaceObjectStyle,
    editing: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    if (!style.visible && !editing) return

    val background = style.backgroundArgb
        ?.let(::Color)
        ?.copy(alpha = style.backgroundAlpha)
        ?: RaisedSurface.copy(alpha = 0.96f * style.backgroundAlpha)
    val border = style.borderArgb?.let(::Color) ?: Mint.copy(alpha = 0.26f)

    Surface(
        modifier = modifier
            .offset(x = style.offsetXDp.dp, y = style.offsetYDp.dp)
            .zIndex(style.zIndex)
            .rotate(style.rotationDegrees)
            .scale(style.contentScale)
            .alpha(if (style.visible) style.opacity else 0.22f),
        color = background,
        shape = RoundedCornerShape(style.cornerDp.dp),
        border = style.borderWidthDp
            .takeIf { it > 0f }
            ?.let { BorderStroke(it.dp, border) },
        tonalElevation = style.elevationDp.dp,
        shadowElevation = style.elevationDp.dp,
    ) {
        CompositionLocalProvider(LocalWorkspaceObjectStyle provides style) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(style.contentPaddingDp.dp),
                content = content,
            )
        }
    }
}
