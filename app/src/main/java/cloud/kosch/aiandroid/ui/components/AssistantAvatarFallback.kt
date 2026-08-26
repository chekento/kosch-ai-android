package cloud.kosch.aiandroid.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import cloud.kosch.aiandroid.model.AssistantVisualState
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Violet
import cloud.kosch.aiandroid.ui.theme.Warm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Assistant avatar boundary.
 *
 * A complete, calibrated matrix pack activates the layered WebP compositor. Any missing, corrupt,
 * oversized or uncalibrated asset keeps HOME on the procedural robot. Both renderers consume the
 * same deterministic eye, mouth, portal, speech and reduced-motion frame.
 */
@Composable
fun AssistantAvatarFallback(
    state: AssistantVisualState,
    modifier: Modifier = Modifier,
    speechSignal: AssistantSpeechSignal = AssistantSpeechSignal.Idle,
    reducedMotion: Boolean = false,
) {
    val context = LocalContext.current
    val runtime = remember(context.applicationContext) {
        AssistantAssetRuntime(context.applicationContext)
    }
    val inspector = remember(context.applicationContext) {
        AssistantAssetPackInspector(context.applicationContext)
    }
    val spritesReady = remember(inspector) { inspector.auditDefault().activationReady }
    val enabled = state != AssistantVisualState.DISABLED
    val stateStartedAt = remember(state) { SystemClock.uptimeMillis() }
    var nowUptimeMillis by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }

    LaunchedEffect(state, reducedMotion, speechSignal.active) {
        nowUptimeMillis = SystemClock.uptimeMillis()
        if (reducedMotion) return@LaunchedEffect
        while (isActive) {
            withFrameNanos { nowUptimeMillis = SystemClock.uptimeMillis() }
        }
    }

    val appearance = remember { Animatable(0f) }
    LaunchedEffect(enabled, reducedMotion) {
        val target = if (enabled) 1f else 0f
        if (reducedMotion) {
            appearance.snapTo(target)
        } else {
            appearance.animateTo(
                targetValue = target,
                animationSpec = tween(
                    durationMillis = if (enabled) 760 else 520,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    val stateElapsedMillis = (nowUptimeMillis - stateStartedAt).coerceAtLeast(0L)
    val animationFrame = AssistantAnimationDirector.frame(
        state = state,
        stateElapsedMillis = stateElapsedMillis,
        nowUptimeMillis = nowUptimeMillis,
        speechSignal = speechSignal,
        reducedMotion = reducedMotion,
    )
    val appearanceFrame = when {
        enabled && appearance.value < 0.999f -> (appearance.value * 15f).roundToInt().coerceIn(0, 15)
        !enabled && appearance.value > 0.001f -> (appearance.value * 15f).roundToInt().coerceIn(0, 15)
        else -> null
    }

    val spriteVisual by produceState<AssistantSpriteVisual?>(
        null,
        runtime,
        spritesReady,
        state,
        animationFrame.eye,
        animationFrame.mouth,
        animationFrame.portalFrame,
        appearanceFrame,
        enabled,
    ) {
        value = withContext(Dispatchers.IO) {
            if (!spritesReady) return@withContext null
            val portal = runtime.loadPortal(animationFrame.portalFrame) ?: return@withContext null
            when {
                appearanceFrame != null -> {
                    val body = runtime.loadSpawn(appearanceFrame) ?: return@withContext null
                    AssistantSpriteVisual(body = body, eye = null, mouth = null, portal = portal)
                }
                !enabled -> AssistantSpriteVisual(body = null, eye = null, mouth = null, portal = portal)
                else -> runtime.loadState(
                    state = state,
                    eyeShape = animationFrame.eye,
                    mouthShape = animationFrame.mouth,
                )?.let { frame ->
                    AssistantSpriteVisual(
                        body = frame.body,
                        eye = frame.eye,
                        mouth = frame.mouth,
                        portal = portal,
                    )
                }
            }
        }
    }

    val visual = spriteVisual
    if (visual != null) {
        AssistantSpriteAvatar(
            visual = visual,
            calibration = DefaultAssistantAssetManifest.manifest.faceCalibration,
            animationFrame = animationFrame,
            appearance = appearance.value,
            modifier = modifier,
        )
    } else {
        AssistantCanvasAvatar(
            state = state,
            animationFrame = animationFrame,
            appearance = appearance.value,
            speechAmplitude = speechSignal.amplitude,
            modifier = modifier,
        )
    }
}

@Composable
private fun AssistantSpriteAvatar(
    visual: AssistantSpriteVisual,
    calibration: AssistantFaceCalibration,
    animationFrame: AssistantAnimationFrame,
    appearance: Float,
    modifier: Modifier,
) {
    Canvas(modifier = modifier) {
        val side = min(size.width, size.height)
        val bodySide = side * 0.94f
        val bodyLeft = (size.width - bodySide) / 2f
        val bodyTop = (size.height - bodySide) / 2f + animationFrame.bodyBob * side
        val portalSide = side * 0.92f
        drawImage(
            image = visual.portal.image,
            dstOffset = IntOffset(
                x = ((size.width - portalSide) / 2f).roundToInt(),
                y = (size.height * 0.51f).roundToInt(),
            ),
            dstSize = IntSize(portalSide.roundToInt(), portalSide.roundToInt()),
            alpha = (0.46f + animationFrame.glow * 0.46f).coerceIn(0f, 1f),
            filterQuality = FilterQuality.Medium,
        )

        val body = visual.body ?: return@Canvas
        val center = Offset(bodyLeft + bodySide / 2f, bodyTop + bodySide / 2f)
        translate(center.x, center.y) {
            scale(animationFrame.bodyScale, animationFrame.bodyScale) {
                rotate(animationFrame.headTiltDegrees * 0.22f) {
                    translate(-center.x, -center.y) {
                        drawImage(
                            image = body.image,
                            dstOffset = IntOffset(bodyLeft.roundToInt(), bodyTop.roundToInt()),
                            dstSize = IntSize(bodySide.roundToInt(), bodySide.roundToInt()),
                            alpha = appearance.coerceIn(0f, 1f),
                            filterQuality = FilterQuality.Medium,
                        )
                        visual.eye?.let { eye ->
                            calibration.eyeAnchor?.let { anchor ->
                                drawOverlay(eye, anchor, bodyLeft, bodyTop, bodySide, appearance)
                            }
                        }
                        visual.mouth?.let { mouth ->
                            calibration.mouthAnchor?.let { anchor ->
                                drawOverlay(mouth, anchor, bodyLeft, bodyTop, bodySide, appearance)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawOverlay(
    asset: AssistantDecodedAsset,
    anchor: AssistantNormalizedRect,
    bodyLeft: Float,
    bodyTop: Float,
    bodySide: Float,
    alpha: Float,
) {
    drawImage(
        image = asset.image,
        dstOffset = IntOffset(
            x = (bodyLeft + anchor.left * bodySide).roundToInt(),
            y = (bodyTop + anchor.top * bodySide).roundToInt(),
        ),
        dstSize = IntSize(
            width = (anchor.width * bodySide).roundToInt(),
            height = (anchor.height * bodySide).roundToInt(),
        ),
        alpha = alpha.coerceIn(0f, 1f),
        filterQuality = FilterQuality.Medium,
    )
}

@Composable
private fun AssistantCanvasAvatar(
    state: AssistantVisualState,
    animationFrame: AssistantAnimationFrame,
    appearance: Float,
    speechAmplitude: Float,
    modifier: Modifier,
) {
    val accent = when (state) {
        AssistantVisualState.ERROR -> Warm
        AssistantVisualState.OFFLINE -> Sky
        AssistantVisualState.THINKING -> Violet
        AssistantVisualState.DISABLED -> Color(0xFF60717D)
        else -> Mint
    }

    Canvas(modifier = modifier) {
        val unit = min(size.width, size.height)
        val centerX = size.width / 2f
        val portalY = size.height * 0.84f

        drawPortal(
            center = Offset(centerX, portalY),
            unit = unit,
            accent = accent,
            glow = animationFrame.glow,
            frame = animationFrame.portalFrame,
        )
        if (appearance <= 0.001f) return@Canvas

        val reveal = appearance.coerceIn(0f, 1f)
        val rise = (1f - reveal) * unit * 0.42f
        val bob = animationFrame.bodyBob * unit
        val robotScale = (0.76f + reveal * 0.24f) * animationFrame.bodyScale

        clipRect(left = 0f, top = 0f, right = size.width, bottom = portalY + unit * 0.025f) {
            translate(centerX, portalY - unit * 0.40f + rise + bob) {
                scale(robotScale, robotScale) {
                    translate(-centerX, -(portalY - unit * 0.40f)) {
                        drawProceduralRobot(
                            origin = Offset(centerX, portalY - unit * 0.40f),
                            unit = unit,
                            state = state,
                            frame = animationFrame,
                            accent = accent,
                            speechAmplitude = speechAmplitude,
                            alpha = reveal,
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawPortal(
    center: Offset,
    unit: Float,
    accent: Color,
    glow: Float,
    frame: Int,
) {
    val phase = frame / 7f
    val width = unit * (0.48f + phase * 0.05f)
    val height = unit * (0.085f + abs(0.5f - phase) * 0.018f)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.34f * glow), Color.Transparent),
            center = center,
            radius = width * 0.64f,
        ),
        topLeft = Offset(center.x - width * 0.64f, center.y - height * 1.8f),
        size = Size(width * 1.28f, height * 3.6f),
    )
    repeat(3) { ring ->
        val inset = ring * unit * 0.028f
        drawOval(
            color = accent.copy(alpha = (0.82f - ring * 0.19f) * glow),
            topLeft = Offset(center.x - width / 2f + inset, center.y - height / 2f + inset * 0.12f),
            size = Size((width - inset * 2f).coerceAtLeast(1f), (height - inset * 0.24f).coerceAtLeast(1f)),
            style = Stroke(width = (1.4f - ring * 0.25f).coerceAtLeast(0.6f).dp.toPx()),
        )
    }
    if (frame in 3..6) {
        val beamHeight = unit * (0.10f + (frame - 3) * 0.025f)
        repeat(4) { beam ->
            val x = center.x + (beam - 1.5f) * unit * 0.065f
            drawLine(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, accent.copy(alpha = 0.24f * glow)),
                    startY = center.y - beamHeight,
                    endY = center.y,
                ),
                start = Offset(x, center.y - beamHeight),
                end = Offset(x, center.y),
                strokeWidth = 1.dp.toPx(),
            )
        }
    }
}

private fun DrawScope.drawProceduralRobot(
    origin: Offset,
    unit: Float,
    state: AssistantVisualState,
    frame: AssistantAnimationFrame,
    accent: Color,
    speechAmplitude: Float,
    alpha: Float,
) {
    val shell = Color(0xFFF1F5F7).copy(alpha = alpha)
    val shellShadow = Color(0xFF91A6B0).copy(alpha = alpha)
    val panel = Color(0xFF061117).copy(alpha = alpha)
    val glow = accent.copy(alpha = frame.glow * alpha)

    val bodyTop = origin.y + unit * 0.10f
    val bodyRect = Rect(
        left = origin.x - unit * 0.16f,
        top = bodyTop,
        right = origin.x + unit * 0.16f,
        bottom = bodyTop + unit * 0.26f,
    )
    val leftShoulder = Offset(bodyRect.left + unit * 0.015f, bodyTop + unit * 0.07f)
    val rightShoulder = Offset(bodyRect.right - unit * 0.015f, bodyTop + unit * 0.07f)
    val (leftHand, rightHand) = armTargets(state, origin, unit, bodyTop)

    drawRobotArm(leftShoulder, leftHand, shell, shellShadow, accent, unit, alpha)
    drawRobotArm(rightShoulder, rightHand, shell, shellShadow, accent, unit, alpha)

    drawRoundRect(
        brush = Brush.linearGradient(listOf(shell, shellShadow), bodyRect.topLeft, bodyRect.bottomRight),
        topLeft = bodyRect.topLeft,
        size = bodyRect.size,
        cornerRadius = CornerRadius(unit * 0.105f),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.46f * alpha),
        topLeft = Offset(bodyRect.left + unit * 0.025f, bodyRect.top + unit * 0.018f),
        size = Size(bodyRect.width - unit * 0.05f, unit * 0.055f),
        cornerRadius = CornerRadius(unit * 0.028f),
    )
    drawCircle(glow, unit * 0.045f, Offset(origin.x, bodyTop + unit * 0.105f))
    drawCircle(panel, unit * 0.026f, Offset(origin.x, bodyTop + unit * 0.105f))
    drawCircle(accent.copy(alpha = alpha), unit * 0.015f, Offset(origin.x, bodyTop + unit * 0.105f))

    val footY = bodyRect.bottom + unit * 0.02f
    listOf(-1f, 1f).forEach { direction ->
        drawRoundRect(
            brush = Brush.linearGradient(listOf(shell, shellShadow)),
            topLeft = Offset(origin.x + direction * unit * 0.07f - unit * 0.055f, footY),
            size = Size(unit * 0.11f, unit * 0.075f),
            cornerRadius = CornerRadius(unit * 0.04f),
        )
        drawRoundRect(
            color = panel,
            topLeft = Offset(origin.x + direction * unit * 0.07f - unit * 0.047f, footY + unit * 0.052f),
            size = Size(unit * 0.094f, unit * 0.024f),
            cornerRadius = CornerRadius(unit * 0.012f),
        )
    }

    val headCenter = Offset(origin.x, origin.y - unit * 0.115f)
    rotate(frame.headTiltDegrees, pivot = headCenter) {
        val headRect = Rect(
            left = headCenter.x - unit * 0.31f,
            top = headCenter.y - unit * 0.20f,
            right = headCenter.x + unit * 0.31f,
            bottom = headCenter.y + unit * 0.20f,
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(Color.White.copy(alpha = alpha), shellShadow),
                headRect.topLeft,
                headRect.bottomRight,
            ),
            topLeft = headRect.topLeft,
            size = headRect.size,
            cornerRadius = CornerRadius(unit * 0.155f),
        )
        drawRoundRect(
            color = panel,
            topLeft = Offset(headRect.left + unit * 0.055f, headRect.top + unit * 0.045f),
            size = Size(headRect.width - unit * 0.11f, headRect.height - unit * 0.09f),
            cornerRadius = CornerRadius(unit * 0.115f),
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(Color.Transparent, accent.copy(alpha = 0.10f * alpha)),
                Offset(headRect.left, headRect.top),
                Offset(headRect.right, headRect.bottom),
            ),
            topLeft = Offset(headRect.left + unit * 0.065f, headRect.top + unit * 0.055f),
            size = Size(headRect.width - unit * 0.13f, headRect.height - unit * 0.11f),
            cornerRadius = CornerRadius(unit * 0.105f),
        )

        listOf(-1f, 1f).forEach { direction ->
            drawRoundRect(
                color = panel,
                topLeft = Offset(
                    x = if (direction < 0) headRect.left - unit * 0.035f else headRect.right - unit * 0.012f,
                    y = headCenter.y - unit * 0.07f,
                ),
                size = Size(unit * 0.047f, unit * 0.14f),
                cornerRadius = CornerRadius(unit * 0.022f),
            )
            drawLine(
                color = accent.copy(alpha = 0.62f * alpha),
                start = Offset(
                    if (direction < 0) headRect.left - unit * 0.012f else headRect.right + unit * 0.012f,
                    headCenter.y - unit * 0.047f,
                ),
                end = Offset(
                    if (direction < 0) headRect.left - unit * 0.012f else headRect.right + unit * 0.012f,
                    headCenter.y + unit * 0.047f,
                ),
                strokeWidth = unit * 0.012f,
            )
        }

        val antennaTop = Offset(headCenter.x, headRect.top - unit * 0.105f)
        drawLine(shellShadow, Offset(headCenter.x, headRect.top), antennaTop, unit * 0.018f)
        drawCircle(glow, unit * 0.035f, antennaTop)
        drawCircle(accent.copy(alpha = alpha), unit * 0.018f, antennaTop)

        drawRobotEyes(
            state = state,
            frame = frame,
            center = Offset(headCenter.x, headCenter.y - unit * 0.035f),
            unit = unit,
            accent = accent.copy(alpha = alpha),
        )
        drawDigitalMouth(
            mouth = frame.mouth,
            center = Offset(headCenter.x, headCenter.y + unit * 0.088f),
            unit = unit,
            accent = accent.copy(alpha = alpha),
            openness = frame.mouthOpen,
            amplitude = speechAmplitude,
        )
    }
}

private fun DrawScope.drawRobotArm(
    shoulder: Offset,
    hand: Offset,
    shell: Color,
    shellShadow: Color,
    accent: Color,
    unit: Float,
    alpha: Float,
) {
    val elbow = Offset((shoulder.x + hand.x) / 2f, (shoulder.y + hand.y) / 2f + unit * 0.018f)
    drawLine(shellShadow, shoulder, elbow, unit * 0.075f)
    drawLine(shell, shoulder, elbow, unit * 0.052f)
    drawLine(shellShadow, elbow, hand, unit * 0.068f)
    drawLine(shell, elbow, hand, unit * 0.046f)
    drawCircle(accent.copy(alpha = 0.55f * alpha), unit * 0.025f, elbow)
    drawCircle(shell, unit * 0.042f, hand)
}

private fun armTargets(
    state: AssistantVisualState,
    origin: Offset,
    unit: Float,
    bodyTop: Float,
): Pair<Offset, Offset> = when (state) {
    AssistantVisualState.LISTENING -> Offset(origin.x - unit * 0.25f, bodyTop - unit * 0.16f) to
        Offset(origin.x + unit * 0.28f, bodyTop + unit * 0.14f)
    AssistantVisualState.THINKING -> Offset(origin.x - unit * 0.27f, bodyTop + unit * 0.17f) to
        Offset(origin.x + unit * 0.13f, bodyTop - unit * 0.05f)
    AssistantVisualState.SPEAKING -> Offset(origin.x - unit * 0.34f, bodyTop + unit * 0.02f) to
        Offset(origin.x + unit * 0.34f, bodyTop + unit * 0.02f)
    AssistantVisualState.ERROR -> Offset(origin.x - unit * 0.24f, bodyTop + unit * 0.23f) to
        Offset(origin.x + unit * 0.24f, bodyTop + unit * 0.23f)
    else -> Offset(origin.x - unit * 0.27f, bodyTop + unit * 0.18f) to
        Offset(origin.x + unit * 0.27f, bodyTop + unit * 0.18f)
}

private fun DrawScope.drawRobotEyes(
    state: AssistantVisualState,
    frame: AssistantAnimationFrame,
    center: Offset,
    unit: Float,
    accent: Color,
) {
    val separation = unit * 0.125f
    val eyeWidth = unit * 0.105f
    val eyeHeight = unit * 0.105f * frame.eyeOpen.coerceIn(0.05f, 1f)
    val gaze = Offset(frame.gazeX * unit * 0.018f, frame.gazeY * unit * 0.014f)

    listOf(-1f, 1f).forEach { direction ->
        val eyeCenter = Offset(center.x + direction * separation, center.y) + gaze
        if (state == AssistantVisualState.ERROR) {
            val half = unit * 0.04f
            drawLine(accent, eyeCenter - Offset(half, half), eyeCenter + Offset(half, half), unit * 0.018f)
            drawLine(accent, eyeCenter - Offset(half, -half), eyeCenter + Offset(half, -half), unit * 0.018f)
        } else if (frame.eye == AssistantEyeShape.HAPPY) {
            drawArc(
                color = accent,
                startAngle = 195f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(eyeCenter.x - eyeWidth / 2f, eyeCenter.y - unit * 0.015f),
                size = Size(eyeWidth, unit * 0.075f),
                style = Stroke(width = unit * 0.019f),
            )
        } else {
            drawRoundRect(
                color = accent,
                topLeft = Offset(eyeCenter.x - eyeWidth / 2f, eyeCenter.y - eyeHeight / 2f),
                size = Size(eyeWidth, eyeHeight.coerceAtLeast(unit * 0.008f)),
                cornerRadius = CornerRadius(eyeHeight / 2f),
            )
        }
    }
}

private fun DrawScope.drawDigitalMouth(
    mouth: AssistantMouthShape,
    center: Offset,
    unit: Float,
    accent: Color,
    openness: Float,
    amplitude: Float,
) {
    when (mouth) {
        is AssistantMouthShape.Emotion -> when (mouth.value) {
            AssistantMouthEmotion.SMILE,
            AssistantMouthEmotion.GRIN,
            AssistantMouthEmotion.LAUGH,
            -> drawArc(
                color = accent,
                startAngle = 18f,
                sweepAngle = 144f,
                useCenter = false,
                topLeft = Offset(center.x - unit * 0.09f, center.y - unit * 0.055f),
                size = Size(unit * 0.18f, unit * (0.085f + openness * 0.035f)),
                style = Stroke(width = unit * 0.015f),
            )
            AssistantMouthEmotion.SAD,
            AssistantMouthEmotion.FROWN,
            -> drawArc(
                color = accent,
                startAngle = 198f,
                sweepAngle = 144f,
                useCenter = false,
                topLeft = Offset(center.x - unit * 0.085f, center.y - unit * 0.005f),
                size = Size(unit * 0.17f, unit * 0.09f),
                style = Stroke(width = unit * 0.015f),
            )
            AssistantMouthEmotion.SURPRISED,
            AssistantMouthEmotion.YAWN,
            -> drawOval(
                color = accent,
                topLeft = Offset(center.x - unit * 0.04f, center.y - unit * 0.035f),
                size = Size(unit * 0.08f, unit * (0.07f + openness * 0.055f)),
                style = Stroke(width = unit * 0.015f),
            )
            AssistantMouthEmotion.NEUTRAL -> drawRoundRect(
                color = accent,
                topLeft = Offset(center.x - unit * 0.072f, center.y - unit * 0.007f),
                size = Size(unit * 0.144f, unit * 0.014f),
                cornerRadius = CornerRadius(unit * 0.007f),
            )
        }
        is AssistantMouthShape.Viseme -> {
            if (mouth.value == AssistantViseme.SIL || mouth.value == AssistantViseme.PP) {
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(center.x - unit * 0.07f, center.y - unit * 0.008f),
                    size = Size(unit * 0.14f, unit * (0.012f + openness * 0.012f)),
                    cornerRadius = CornerRadius(unit * 0.008f),
                )
            } else {
                val bars = 7
                val barWidth = unit * 0.014f
                val gap = unit * 0.008f
                val totalWidth = bars * barWidth + (bars - 1) * gap
                repeat(bars) { index ->
                    val distance = abs(index - (bars - 1) / 2f) / ((bars - 1) / 2f)
                    val spectral = 0.48f + (1f - distance) * 0.52f
                    val energy = (0.58f + amplitude * 0.42f) * openness
                    val height = unit * (0.018f + 0.085f * spectral * energy)
                    val x = center.x - totalWidth / 2f + index * (barWidth + gap)
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset(x, center.y - height / 2f),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(barWidth / 2f),
                    )
                }
            }
        }
    }
}

private data class AssistantSpriteVisual(
    val body: AssistantDecodedAsset?,
    val eye: AssistantDecodedAsset?,
    val mouth: AssistantDecodedAsset?,
    val portal: AssistantDecodedAsset,
)
