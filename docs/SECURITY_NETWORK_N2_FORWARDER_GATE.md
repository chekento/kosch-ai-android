# Security & Network N2 — Forwarder activation gate

Status: **design-enforced, inactive**.

This slice does not create a VPN interface and does not forward traffic. It defines the contract that must be satisfied before a later Android activation slice may attempt either action.

## Why this exists

A local Android VPN becomes dangerous if the app establishes a TUN interface before it has a proven path to forward packets safely. A partial implementation can silently black-hole the user's connectivity or loop its own upstream sockets back through the VPN.

N2 therefore separates four concerns:

1. Android VPN authorization remains the existing explicit N1 boundary.
2. A pure activation gate checks forwarder evidence.
3. A fail-safe state machine governs start/active/stop/cleanup transitions.
4. Android TUN/socket ownership is deferred to a later adapter slice.

## Evidence required before start is even attempted

`N2ActivationGate` blocks activation unless all of the following are true:

- Android reports VPN authorization as `AUTHORIZED`;
- a usable network is currently available;
- evidence exists for the exact forwarder implementation/revision;
- IPv4 forwarding has been verified;
- IPv6 forwarding has been verified;
- upstream sockets are protected from VPN recursion;
- the return path is verified;
- stop-on-fault behavior is verified;
- no-black-hole behavior is verified.

Missing any one property returns a concrete `N2ActivationBlockReason`. There is no permissive fallback.

## State-machine contract

The only legal route to `ACTIVE` is:

`INACTIVE -> RequestStart(Ready) -> STARTING -> ForwarderStarted(same generation) -> ACTIVE`

A stale or unexpected start completion is treated as `FAILED_SAFE`, not as success.

From `STARTING` or `ACTIVE`, any of these events require cleanup:

- user stop;
- VPN authorization revocation;
- network change;
- forwarder fault;
- start failure.

Cleanup must complete before the machine returns to a fresh `INACTIVE` state. Activation generations prevent stale asynchronous completions from reviving an old session.

## Forwarder contract

`N2PacketForwarder` is platform-neutral in this slice. It exposes readiness evidence plus start/stop semantics, but no Android file descriptor, socket or TUN object crosses the core contract yet.

The later Android adapter must own those resources and may only hand an implementation-specific session to the forwarder after the activation gate is `Ready`.

The contract additionally requires:

- positive activation generations;
- explicit IPv4/IPv6 selection;
- bounded MTU configuration;
- duplicate/stale generation rejection;
- idempotent stop semantics;
- cleanup after partial start failure.

## Still explicitly not included

- no `INTERNET` permission;
- no new foreground-service permission/type;
- no `VpnService.Builder.establish()`;
- no TUN descriptor;
- no upstream socket;
- no packet forwarding;
- no DNS or payload inspection;
- no firewall decision engine (N3);
- no proxy/upstream routing selection (N4);
- no persistent traffic history;
- no benchmark-score increase.

## Next activation slice gate

Before active forwarding can be implemented, the repository must select and review a concrete forwarder strategy and prove:

- license/dependency acceptability;
- IPv4 and IPv6 data paths;
- Android `VpnService.protect(...)` handling for upstream sockets;
- deterministic shutdown on revoke/network loss/process failure;
- foreground-service and notification requirements for supported Android versions;
- API 29/33/36 behavior;
- physical-device evidence on target OEM classes;
- no-black-hole recovery if startup or forwarding fails.

Until those gates pass, `KoSchConsentVpnService` remains inert.
