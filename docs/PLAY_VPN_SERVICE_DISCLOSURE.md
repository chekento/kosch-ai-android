# Google Play · VpnService production gate

Status: M2.7 N1 internal/beta readiness document

KoSch declares an Android `VpnService` component as part of the N1 consent-boundary prototype. N1 is intentionally **not** a traffic engine: it does not create a VPN interface, forward packets, inspect DNS or payloads, collect flow history, filter traffic, proxy traffic, install certificates, or monetize routing.

This document is a release gate, not a claim that N1 is already eligible for production distribution through Google Play.

## Current Google Play policy boundary

Official policy references reviewed 2026-08-26:

- https://support.google.com/googleplay/android-developer/answer/16909972
- https://support.google.com/googleplay/android-developer/answer/12564964

Google Play permits `VpnService` for genuine core VPN functionality and specified eligible use cases such as device security/firewall and network-related tools. Apps using `VpnService` must document that use in their Play listing and complete the VpnService declaration. Where personal or sensitive user data is accessed or collected through VpnService, Play additionally requires prominent in-app disclosure and affirmative consent.

Google Play also prohibits redirecting or manipulating traffic from other apps for monetization purposes.

## N1 facts that must be disclosed accurately

For the N1 build currently represented in source:

- Android VPN authorization is a platform capability state, **not** proof of an active KoSch VPN;
- `KoSchConsentVpnService` is inert and stops if started before a later forwarding engine exists;
- `INTERNET` is not declared;
- no packet, DNS, hostname, destination, port, byte, per-app traffic, firewall, proxy, or certificate data is collected through VpnService;
- no VPN authorization state, conflict state, traffic state, or network history is persisted in the workspace;
- none of those states is included in portable workspace backup;
- active/unknown VPN conflict state requires an additional KoSch acknowledgement before Android's consent dialog is launched;
- N1 never silently starts a VPN;
- N1 does not monetize, sell, reroute, or upload third-party traffic.

These facts must not be expanded into claims about forwarding, firewall protection, traffic monitoring, privacy filtering, or proxy support until those capabilities exist and have device evidence.

## Production Play release gate

A production Play artifact must **not** contain the N1 VpnService solely because the consent-boundary prototype exists.

Before shipping a Play production artifact that contains `KoSchConsentVpnService`, one of the following must be true:

### Path A — eligible Security/Network capability is real

1. the relevant N2/N3 Security/Firewall/Network capability is implemented and is a genuine user-facing purpose for VpnService;
2. packet forwarding is functional and cannot black-hole normal connectivity;
3. required manifest permissions and foreground-service behavior are explicit and CI-enforced;
4. traffic-data fields, retention, deletion, export and sharing behavior are documented exactly;
5. prominent in-app disclosure and affirmative consent are implemented wherever Play policy requires them;
6. the Play VpnService declaration is completed accurately;
7. the Play listing documents VpnService use accurately;
8. the required short review video demonstrates the real disclosure/consent flow;
9. physical-device and managed-device evidence covers revoke, replace/conflict, crash/restart, IPv4/IPv6 and recovery paths;
10. no traffic is redirected/manipulated for monetization.

### Path B — VpnService is excluded from the Play artifact

If the eligible Security/Network capability is not production-ready, the Play build must exclude the VpnService component rather than presenting an inert prototype as a production use case.

The internal/beta build may continue to use N1 for architecture and consent-boundary testing.

## Store listing draft — only after Path A is satisfied

The following wording is a **template**, not approved production copy until Path A is complete:

> KoSch uses Android's VpnService for its user-controlled on-device Security & Network capability. Activation is explicit. KoSch does not use VPN routing to monetize or sell third-party traffic. The app explains what network metadata is processed, why it is needed, how long it is retained and how it can be cleared before activation.

Do not publish this wording while N1 remains consent-only; that would imply a real active capability that the current build does not yet provide.

## Score and release discipline

Declaring `VpnService`, presenting a consent screen, or passing an emulator test does not by itself raise the production/security benchmark. Production-grade claims require the actual eligible capability plus measured evidence and Play-policy readiness.