# Security & Network · N2 flow-core foundation

Status: M2.7 N2 pure-core slice

This slice begins N2 without activating Android VPN traffic. It provides the deterministic, testable metadata core that a later reviewed forwarding engine may feed.

## Included

- immutable numeric IPv4/IPv6 addresses with DNS-free parsing;
- IPv4/IPv6 CIDR matching as a generic address primitive;
- strict IPv4/IPv6 packet metadata parsing;
- bounded traversal of IPv6 Hop-by-Hop, Routing, Fragment, AH and Destination Options headers;
- TCP/UDP source and destination ports only when a complete validated transport header is present;
- non-initial fragments never invent transport ports;
- explicit unsupported result for IPv6 jumbograms rather than partial interpretation;
- direction-aware flow identity;
- UID/package attribution only when supplied by a trusted Android-facing adapter;
- unknown ownership remains unknown and is not merged into a later known UID;
- conflicting package names for one UID are marked ambiguous instead of guessed;
- separate inbound/outbound byte and packet counters;
- hard-cap access-order flow table;
- hard-cap metadata-only traffic ledger;
- no packet payload field in parser or event models.

## Deliberately excluded

This is **not** an active VPN implementation. It does not change the manifest or permission budget and does not:

- call `VpnService.Builder.establish()`;
- create a TUN interface;
- forward packets;
- open upstream sockets;
- add `INTERNET` or foreground-service permissions;
- resolve domains or inspect DNS payloads;
- retain TLS/application payloads;
- make allow/block decisions;
- contain firewall rules or firewall verdicts;
- configure a proxy or upstream tunnel;
- persist traffic into the portable Workspace backup;
- raise the launcher/security benchmark score.

Firewall rule semantics remain N3. Proxy/upstream semantics remain N4.

## Parser safety contract

The parser returns one of three typed outcomes: `Parsed`, `Malformed`, or `Unsupported`. Callers must branch on the outcome and must not blindly cast malformed input to parsed metadata.

The previous draft #22 contained a failing TCP unit fixture: it supplied source/destination ports but left the TCP data-offset nibble at zero. The parser correctly rejected that packet; the test then blindly cast the malformed outcome and failed with `ClassCastException`. The N2 rebuild fixes the fixture with data offset `0x50` and uses typed outcome assertions.

Random hostile input is fuzzed through the pure parser and must not escape as an exception.

## Attribution boundary

The pure core does not call Android connection-owner APIs itself. A later Android adapter may supply a UID/package attribution only when the platform actually resolves it. Unknown traffic remains `ownerUid = null`; package attribution without a known UID is rejected. Work-profile separation is naturally preserved by Android UID identity and must not be collapsed by UI adapters.

## Retention boundary

`BoundedFlowTable` and `BoundedTrafficLedger` are session-local memory structures. Both reject unbounded capacities and evict old records when full. No payload bytes, hostnames, cookies, messages, credentials, request bodies or firewall verdicts exist in the stored event schema.

Persistence beyond the active session requires a later explicit privacy/retention design and is not introduced here.

## Active-forwarding gate

A later N2 activation slice may establish a VPN only after a forwarding strategy is reviewed and implemented together with:

1. working outbound and inbound forwarding so normal connectivity cannot be black-holed;
2. protected upstream sockets (`VpnService.protect(...)`) where applicable to prevent VPN loops;
3. the exact `INTERNET`/foreground-service permission update and CI allowlist changes;
4. persistent foreground notification and explicit stop control where Android requires them;
5. crash, revoke, replace/conflict, network-change and process-recovery behavior;
6. API 29/33/36 evidence plus physical OEM validation;
7. bounded metadata flow from the TUN reader into this core;
8. no firewall blocking until N3 has its own recovery and rule tests.

Until that gate is satisfied, N2 remains a pure telemetry-core foundation and `KoSchConsentVpnService` stays inert.
