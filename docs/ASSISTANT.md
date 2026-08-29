# KoSch Assistant

The KoSch Assistant is an **optional** launcher companion. It is designed to remain useful in the local-first build without pretending that a generative model or continuous wake-word detector is already bundled.

## Current foundation

- disabled by default;
- enabled from the Assistant sheet opened through the existing Ask-Dock companion;
- bounded in-session text chat (maximum 80 messages, 4,096 characters per message);
- settings persist, transcript does not;
- Android system speech recognition is launched only after an explicit tap;
- Android Text-to-Speech is optional, disabled by default and not initialized while Assistant or speech output is disabled;
- TTS range callbacks and synthesized PCM level drive an ephemeral 15-viseme mouth signal where the selected engine supports them;
- reduced motion is independently selectable and Android's disabled-animator setting is also respected;
- direct presses on the visible avatar become a short-lived normalized gaze target, followed by a bounded activation microexpression;
- local launcher commands are interpreted by `KoSch Local Core`;
- free-form generative requests remain in the Assistant session and expose an explicit provider-selection handoff;
- direct OpenRouter execution exists separately in the AI Hub and still requires an explicitly connected provider, Cloud Access and a foreground send action;
- no hidden provider call and no claim that the APK contains an embedded generative LLM.

The existing Ask-Dock voice icon remains a fast launcher-command route. Tapping the companion face opens Assistant setup/chat instead.

## Privacy boundary

The Assistant runtime itself does **not** require or add:

- `RECORD_AUDIO`;
- `READ_CONTACTS`;
- broad storage access;
- phone-call permission;
- accessibility-service privileges;
- continuous wake-word or background microphone capture.

The **application package does contain `INTERNET` and `ACCESS_NETWORK_STATE`** for the separate, optional Provider Connections layer. Those package permissions are not an Assistant cloud authorization: Cloud Access defaults to OFF, a provider must be explicitly connected, and direct provider execution remains foreground/user initiated. Screen and Camera Awareness are independent opt-ins and are never enabled by provider connectivity.

Speech input is handled by Android's recognizer UI after an explicit user action, so the launcher itself does not hold `RECORD_AUDIO`. File actions reuse SAF plus `DocumentGrantManager`. Contact actions use the one-time phone contact picker. Assistant free-form generative text is handed off only after the user chooses a route; the Assistant does not silently invoke the direct-provider path.

The attention response uses only the press position inside the currently visible avatar bounds. It does not use a camera, eye tracking, device motion, proximity sensing or a background observer. Coordinates and reaction timestamps remain ephemeral ViewModel state, are not audited and are never written to settings or backup.

## Persisted Assistant state

`kosch_assistant_settings` persists only the session-controller preferences:

- enabled/disabled;
- voice input enabled/disabled;
- speech output enabled/disabled;
- reduced motion enabled/disabled;
- assistant ID.

Agent preferences such as selected character, assistant name, planned presence/wake-word choices, Screen/Camera capability opt-ins and action-policy switches are stored separately by `AssistantAgentStore`. Device-local TTS voice assignments are stored separately again. Android MediaProjection consent, Camera runtime grants and active capture sessions are not portable Assistant settings and are never restored as active capture.

Chat text is session memory owned by `LauncherViewModel` and is neither written to SharedPreferences nor included in portable workspace backup.

## Character and voice contract

Three built-in character profiles currently exist:

- `default` / KoSch Default — neutral voice contract;
- `anime_female` / Anime Companion · Female — female voice contract;
- `anime_male` / Anime Companion · Male — male voice contract.

Female and male character speech fails closed when the corresponding device-local voice slot is not assigned or no longer exists. The runtime never silently falls back to the opposite-gender slot. Because Android TTS exposes no reliable cross-engine gender metadata, the user deliberately assigns the female/male slots after listening to the device voices.

Until calibrated matrix WebP packs are packaged, the character IDs remain visually usable through procedural fallbacks. The female and male anime profiles now have distinct animated procedural presentations rather than silently rendering as the same default robot; final artwork can replace these through the existing asset-pack IDs without changing the character/voice/session policy.

## Screen and Camera Awareness

Both capabilities default to **OFF**.

- Screen Awareness must first be enabled by the user, then each live session requires Android's visible MediaProjection consent. Capture runs only through the visible foreground-service boundary.
- Camera Awareness must first be enabled by the user, then Camera runtime permission and the visible in-sheet CameraX preview are required. Leaving that preview tears the camera session down.
- only one visual observation source is active at a time;
- a requested visual context frame is short-lived in RAM;
- the current Assistant Stage does **not** transmit that visual frame to an LLM.

Provider connectivity never auto-enables or auto-attaches either visual source.

## Wake-word maturity

Wake-word **configuration** is present: `OFF`, `Computer`, selected Assistant name and a custom phrase are modeled, validated and persistable. The default is `OFF` and `localWakeWordOnly` defaults true.

A continuous/local audio detector is **not implemented in this APK**. Selecting a phrase therefore prepares the future local wake-word policy; it does not start background listening. There is deliberately no launcher-owned `RECORD_AUDIO` permission or hidden microphone service. A future implementation must introduce its own explicit runtime/Play/privacy gate instead of turning the current preference into silent capture.

## Presence-mode maturity

`PORTAL_ONLY`, `AMBIENT`, `FLOATING`, `FULL_COMPANION` and `AGENT` are modeled and persistable as presentation/agent preferences, but the complete visual/runtime differentiation of every mode is not yet implemented. They are an architectural contract for later presentation stages, not five fully distinct live launcher behaviors in the current APK.

## Visual state machine

The runtime-facing Assistant session states are:

1. `DISABLED`
2. `IDLE`
3. `LISTENING`
4. `THINKING`
5. `SPEAKING`
6. `WORKING`
7. `OFFLINE`
8. `ERROR`

The APK uses state-aware procedural fallbacks with portal appearance/disappearance, independent gaze/blink, state poses and speech mouth motion. Ambient gaze interpolates between targets instead of snapping, while a direct press temporarily takes precedence and decays smoothly after release. A successful activation adds a brief reaction without hiding error or disabled states. Missing or not-yet-exported sprites therefore remain non-fatal and HOME stays usable.

The matrix WebP compositor is implemented but activates the calibrated Default pack only after the complete required export and measured eye/mouth anchors pass the manifest gate. It draws loaded body, eye and mouth layers together; a corrupt active frame returns to the complete procedural fallback instead of showing a partial face.

## Supplied Assistant Matrix contract

The runtime catalog follows the project asset matrix so character Canvas fallbacks can later be replaced by compressed WebP packs without changing state logic.

### Naming

- body: `asst_<assistant>_body_<state>.webp`
- eye overlay: `asst_<assistant>_eye_<state>.webp`
- mouth viseme: `asst_<assistant>_mouth_viseme_<code>.webp`
- spawn: `asst_<assistant>_spawn_<frame3>.webp`
- Y rotation: `asst_<assistant>_turn_y_<deg3>.webp`
- shared portal: `portal_<theme>_<frame3>.webp`

### Required animation sets

- portal: 8 frames, `000..007`;
- spawn: 16 frames, `000..015`;
- despawn: reverse spawn playback;
- Y rotation: 24 frames from `000` through `345` in 15-degree steps, including true back view at `180`;
- visemes: `sil`, `pp`, `ff`, `th`, `dd`, `kk`, `ch`, `ss`, `nn`, `rr`, `aa`, `e`, `ih`, `oh`, `ou`.

### Target runtime budgets from the matrix

- body: 384 px target, <= 35 KB per WebP;
- eye/mouth overlays: 128 px target, <= 12 KB;
- portal: 256 px target, <= 20 KB;
- PNG/RGBA masters remain source assets, not APK runtime assets.

## What is deliberately not complete yet

- continuous listening or a live wake-word detector;
- an embedded generative LLM;
- direct-provider execution inside the Assistant conversation itself;
- persistent conversation history;
- studio-grade phoneme forced alignment;
- calibrated final WebP sprite packs for every character;
- fully differentiated runtime behavior for every Presence Mode;
- autonomous Android actions without the existing risk/confirmation boundaries.

Final calibrated asset export and an embedded local model remain follow-up work in issue #17. The B-C behavior and TTS signal runtime are documented in `ASSISTANT_RUNTIME_BC.md`; the permission-free attention layer is documented in `ASSISTANT_RUNTIME_D.md`. A future local generative route must enter through an isolated `LocalModelBackend` and preserve the same opt-in, privacy, cancellation and provider-choice boundaries.
