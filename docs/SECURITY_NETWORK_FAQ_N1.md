# Security & Network Center · N1 FAQ

This FAQ is the user-facing contract for the M2.7 N1 Security & Network stage. N1 is intentionally traffic-neutral: it prepares and explains Android's VPN authorization boundary but does not yet operate a VPN tunnel, firewall, traffic analyzer, or proxy.

## Is KoSch already acting as a VPN in N1?

No. Android may report KoSch as authorized to use `VpnService`, but authorization is not an active VPN. N1 does not establish a VPN interface, forward traffic, inspect packets or DNS, filter connections, or change routing.

## Does KoSch start VPN access automatically?

No. The Android VPN authorization path can only be opened after an explicit user action. A cancellation or rejected authorization leaves networking unchanged. KoSch must re-check Android's state after the system result instead of assuming success.

## What happens if another VPN is already active?

Android permits only one active VPN path per user/profile. N1 checks for a visible VPN transport and must not silently displace it. A detected or uncertain conflict requires an explicit acknowledgement before KoSch can continue to Android's own authorization dialog.

## What network data does N1 inspect or store?

None. N1 does not collect or persist packet payloads, DNS requests, hostnames, destination IPs, ports, per-app flow history, upload/download totals, allow/block decisions, proxy credentials, certificates, or firewall telemetry. The N1 UI traffic counters are required to remain zero.

## Are Security & Network data included in the Workspace backup?

No runtime network state is exported in N1. VPN authorization, traffic history, temporary UID/package mappings, network identifiers, conflict state, traffic counters, proxy/upstream credentials and certificate/key material are excluded from the normal portable Workspace backup. N1 also leaves the persistent `HomePage`/backup schema unchanged for compatibility with older readers.

## Does N1 add broad network permissions?

No. N1 deliberately does not add `INTERNET`, foreground-service permissions, usage access, location, contacts, storage, or notification permissions. The inert VPN service component is protected by Android's `BIND_VPN_SERVICE` service contract. Permission-budget checks in CI remain authoritative.

## Is the N1 VpnService ready for a Google Play production release?

Not by itself. N1 is an internal/beta architecture and consent-boundary stage. Before a production Play artifact includes VpnService, KoSch must either implement and validate an actually eligible Security/Firewall/Network function in the later stages and complete the applicable Play declaration, listing disclosure, prominent disclosure/consent and review evidence, or exclude the VpnService component from that production artifact.

## When does real traffic analysis begin?

Only in N2 after a separate review. The gate requires a real forwarding path that cannot silently black-hole traffic, explicit foreground activation and stop control, exact permission allowlists, bounded metadata retention, safe UID/app attribution where Android permits it, failure/recovery handling, and API/OEM evidence. N1 must not simulate these capabilities.

## When does the firewall become active?

Firewall decisions belong to N3. The initial policy will default to allow, rules will be deterministic and ordered, and recovery/emergency-disable behavior must be proven before active blocking is accepted. N1 performs no allow/block decisions.

## When will proxy or upstream routing be available?

Proxy/upstream routing belongs to N4. The UI must distinguish advisory Android HTTP-proxy configuration from transports that KoSch actually forwards itself. Credentials must remain encrypted and outside ordinary Workspace backups. KoSch must not claim universal transparent proxy support before it exists and has been tested.

## Does adding VpnService improve the benchmark score?

No. Declaring a component, receiving Android authorization, or rendering an inactive Security page does not increase the security or launcher benchmark score. Score changes require measured implementation evidence against the project's quality gates.

## Release-policy references

Reviewed for the N1 production gate on 2026-08-26:

- https://support.google.com/googleplay/android-developer/answer/16909972
- https://support.google.com/googleplay/android-developer/answer/12564964
- https://developer.android.com/reference/android/net/VpnService
