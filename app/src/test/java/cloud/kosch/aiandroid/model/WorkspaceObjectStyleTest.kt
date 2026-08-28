package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WorkspaceObjectStyleTest {
    @Test
    fun objectOverridesWinAndAreBounded() {
        val document = ScopedSettingsDocument()
            .withPageOverride("page", FEATURE_OBJECT_OPACITY, PortableSettingValue.Decimal(0.8))
            .withObjectOverride("item", FEATURE_HOME_ICON_SCALE, PortableSettingValue.Decimal(1.5))
            .withObjectOverride("item", FEATURE_HOME_LABEL_MODE, PortableSettingValue.Bool(false))
            .withObjectOverride("item", FEATURE_OBJECT_OPACITY, PortableSettingValue.Decimal(0.1))
            .withObjectOverride("item", FEATURE_OBJECT_CORNER_DP, PortableSettingValue.Integer(99))
            .withObjectOverride("item", FEATURE_OBJECT_ROTATION_DEG, PortableSettingValue.Decimal(-90.0))

        val style = WorkspaceObjectStyleResolver.resolve(
            document = document,
            pageId = "page",
            itemId = "item",
            globalIconScale = 1f,
            globalShowLabels = true,
        )

        assertEquals(1.5f, style.iconScale, 0.001f)
        assertFalse(style.showLabel)
        assertEquals(0.35f, style.opacity, 0.001f)
        assertEquals(48, style.cornerDp)
        assertEquals(-45f, style.rotationDegrees, 0.001f)
    }

    @Test
    fun pageOverrideWinsWhenObjectInherits() {
        val document = ScopedSettingsDocument()
            .withPageOverride("page", FEATURE_HOME_ICON_SCALE, PortableSettingValue.Decimal(1.25))
            .withPageOverride("page", FEATURE_OBJECT_ROTATION_DEG, PortableSettingValue.Decimal(12.0))

        val style = WorkspaceObjectStyleResolver.resolve(
            document = document,
            pageId = "page",
            itemId = "item",
            globalIconScale = 0.9f,
            globalShowLabels = true,
        )

        assertEquals(1.25f, style.iconScale, 0.001f)
        assertEquals(12f, style.rotationDegrees, 0.001f)
    }

    @Test
    fun wrongPortableTypesFallBackToSafeGlobals() {
        val document = ScopedSettingsDocument()
            .withObjectOverride("item", FEATURE_OBJECT_OPACITY, PortableSettingValue.Text("transparent"))
            .withObjectOverride("item", FEATURE_HOME_LABEL_MODE, PortableSettingValue.Integer(0))

        val style = WorkspaceObjectStyleResolver.resolve(
            document = document,
            pageId = "page",
            itemId = "item",
            globalIconScale = 0.85f,
            globalShowLabels = true,
        )

        assertEquals(1f, style.opacity, 0.001f)
        assertEquals(true, style.showLabel)
        assertEquals(0.85f, style.iconScale, 0.001f)
    }
}
