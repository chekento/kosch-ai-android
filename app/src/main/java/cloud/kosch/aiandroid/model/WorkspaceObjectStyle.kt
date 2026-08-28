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

/** Pure GLOBAL → PAGE → OBJECT style resolver with bounded renderer values. */
object WorkspaceObjectStyleResolver {
    fun resolve(
        document: ScopedSettingsDocument,
        pageId: String,
        itemId: String,
        globalIconScale: Float,
        globalShowLabels: Boolean,
    ): WorkspaceObjectStyle = WorkspaceObjectStyle(
        iconScale = decimal(
            document,
            FEATURE_HOME_ICON_SCALE,
            globalIconScale.toDouble(),
            pageId,
            itemId,
        ).toFloat().coerceIn(0.5f, 1.75f),
        showLabel = bool(
            document,
            FEATURE_HOME_LABEL_MODE,
            globalShowLabels,
            pageId,
            itemId,
        ),
        opacity = decimal(
            document,
            FEATURE_OBJECT_OPACITY,
            1.0,
            pageId,
            itemId,
        ).toFloat().coerceIn(0.35f, 1f),
        cornerDp = integer(
            document,
            FEATURE_OBJECT_CORNER_DP,
            18,
            pageId,
            itemId,
        ).coerceIn(0, 48),
        rotationDegrees = decimal(
            document,
            FEATURE_OBJECT_ROTATION_DEG,
            0.0,
            pageId,
            itemId,
        ).toFloat().coerceIn(-45f, 45f),
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
