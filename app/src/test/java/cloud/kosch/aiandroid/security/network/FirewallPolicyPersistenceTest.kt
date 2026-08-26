package cloud.kosch.aiandroid.security.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirewallPolicyPersistenceTest {
    @Test
    fun roundTrip_preservesNormalizedRulesAndAllMatchDimensions() {
        val document = FirewallPolicyDocument(
            rules = listOf(
                FirewallRule(
                    id = "z-allow-v6",
                    priority = 50,
                    verdict = FirewallVerdict.ALLOW,
                    enabled = false,
                    direction = TrafficDirection.INBOUND,
                    protocol = TrafficProtocol.UDP,
                    remoteCidr = CidrBlock.parse("2001:db8::/32"),
                    remotePortRange = PortRange(1000, 2000),
                    ownerUid = 10124,
                    packageName = "cloud.example.two",
                ),
                FirewallRule(
                    id = "a-block-v4",
                    priority = 10,
                    verdict = FirewallVerdict.BLOCK,
                    direction = TrafficDirection.OUTBOUND,
                    protocol = TrafficProtocol.TCP,
                    remoteCidr = CidrBlock.parse("203.0.113.0/24"),
                    remotePortRange = PortRange(443, 443),
                    ownerUid = 10123,
                    packageName = "cloud.example.one",
                ),
            ),
        )

        val encoded = FirewallPolicyCodec.encode(document)
        val decoded = FirewallPolicyCodec.decode(encoded) as FirewallPolicyDecodeResult.Valid

        assertEquals(listOf("a-block-v4", "z-allow-v6"), decoded.document.normalizedRules().map(FirewallRule::id))
        val first = decoded.document.normalizedRules().first()
        assertEquals(FirewallVerdict.BLOCK, first.verdict)
        assertEquals(TrafficDirection.OUTBOUND, first.direction)
        assertEquals(TrafficProtocol.TCP, first.protocol)
        assertEquals("203.0.113.0/24", first.remoteCidr.toString())
        assertEquals(PortRange(443, 443), first.remotePortRange)
        assertEquals(10123, first.ownerUid)
        assertEquals("cloud.example.one", first.packageName)
    }

    @Test
    fun encode_isDeterministicAcrossInputOrder() {
        val first = FirewallRule("b", 20, FirewallVerdict.ALLOW)
        val second = FirewallRule("a", 10, FirewallVerdict.BLOCK)

        val left = FirewallPolicyCodec.encode(FirewallPolicyDocument(rules = listOf(first, second)))
        val right = FirewallPolicyCodec.encode(FirewallPolicyDocument(rules = listOf(second, first)))

        assertEquals(left, right)
    }

    @Test
    fun futureSchema_isReportedWithoutDowngrade() {
        val result = FirewallPolicyCodec.decode("KOSCH_FIREWALL_POLICY|2\n")

        assertEquals(FirewallPolicyDecodeResult.FutureSchema(2), result)
    }

    @Test
    fun malformedAndLegacyData_isRejectedInsteadOfRepaired() {
        val malformed = FirewallPolicyCodec.decode("not-a-policy")
        val legacy = FirewallPolicyCodec.decode("KOSCH_FIREWALL_POLICY|0\n")
        val malformedRule = FirewallPolicyCodec.decode(
            "KOSCH_FIREWALL_POLICY|1\nnot-base64\t1\tBLOCK\t1\t-\t-\t-\t-\t-\t-\t-\t0\n",
        )

        assertTrue(malformed is FirewallPolicyDecodeResult.Corrupt)
        assertTrue(legacy is FirewallPolicyDecodeResult.Corrupt)
        assertTrue(malformedRule is FirewallPolicyDecodeResult.Corrupt)
    }

    @Test
    fun codec_rejectsIncompletePortRangeAndUnknownReservedFlags() {
        val base = FirewallRule(
            id = "port",
            priority = 1,
            verdict = FirewallVerdict.BLOCK,
            protocol = TrafficProtocol.TCP,
            remotePortRange = PortRange(443, 443),
        )
        val encoded = FirewallPolicyCodec.encode(FirewallPolicyDocument(rules = listOf(base)))
        val ruleLine = encoded.lineSequence().drop(1).first { it.isNotEmpty() }
        val fields = ruleLine.split('\t').toMutableList()

        fields[8] = "-"
        assertTrue(
            FirewallPolicyCodec.decode("KOSCH_FIREWALL_POLICY|1\n${fields.joinToString("\t")}\n") is
                FirewallPolicyDecodeResult.Corrupt,
        )

        fields[8] = "443"
        fields[11] = "1"
        assertTrue(
            FirewallPolicyCodec.decode("KOSCH_FIREWALL_POLICY|1\n${fields.joinToString("\t")}\n") is
                FirewallPolicyDecodeResult.Corrupt,
        )
    }

    @Test
    fun document_reusesFirewallPolicyLimits() {
        val maxRules = (0 until FirewallPolicySet.MAX_RULES).map { index ->
            FirewallRule("r-$index", index, FirewallVerdict.ALLOW)
        }
        FirewallPolicyDocument(rules = maxRules)

        val tooMany = maxRules + FirewallRule("overflow", 99_999, FirewallVerdict.ALLOW)
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            FirewallPolicyDocument(rules = tooMany)
        }
    }
}
