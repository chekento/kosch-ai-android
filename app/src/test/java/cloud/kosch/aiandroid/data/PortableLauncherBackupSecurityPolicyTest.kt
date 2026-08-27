package cloud.kosch.aiandroid.data

import cloud.kosch.aiandroid.model.BackupSettings
import cloud.kosch.aiandroid.model.LauncherSettingsDocument
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PortableLauncherBackupSecurityPolicyTest {
    @Test
    fun projection_forcesSecretAndDeviceIdExclusionEvenWhenInputDisablesThem() {
        val projected = PortableLauncherSettingsPolicy.project(
            LauncherSettingsDocument(
                backup = BackupSettings(
                    includeUsageLearning = true,
                    excludeSecretsAlways = false,
                    excludeWidgetHostIdsAlways = false,
                ),
            ),
        )

        assertTrue(projected.backup.excludeSecretsAlways)
        assertTrue(projected.backup.excludeWidgetHostIdsAlways)
        assertTrue(projected.backup.includeUsageLearning)
    }

    @Test
    fun projection_doesNotTurnOrdinaryPortableBackupChoicesIntoSecurityLocks() {
        val projected = PortableLauncherSettingsPolicy.project(
            LauncherSettingsDocument(
                backup = BackupSettings(
                    includeLauncherSettings = false,
                    includeWorkspaceLayout = false,
                    includeThemes = false,
                    includeAssistantPreferences = false,
                ),
            ),
        )

        assertFalse(projected.backup.includeLauncherSettings)
        assertFalse(projected.backup.includeWorkspaceLayout)
        assertFalse(projected.backup.includeThemes)
        assertFalse(projected.backup.includeAssistantPreferences)
        assertTrue(projected.backup.excludeSecretsAlways)
        assertTrue(projected.backup.excludeWidgetHostIdsAlways)
    }
}
