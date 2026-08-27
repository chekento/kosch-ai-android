# Security & Network · N3 firewall policy persistence

This stage persists **policy configuration only**. It does not enforce a firewall verdict, acquire a TUN interface, forward packets, add `INTERNET`, or change Android routing.

## Safety contract

- Schema version: `1`.
- Maximum rule count remains the N3 core limit of 256.
- Maximum serialized policy size: 128 KiB.
- Encoding is deterministic and dependency-free; strings are URL-safe Base64 and rule fields are canonical enums/integers.
- Future schema versions are reported as future state and are never downgraded.
- Corrupt state is reported as corrupt and is never repaired, rewritten, or replaced during load.
- Loading is side-effect free.
- Replacing or resetting policy state is an explicit operation only.
- A valid stored policy is **not** an activation signal. Live enforcement remains blocked behind N2 forwarder evidence, no-black-hole/revoke/fault gates, explicit user activation, and device evidence.
- The unmatched policy-core default remains `ALLOW`; however an invalid/future stored document is not converted into `ALLOW_ALL` for activation. A later runtime gate must refuse activation until the policy state is explicitly resolved.

## Android storage boundary

`SharedPreferencesFirewallPolicyStorage` uses app-private `MODE_PRIVATE` preferences and synchronous `commit()` for explicit policy writes/removal. The application already disables Android Auto Backup. Firewall policy is also not part of the portable Workspace backup schema.

A future rule export/import feature, if added, must be a separate explicit security-policy operation with preview, schema validation and user confirmation. Proxy credentials or other future secrets must never be stored in this plain policy document.

## Evidence expected before merge

- JVM roundtrip/determinism/future/corrupt/limit tests.
- JVM repository tests proving reads never rewrite corrupt/future raw state.
- API 36 instrumentation proving future raw state survives fresh Android repository instances and explicit replacement roundtrips.
- Existing Manifest and packaged-permission budgets remain unchanged.

## Still not implemented here

- no active firewall;
- no packet drop/allow enforcement;
- no TUN/VPN activation;
- no traffic capture;
- no DNS/TLS/payload inspection;
- no proxy routing;
- no rule editor UI;
- no benchmark/rank increase.
