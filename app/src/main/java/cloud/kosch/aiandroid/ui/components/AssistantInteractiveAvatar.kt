package cloud.kosch.aiandroid.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import cloud.kosch.aiandroid.model.AssistantVisualState
import kotlinx.coroutines.flow.collect

/** Avatar surface that derives attention only from the user's explicit press position. */
@Composable
fun AssistantInteractiveAvatar(
    state: AssistantVisualState,
    speechSignal: AssistantSpeechSignal,
    reducedMotion: Boolean,
    attentionSignal: AssistantAttentionSignal,
    contentDescription: String,
    onPointerAttention: (normalizedX: Float, normalizedY: Float, pressed: Boolean) -> Unit,
    onActivate: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    assistantId: String = AssistantAssetCatalog.DEFAULT_ASSISTANT_ID,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = LocalIndication.current
    var measuredSize by remember { mutableStateOf(IntSize.Zero) }
    val activePress = remember { mutableStateOf<Offset?>(null) }
    val currentSize by rememberUpdatedState(measuredSize)
    val currentPointerAttention by rememberUpdatedState(onPointerAttention)

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    val normalized = interaction.pressPosition.normalizedIn(currentSize)
                    activePress.value = normalized
                    currentPointerAttention(normalized.x, normalized.y, true)
                }
                is PressInteraction.Release -> {
                    val normalized = activePress.value
                        ?: interaction.press.pressPosition.normalizedIn(currentSize)
                    currentPointerAttention(normalized.x, normalized.y, false)
                    activePress.value = null
                }
                is PressInteraction.Cancel -> {
                    val normalized = activePress.value
                        ?: interaction.press.pressPosition.normalizedIn(currentSize)
                    currentPointerAttention(normalized.x, normalized.y, false)
                    activePress.value = null
                }
                else -> Unit
            }
        }
    }

    DisposableEffect(interactionSource) {
        onDispose {
            activePress.value?.let { normalized ->
                currentPointerAttention(normalized.x, normalized.y, false)
            }
            activePress.value = null
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { measuredSize = it }
            .semantics { this.contentDescription = contentDescription }
            .clickable(
                interactionSource = interactionSource,
                indication = indication,
                role = Role.Button,
                onClick = {
                    onActivate()
                    onClick()
                },
            ),
    ) {
        if (AssistantProceduralCharacterResolver.resolve(assistantId) != null) {
            AssistantCharacterProceduralFallback(
                assistantId = assistantId,
                state = state,
                speechSignal = speechSignal,
                reducedMotion = reducedMotion,
                attentionSignal = attentionSignal,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AssistantAvatarFallback(
                state = state,
                speechSignal = speechSignal,
                reducedMotion = reducedMotion,
                attentionSignal = attentionSignal,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

internal fun Offset.normalizedIn(size: IntSize): Offset {
    if (size.width <= 0 || size.height <= 0 || !x.isFinite() || !y.isFinite()) return Offset.Zero
    return Offset(
        x = ((x / size.width) * 2f - 1f).coerceIn(-1f, 1f),
        y = ((y / size.height) * 2f - 1f).coerceIn(-1f, 1f),
    )
}
