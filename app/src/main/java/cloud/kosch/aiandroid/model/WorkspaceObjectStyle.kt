package cloud.kosch.aiandroid.model

const val FEATURE_HOME_ICON_SCALE = "home.icon.scale"
const val FEATURE_OBJECT_VISIBLE = "workspace.style.visible"
const val FEATURE_OBJECT_OPACITY = "workspace.style.opacity"
const val FEATURE_OBJECT_ROTATION_DEG = "workspace.style.rotation_deg"
const val FEATURE_OBJECT_OFFSET_X_DP = "workspace.style.offset_x_dp"
const val FEATURE_OBJECT_OFFSET_Y_DP = "workspace.style.offset_y_dp"
const val FEATURE_OBJECT_Z_INDEX = "workspace.style.z_index"
const val FEATURE_OBJECT_CORNER_DP = "workspace.style.corner_dp"
const val FEATURE_OBJECT_CONTENT_SCALE = "workspace.style.content_scale"
const val FEATURE_OBJECT_CONTENT_PADDING_DP = "workspace.style.content_padding_dp"
const val FEATURE_OBJECT_LABEL_VISIBLE = "workspace.style.label_visible"
const val FEATURE_OBJECT_LABEL_SCALE = "workspace.style.label_scale"
const val FEATURE_OBJECT_BACKGROUND_ALPHA = "workspace.style.background_alpha"
const val FEATURE_OBJECT_BACKGROUND_ARGB = "workspace.style.background_argb"
const val FEATURE_OBJECT_BORDER_WIDTH_DP = "workspace.style.border_width_dp"
const val FEATURE_OBJECT_BORDER_ARGB = "workspace.style.border_argb"
const val FEATURE_OBJECT_ELEVATION_DP = "workspace.style.elevation_dp"

data class WorkspaceObjectStyle(
    val visible: Boolean = true,
    val iconScale: Float = 1f,
    val contentScale: Float = 1f,
    val showLabel: Boolean = true,
    val labelScale: Float = 1f,
    val opacity: Float = 1f,
    val rotationDegrees: Float = 0f,
    val offsetXDp: Float = 0f,
    val offsetYDp: Float = 0f,
    val zIndex: Float = 0f,
    val cornerDp: Int = 18,
    val contentPaddingDp: Int = 8,
    val backgroundAlpha: Float = 1f,
    val backgroundArgb: Int? = null,
    val borderWidthDp: Float = 1f,
    val borderArgb: Int? = null,
    val elevationDp: Float = 0f,
)

/** Pure GLOBAL → PAGE → OBJECT style resolver with bounded renderer values. */
object WorkspaceObjectStyleResolver {
    fun resolve(
        document: ScopedSettingsDocument,
        pageId: String,
        itemId: String,
        globalIconScale: Float,
        globalShowLabels: Boolean,
    ): WorkspaceObjectStyle = WorkspaceObjectStyle(
        visible = bool(document, FEATURE_OBJECT_VISIBLE, true, pageId, itemId),
        iconScale = decimal(
            document,
            FEATURE_HOME_ICON_SCALE,
            globalIconScale.toDouble(),
            pageId,
            itemId,
        ).toFloat().coerceIn(0.25f, 2.5f),
        contentScale = decimal(
            document,
            FEATURE_OBJECT_CONTENT_SCALE,
            1.0,
            pageId,
            itemId,
        ).toFloat().coerceIn(0.25f, 2.5f),
        showLabel = bool(document, FEATURE_OBJECT_LABEL_VISIBLE, globalShowLabels, pageId, itemId),
        labelScale = decimal(
            document,
            FEATURE_OBJECT_LABEL_SCALE,
            1.0,
            pageId,
            itemId,
        ).toFloat().coerceIn(0.5f, 2f),
        opacity = decimal(
            document,
            FEATURE_OBJECT_OPACITY,
            1.0,
            pageId,
            itemId,
        ).toFloat().coerceIn(0.05f, 1f),
        rotationDegrees = decimal(
            document,
            FEATURE_OBJECT_ROTATION_DEG,
            0.0,
            pageId,
            itemId,
        ).toFloat().coerceIn(-180f, 180f),
        offsetXDp = decimal(
            document,
            FEATURE_OBJECT_OFFSET_X_DP,
            0.0,
            pageId,
            itemId,
        ).toFloat().coerceIn(-128f, 128f),
        offsetYDp = decimal(
            document,
            FEATURE_OBJECT_OFFSET_Y_DP,
            0.0,
            pageId,
            itemId,
        ).toFloat().coerceIn(-128f, 128f),
        zIndex = integer(document, FEATURE_OBJECT_Z_INDEX, 0, pageId, itemId).coerceIn(-32, 32).toFloat(),
        cornerDp = integer(document, FEATURE_OBJECT_CORNER_DP, 18, pageId, itemId).coerceIn(0, 96),
        contentPaddingDp = integer(
            document,
            FEATURE_OBJECT_CONTENT_PADDING_DP,
            8,
            pageId,
            itemId,
        ).coerceIn(0, 48),
        backgroundAlpha = decimal(
            document,
            FEATURE_OBJECT_BACKGROUND_ALPHA,
            1.0,
            pageId,
            itemId,
        ).toFloat().coerceIn(0f, 1f),
        backgroundArgb = optionalInteger(document, FEATURE_OBJECT_BACKGROUND_ARGB, pageId, itemId),
        borderWidthDp = decimal(
            document,
            FEATURE_OBJECT_BORDER_WIDTH_DP,
            1.0,
            pageId,
            itemId,
        ).toFloat().coerceIn(0f, 12f),
        borderArgb = optionalInteger(document, FEATURE_OBJECT_BORDER_ARGB, pageId, itemId),
        elevationDp = decimal(
            document,
            FEATURE_OBJECT_ELEVATION_DP,
            0.0,
            pageId,
            itemId,
        ).toFloat().coerceIn(0f, 32f),
    )

    private fun decimal(
        document: ScopedSettingsDocument,
        featureId: String,
        global: Double,
        pageId: String,
        itemId: String,
    ): Double = (resolve(
        document,
        featureId,
        PortableSettingValue.Decimal(global),
        pageId,
        itemId,
    ) as? PortableSettingValue.Decimal)?.value?.takeIf(Double::isFinite) ?: global

    private fun integer(
        document: ScopedSettingsDocument,
        featureId: String,
        global: Int,
        pageId: String,
        itemId: String,
    ): Int = (resolve(
        document,
        featureId,
        PortableSettingValue.Integer(global),
        pageId,
        itemId,
    ) as? PortableSettingValue.Integer)?.value ?: global

    private fun bool(
        document: ScopedSettingsDocument,
        featureId: String,
        global: Boolean,
        pageId: String,
        itemId: String,
    ): Boolean = (resolve(
        document,
        featureId,
        PortableSettingValue.Bool(global),
        pageId,
        itemId,
    ) as? PortableSettingValue.Bool)?.value ?: global

    private fun optionalInteger(
        document: ScopedSettingsDocument,
        featureId: String,
        pageId: String,
        itemId: String,
    ): Int? = when (val value = document.objectOverride(itemId, featureId) ?: document.pageOverride(pageId, featureId)) {
        is PortableSettingValue.Integer -> value.value
        else -> null
    }

    private fun resolve(
        document: ScopedSettingsDocument,
        featureId: String,
        global: PortableSettingValue,
        pageId: String,
        itemId: String,
    ): PortableSettingValue = SettingsScopeResolver.resolve(
        ScopedSettingValue(
            featureId = featureId,
            global = global,
            page = document.pageOverride(pageId, featureId)
                ?.let { SettingOverride.Value(it) }
                ?: SettingOverride.Inherit,
            objectValue = document.objectOverride(itemId, featureId)
                ?.let { SettingOverride.Value(it) }
                ?: SettingOverride.Inherit,
        ),
    ).value
}
