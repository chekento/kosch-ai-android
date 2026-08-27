package cloud.kosch.aiandroid.ui

import androidx.compose.runtime.staticCompositionLocalOf
import cloud.kosch.aiandroid.model.LauncherSettingsDocument

/**
 * Read-only Compose view of LauncherSettingsController.document.
 *
 * The controller/store remain the only source of truth. This CompositionLocal exists only to avoid threading the
 * same immutable settings snapshot through every launcher surface and to make runtime settings changes recompose
 * phone, foldable and desktop UI consistently.
 */
val LocalLauncherSettings = staticCompositionLocalOf { LauncherSettingsDocument() }
