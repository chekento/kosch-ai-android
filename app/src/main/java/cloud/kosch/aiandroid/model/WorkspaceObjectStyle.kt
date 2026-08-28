package cloud.kosch.aiandroid.model

const val FEATURE_HOME_ICON_SCALE = "home.icon.scale"
const val FEATURE_HOME_LABEL_MODE = "home.label.mode"
const val FEATURE_OBJECT_OPACITY = "workspace.style.opacity"
const val FEATURE_OBJECT_CORNER_DP = "workspace.style.corner_dp"
const val FEATURE_OBJECT_ROTATION_DEG = "workspace.style.rotation_deg"

data class WorkspaceObjectStyle(
    val iconScale: Float = 1f,
    val showLabel: Boolean = true,
    val opacity: Float = 1f,
    val cornerDp: Int = 18,
    val rotationDegrees: Float = 0f,
)
