# KoSch Assistant

The KoSch Assistant is an **optional** launcher companion. It is designed to remain useful in the offline build without pretending that a generative model is already bundled.

## Current foundation

- disabled by default;
- enabled from the Assistant sheet opened through the existing Ask-Dock companion;
- bounded in-session text chat (maximum 80 messages, 4,096 characters per message);
- settings persist, transcript does not;
- Android system speech recognition is launched only after an explicit tap;
- Android Text-to-Speech is optional, disabled by default and not initialized while Assistant or speech output is disabled;
- local launcher commands are interpreted by `KoSch Local Core`;
- free-form generative requests remain in the Assistant session and expose an explicit provider-selection handoff;
- no hidden provider call and no claim that the offline APK contains an LLM.

The existing Ask-Dock voice icon remains a fast launcher-command route. Tapping the companion face opens Assistant setup/chat instead.

## Privacy boundary

The foundation does **not** add:

- `INTERNET`;
- `RECORD_AUDIO`;
- `READ_CONTACTS`;
- broad storage access;
- phone-call permission;
- accessibility-service privileges;
- wake-word or background microphone capture.

Speech input is handled by Android's recognizer UI. File actions reuse SAF plus `DocumentGrantManager`. Contact actions use the one-time phone contact picker. Generative text is handed off only after the user chooses a provider.

Only these Assistant settings are persisted in `kosch_assistant_settings`:

- enabled/disabled;
- voice input enabled/disabled;
- speech output enabled/disabled;
- assistant ID.

Chat text is session memory owned by `LauncherViewModel` and is neither written to SharedPreferences nor included in portable workspace backup.

## Visual state machine

The runtime-facing states are:

1. `DISABLED`
2. `IDLE`
3. `LISTENING`
4. `THINKING`
5. `SPEAKING`
6. `WORKING`
7. `OFFLINE`
8. `ERROR`

The APK currently uses a state-aware Canvas fallback. This makes missing or not-yet-exported sprites non-fatal and keeps the HOME shell usable.

## Supplied Assistant Matrix contract

The runtime catalog follows the project asset matrix so the Default Assistant can be swapped from the Canvas fallback to compressed WebP assets without changing state logic.

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

## What is deliberately not in this foundation

- continuous listening or a wake word;
- an embedded generative LLM;
- hidden direct cloud API calls;
- persistent conversation history;
- viseme-timed lip sync;
- full sprite export from the supplied overview artwork;
- autonomous Android actions.

Those belong to the measured follow-up in issue #17. The future local generative route must enter through an isolated `LocalModelBackend` and preserve the same opt-in, privacy, cancellation and provider-choice boundaries.
