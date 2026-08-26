# Security & Network N3 — Firewall policy core

Status: **pure policy foundation only — no live blocking**

This stage defines the deterministic rule language that may later sit between validated N2 live-flow metadata and a packet-forwarding verdict. It deliberately has no Android, VPN, socket, packet-buffer or persistence side effects.

## Safety defaults

- Unmatched traffic evaluates to `ALLOW`.
- `BLOCK` can only result from an explicit matched rule id.
- Lower numeric priority wins; equal-priority rules are ordered by stable rule id, not insertion order.
- Disabled rules never match.
- Rule count is capped at 256.
- IDs, UIDs, package names and port ranges are bounded and validated.
- Package-specific rules require both an explicit UID and an exact package name.
- Unknown or ambiguous package attribution never matches a package-specific rule.
- UID-only rules may still match a known UID even when package attribution is ambiguous.
- CIDR matching uses the existing numeric `NetworkAddress` / `CidrBlock` implementation and performs no DNS lookup.
- Port matching is valid only for TCP/UDP (or a protocol-agnostic rule that naturally fails when no live remote port exists).
- Direction is supplied explicitly by the live-flow caller. It is never inferred from aggregated telemetry because an established flow can contain both inbound and outbound packets.

## Rule dimensions

A rule may constrain any combination of:

- direction: inbound / outbound;
- protocol: TCP / UDP / ICMP / ICMPv6 / other;
- remote IPv4 or IPv6 CIDR;
- remote TCP/UDP port range;
- owner UID;
- exact package name tied to that UID.

A rule resolves to `ALLOW` or `BLOCK`. The first deterministic match wins.

## Explicitly not included

This core does not:

- establish or configure a VPN;
- acquire a TUN file descriptor;
- forward or drop a packet;
- add `INTERNET` or foreground-service permissions;
- persist firewall rules;
- inspect packet payloads, TLS, DNS contents or hostnames;
- install certificates;
- expose proxy routing;
- claim benchmark/ranking improvement.

## Activation gate

Live blocking remains forbidden until N2 has separately demonstrated a working fail-safe forwarder with protected upstream sockets, deterministic stop/revoke handling, no-black-hole recovery and device evidence. A later N3 runtime slice must then prove that the exact live-flow context evaluated by this core corresponds to the packet being allowed or dropped, including unknown/ambiguous UID cases and IPv4/IPv6 traffic.
