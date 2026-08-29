package cloud.kosch.aiandroid.model

/**
 * Converts one complete Home Studio style draft into the portable scoped-setting representation.
 *
 * The map deliberately contains every object-style feature. Applying it through
 * ScopedSettingsController.setObjectOverrides() is one persistent transaction, so a failed write cannot leave a
 * half-updated object. Null colors mean INHERIT for that token while the remaining draft values stay explicit.
 */
object WorkspaceObjectStyleOverrides {
    fun from(style: WorkspaceObjectStyle): Map<String, PortableSettingValue?> = mapOf(
        FEATURE_OBJECT_VISIBLE to PortableSettingValue.Bool(style.visible),
        FEATURE_HOME_ICON_SCALE to PortableSettingValue.Decimal(style.iconScale.toDouble()),
        FEATURE_OBJECT_CONTENT_SCALE to PortableSettingValue.Decimal(style.contentScale.toDouble()),
        FEATURE_OBJECT_LABEL_VISIBLE to PortableSettingValue.Bool(style.showLabel),
        FEATURE_OBJECT_LABEL_SCALE to PortableSettingValue.Decimal(style.labelScale.toDouble()),
        FEATURE_OBJECT_OPACITY to PortableSettingValue.Decimal(style.opacity.toDouble()),
        FEATURE_OBJECT_ROTATION_DEG to PortableSettingValue.Decimal(style.rotationDegrees.toDouble()),
        FEATURE_OBJECT_OFFSET_X_DP to PortableSettingValue.Decimal(style.offsetXDp.toDouble()),
        FEATURE_OBJECT_OFFSET_Y_DP to PortableSettingValue.Decimal(style.offsetYDp.toDouble()),
        FEATURE_OBJECT_Z_INDEX to PortableSettingValue.Integer(style.zIndex.toInt()),
        FEATURE_OBJECT_CORNER_DP to PortableSettingValue.Integer(style.cornerDp),
        FEATURE_OBJECT_CONTENT_PADDING_DP to PortableSettingValue.Integer(style.contentPaddingDp),
        FEATURE_OBJECT_BACKGROUND_ALPHA to PortableSettingValue.Decimal(style.backgroundAlpha.toDouble()),
        FEATURE_OBJECT_BACKGROUND_ARGB to style.backgroundArgb?.let(PortableSettingValue::Integer),
        FEATURE_OBJECT_BORDER_WIDTH_DP to PortableSettingValue.Decimal(style.borderWidthDp.toDouble()),
        FEATURE_OBJECT_BORDER_ARGB to style.borderArgb?.let(PortableSettingValue::Integer),
        FEATURE_OBJECT_ELEVATION_DP to PortableSettingValue.Decimal(style.elevationDp.toDouble()),
    )
}