package cloud.kosch.aiandroid.security.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class FirewallPolicyTest {
    private val remote = NetworkAddress.parseNumeric("203.0.113.42")

    private fun flow(
        direction: TrafficDirection = TrafficDirection.OUTBOUND,
        protocol: TrafficProtocol = TrafficProtocol.TCP,
        remoteAddress: NetworkAddress = remote,
        remotePort: Int? = 443,
        uid: Int? = 10123,
        packageName: String? = "cloud.example.app",
        ambiguous: Boolean = false,
    ) = FirewallFlowContext(
        direction = direction,
        protocol = protocol,
        remoteAddress = remoteAddress,
        remotePort = remotePort,
        ownerUid = uid,
        packageName = packageName,
        packageAttributionAmbiguous = ambiguous,
    )

    @Test
    fun emptyPolicy_allowsByDefault() {
        val result = FirewallPolicySet.ALLOW_ALL.evaluate(flow())

        assertEquals(FirewallVerdict.ALLOW, result.verdict)
        assertNull(result.matchedRuleId)
    }

    @Test
    fun lowerPriorityNumberWins_independentOfInsertionOrder() {
        val allow = FirewallRule(id = "allow", priority = 100, verdict = FirewallVerdict.ALLOW)
        val block = FirewallRule(id = "block", priority = 10, verdict = FirewallVerdict.BLOCK)

        val result = FirewallPolicySet(listOf(allow, block)).evaluate(flow())

        assertEquals(FirewallVerdict.BLOCK, result.verdict)
        assertEquals("block", result.matchedRuleId)
    }

    @Test
    fun equalPriority_usesStableRuleId() {
        val z = FirewallRule(id = "z-rule", priority = 20, verdict = FirewallVerdict.ALLOW)
        val a = FirewallRule(id = "a-rule", priority = 20, verdict = FirewallVerdict.BLOCK)

        val result = FirewallPolicySet(listOf(z, a)).evaluate(flow())

        assertEquals(FirewallVerdict.BLOCK, result.verdict)
        assertEquals("a-rule", result.matchedRuleId)
    }

    @Test
    fun ruleMatchesDirectionProtocolCidrPortUidAndPackage() {
        val rule = FirewallRule(
            id = "block-example-https",
            priority = 5,
            verdict = FirewallVerdict.BLOCK,
            direction = TrafficDirection.OUTBOUND,
            protocol = TrafficProtocol.TCP,
            remoteCidr = CidrBlock.parse("203.0.113.0/24"),
            remotePortRange = PortRange(443, 443),
            ownerUid = 10123,
            packageName = "cloud.example.app",
        )

        val result = FirewallPolicySet(listOf(rule)).evaluate(flow())

        assertEquals(FirewallVerdict.BLOCK, result.verdict)
        assertEquals(rule.id, result.matchedRuleId)
    }

    @Test
    fun packageRuleNeverGuessesUnknownOrAmbiguousAttribution() {
        val rule = FirewallRule(
            id = "block-app",
            priority = 1,
            verdict = FirewallVerdict.BLOCK,
            ownerUid = 10123,
            packageName = "cloud.example.app",
        )
        val policy = FirewallPolicySet(listOf(rule))

        assertEquals(
            FirewallVerdict.ALLOW,
            policy.evaluate(flow(uid = null, packageName = null)).verdict,
        )
        assertEquals(
            FirewallVerdict.ALLOW,
            policy.evaluate(flow(packageName = null, ambiguous = true)).verdict,
        )
    }

    @Test
    fun uidRuleMayMatchEvenWhenPackageAttributionIsAmbiguous() {
        val rule = FirewallRule(
            id = "block-uid",
            priority = 1,
            verdict = FirewallVerdict.BLOCK,
            ownerUid = 10123,
        )

        val result = FirewallPolicySet(listOf(rule)).evaluate(flow(packageName = null, ambiguous = true))

        assertEquals(FirewallVerdict.BLOCK, result.verdict)
    }

    @Test
    fun disabledRuleNeverMatches() {
        val rule = FirewallRule(
            id = "disabled-block",
            priority = 1,
            verdict = FirewallVerdict.BLOCK,
            enabled = false,
        )

        assertEquals(FirewallVerdict.ALLOW, FirewallPolicySet(listOf(rule)).evaluate(flow()).verdict)
    }

    @Test
    fun cidrFamilyMismatchDoesNotMatch() {
        val rule = FirewallRule(
            id = "ipv6-only",
            priority = 1,
            verdict = FirewallVerdict.BLOCK,
            remoteCidr = CidrBlock.parse("2001:db8::/32"),
        )

        assertEquals(FirewallVerdict.ALLOW, FirewallPolicySet(listOf(rule)).evaluate(flow()).verdict)
    }

    @Test
    fun nonTcpUdpPortRuleWithExplicitProtocolIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            FirewallRule(
                id = "bad-icmp-port",
                priority = 1,
                verdict = FirewallVerdict.BLOCK,
                protocol = TrafficProtocol.ICMP,
                remotePortRange = PortRange(1, 1),
            )
        }
    }

    @Test
    fun nonTcpUdpFlowContextRejectsFabricatedPort() {
        assertThrows(IllegalArgumentException::class.java) {
            flow(
                protocol = TrafficProtocol.ICMP,
                remotePort = 7,
                uid = null,
                packageName = null,
            )
        }
    }

    @Test
    fun protocolAgnosticPortRuleCannotMatchIcmp() {
        val policy = FirewallPolicySet(
            listOf(
                FirewallRule(
                    id = "block-port-53",
                    priority = 1,
                    verdict = FirewallVerdict.BLOCK,
                    remotePortRange = PortRange(53, 53),
                ),
            ),
        )

        assertEquals(FirewallVerdict.BLOCK, policy.evaluate(flow(protocol = TrafficProtocol.TCP, remotePort = 53)).verdict)
        assertEquals(FirewallVerdict.BLOCK, policy.evaluate(flow(protocol = TrafficProtocol.UDP, remotePort = 53)).verdict)
        assertEquals(
            FirewallVerdict.ALLOW,
            policy.evaluate(
                flow(
                    protocol = TrafficProtocol.ICMP,
                    remotePort = null,
                    uid = null,
                    packageName = null,
                ),
            ).verdict,
        )
    }

    @Test
    fun packageSpecificRuleRequiresExplicitUid() {
        assertThrows(IllegalArgumentException::class.java) {
            FirewallRule(
                id = "bad-package",
                priority = 1,
                verdict = FirewallVerdict.BLOCK,
                packageName = "cloud.example.app",
            )
        }
    }

    @Test
    fun duplicateRuleIdsAndOversizedSetsAreRejected() {
        val rule = FirewallRule("same", 1, FirewallVerdict.ALLOW)
        assertThrows(IllegalArgumentException::class.java) {
            FirewallPolicySet(listOf(rule, rule.copy(priority = 2)))
        }

        val maxAllowed = (0 until FirewallPolicySet.MAX_RULES).map { index ->
            FirewallRule("allowed-$index", index, FirewallVerdict.ALLOW)
        }
        assertEquals(FirewallPolicySet.MAX_RULES, FirewallPolicySet(maxAllowed).rules().size)

        val tooMany = (0..FirewallPolicySet.MAX_RULES).map { index ->
            FirewallRule("rule-$index", index, FirewallVerdict.ALLOW)
        }
        assertThrows(IllegalArgumentException::class.java) { FirewallPolicySet(tooMany) }
    }

    @Test
    fun blockEvaluationRequiresMatchedRule() {
        assertThrows(IllegalArgumentException::class.java) {
            FirewallEvaluation(FirewallVerdict.BLOCK, matchedRuleId = null)
        }
    }

    @Test
    fun unmatchedPortOrProtocolFallsBackToAllow() {
        val rule = FirewallRule(
            id = "block-dns-udp",
            priority = 1,
            verdict = FirewallVerdict.BLOCK,
            protocol = TrafficProtocol.UDP,
            remotePortRange = PortRange(53, 53),
        )
        val policy = FirewallPolicySet(listOf(rule))

        assertEquals(FirewallVerdict.ALLOW, policy.evaluate(flow(protocol = TrafficProtocol.TCP, remotePort = 53)).verdict)
        assertEquals(FirewallVerdict.ALLOW, policy.evaluate(flow(protocol = TrafficProtocol.UDP, remotePort = 443)).verdict)
    }
}
