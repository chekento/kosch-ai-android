# KoSch Assistant · Stage G Observation Sessions

Stage G connects the consent-first Agent Core to real Android screen and camera session boundaries. The goal is not silent capture. The goal is a technically real, visibly controlled observation channel that can later supply short-lived context to the Assistant.

## Core rule

Capability, platform permission/consent and a running session are three different states.

1. **Capability opt-in** – Screen Awareness or Camera Awareness must first be enabled manually in the Assistant Control Center.
2. **Android consent** – a real session still requires Android's own MediaProjection consent or the CAMERA runtime permission.
3. **Visible session state** – only after both checks does KoSch enter an observation state and expose a live privacy indicator plus an explicit Stop control.

No provider, agent plan or automation can turn an Awareness capability from off to on.

## Screen Share

Screen Share uses Android MediaProjection behind `AssistantScreenShareService`.

- the Activity launches Android's standard screen-capture consent UI;
- only an `RESULT_OK` consent result may reach the service;
- the service is `exported=false` and non-sticky;
- while active it runs as a foreground service of type `mediaProjection`;
- a persistent system notification identifies the active KoSch Assistant screen share;
- the notification has its own Stop action;
- stopping in the Control Center, stopping from Android, revoking projection, or disabling the Assistant tears the session down;
- MediaProjection result data is never written to preferences, files or backups.

The virtual display is bounded to a maximum capture edge of 960 px for the Stage G transport probe.

### Current frame handling

Stage G does **not** send screen pixels to an LLM and does not save screenshots. `ImageReader` acquires the latest frame, increments ephemeral process telemetry, and closes the image immediately.

The visible Control Center may show resolution and the count of transient frames observed. That count resets when the session ends.

## Camera session

Camera Awareness uses stable CameraX with `Preview` + `ImageAnalysis`.

- the CAMERA runtime permission is requested only after the user presses **Kamera starten**;
- the session exists only while the Assistant Control Center renders the visible preview;
- leaving that surface unbinds CameraX immediately;
- ImageAnalysis uses `STRATEGY_KEEP_ONLY_LATEST`;
- every `ImageProxy` is closed immediately after incrementing ephemeral telemetry;
- Stage G does not convert, store or upload camera pixels.

The rear camera is the Stage G default. Camera switching can be introduced later without changing the consent contract.

## One visual source at a time

KoSch deliberately avoids parallel visual capture in Stage G.

- starting Camera while Screen Share is active explicitly stops Screen Share first;
- starting Screen Share clears the requested Camera session and stops the Camera observation state;
- Stop operations only clear the matching source so that a delayed callback cannot accidentally tear down a newly selected source.

## Privacy indicators

`Privacy Live` reflects the real process runtime rather than only desired Agent state:

- **MIC** – existing visible Android speech-input flow;
- **SCREEN** – actual MediaProjection runtime active;
- **CAM** – actual CameraX runtime active;
- **ACTING** – Agent execution state.

Camera additionally uses the live preview itself as a strong privacy cue. Screen Share additionally has Android's foreground-service notification.

## Permission budget

Stage G intentionally expands the manifest only to:

- `android.permission.ACCESS_NETWORK_STATE` (existing)
- `android.permission.CAMERA`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION`

CI rejects unexpected manifest permissions and separately rejects packaged `INTERNET` and `RECORD_AUDIO` permissions. The launcher therefore does not gain silent networking or permanent microphone capture as a side effect of Screen/Camera Awareness.

## Process-only telemetry

`AssistantObservationRuntime` contains only volatile state:

- active/not active;
- screen dimensions;
- transient frame count;
- transient failure message/generation.

It contains no image bytes, projection token, camera handle, Android permission result or provider payload. Process death clears it.

## Tests

Stage G adds coverage for:

- process-only screen/camera lifecycle reset;
- failure-generation semantics;
- private/non-exported MediaProjection foreground service declaration;
- exact observation permission budget;
- absence of INTERNET and RECORD_AUDIO in the requested permissions.

The API 36 managed-device suite does not automate approval of Android's security consent dialogs. Those dialogs remain user-controlled system UI by design.

## Next stage: explicit transient context frames

The next observation step must preserve the same boundary:

1. the user explicitly asks about the current screen/camera context;
2. KoSch derives a bounded in-memory frame from an already visible, consented session;
3. redaction and provider/local-model destination are evaluated;
4. the UI distinguishes **capture active** from **frame shared with intelligence**;
5. the frame has a short TTL and is never silently converted into continuous model streaming.

This avoids the dangerous equivalence `capture permission = permission to continuously disclose content`.
