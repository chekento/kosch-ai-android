package cloud.kosch.aiandroid.security.network

/** Pure N3 policy types. This file has no packet-forwarding or Android side effects. */
enum class FirewallVerdict { ALLOW, BLOCK }

data class PortRange(
    val first: Int,
    val last: Int,
) {
    init {
        require(first in 0..65535 && last in 0..65535 && first <= last) {
            "Firewall port range is invalid"
        }
    }

    operator fun contains(port: Int): Boolean = port in first..last
}

/**
 * Explicit live-flow input for N3 evaluation.
 * Direction is never inferred from aggregated telemetry because a flow snapshot can contain both directions.
 */
data class FirewallFlowContext(
    val direction: TrafficDirection,
    val protocol: TrafficProtocol,
    val remoteAddress: NetworkAddress,
    val remotePort: Int?,
    val ownerUid: Int?,
    val packageName: String?,
    val packageAttributionAmbiguous: Boolean = false,
) {
    init {
        require(remotePort == null || remotePort in 0..65535) { "Remote port is invalid" }
        require(ownerUid == null || ownerUid >= 0) { "Owner UID is invalid" }
        require(packageName == null || (packageName.isNotBlank() && packageName.length <= 255)) {
            "Package name is invalid"
        }
        require(packageName == null || ownerUid != null) { "Package attribution requires a UID" }
        require(!packageAttributionAmbiguous || packageName == null) {
            "Ambiguous package attribution cannot claim one package"
        }
    }
}

data class FirewallRule(
    val id: String,
    val priority: Int,
    val verdict: FirewallVerdict,
    val enabled: Boolean = true,
    val direction: TrafficDirection? = null,
    val protocol: TrafficProtocol? = null,
    val remoteCidr: CidrBlock? = null,
    val remotePortRange: PortRange? = null,
    val ownerUid: Int? = null,
    val packageName: String? = null,
) {
    init {
        require(id.matches(Regex("[A-Za-z0-9][A-Za-z0-9._:-]{0,63}"))) { "Firewall rule id is invalid" }
        require(priority in 0..100_000) { "Firewall rule priority is invalid" }
        require(ownerUid == null || ownerUid >= 0) { "Firewall rule UID is invalid" }
        require(packageName == null || (packageName.isNotBlank() && packageName.length <= 255)) {
            "Firewall package name is invalid"
        }
        require(packageName == null || ownerUid != null) {
            "Package-specific firewall rules require an explicit UID"
        }
        require(
            remotePortRange == null || protocol == null ||
                protocol == TrafficProtocol.TCP || protocol == TrafficProtocol.UDP,
        ) { "Port ranges are only valid for TCP/UDP or protocol-agnostic rules" }
    }

    fun matches(flow: FirewallFlowContext): Boolean {
        if (!enabled) return false
        if (direction != null && direction != flow.direction) return false
        if (protocol != null && protocol != flow.protocol) return false
        if (remoteCidr != null && !remoteCidr.contains(flow.remoteAddress)) return false
        if (ownerUid != null && ownerUid != flow.ownerUid) return false

        if (packageName != null) {
            if (flow.packageAttributionAmbiguous) return false
            if (flow.packageName != packageName) return false
        }

        if (remotePortRange != null) {
            val port = flow.remotePort ?: return false
            if (port !in remotePortRange) return false
        }
        return true
    }
}

data class FirewallEvaluation(
    val verdict: FirewallVerdict,
    val matchedRuleId: String?,
) {
    init {
        require(verdict != FirewallVerdict.BLOCK || matchedRuleId != null) {
            "Blocking requires an explicit matched rule"
        }
    }
}

/**
 * Bounded, deterministic firewall policy evaluator.
 *
 * Lower numeric priority wins. Ties are resolved by stable rule id, never insertion order.
 * The unmatched default is ALLOW to avoid turning a malformed/empty policy into a device-wide outage.
 * This pure core does not enforce the result; active blocking remains behind the N2/N3 runtime gates.
 */
class FirewallPolicySet(rules: List<FirewallRule>) {
    private val orderedRules: List<FirewallRule>

    init {
        require(rules.size <= MAX_RULES) { "Firewall rule count exceeds limit" }
        require(rules.map(FirewallRule::id).toSet().size == rules.size) { "Firewall rule ids must be unique" }
        orderedRules = rules.sortedWith(compareBy(FirewallRule::priority, FirewallRule::id))
    }

    fun evaluate(flow: FirewallFlowContext): FirewallEvaluation {
        val rule = orderedRules.firstOrNull { it.matches(flow) }
            ?: return FirewallEvaluation(FirewallVerdict.ALLOW, null)
        return FirewallEvaluation(rule.verdict, rule.id)
    }

    fun rules(): List<FirewallRule> = orderedRules.toList()

    companion object {
        const val MAX_RULES = 256
        val ALLOW_ALL = FirewallPolicySet(emptyList())
    }
}
