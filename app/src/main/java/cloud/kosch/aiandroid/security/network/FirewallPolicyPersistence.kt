package cloud.kosch.aiandroid.security.network

import java.nio.charset.StandardCharsets
import java.util.Base64

const val FIREWALL_POLICY_SCHEMA_VERSION = 1
const val FIREWALL_POLICY_MAX_SERIALIZED_BYTES = 128 * 1024

/**
 * Portable-in-process representation of the dormant N3 rule set.
 *
 * This document is not an activation signal. Runtime enforcement remains blocked behind the N2/N3
 * activation gates even when a document is valid and non-empty.
 */
data class FirewallPolicyDocument(
    val schemaVersion: Int = FIREWALL_POLICY_SCHEMA_VERSION,
    val rules: List<FirewallRule>,
) {
    init {
        require(schemaVersion == FIREWALL_POLICY_SCHEMA_VERSION) { "Unsupported firewall policy schema" }
        FirewallPolicySet(rules)
    }

    fun normalizedRules(): List<FirewallRule> = FirewallPolicySet(rules).rules()
}

sealed interface FirewallPolicyDecodeResult {
    data class Valid(val document: FirewallPolicyDocument) : FirewallPolicyDecodeResult
    data class FutureSchema(val schemaVersion: Int) : FirewallPolicyDecodeResult
    data class Corrupt(val reason: String) : FirewallPolicyDecodeResult
}

/**
 * Strict text codec for N3 policy persistence.
 *
 * Strings use URL-safe Base64 and the remaining fields are canonical enums/integers. The format is
 * intentionally small and dependency-free so it can be unit-tested on the JVM and validated before any
 * Android storage write. Decode never repairs or downgrades malformed/future data.
 */
object FirewallPolicyCodec {
    private const val MAGIC = "KOSCH_FIREWALL_POLICY"
    private const val NULL = "-"
    private const val FIELD_COUNT = 12

    fun encode(document: FirewallPolicyDocument): String {
        require(document.schemaVersion == FIREWALL_POLICY_SCHEMA_VERSION)
        val normalized = document.normalizedRules()
        val encoded = buildString {
            append(MAGIC)
            append('|')
            append(FIREWALL_POLICY_SCHEMA_VERSION)
            append('\n')
            normalized.forEach { rule ->
                append(encodeRule(rule))
                append('\n')
            }
        }
        require(encoded.toByteArray(StandardCharsets.UTF_8).size <= FIREWALL_POLICY_MAX_SERIALIZED_BYTES) {
            "Firewall policy exceeds serialized size limit"
        }
        return encoded
    }

    fun decode(raw: String): FirewallPolicyDecodeResult {
        val byteSize = raw.toByteArray(StandardCharsets.UTF_8).size
        if (byteSize <= 0) return FirewallPolicyDecodeResult.Corrupt("empty policy")
        if (byteSize > FIREWALL_POLICY_MAX_SERIALIZED_BYTES) {
            return FirewallPolicyDecodeResult.Corrupt("policy exceeds serialized size limit")
        }

        return runCatching {
            val lines = raw.split('\n')
            val header = lines.firstOrNull().orEmpty()
            val headerParts = header.split('|')
            require(headerParts.size == 2 && headerParts[0] == MAGIC) { "invalid firewall policy header" }
            val schema = headerParts[1].toIntOrNull()
                ?: throw IllegalArgumentException("invalid firewall policy schema")
            if (schema > FIREWALL_POLICY_SCHEMA_VERSION) {
                return FirewallPolicyDecodeResult.FutureSchema(schema)
            }
            require(schema == FIREWALL_POLICY_SCHEMA_VERSION) { "unsupported legacy firewall policy schema" }

            val ruleLines = lines.drop(1).filter { it.isNotEmpty() }
            val rules = ruleLines.map(::decodeRule)
            FirewallPolicyDecodeResult.Valid(
                FirewallPolicyDocument(
                    schemaVersion = schema,
                    rules = rules,
                ),
            )
        }.getOrElse { error ->
            FirewallPolicyDecodeResult.Corrupt(error.message ?: "invalid firewall policy")
        }
    }

    private fun encodeRule(rule: FirewallRule): String = listOf(
        b64(rule.id),
        rule.priority.toString(),
        rule.verdict.name,
        if (rule.enabled) "1" else "0",
        rule.direction?.name ?: NULL,
        rule.protocol?.name ?: NULL,
        rule.remoteCidr?.toString()?.let(::b64) ?: NULL,
        rule.remotePortRange?.first?.toString() ?: NULL,
        rule.remotePortRange?.last?.toString() ?: NULL,
        rule.ownerUid?.toString() ?: NULL,
        rule.packageName?.let(::b64) ?: NULL,
        "0", // reserved for v1; must remain zero until a versioned schema assigns semantics.
    ).joinToString("\t")

    private fun decodeRule(line: String): FirewallRule {
        val fields = line.split('\t')
        require(fields.size == FIELD_COUNT) { "invalid firewall rule field count" }
        require(fields[11] == "0") { "unsupported firewall rule flags" }

        val portFirst = nullableInt(fields[7], "invalid firewall rule first port")
        val portLast = nullableInt(fields[8], "invalid firewall rule last port")
        require((portFirst == null) == (portLast == null)) { "incomplete firewall port range" }

        return FirewallRule(
            id = unb64(fields[0]),
            priority = fields[1].toIntOrNull()
                ?: throw IllegalArgumentException("invalid firewall rule priority"),
            verdict = enumValue<FirewallVerdict>(fields[2], "invalid firewall verdict"),
            enabled = when (fields[3]) {
                "1" -> true
                "0" -> false
                else -> throw IllegalArgumentException("invalid firewall enabled flag")
            },
            direction = nullableEnum<TrafficDirection>(fields[4], "invalid firewall direction"),
            protocol = nullableEnum<TrafficProtocol>(fields[5], "invalid firewall protocol"),
            remoteCidr = nullableString(fields[6])?.let { CidrBlock.parse(unb64(it)) },
            remotePortRange = if (portFirst != null && portLast != null) PortRange(portFirst, portLast) else null,
            ownerUid = nullableInt(fields[9], "invalid firewall UID"),
            packageName = nullableString(fields[10])?.let(::unb64),
        )
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String, error: String): T =
        enumValues<T>().firstOrNull { it.name == value } ?: throw IllegalArgumentException(error)

    private inline fun <reified T : Enum<T>> nullableEnum(value: String, error: String): T? =
        if (value == NULL) null else enumValue<T>(value, error)

    private fun nullableInt(value: String, error: String): Int? =
        if (value == NULL) null else value.toIntOrNull() ?: throw IllegalArgumentException(error)

    private fun nullableString(value: String): String? = if (value == NULL) null else value

    private fun b64(value: String): String = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun unb64(value: String): String = runCatching {
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    }.getOrElse { throw IllegalArgumentException("invalid firewall string encoding") }
}
