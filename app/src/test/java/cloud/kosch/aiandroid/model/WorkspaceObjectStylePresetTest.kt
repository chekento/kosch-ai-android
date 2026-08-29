package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceObjectStylePresetTest {
    @Test
    fun `inherit clears every object style token`() {
        val overrides = WorkspaceObjectStylePresets.overrides(WorkspaceObjectStylePreset.INHERIT)

        assertEquals(WorkspaceObjectStylePresets.objectFeatureIds, overrides.keys)
        assertFalse(overrides.values.any { it != null })
    }

    @Test
    fun `preset clears stale fields while setting its own values`() {
        val overrides = WorkspaceObjectStylePresets.overrides(WorkspaceObjectStylePreset.COMPACT)

        assertEquals(WorkspaceObjectStylePresets.objectFeatureIds, overrides.keys)
        assertEquals(PortableSettingValue.Bool(false), overrides[FEATURE_OBJECT_LABEL_VISIBLE])
        assertEquals(PortableSettingValue.Decimal(0.82), overrides[FEATURE_HOME_ICON_SCALE])
        assertNull(overrides[FEATURE_OBJECT_ROTATION_DEG])
        assertNull(overrides[FEATURE_OBJECT_BACKGROUND_ARGB])
    }
}
