package cloud.kosch.aiandroid.security.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirewallPolicyStorageInstrumentationTest {
    @Test
    fun privateStorage_preservesFutureRawAcrossRepositoryInstances_untilExplicitReset() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences(
            SharedPreferencesFirewallPolicyStorage.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
        val original = preferences.getString(SharedPreferencesFirewallPolicyStorage.KEY_POLICY, null)
        val futureRaw = "KOSCH_FIREWALL_POLICY|9\nfuture-state-that-must-survive\n"

        try {
            preferences.edit()
                .putString(SharedPreferencesFirewallPolicyStorage.KEY_POLICY, futureRaw)
                .commit()

            val first = FirewallPolicyRepository(SharedPreferencesFirewallPolicyStorage(context))
            val firstState = first.load() as FirewallPolicyLoadState.FutureSchema
            assertEquals(9, firstState.schemaVersion)
            assertEquals(futureRaw, preferences.getString(SharedPreferencesFirewallPolicyStorage.KEY_POLICY, null))

            val second = FirewallPolicyRepository(SharedPreferencesFirewallPolicyStorage(context))
            assertTrue(second.load() is FirewallPolicyLoadState.FutureSchema)
            assertEquals(futureRaw, preferences.getString(SharedPreferencesFirewallPolicyStorage.KEY_POLICY, null))

            assertTrue(second.reset())
            assertEquals(FirewallPolicyLoadState.Absent, second.load())
        } finally {
            preferences.edit().apply {
                if (original == null) remove(SharedPreferencesFirewallPolicyStorage.KEY_POLICY)
                else putString(SharedPreferencesFirewallPolicyStorage.KEY_POLICY, original)
            }.commit()
        }
    }

    @Test
    fun explicitReplace_roundTripsAcrossFreshAndroidRepository() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences(
            SharedPreferencesFirewallPolicyStorage.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
        val original = preferences.getString(SharedPreferencesFirewallPolicyStorage.KEY_POLICY, null)

        try {
            val writer = FirewallPolicyRepository(SharedPreferencesFirewallPolicyStorage(context))
            assertTrue(
                writer.replace(
                    FirewallPolicyDocument(
                        rules = listOf(
                            FirewallRule(
                                id = "block-example",
                                priority = 10,
                                verdict = FirewallVerdict.BLOCK,
                                protocol = TrafficProtocol.TCP,
                                remoteCidr = CidrBlock.parse("203.0.113.0/24"),
                                remotePortRange = PortRange(443, 443),
                            ),
                        ),
                    ),
                ),
            )

            val reader = FirewallPolicyRepository(SharedPreferencesFirewallPolicyStorage(context))
            val state = reader.load() as FirewallPolicyLoadState.Ready
            assertEquals("block-example", state.document.normalizedRules().single().id)
        } finally {
            preferences.edit().apply {
                if (original == null) remove(SharedPreferencesFirewallPolicyStorage.KEY_POLICY)
                else putString(SharedPreferencesFirewallPolicyStorage.KEY_POLICY, original)
            }.commit()
        }
    }
}
