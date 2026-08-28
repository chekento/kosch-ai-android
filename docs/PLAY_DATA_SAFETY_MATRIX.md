# Google Play · Data Safety truth matrix

Status: release-readiness source of truth for the current **KAL (KoSch AI Launcher)** branch.

This file documents observed/current product behavior. It is not permission to broaden a store claim. If implementation changes, `ReleaseComplianceCatalog`, this matrix, the in-app disclosure and Play Console declarations must be reviewed together.

## Current production boundary

| Capability | User data / sensitive surface | Default | Persistence inside KAL | KAL network transfer | External handoff | Production release |
|---|---|---:|---|---:|---|---:|
| Camera Awareness | Camera context frame | OFF | Process-only requested frame | No by default | None by default | Yes |
| Screen Awareness | Screen context frame | OFF | Process-only requested frame | No by default | None by default | Yes |
| Notification badges | Package-level notification counts | Android access not granted by default | Process-only counters | No | No | Yes |
| Local Usage Learning | App key, launch count, last-used time | ON | Bounded local model | No | No | Yes |
| Local Audit Log | Timestamp + action + outcome metadata | ON | Bounded local log; no free text | No | User-controlled CSV export | Yes |
| AI prompt handoff | User-entered text | No automatic handoff | Process/UI state only | No direct KAL upload | Two-step explicit Android Share to selected destination app | Yes |
| Provider authentication | OAuth authorization metadata / provider credential | OFF | Token/key encrypted with Android Keystore; excluded from portable backup | **Yes, only after Connect** | System browser for OAuth consent | Yes |
| Direct provider request | User-selected prompt/context sent to a connected LLM provider | OFF | Request content process-only unless another feature explicitly persists it | **Yes, only after Cloud Access + foreground request** | Direct HTTPS request to selected provider | Yes |
| N1 VPN prototype | No traffic processing in N1 | OFF | None | No | No | **No – debug only** |

## Important interpretation rules

### KAL vs. destination app/provider

KAL has two distinct external AI paths:

1. **Android handoff:** KAL does not upload the prompt itself. Android transfers the user-selected text to the selected destination app only after KAL's explicit handoff boundary. Subsequent processing is controlled by that app/provider.
2. **Direct Provider Connection:** after the user explicitly connects a provider and enables Cloud Access, KAL may transmit the confirmed prompt/context directly to that provider over HTTPS. This is a KAL-controlled network transfer and must be disclosed as such.

The product must never blur these two paths in UI, privacy text or Play declarations.

### Cloud Access and provider connections

`android.permission.INTERNET` is present because KAL now has an optional direct-provider layer. The permission does **not** mean cloud use is enabled by default.

The production invariants are:

- Cloud Access defaults to **OFF**.
- A provider must be explicitly connected before KAL may use it directly.
- OAuth opens in the system browser; KAL never collects a provider password.
- Native/public OAuth uses PKCE where supported; no reusable OAuth client secret may be embedded in the APK.
- KAL performs no background LLM requests.
- User content requires a foreground user action or an assistant action that has passed the applicable confirmation gate.
- Provider access/refresh tokens and generated API keys are encrypted with Android Keystore and are not part of portable backup.
- Disconnect removes the KAL-held provider credential.

OpenRouter is the first direct OAuth route: KAL uses OpenRouter's PKCE authorization flow with a one-shot loopback callback on `127.0.0.1`, then exchanges the returned authorization code for a user-controlled OpenRouter key over HTTPS.

### Camera and screen

Camera and Screen Awareness are separate manual opt-ins. They must never be restored from portable backup as enabled and must never be activated solely by AI routing, preferences, recommendations or imported settings.

Direct Provider Connections do not change that rule. A connected provider does not automatically gain Camera or Screen content. Any visual context release remains subject to its own existing capture/consent boundary plus the direct-provider content disclosure.

### Notifications

Notification access is an Android special access grant. KAL's production badge implementation must remain package-count-only unless this matrix and the corresponding disclosure are deliberately revised. Notification title, body, people and extras are not part of the current badge data model.

### Local learning and audit

Local Usage Learning is content-free and bounded. The audit log contains action/outcome metadata and has no free-text field. Both are local product data, not analytics/advertising telemetry.

### VPN

`KoSchConsentVpnService` and `SecurityNetworkActivity` are declared only in `src/debug/AndroidManifest.xml`. The production manifest must remain free of `VpnService` until a genuine eligible security/network capability is implemented, reviewed and declared for Google Play. The presence of ordinary HTTPS provider connectivity does not justify a release `VpnService`.

## Store declaration gate

Before every public Play release:

1. Build the release artifact and inspect the merged release manifest.
2. Confirm that `INTERNET` is present only as part of the reviewed direct-provider capability and that no new network SDK has silently entered the dependency graph.
3. Confirm that `VpnService` and the N1 Security surface are absent unless a later explicit production gate has superseded this rule.
4. Reconcile every sensitive capability with `ReleaseComplianceCatalog`.
5. Reconcile the Play Data Safety form with this matrix and the actual release binary, including direct transmission of prompts/context to user-selected AI providers.
6. Reconcile the public privacy policy with the same behavior and identify third-party provider processing as applicable.
7. Confirm there are no newly introduced analytics, advertising, crash-reporting or background-network SDKs without an explicit review.
8. Confirm that an unconnected provider cannot receive content and that Cloud Access remains OFF on a fresh install/restore.
9. Confirm that user text cannot leave KAL through either handoff or direct-provider execution without an affirmative foreground action.
10. Confirm provider credentials are absent from KAL portable backup/export artifacts.

## Current marketing-safe wording

Safe:

> KAL performs launcher routing and context classification locally by default. Optional direct AI provider connections are off until the user connects a provider and enables Cloud Access. When a direct provider is used, KAL sends only the confirmed request/context required for that action; provider credentials are encrypted on-device with Android Keystore. Sensitive Camera and Screen Awareness remain separate opt-ins.

Avoid blanket claims such as:

> Everything always stays on-device.

That statement is inaccurate after the user deliberately hands content to a third-party AI app or uses a direct connected provider.
