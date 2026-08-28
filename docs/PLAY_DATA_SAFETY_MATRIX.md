# Google Play · Data Safety truth matrix

Status: release-readiness source of truth for the current KoSch AI Launcher branch.

This file documents observed/current product behavior. It is not permission to broaden a store claim. If implementation changes, `ReleaseComplianceCatalog`, this matrix, the in-app disclosure and Play Console declarations must be reviewed together.

## Current production boundary

| Capability | User data / sensitive surface | Default | Persistence inside KoSch | KoSch network upload | External handoff | Production release |
|---|---|---:|---|---:|---|---:|
| Camera Awareness | Camera context frame | OFF | Process-only requested frame | No | None by default | Yes |
| Screen Awareness | Screen context frame | OFF | Process-only requested frame | No | None by default | Yes |
| Notification badges | Package-level notification counts | Android access not granted by default | Process-only counters | No | No | Yes |
| Local Usage Learning | App key, launch count, last-used time | ON | Bounded local model | No | No | Yes |
| Local Audit Log | Timestamp + action + outcome metadata | ON | Bounded local log; no free text | No | User-controlled CSV export | Yes |
| AI prompt handoff | User-entered text | No automatic handoff | Process/UI state only | No direct KoSch upload | Two-step explicit Android Share to selected destination app | Yes |
| N1 VPN prototype | No traffic processing in N1 | OFF | None | No | No | **No – debug only** |

## Important interpretation rules

### KoSch vs. destination app

KoSch does not claim that data remains on-device after the user explicitly shares text with a third-party AI application. KoSch itself does not upload the prompt; Android transfers the exact user-selected text to the selected destination app after KoSch's two-step disclosure/confirmation boundary. Subsequent processing is controlled by the destination app and its provider.

### Camera and screen

Camera and Screen Awareness are separate manual opt-ins. They must never be restored from portable backup as enabled and must never be activated solely by AI routing, preferences, recommendations or imported settings.

### Notifications

Notification access is an Android special access grant. KoSch's production badge implementation must remain package-count-only unless this matrix and the corresponding disclosure are deliberately revised. Notification title, body, people and extras are not part of the current badge data model.

### Local learning and audit

Local Usage Learning is content-free and bounded. The audit log contains action/outcome metadata and has no free-text field. Both are local product data, not analytics/advertising telemetry.

### VPN

`KoSchConsentVpnService` and `SecurityNetworkActivity` are declared only in `src/debug/AndroidManifest.xml`. The production manifest must remain free of `VpnService` until a genuine eligible security/network capability is implemented, reviewed and declared for Google Play.

## Store declaration gate

Before every public Play release:

1. Build the release artifact and inspect the merged release manifest.
2. Confirm that `VpnService` and the N1 Security surface are absent unless a later explicit production gate has superseded this rule.
3. Reconcile every sensitive capability with `ReleaseComplianceCatalog`.
4. Reconcile the Play Data Safety form with this matrix and the actual release binary.
5. Reconcile the public privacy policy with the same behavior.
6. Confirm there are no newly introduced analytics, advertising, crash-reporting or network SDKs without an explicit review.
7. Confirm that user text cannot leave KoSch through an external handoff without an affirmative user action.

## Current marketing-safe wording

Safe:

> KoSch performs launcher routing and context classification locally. Sensitive Camera and Screen Awareness features are off by default and require explicit user activation. Text is transferred to an external AI app only after a deliberate handoff.

Avoid blanket claims such as:

> Everything always stays on-device.

That statement would become inaccurate after a user deliberately hands text to a third-party AI application.
