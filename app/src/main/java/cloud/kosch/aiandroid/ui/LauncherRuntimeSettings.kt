package cloud.kosch.aiandroid.ui

import androidx.compose.runtime.staticCompositionLocalOf
import cloud.kosch.aiandroid.model.LauncherSettingsDocument
import cloud.kosch.aiandroid.model.ScopedSettingsDocument

/**
 * Read-only Compose views of launcher settings state.
 *
 * Controllers/stores remain the only sources of truth. These CompositionLocals exist only to avoid threading the
 * same immutable snapshots through every workspace renderer and to make runtime settings changes recompose phone,
 * foldable and desktop UI consistently. Scoped values remain portable presentation data; no device grants or secrets
 * are exposed through this layer.
 */
val LocalLauncherSettings = staticCompositionLocalOf { LauncherSettingsDocument() }
val LocalScopedSettings = staticCompositionLocalOf { ScopedSettingsDocument() }
