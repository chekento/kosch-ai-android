package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceObjectStyleOverridesTest {
    @Test
    fun `full style draft maps every whitelisted object style token`() {
        val style = WorkspaceObjectStyle(
            visible = false,
            iconScale = 1.3f,
            contentScale = 0.9f,
            showLabel = false,
            labelScale = 1.15f,
            opacity = 0.72f,
            rotationDegrees = -17f,
            offsetXDp = 12f,
            offsetYDp = -9f,
            zIndex = 4f,
            cornerDp = 31,
            contentPaddingDp = 6,
            backgroundAlpha = 0.48f,
            backgroundArgb = 0xCC112233.toInt(),
            borderWidthDp = 2.5f,
            borderArgb = 0xFF445566.toInt(),
            elevationDp = 13f,
        )

        val values = WorkspaceObjectStyleOverrides.from(style)

        assertEquals(WorkspaceObjectStylePresets.objectFeatureIds, values.keys)
        assertEquals(PortableSettingValue.Bool(false), values[FEATURE_OBJECT_VISIBLE])
        assertEquals(PortableSettingValue.Integer(31), values[FEATURE_OBJECT_CORNER_DP])
        assertEquals(PortableSettingValue.Integer(4), values[FEATURE_OBJECT_Z_INDEX])
        assertEquals(PortableSettingValue.Integer(0xCC112233.toInt()), values[FEATURE_OBJECT_BACKGROUND_ARGB])
        assertEquals(PortableSettingValue.Integer(0xFF445566.toInt()), values[FEATURE_OBJECT_BORDER_ARGB])
        assertTrue((values[FEATURE_OBJECT_OPACITY] as PortableSettingValue.Decimal).value in 0.719..0.721)
        assertFalse((values[FEATURE_OBJECT_LABEL_VISIBLE] as PortableSettingValue.Bool).value)
    }

    @Test
    fun `unset colors become inherit while all other draft values stay explicit`() {
        val values = WorkspaceObjectStyleOverrides.from(WorkspaceObjectStyle())

        assertNull(values[FEATURE_OBJECT_BACKGROUND_ARGB])
        assertNull(values[FEATURE_OBJECT_BORDER_ARGB])
        assertEquals(WorkspaceObjectStylePresets.objectFeatureIds, values.keys)
    }
}