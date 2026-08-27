# M2.6 Assistant Stage B-C — believable visual and speech runtime

This slice turns the existing Assistant foundation and v1 asset manifest into one fail-safe behavior runtime. It does not claim that the final Default Assistant WebPs or an embedded generative model are already packaged.

## Delivered

- one deterministic `AssistantAnimationDirector` drives both the procedural fallback and future WebP renderer;
- the disabled companion is a floor portal without a visible body;
- enable and disable use the same 16-step appearance progress, so despawn is the exact reverse of spawn;
- the procedural fallback now renders a complete white/cyan robot with head, body, antenna, arms, feet, chest status light and state-dependent poses;
- blink, gaze, body breathing, hover, head tilt, portal and mouth are independent channels;
- the WebP path now composites body, measured eye overlay and measured mouth overlay instead of discarding loaded overlays;
- the current frame falls back as a unit when body, eye, mouth or portal cannot be decoded within the matrix size/dimension contract;
- app-level Reduced Motion and Android's disabled-animator setting stop decorative loops while preserving state cues and simplified speech motion;
- Android TTS range callbacks feed the exact 15-viseme matrix contract;
- Android TTS PCM callbacks feed a normalized, ephemeral RMS envelope when range timing is absent or coarse;
- utterance IDs prevent delayed callbacks from an interrupted or replaced TTS request from moving the current face;
- stop, disable, voice-input handoff and TTS teardown clear the speech signal immediately.

## Speech boundary

The launcher does not record sound for lip sync. It has no `RECORD_AUDIO` permission. The PCM bytes used for the envelope are synthesized output supplied by the selected Android TTS engine, are reduced immediately to a `0..1` value and are neither persisted nor added to backup or audit.

Range timing and PCM callbacks are engine-dependent. The runtime therefore degrades in this order:

1. range-derived German/English grapheme-to-viseme sequence plus PCM openness;
2. PCM-envelope mouth shapes;
3. closed/silence mouth when neither signal is supplied.

This is responsive launcher lip sync, not studio phoneme forced alignment. It never pretends that a TTS engine supplied timing it did not supply.

## Asset activation remains strict

The packaged sprite path remains inactive until all 150 v1 files exist and the separate eye and mouth rectangles have been measured against `asst_default_body_idle_neutral.webp`. The supplied overview sheet is a visual reference, not a substitute for final 384 px anchor measurements.

The safe procedural robot is therefore the user-visible renderer in the current source tree. Adding a partial export cannot silently activate a half-composited face.

## Still gated

- final transparent WebP export and visual calibration at actual companion size;
- physical-device visual, TalkBack, 200 % text, battery and frame-time evidence;
- API 29/33/36/37 device matrix;
- local model import and isolated `LocalModelBackend` process;
- full-duplex interruption or wake word;
- autonomous Android actions.

The embedded model remains behind the measured-beta, licensing, hash, cancellation, thermal and process-isolation gates in issue #17.
