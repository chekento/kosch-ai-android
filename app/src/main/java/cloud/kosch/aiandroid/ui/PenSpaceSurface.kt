package cloud.kosch.aiandroid.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.os.Bundle
import android.util.AttributeSet
import android.view.InputDevice
import android.view.MotionEvent
import android.view.PointerIcon
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BorderColor
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cloud.kosch.aiandroid.AiContextHandoffController
import cloud.kosch.aiandroid.LauncherController
import cloud.kosch.aiandroid.R
import cloud.kosch.aiandroid.ai.AiContextHandoffPolicy
import cloud.kosch.aiandroid.ai.PenAiContextPlanner
import cloud.kosch.aiandroid.data.InkStrokeNormalizer
import cloud.kosch.aiandroid.model.InkPoint
import cloud.kosch.aiandroid.model.InkStroke
import cloud.kosch.aiandroid.model.InkTool
import cloud.kosch.aiandroid.ui.theme.DeepSurface
import cloud.kosch.aiandroid.ui.theme.Mint
import cloud.kosch.aiandroid.ui.theme.MutedMist
import cloud.kosch.aiandroid.ui.theme.RaisedSurface
import cloud.kosch.aiandroid.ui.theme.Sky
import cloud.kosch.aiandroid.ui.theme.Violet
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.hypot

@Composable
fun ColumnScope.PenSpaceSurface(
    controller: LauncherController,
    onAsk: () -> Unit,
    onSystemNote: () -> Unit,
    onExport: () -> Unit,
) {
    var selectedTool by remember { mutableStateOf(InkTool.PEN) }
    var inkView by remember { mutableStateOf<PressureInkView?>(null) }
    var strokeCount by remember { mutableIntStateOf(controller.loadInkStrokes().size) }
    var lassoMode by remember { mutableStateOf(false) }
    val aiHandoff = remember { AiContextHandoffController() }
    val stylus = controller.stylusState

    fun cancelLasso() {
        lassoMode = false
        inkView?.setLassoMode(false)
    }

    Surface(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        color = DeepSurface.copy(alpha = 0.95f),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (stylus.active) Mint.copy(alpha = 0.22f) else Violet.copy(alpha = 0.14f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Draw, contentDescription = null, tint = if (stylus.active) Mint else Sky)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pen Space", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (stylus.present) stylus.capabilitySummary else "Stift getrennt · Zeichenfläche geschützt",
                        color = MutedMist,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Surface(
                    color = if (stylus.active) Mint.copy(alpha = 0.16f) else RaisedSurface,
                    shape = CircleShape,
                ) {
                    Text(
                        text = if (lassoMode) {
                            "Lasso aktiv"
                        } else if (stylus.active) {
                            "${stylus.lastTool.title} · ${(stylus.pressure * 100).toInt()} %"
                        } else {
                            "$strokeCount Striche"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        color = if (lassoMode || stylus.active) Mint else MutedMist,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PenToolChip(InkTool.PEN, selectedTool, Icons.Rounded.BorderColor) {
                    cancelLasso()
                    selectedTool = it
                    inkView?.setTool(it)
                }
                PenToolChip(InkTool.HIGHLIGHTER, selectedTool, Icons.Rounded.Draw) {
                    cancelLasso()
                    selectedTool = it
                    inkView?.setTool(it)
                }
                PenToolChip(InkTool.ERASER, selectedTool, Icons.Rounded.DeleteOutline) {
                    cancelLasso()
                    selectedTool = it
                    inkView?.setTool(it)
                }
                AssistChip(
                    onClick = {
                        cancelLasso()
                        inkView?.undo()
                    },
                    label = { Text("Undo") },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                )
                AssistChip(
                    onClick = {
                        cancelLasso()
                        inkView?.clearInk()
                    },
                    label = { Text("Leeren") },
                    leadingIcon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
                AssistChip(
                    onClick = {
                        val strokes = controller.loadInkStrokes()
                        if (lassoMode) {
                            cancelLasso()
                            controller.postNotice("Lasso-Auswahl abgebrochen")
                        } else if (strokes.isEmpty()) {
                            controller.postNotice("Für Lasso → Ask braucht die Skizze mindestens einen Strich")
                        } else {
                            lassoMode = true
                            inkView?.setLassoMode(true)
                            controller.postNotice("Lasso aktiv · gewünschten Bereich mit dem Stift umranden")
                        }
                    },
                    label = { Text(if (lassoMode) "Lasso abbrechen" else "Lasso → Ask") },
                    leadingIcon = {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                )
                AssistChip(
                    onClick = {
                        cancelLasso()
                        val strokes = controller.loadInkStrokes()
                        if (strokes.isEmpty()) {
                            controller.postNotice("Noch keine Skizze vorhanden · Ask bleibt als normaler Texteinstieg verfügbar")
                            onAsk()
                        } else {
                            val summary = PenAiContextPlanner.summarize(strokes)
                            aiHandoff.prepare(
                                AiContextHandoffPolicy.fromPenSketch(
                                    title = "Pen-Space-Skizze",
                                    summary = summary.text,
                                    textualDescription = "Lokale Aggregatanalyse der Skizze; keine Rohkoordinaten oder SVG-Daten enthalten.",
                                ),
                            )
                        }
                    },
                    label = { Text("An Ask") },
                    leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
                AssistChip(
                    onClick = {
                        cancelLasso()
                        onSystemNote()
                    },
                    label = { Text("Systemnotiz") },
                    leadingIcon = { Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
                AssistChip(
                    onClick = {
                        cancelLasso()
                        onExport()
                    },
                    label = { Text("SVG Export") },
                    leadingIcon = { Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF07141D)),
                shape = RoundedCornerShape(22.dp),
            ) {
                AndroidView(
                    factory = { context ->
                        PressureInkView(context).apply {
                            contentDescription = "Druckempfindliche Pen-Space-Zeichenfläche"
                            setTool(selectedTool)
                            setStylusRequired(true)
                            setInitialStrokes(controller.loadInkStrokes())
                            setOnInkChanged { strokes ->
                                strokeCount = strokes.size
                                controller.saveInkStrokes(strokes)
                            }
                            setOnLassoCompleted { lassoPoints ->
                                lassoMode = false
                                if (lassoPoints.size < 3) {
                                    controller.postNotice("Lasso abgebrochen oder zu klein")
                                } else {
                                    val summary = PenAiContextPlanner.summarizeLassoSelection(
                                        strokes = controller.loadInkStrokes(),
                                        lassoPoints = lassoPoints,
                                    )
                                    if (summary.selectedStrokeCount <= 0) {
                                        controller.postNotice("Im Lasso wurden keine Striche erkannt")
                                    } else {
                                        aiHandoff.prepare(
                                            AiContextHandoffPolicy.fromPenSketch(
                                                title = "Pen-Lasso-Auswahl",
                                                summary = summary.text,
                                                textualDescription = "Nur lokale Aggregatanalyse der Lasso-Auswahl; Lasso-Punkte, Rohkoordinaten und SVG bleiben auf dem Gerät.",
                                            ),
                                        )
                                    }
                                }
                            }
                            setLassoMode(lassoMode)
                            strokeCount = strokeCount()
                            inkView = this
                        }
                    },
                    update = { view ->
                        view.setTool(selectedTool)
                        view.setStylusRequired(true)
                        view.setLassoMode(lassoMode)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (lassoMode) {
                        "Lasso lokal aktiv · Polygon wird nach Pen-up verworfen"
                    } else {
                        "Lokal autosaved · normierte Vektorstriche"
                    },
                    color = if (lassoMode) Sky else MutedMist,
                    style = MaterialTheme.typography.labelSmall,
                )
                if (stylus.barrelButtonPressed) {
                    Text("Stifttaste erkannt", color = Sky, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    aiHandoff.draft?.let { draft ->
        AiContextHandoffConsentSurface(
            draft = draft,
            onCancel = aiHandoff::cancel,
            onConfirm = { question, selection ->
                val confirmed = aiHandoff.confirm(
                    userPrompt = question,
                    userConfirmed = true,
                    selection = selection,
                )
                if (confirmed != null) {
                    controller.openProviderChooser(confirmed.prompt)
                }
            },
        )
    }
}

@Composable
private fun PenToolChip(
    tool: InkTool,
    selected: InkTool,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onSelected: (InkTool) -> Unit,
) {
    FilterChip(
        selected = selected == tool,
        onClick = { onSelected(tool) },
        label = { Text(tool.title) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
    )
}

class PressureInkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val strokes = mutableListOf<InkStroke>()
    private val undoStack = ArrayDeque<List<InkStroke>>()
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(26, 128, 191, 255)
        strokeWidth = resources.displayMetrics.density
    }
    private val hoverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(190, 105, 230, 215)
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 1.5f
    }
    private val lassoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(225, 105, 230, 215)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = resources.displayMetrics.density * 2.2f
    }
    private var activePoints: MutableList<InkPoint>? = null
    private var activeLassoPoints: MutableList<InkPoint>? = null
    private var selectedTool = InkTool.PEN
    private var stylusRequired = true
    private var lassoMode = false
    private var onInkChanged: (List<InkStroke>) -> Unit = {}
    private var onLassoCompleted: (List<InkPoint>) -> Unit = {}
    private var hoverPoint: PointF? = null
    private var eraserChanged = false

    init {
        isFocusable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        setBackgroundColor(AndroidColor.TRANSPARENT)
    }

    fun setInitialStrokes(value: List<InkStroke>) {
        if (strokes.isNotEmpty()) return
        strokes += InkStrokeNormalizer.normalize(value).deepCopy()
        invalidate()
    }

    fun setTool(value: InkTool) {
        selectedTool = value
        invalidate()
    }

    fun setStylusRequired(value: Boolean) {
        stylusRequired = value
    }

    fun setLassoMode(enabled: Boolean) {
        if (lassoMode == enabled) return
        lassoMode = enabled
        activePoints = null
        if (!enabled) activeLassoPoints = null
        invalidate()
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
    }

    fun setOnInkChanged(callback: (List<InkStroke>) -> Unit) {
        onInkChanged = callback
    }

    fun setOnLassoCompleted(callback: (List<InkPoint>) -> Unit) {
        onLassoCompleted = callback
    }

    fun strokeCount(): Int = strokes.size

    fun undo() {
        if (undoStack.isEmpty()) return
        strokes.clear()
        strokes += undoStack.removeLast().deepCopy()
        notifyChanged()
        invalidate()
    }

    fun clearInk() {
        if (strokes.isEmpty()) return
        rememberUndo()
        strokes.clear()
        activePoints = null
        notifyChanged()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawGrid(canvas)
        strokes.forEach { drawStroke(canvas, it) }
        activePoints?.takeIf { it.isNotEmpty() }?.let { points ->
            drawStroke(canvas, InkStroke(selectedTool, points))
        }
        activeLassoPoints?.takeIf { it.isNotEmpty() }?.let { points ->
            drawLasso(canvas, points)
        }
        hoverPoint?.let { point ->
            val radius = resources.displayMetrics.density * when (selectedTool) {
                InkTool.PEN -> 7f
                InkTool.HIGHLIGHTER -> 13f
                InkTool.ERASER -> 18f
            }
            canvas.drawCircle(point.x, point.y, radius, hoverPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val index = event.actionIndex.coerceIn(0, event.pointerCount - 1)
        val hardwareTool = event.getToolType(index)
        val fromStylus = hardwareTool == MotionEvent.TOOL_TYPE_STYLUS ||
            hardwareTool == MotionEvent.TOOL_TYPE_ERASER ||
            event.isFromSource(InputDevice.SOURCE_STYLUS)
        if (stylusRequired && !fromStylus) return false
        if (lassoMode) return handleLassoTouch(event, index)

        val erasing = selectedTool == InkTool.ERASER || hardwareTool == MotionEvent.TOOL_TYPE_ERASER
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                requestFocus()
                rememberUndo()
                eraserChanged = false
                if (erasing) {
                    eraserChanged = eraseAt(event.x, event.y)
                } else {
                    activePoints = mutableListOf(event.point(index))
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (erasing) {
                    for (historyIndex in 0 until event.historySize) {
                        eraserChanged = eraseAt(
                            event.getHistoricalX(index, historyIndex),
                            event.getHistoricalY(index, historyIndex),
                        ) || eraserChanged
                    }
                    eraserChanged = eraseAt(event.x, event.y) || eraserChanged
                } else {
                    val target = activePoints ?: return false
                    for (historyIndex in 0 until event.historySize) {
                        target += event.historicalPoint(index, historyIndex)
                    }
                    target += event.point(index)
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!erasing) {
                    val target = activePoints.orEmpty().toMutableList()
                    target += event.point(index)
                    InkStrokeNormalizer.normalizeStroke(InkStroke(selectedTool, target))?.let(strokes::add)
                    activePoints = null
                }
                if (!erasing || eraserChanged) notifyChanged()
                parent?.requestDisallowInterceptTouchEvent(false)
                performClick()
                invalidate()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                activePoints = null
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val tool = event.getToolType(0)
        if (tool != MotionEvent.TOOL_TYPE_STYLUS && tool != MotionEvent.TOOL_TYPE_ERASER) {
            return super.onGenericMotionEvent(event)
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER,
            MotionEvent.ACTION_HOVER_MOVE -> hoverPoint = PointF(event.x, event.y)

            MotionEvent.ACTION_HOVER_EXIT -> hoverPoint = null
        }
        invalidate()
        return true
    }

    override fun onResolvePointerIcon(event: MotionEvent, pointerIndex: Int): PointerIcon =
        PointerIcon.getSystemIcon(context, PointerIcon.TYPE_CROSSHAIR)

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = View::class.java.name
        info.contentDescription = if (lassoMode) {
            "Pen-Space-Zeichenfläche, Lasso-Auswahl aktiv, ${strokes.size} Striche"
        } else {
            "Pen-Space-Zeichenfläche, ${strokes.size} Striche, Werkzeug ${selectedTool.title}"
        }
        if (undoStack.isNotEmpty() && !lassoMode) {
            info.addAction(
                AccessibilityNodeInfo.AccessibilityAction(
                    R.id.accessibility_action_undo_ink,
                    "Letzten Stiftschritt rückgängig machen",
                ),
            )
        }
        if (strokes.isNotEmpty() && !lassoMode) {
            info.addAction(
                AccessibilityNodeInfo.AccessibilityAction(
                    R.id.accessibility_action_clear_ink,
                    "Alle Stiftstriche löschen",
                ),
            )
        }
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean = when (action) {
        R.id.accessibility_action_undo_ink -> true.also { undo() }
        R.id.accessibility_action_clear_ink -> true.also { clearInk() }
        else -> super.performAccessibilityAction(action, arguments)
    }

    private fun handleLassoTouch(event: MotionEvent, index: Int): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                requestFocus()
                activeLassoPoints = mutableListOf(event.point(index))
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val target = activeLassoPoints ?: return false
                for (historyIndex in 0 until event.historySize) {
                    appendLassoPoint(target, event.historicalPoint(index, historyIndex))
                }
                appendLassoPoint(target, event.point(index))
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                val target = activeLassoPoints.orEmpty().toMutableList()
                appendLassoPoint(target, event.point(index))
                val completed = target.toList()
                activeLassoPoints = null
                lassoMode = false
                parent?.requestDisallowInterceptTouchEvent(false)
                performClick()
                invalidate()
                onLassoCompleted(if (completed.size >= MIN_LASSO_POINTS) completed else emptyList())
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                activeLassoPoints = null
                lassoMode = false
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                onLassoCompleted(emptyList())
                return true
            }
        }
        return false
    }

    private fun appendLassoPoint(target: MutableList<InkPoint>, point: InkPoint) {
        if (target.size < MAX_LASSO_POINTS) target += point
    }

    private fun drawLasso(canvas: Canvas, points: List<InkPoint>) {
        val first = points.firstOrNull() ?: return
        val path = Path().apply {
            moveTo(first.x * width, first.y * height)
            points.drop(1).forEach { point ->
                lineTo(point.x * width, point.y * height)
            }
            if (points.size >= MIN_LASSO_POINTS) close()
        }
        canvas.drawPath(path, lassoPaint)
    }

    private fun drawGrid(canvas: Canvas) {
        val gap = resources.displayMetrics.density * 32f
        var x = gap
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += gap
        }
        var y = gap
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += gap
        }
    }

    private fun drawStroke(canvas: Canvas, stroke: InkStroke) {
        if (stroke.points.isEmpty()) return
        val density = resources.displayMetrics.density
        strokePaint.color = when (stroke.tool) {
            InkTool.PEN -> AndroidColor.rgb(216, 243, 240)
            InkTool.HIGHLIGHTER -> AndroidColor.argb(112, 183, 167, 255)
            InkTool.ERASER -> AndroidColor.TRANSPARENT
        }
        if (stroke.points.size == 1) {
            val point = stroke.points.first()
            strokePaint.style = Paint.Style.FILL
            canvas.drawCircle(point.x * width, point.y * height, widthFor(point, stroke.tool, density) / 2f, strokePaint)
            strokePaint.style = Paint.Style.STROKE
            return
        }
        stroke.points.zipWithNext().forEach { (start, end) ->
            strokePaint.strokeWidth = (widthFor(start, stroke.tool, density) + widthFor(end, stroke.tool, density)) / 2f
            val path = Path().apply {
                moveTo(start.x * width, start.y * height)
                lineTo(end.x * width, end.y * height)
            }
            canvas.drawPath(path, strokePaint)
        }
    }

    private fun widthFor(point: InkPoint, tool: InkTool, density: Float): Float {
        val base = when (tool) {
            InkTool.PEN -> 3.2f
            InkTool.HIGHLIGHTER -> 15f
            InkTool.ERASER -> 24f
        }
        val pressure = if (point.pressure <= 0f) 0.45f else point.pressure
        val tiltBoost = 1f + abs(point.tiltRadians).coerceAtMost(1f) * 0.18f
        return base * density * (0.48f + pressure * 1.05f) * tiltBoost
    }

    private fun eraseAt(x: Float, y: Float): Boolean {
        val radius = resources.displayMetrics.density * 24f
        val removed = strokes.removeAll { stroke ->
            stroke.points.any { point ->
                hypot(point.x * width - x, point.y * height - y) <= radius
            }
        }
        return removed
    }

    private fun MotionEvent.point(index: Int): InkPoint = InkPoint(
        x = (getX(index) / width.coerceAtLeast(1)).coerceIn(0f, 1f),
        y = (getY(index) / height.coerceAtLeast(1)).coerceIn(0f, 1f),
        pressure = getPressure(index).coerceIn(0f, 1f),
        tiltRadians = getAxisValue(MotionEvent.AXIS_TILT, index),
    )

    private fun MotionEvent.historicalPoint(index: Int, historyIndex: Int): InkPoint = InkPoint(
        x = (getHistoricalX(index, historyIndex) / width.coerceAtLeast(1)).coerceIn(0f, 1f),
        y = (getHistoricalY(index, historyIndex) / height.coerceAtLeast(1)).coerceIn(0f, 1f),
        pressure = getHistoricalPressure(index, historyIndex).coerceIn(0f, 1f),
        tiltRadians = getHistoricalAxisValue(MotionEvent.AXIS_TILT, index, historyIndex),
    )

    private fun rememberUndo() {
        if (undoStack.size >= MAX_UNDO_STEPS) undoStack.removeFirst()
        undoStack.addLast(strokes.deepCopy())
    }

    private fun notifyChanged() {
        val normalized = InkStrokeNormalizer.normalize(strokes)
        if (normalized != strokes) {
            strokes.clear()
            strokes += normalized
        }
        onInkChanged(strokes.deepCopy())
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
    }

    private fun List<InkStroke>.deepCopy(): List<InkStroke> = map { stroke ->
        stroke.copy(points = stroke.points.toList())
    }

    private companion object {
        const val MAX_UNDO_STEPS = 24
        const val MIN_LASSO_POINTS = 3
        const val MAX_LASSO_POINTS = 2_048
    }
}
