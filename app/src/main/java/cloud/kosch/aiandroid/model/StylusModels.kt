package cloud.kosch.aiandroid.model

enum class StylusTool(val title: String) {
    NONE("Kein Stiftkontakt"),
    PEN("Stift"),
    ERASER("Radierer"),
}

data class StylusCapabilities(
    val present: Boolean = false,
    val active: Boolean = false,
    val deviceCount: Int = 0,
    val supportsPressure: Boolean = false,
    val supportsTilt: Boolean = false,
    val supportsHover: Boolean = false,
    val supportsBluetooth: Boolean = false,
    val lastTool: StylusTool = StylusTool.NONE,
    val pressure: Float = 0f,
    val tiltRadians: Float = 0f,
    val orientationRadians: Float = 0f,
    val barrelButtonPressed: Boolean = false,
) {
    val capabilitySummary: String
        get() = buildList {
            if (supportsPressure) add("Druck")
            if (supportsTilt) add("Neigung")
            if (supportsHover) add("Hover")
            if (supportsBluetooth) add("Bluetooth")
        }.ifEmpty { listOf("Basis-Stifteingabe") }.joinToString(" · ")
}

enum class InkTool(val title: String) {
    PEN("Stift"),
    HIGHLIGHTER("Marker"),
    ERASER("Radierer"),
}

data class InkPoint(
    val x: Float,
    val y: Float,
    val pressure: Float,
    val tiltRadians: Float,
)

data class InkStroke(
    val tool: InkTool,
    val points: List<InkPoint>,
)
