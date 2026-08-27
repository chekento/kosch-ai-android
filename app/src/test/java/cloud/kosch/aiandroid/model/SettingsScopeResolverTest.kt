package cloud.kosch.aiandroid.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsScopeResolverTest {
    @Test
    fun objectOverride_winsThenPageThenGlobal() {
        val objectResolved = SettingsScopeResolver.resolve(
            ScopedSettingValue(
                featureId = "home.icon.scale",
                global = 1f,
                page = SettingOverride.Value(1.1f),
                objectValue = SettingOverride.Value(1.35f),
            ),
        )
        assertEquals(1.35f, objectResolved.value)
        assertEquals(SettingScope.OBJECT, objectResolved.source)

        val pageResolved = SettingsScopeResolver.resolve(
            ScopedSettingValue(
                featureId = "home.icon.scale",
                global = 1f,
                page = SettingOverride.Value(1.1f),
            ),
        )
        assertEquals(1.1f, pageResolved.value)
        assertEquals(SettingScope.PAGE, pageResolved.source)

        val globalResolved = SettingsScopeResolver.resolve(
            ScopedSettingValue(featureId = "home.icon.scale", global = 1f),
        )
        assertEquals(1f, globalResolved.value)
        assertEquals(SettingScope.GLOBAL, globalResolved.source)
    }

    @Test
    fun inherit_isExplicitAndDoesNotCopyParentValue() {
        val scoped = ScopedSettingValue(
            featureId = "appearance.parallax",
            global = false,
            page = SettingOverride.Value(true),
            objectValue = SettingOverride.Inherit,
        )
        val resolved = SettingsScopeResolver.resolve(scoped)
        assertTrue(resolved.value)
        assertEquals(SettingScope.PAGE, resolved.source)
    }

    @Test
    fun unsupportedScope_isRejectedInsteadOfSilentlyPersisted() {
        val failure = runCatching {
            SettingsScopeResolver.resolve(
                ScopedSettingValue(
                    featureId = "home.grid.columns",
                    global = 12,
                    page = SettingOverride.Value(8),
                ),
            )
        }
        assertTrue(failure.isFailure)
    }

    @Test
    fun deviceAndSessionFeatures_cannotBecomePortableOverrides() {
        assertFalse(SettingsScopeResolver.canOverride("assistant.screen_awareness", SettingScope.PAGE))
        assertFalse(SettingsScopeResolver.canOverride("assistant.screen_session", SettingScope.OBJECT))
        assertFalse(SettingsScopeResolver.canOverride("widgets.host_id", SettingScope.PAGE))
    }

    @Test
    fun catalogScope_drivesOverrideAvailability() {
        assertTrue(SettingsScopeResolver.canOverride("home.icon.scale", SettingScope.GLOBAL))
        assertTrue(SettingsScopeResolver.canOverride("home.icon.scale", SettingScope.PAGE))
        assertTrue(SettingsScopeResolver.canOverride("home.icon.scale", SettingScope.OBJECT))
        assertFalse(SettingsScopeResolver.canOverride("appearance.material_you", SettingScope.PAGE))
    }
}
