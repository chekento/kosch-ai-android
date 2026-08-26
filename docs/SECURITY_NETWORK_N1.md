# Security & Network Center · N1 consent boundary

Status: M2.7 N1 foundation

N1 prepares the Android VPN consent boundary without becoming a traffic engine. The implementation follows Android's `VpnService` contract but deliberately stops before service activation, interface establishment, packet forwarding, traffic inspection, firewall decisions, proxy routing, traffic history, or per-app network accounting.

Official platform reference: https://developer.android.com/reference/android/net/VpnService

## Fail-closed state model

| Android authorization | KoSch engine state | Traffic inspection | Firewall | Proxy | Counters |
|---|---|---|---|---|---|
| Unknown | Inactive | Off | Off | Direct | Zero |
| Consent required | Inactive | Off | Off | Direct | Zero |
| Authorized | Ready for N2 | Off | Off | Direct | Zero |

`AUTHORIZED` is therefore not presented as “VPN active”. It only means Android currently reports KoSch as prepared to use `VpnService`. That right can be revoked outside the launcher and must be re-checked before any later N2 activation.

## Android boundary

- `VpnConsentGateway` is the only N1 code path that calls `VpnService.prepare(context)`.
- A non-null result is only a system consent Intent. N1 does not launch it from background code.
- A null result means Android currently reports the package as prepared.
- Exceptions map to `UNKNOWN`, never to authorized.
- `KoSchConsentVpnService` is deliberately inert and immediately stops if it is started before N2 exists.
- The service is protected by `android.permission.BIND_VPN_SERVICE` and advertises the required `android.net.VpnService` service action.
- `android.net.VpnService.SUPPORTS_ALWAYS_ON` is explicitly `false` in N1.
- A visible active VPN is treated as a conflict boundary. KoSch must not silently displace another VPN; the user must receive a clear warning and make an explicit decision.

## Permission budget

N1 does not add `INTERNET`, usage access, package-wide traffic permissions, foreground-service permissions, notification permissions, location, contacts, or storage permissions. The app-level `uses-permission` allowlist remains the existing `ACCESS_NETWORK_STATE` only.

The CI source and packaged-APK permission gates remain authoritative. If an unexpected permission appears, the build must fail rather than silently widening access.

## Privacy contract

N1 collects no packet payloads, DNS queries, hostnames, IP-flow history, ports, byte totals, per-app network history, proxy credentials, certificates, or firewall telemetry. The state model rejects synthetic non-zero traffic counters so UI code cannot accidentally imply that monitoring already exists.

VPN authorization is a platform capability state, not proof that traffic is routed through KoSch.

### Persistence and backup boundary

N1 intentionally does not persist or export security/network runtime state. In particular, the normal portable Workspace backup must not contain:

- Android VPN authorization state or an assumed “active VPN” state;
- traffic history, destination IPs, ports, DNS observations or flow mappings;
- upload/download counters or allow/block counters;
- temporary UID/package attribution;
- proxy, upstream-gateway or tunnel credentials;
- certificates or private key material;
- transient VPN-conflict state;
- sensitive network identifiers.

The N1 navigation slice also leaves the persistent `HomePage` enum and portable backup v3 schema unchanged. That preserves compatibility with older backup readers and prevents an N1-only runtime route from becoming a portable device-state claim.

Future firewall rules may receive a dedicated portable representation only after secrets and device-local references are cleanly separated. Runtime traffic history and credentials remain excluded from ordinary Workspace backup.

## Google Play production gate

N1 is an internal/beta architecture and consent-boundary stage. The inert `KoSchConsentVpnService` is **not**, by itself, evidence that KoSch already provides a production-eligible VPN/security function.

Before a Google Play production artifact includes the VpnService component, one of these paths is required:

1. implement and validate an actually eligible Security/Firewall/Network capability in the later N2/N3 stages, complete the required Play VpnService declaration and listing disclosure, provide any required prominent in-app disclosure/consent, and retain review evidence; or
2. exclude the VpnService component from the Play production artifact.

There is no benchmark or security-score increase merely for declaring a VpnService or receiving Android authorization.

Policy references reviewed for this gate on 2026-08-26:

- https://support.google.com/googleplay/android-developer/answer/16909972
- https://support.google.com/googleplay/android-developer/answer/12564964

## N2 entry gate

N2 may only begin after a separate reviewable change provides all of the following together:

1. explicit foreground activation initiated by the user;
2. a real forwarding path that does not black-hole traffic;
3. exact new manifest permissions with CI allowlist updates;
4. persistent notification and stop control when required by Android;
5. fail-open/fail-closed behavior documented for crashes, reboot, revoke, provider conflicts and network changes;
6. API 29/33/36 device evidence plus physical OEM testing;
7. privacy disclosures for every retained metadata field;
8. no payload inspection unless a later feature has a separate explicit justification and consent design;
9. firewall/proxy features disabled until their own tests and recovery paths are complete.

Until those gates pass, N1 must remain consent-only and traffic-neutral.
