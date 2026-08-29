# KoSch Assistant · Observation Sessions

Stage G connected the consent-first Agent Core to real Android screen and camera session boundaries. Stage H has since added explicit one-shot Visual Context on top. The goal remains a technically real, visibly controlled observation channel — never silent capture.

## Core rule

Capability, platform permission/consent, a running observation session and a frame shared with intelligence are different states.

1. **Capability opt-in** – Screen Awareness or Camera Awareness must first be enabled manually in the Assistant Control Center.
2. **Android consent** – a real session still requires Android's own MediaProjection consent or the CAMERA runtime permission.
3. **Visible session state** – only after both checks does KoSch enter an observation state and expose a live privacy indicator plus an explicit Stop control.
4. **One-shot context request** – a user may explicitly request one current frame from an already active source.
5. **Model transfer/inference** – not implemented by Stage H and must remain a separate future consent boundary.

No provider, agent plan or automation can turn an Awareness capability from off to on.

## Screen Share · LIVE

Screen Share uses Android MediaProjection behind `AssistantScreenShareService`.

- the Activity launches Android's standard screen-capture consent UI;
- only a `RESULT_OK` consent result may reach the service;
- the service is `exported=false` and non-sticky;
- while active it runs as a foreground service of type `mediaProjection`;
- a persistent system notification identifies the active KoSch Assistant screen share;
- the notification has its own Stop action;
- stopping in the Control Center, stopping from Android, revoking projection, or disabling the Assistant tears the session down;
- MediaProjection result data is never written to preferences, files or backups.

The virtual display is bounded to a maximum capture edge of 960 px for the observation transport.

Ordinary live frames are not sent to an LLM and are not saved as screenshots. Outside an explicit one-shot Visual Context request, `ImageReader` acquires the latest frame, updates ephemeral process telemetry and closes the image immediately.

## Camera session · LIVE

Camera Awareness uses CameraX with `Preview` + `ImageAnalysis`.

- CAMERA runtime permission is requested only after the user presses **Kamera starten**;
- the session exists only while the Assistant Control Center renders the visible preview;
- leaving that surface unbinds CameraX immediately;
- ImageAnalysis uses `STRATEGY_KEEP_ONLY_LATEST`;
- ordinary `ImageProxy` frames are closed immediately after ephemeral telemetry unless exactly one pending Visual Context request claims a matching frame;
- no continuous camera stream is uploaded to a provider.

The rear camera is the current default. Camera switching can be introduced later without changing the consent contract.

## One visual source at a time

KoSch deliberately avoids parallel visual observation.

- starting Camera while Screen Share is active explicitly stops Screen Share first;
- starting Screen Share clears the requested Camera session and stops the Camera observation state;
- Stop operations only clear the matching source so a delayed callback cannot accidentally tear down a newly selected source.

## One-shot Visual Context · LIVE, no inference yet

`AssistantVisualContextRuntime` can claim exactly one matching live frame after an explicit visual-context request. That frame is encoded within bounded dimensions/bytes, held only briefly in process memory and actively discarded after its TTL.

The current Assistant tells the user when a frame is requested and when it is ready, including source, dimensions and approximate size. It also explicitly states that the frame has **not** been sent to an AI model.

Capture permission therefore never means continuous disclosure permission.

## Privacy indicators

`Privacy Live` reflects real process runtime rather than only desired Agent state:

- **MIC** – explicit Android speech-recognizer interaction started by the user;
- **SCREEN** – actual MediaProjection runtime active;
- **CAM** – actual CameraX runtime active;
- **ACTING** – Agent execution state.

Camera additionally uses the live preview itself as a strong privacy cue. Screen Share additionally has Android's foreground-service notification.

## Permission budget

The **current production APK** contains exactly the reviewed package permission budget:

- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`
- `android.permission.CAMERA`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION`

`INTERNET` exists for the separate optional Provider Connections layer. It is **not** introduced or activated by Screen/Camera Awareness and is not an observation authorization. Cloud Access defaults OFF and direct provider requests pass separate product-level gates.

The Observation runtime itself does not require `RECORD_AUDIO`, location, contacts, call log, SMS, phone state or `QUERY_ALL_PACKAGES`. Speech input uses Android's visible recognizer UI after an explicit tap rather than a launcher-owned permanent microphone capture path.

CI checks both the source manifest and packaged APKs against the permission/release contract.

## Process-only telemetry

`AssistantObservationRuntime` contains only volatile state such as:

- active/not active;
- screen dimensions;
- transient frame count;
- transient failure message/generation.

The one-shot binary payload lives separately in `AssistantVisualContextRuntime` and is short-lived. Neither runtime persists projection tokens, camera handles, Android permission results or provider payload history. Process death clears them.

## Tests

Current coverage includes:

- process-only screen/camera lifecycle reset;
- failure-generation semantics;
- private/non-exported MediaProjection foreground service declaration;
- reviewed production permission budget including intentional Provider Connections `INTERNET`;
- explicit absence of `RECORD_AUDIO` and other forbidden sensitive permission expansion;
- API 36 managed-device coverage for the launcher/Assistant contracts that do not require automating Android's protected consent dialogs.

The managed-device suite intentionally does not auto-approve MediaProjection or CAMERA security UI. Those remain user-controlled Android boundaries by design.

## Next observation/inference step

Future model use must preserve the existing separation:

1. the user explicitly asks about current screen/camera context;
2. KoSch derives a bounded in-memory frame from an already visible, consented session — **implemented**;
3. redaction/crop and destination are evaluated — **not yet complete**;
4. UI distinguishes **capture active**, **context ready** and **sent to model/local inference** — first two implemented;
5. external transfer requires a separately visible affirmative action;
6. no path converts a one-shot frame into silent continuous model streaming.

This avoids the dangerous equivalence `capture permission = permission to continuously disclose content`.