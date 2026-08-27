# M2.6 Assistant Stage D — attentive presence

This slice makes the existing Assistant respond to deliberate touch without adding observation permissions or pretending to perform camera-based eye tracking.

## Delivered

- the press position inside each visible avatar is normalized to a bounded `-1..1` gaze target;
- the Canvas renderer uses the target continuously, while the future WebP renderer selects the corresponding matrix eye overlay;
- ambient idle and thinking gaze now interpolate into the next target rather than snapping between directions;
- release keeps a short, smooth gaze linger instead of returning mechanically to center;
- successful activation produces a bounded wink/smile response and a small glow lift;
- state clarity wins over personality: disabled and error faces cannot be replaced by the touch reaction;
- Reduced Motion keeps direct eye response as functional feedback but removes the added head movement and all existing decorative translation;
- Ask Dock, floating Assistant and the Assistant-sheet portrait share the same ephemeral attention signal.

## Privacy boundary

Attention is derived exclusively from Compose press events on the visible avatar. The runtime does not request or inspect:

- camera or eye-tracking input;
- microphone or speech input;
- accelerometer, gyroscope, proximity or ambient sensors;
- accessibility observation;
- network data.

The normalized target and uptime timestamps live only in the Activity-recreation-safe Assistant session. They are not written to `AssistantStore`, transcript, audit or backup. Disabling the Assistant or clearing its session clears the signal immediately.

## Deterministic behavior

`AssistantAnimationDirector` remains the single source of truth for Canvas and WebP frames. Touch priority, reaction duration, gaze decay, state precedence and Reduced Motion behavior are pure and unit tested; UI code only supplies a normalized target.

This stage does not weaken the 150-file plus measured-face-anchor activation gate. The low-resolution overview montage remains a visual reference, not a production sprite export.
