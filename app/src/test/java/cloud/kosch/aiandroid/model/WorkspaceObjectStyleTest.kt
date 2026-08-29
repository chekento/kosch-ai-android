package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceObjectStyleTest {
    @Test
    fun `object overrides win and renderer values are bounded`() {
        val document = ScopedSettingsDocument()
            .withPageOverride("page", FEATURE_OBJECT_OPACITY, PortableSettingValue.Decimal(0.8))
            .withObjectOverride("item", FEATURE_HOME_ICON_SCALE, PortableSettingValue.Decimal(3.0))
            .withObjectOverride("item", FEATURE_OBJECT_LABEL_VISIBLE, PortableSettingValue.Bool(false))
            .withObjectOverride("item", FEATURE_OBJECT_VISIBLE, PortableSettingValue.Bool(false))
            .withObjectOverride("item", FEATURE_OBJECT_OPACITY, PortableSettingValue.Decimal(0.001))
            .withObjectOverride("item", FEATURE_OBJECT_CORNER_DP, PortableSettingValue.Integer(999))
            .withObjectOverride("item", FEATURE_OBJECT_ROTATION_DEG, PortableSettingValue.Decimal(-999.0))
            .withObjectOverride("item", FEATURE_OBJECT_OFFSET_X_DP, PortableSettingValue.Decimal(999.0))
            .withObjectOverride("item", FEATURE_OBJECT_OFFSET_Y_DP, PortableSettingValue.Decimal(-999.0))
            .withObjectOverride("item", FEATURE_OBJECT_Z_INDEX, PortableSettingValue.Integer(999))
            .withObjectOverride("item", FEATURE_OBJECT_CONTENT_SCALE, PortableSettingValue.Decimal(4.0))
            .withObjectOverride("item", FEATURE_OBJECT_LABEL_SCALE, PortableSettingValue.Decimal(0.1))
            .withObjectOverride("item", FEATURE_OBJECT_CONTENT_PADDING_DP, PortableSettingValue.Integer(99))
            .withObjectOverride("item", FEATURE_OBJECT_BACKGROUND_ALPHA, PortableSettingValue.Decimal(-1.0))
            .withObjectOverride("item", FEATURE_OBJECT_BORDER_WIDTH_DP, PortableSettingValue.Decimal(99.0))
            .withObjectOverride("item", FEATURE_OBJECT_ELEVATION_DP, PortableSettingValue.Decimal(99.0))
            .withObjectOverride("item", FEATURE_OBJECT_BACKGROUND_ARGB, PortableSettingValue.Integer(0x11223344))
            .withObjectOverride("item", FEATURE_OBJECT_BORDER_ARGB, PortableSettingValue.Integer(0x55667788))

        val style = WorkspaceObjectStyleResolver.resolve(
            document = document,
            pageId = "page",
            itemId = "item",
            globalIconScale = 1f,
            globalShowLabels = true,
        )

        assertFalse(style.visible)
        assertEquals(2.5f, style.iconScale, 0.001f)
        assertFalse(style.showLabel)
        assertEquals(0.05f, style.opacity, 0.001f)
        assertEquals(96, style.cornerDp)
        assertEquals(-180f, style.rotationDegrees, 0.001f)
        assertEquals(128f, style.offsetXDp, 0.001f)
        assertEquals(-128f, style.offsetYDp, 0.001f)
        assertEquals(32f, style.zIndex, 0.001f)
        assertEquals(2.5f, style.contentScale, 0.001f)
        assertEquals(0.5f, style.labelScale, 0.001f)
        assertEquals(48, style.contentPaddingDp)
        assertEquals(0f, style.backgroundAlpha, 0.001f)
        assertEquals(12f, style.borderWidthDp, 0.001f)
        assertEquals(32f, style.elevationDp, 0.001f)
        assertEquals(0x11223344, style.backgroundArgb)
        assertEquals(0x55667788, style.borderArgb)
    }

    @Test
    fun `page override wins when object inherits`() {
        val document = ScopedSettingsDocument()
            .withPageOverride("page", FEATURE_HOME_ICON_SCALE, PortableSettingValue.Decimal(1.25))
            .withPageOverride("page", FEATURE_OBJECT_ROTATION_DEG, PortableSettingValue.Decimal(12.0))
            .withPageOverride("page", FEATURE_OBJECT_LABEL_VISIBLE, PortableSettingValue.Bool(false))

        val style = WorkspaceObjectStyleResolver.resolve(
            document = document,
            pageId = "page",
            itemId = "item",
            globalIconScale = 0.9f,
            globalShowLabels = true,
        )

        assertEquals(1.25f, style.iconScale, 0.001f)
        assertEquals(12f, style.rotationDegrees, 0.001f)
        assertFalse(style.showLabel)
    }

    @Test
    fun `wrong portable types fall back to safe globals`() {
        val document = ScopedSettingsDocument()
            .withObjectOverride("item", FEATURE_OBJECT_OPACITY, PortableSettingValue.Text("transparent"))
            .withObjectOverride("item", FEATURE_OBJECT_LABEL_VISIBLE, PortableSettingValue.Integer(0))
            .withObjectOverride("item", FEATURE_OBJECT_BACKGROUND_ARGB, PortableSettingValue.Text("#fff"))

        val style = WorkspaceObjectStyleResolver.resolve(
            document = document,
            pageId = "page",
            itemId = "item",
            globalIconScale = 0.85f,
            globalShowLabels = true,
        )

        assertEquals(1f, style.opacity, 0.001f)
        assertTrue(style.showLabel)
        assertEquals(0.85f, style.iconScale, 0.001f)
        assertNull(style.backgroundArgb)
    }
}
