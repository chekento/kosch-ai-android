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

## Permission budget

N1 does not add `INTERNET`, usage access, package-wide traffic permissions, foreground-service permissions, notification permissions, location, contacts, or storage permissions. The app-level `uses-permission` allowlist remains the existing `ACCESS_NETWORK_STATE` only.

The CI source and packaged-APK permission gates remain authoritative. If an unexpected permission appears, the build must fail rather than silently widening access.

## Privacy contract

N1 collects no packet payloads, DNS queries, hostnames, IP-flow history, ports, byte totals, per-app network history, proxy credentials, certificates, or firewall telemetry. The state model rejects synthetic non-zero traffic counters so UI code cannot accidentally imply that monitoring already exists.

VPN authorization is a platform capability state, not proof that traffic is routed through KoSch.

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
