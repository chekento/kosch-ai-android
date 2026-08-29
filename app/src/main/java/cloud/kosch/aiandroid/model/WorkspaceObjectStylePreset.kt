package cloud.kosch.aiandroid.model

enum class WorkspaceObjectStylePreset(val title: String) {
    INHERIT("Vererben"),
    CLEAN("Clean"),
    COMPACT("Kompakt"),
    FOCUS("Fokus"),
    FLOATING("Floating"),
    GLASS("Glass"),
    TILT_LEFT("Links geneigt"),
    TILT_RIGHT("Rechts geneigt"),
}

object WorkspaceObjectStylePresets {
    val objectFeatureIds: Set<String> = setOf(
        FEATURE_HOME_ICON_SCALE,
        FEATURE_OBJECT_VISIBLE,
        FEATURE_OBJECT_OPACITY,
        FEATURE_OBJECT_ROTATION_DEG,
        FEATURE_OBJECT_OFFSET_X_DP,
        FEATURE_OBJECT_OFFSET_Y_DP,
        FEATURE_OBJECT_Z_INDEX,
        FEATURE_OBJECT_CORNER_DP,
        FEATURE_OBJECT_CONTENT_SCALE,
        FEATURE_OBJECT_CONTENT_PADDING_DP,
        FEATURE_OBJECT_LABEL_VISIBLE,
        FEATURE_OBJECT_LABEL_SCALE,
        FEATURE_OBJECT_BACKGROUND_ALPHA,
        FEATURE_OBJECT_BACKGROUND_ARGB,
        FEATURE_OBJECT_BORDER_WIDTH_DP,
        FEATURE_OBJECT_BORDER_ARGB,
        FEATURE_OBJECT_ELEVATION_DP,
    )

    /** Null values mean remove this object-level override and inherit page/global state. */
    fun overrides(preset: WorkspaceObjectStylePreset): Map<String, PortableSettingValue?> = when (preset) {
        WorkspaceObjectStylePreset.INHERIT -> objectFeatureIds.associateWith { null as PortableSettingValue? }
        WorkspaceObjectStylePreset.CLEAN -> base(
            FEATURE_OBJECT_CORNER_DP to PortableSettingValue.Integer(18),
            FEATURE_OBJECT_CONTENT_SCALE to PortableSettingValue.Decimal(1.0),
            FEATURE_OBJECT_CONTENT_PADDING_DP to PortableSettingValue.Integer(8),
            FEATURE_OBJECT_LABEL_VISIBLE to PortableSettingValue.Bool(true),
            FEATURE_OBJECT_OPACITY to PortableSettingValue.Decimal(1.0),
            FEATURE_OBJECT_BORDER_WIDTH_DP to PortableSettingValue.Decimal(1.0),
            FEATURE_OBJECT_ELEVATION_DP to PortableSettingValue.Decimal(0.0),
        )
        WorkspaceObjectStylePreset.COMPACT -> base(
            FEATURE_HOME_ICON_SCALE to PortableSettingValue.Decimal(0.82),
            FEATURE_OBJECT_CONTENT_SCALE to PortableSettingValue.Decimal(0.90),
            FEATURE_OBJECT_CONTENT_PADDING_DP to PortableSettingValue.Integer(4),
            FEATURE_OBJECT_LABEL_VISIBLE to PortableSettingValue.Bool(false),
            FEATURE_OBJECT_CORNER_DP to PortableSettingValue.Integer(12),
            FEATURE_OBJECT_BORDER_WIDTH_DP to PortableSettingValue.Decimal(0.5),
        )
        WorkspaceObjectStylePreset.FOCUS -> base(
            FEATURE_HOME_ICON_SCALE to PortableSettingValue.Decimal(1.24),
            FEATURE_OBJECT_CONTENT_SCALE to PortableSettingValue.Decimal(1.08),
            FEATURE_OBJECT_LABEL_SCALE to PortableSettingValue.Decimal(1.08),
            FEATURE_OBJECT_CORNER_DP to PortableSettingValue.Integer(26),
            FEATURE_OBJECT_BORDER_WIDTH_DP to PortableSettingValue.Decimal(2.0),
            FEATURE_OBJECT_ELEVATION_DP to PortableSettingValue.Decimal(8.0),
        )
        WorkspaceObjectStylePreset.FLOATING -> base(
            FEATURE_OBJECT_OPACITY to PortableSettingValue.Decimal(0.98),
            FEATURE_OBJECT_CORNER_DP to PortableSettingValue.Integer(30),
            FEATURE_OBJECT_BACKGROUND_ALPHA to PortableSettingValue.Decimal(0.88),
            FEATURE_OBJECT_BORDER_WIDTH_DP to PortableSettingValue.Decimal(1.0),
            FEATURE_OBJECT_ELEVATION_DP to PortableSettingValue.Decimal(16.0),
        )
        WorkspaceObjectStylePreset.GLASS -> base(
            FEATURE_OBJECT_OPACITY to PortableSettingValue.Decimal(0.94),
            FEATURE_OBJECT_CORNER_DP to PortableSettingValue.Integer(24),
            FEATURE_OBJECT_BACKGROUND_ALPHA to PortableSettingValue.Decimal(0.52),
            FEATURE_OBJECT_BORDER_WIDTH_DP to PortableSettingValue.Decimal(1.0),
            FEATURE_OBJECT_ELEVATION_DP to PortableSettingValue.Decimal(6.0),
        )
        WorkspaceObjectStylePreset.TILT_LEFT -> base(
            FEATURE_OBJECT_ROTATION_DEG to PortableSettingValue.Decimal(-6.0),
            FEATURE_OBJECT_CORNER_DP to PortableSettingValue.Integer(20),
        )
        WorkspaceObjectStylePreset.TILT_RIGHT -> base(
            FEATURE_OBJECT_ROTATION_DEG to PortableSettingValue.Decimal(6.0),
            FEATURE_OBJECT_CORNER_DP to PortableSettingValue.Integer(20),
        )
    }

    /** Presets replace the object style as one coherent state instead of leaving stale overrides from older presets. */
    private fun base(vararg values: Pair<String, PortableSettingValue>): Map<String, PortableSettingValue?> {
        val reset = objectFeatureIds.associateWith { null as PortableSettingValue? }.toMutableMap()
        values.forEach { (featureId, value) -> reset[featureId] = value }
        return reset
    }
}
