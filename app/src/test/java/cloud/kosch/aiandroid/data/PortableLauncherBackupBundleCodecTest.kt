package cloud.kosch.aiandroid.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PortableLauncherBackupBundleCodecTest {
    private val bundle = PortableLauncherBackupBundle(
        createdAtEpochMillis = 1_700_000_000_000L,
        workspacePayload = "{\"format\":\"workspace\"}",
        launcherSettingsPayload = "settings-payload",
        scopedSettingsPayload = "schema=1\n",
        customActionsPayload = "schema=1\n",
        assistantPreferencesPayload = "schema=1\ncharacter=ZGVmYXVsdA\nname=\npresence=AMBIENT\nwake=OFF\ncustomWake=\nlocalWakeOnly=1\n",
    )

    @Test
    fun bundle_roundTripsDeterministically() {
        val first = PortableLauncherBackupBundleCodec.encode(bundle)
        val decoded = PortableLauncherBackupBundleCodec.decodeOrNull(first)
        val second = PortableLauncherBackupBundleCodec.encode(requireNotNull(decoded))

        assertEquals(bundle, decoded)
        assertTrue(first.contentEquals(second))
        assertTrue(PortableLauncherBackupBundleCodec.isBundle(first))
    }

    @Test
    fun legacyWorkspacePayload_isNotMisclassifiedAsBundle() {
        val legacy = "{\"format\":\"kosch-workspace\",\"version\":3}".encodeToByteArray()
        assertNull(PortableLauncherBackupBundleCodec.decodeOrNull(legacy))
        assertFalse(PortableLauncherBackupBundleCodec.isBundle(legacy))
    }

    @Test
    fun missingUnknownOrDuplicateSections_areRejected() {
        val encoded = PortableLauncherBackupBundleCodec.encode(bundle).decodeToString()
        val missing = encoded.lineSequence().filterNot { it.startsWith("actions|") }.joinToString("\n").encodeToByteArray()
        assertThrows(IllegalArgumentException::class.java) {
            PortableLauncherBackupBundleCodec.decodeOrNull(missing)
        }

        val unknown = (encoded + "unknown|eA\n").encodeToByteArray()
        assertThrows(IllegalArgumentException::class.java) {
            PortableLauncherBackupBundleCodec.decodeOrNull(unknown)
        }

        val assistantLine = encoded.lineSequence().first { it.startsWith("assistant|") }
        val duplicate = (encoded + assistantLine + "\n").encodeToByteArray()
        assertThrows(IllegalArgumentException::class.java) {
            PortableLauncherBackupBundleCodec.decodeOrNull(duplicate)
        }
    }
}
