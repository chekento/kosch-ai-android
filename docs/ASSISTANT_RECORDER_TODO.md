# KoSch Capture Studio / Recorder — planned module

Status: **TODO after Assistant visual context / inference bridge**

The recorder should reuse the proven MediaProjection capture boundary without confusing Assistant awareness, one-shot AI context, and durable recording. A user must always be able to tell which of these three modes is active.

## Product scope

### Screenshot recorder

- Capture the current full screen or Android-selected single app after MediaProjection consent.
- Quick action, Control Center action, hardware/stylus shortcut and Assistant command.
- Optional short countdown and annotation handoff to Pen Space.
- Crop/markup flow after capture without altering the original unless explicitly saved.
- Formats:
  - WebP lossy as the compact default on API 30+ when appropriate.
  - WebP lossless for UI/text-heavy captures when size remains reasonable.
  - JPEG compatibility export with adaptive quality.
  - PNG only when lossless compatibility is explicitly preferred.
- Compression should be content/size aware, not a fixed quality number.
- Save through Android MediaStore; never require broad storage permission.

### Screen video recorder

- MediaProjection surface -> hardware encoder -> MP4 muxer.
- Codec negotiation by actual device capability:
  1. AV1 when a stable hardware encoder is available and the selected compatibility profile allows it.
  2. HEVC/H.265 for high compression on capable devices.
  3. H.264/AVC as the broad compatibility fallback.
- Presets: Efficient, Balanced, High Quality, Custom.
- Adaptive bitrate derived from resolution, frame rate and codec rather than one global bitrate.
- Optional 30/60 fps where the device can sustain it.
- Pause/resume where the chosen encoder/muxer path supports a clean timeline.
- Persistent recording notification with duration, approximate size, Pause and Stop.
- Stop immediately when MediaProjection is revoked or Android ends the token.
- Never silently restart after process death, reboot or token loss.

## Audio is a separate consent surface

The current launcher intentionally forbids `RECORD_AUDIO`. Keep that boundary for the Assistant/launcher build.

A future recorder audio option must be explicit and separately reviewed:

- **No audio** — default and permission-minimal.
- **Device playback audio** — Android AudioPlaybackCapture; requires `RECORD_AUDIO`, MediaProjection consent and source-app/profile eligibility.
- **Microphone** — separate runtime permission and persistent active indication.
- **Device + microphone mix** — only after both permissions are explicit and the UX can show both sources unambiguously.

The recorder must not add `RECORD_AUDIO` merely because video recording exists.

## Assistant / agent control

Target commands include:

- “Mach einen Screenshot.”
- “Nimm den Bildschirm auf.”
- “Starte eine Bildschirmaufnahme in effizient.”
- “Pausiere die Aufnahme.”
- “Setze die Aufnahme fort.”
- “Stoppe und speichere die Aufnahme.”
- “Wie groß ist die aktuelle Aufnahme?”

Rules:

1. The Assistant may prepare a capture request, choose a user-approved preset and control an already authorized recording session.
2. It may **never** fabricate or bypass Android MediaProjection consent.
3. Starting durable recording is a visible side effect and requires the corresponding user-authorized Agent capability; first use and sensitive source changes remain confirmable.
4. Stop is always immediately available to the user and to the Assistant.
5. Recording state must appear in Privacy Live independently from SCREEN Awareness and independently from an AI context-frame transfer.
6. The Assistant must say whether it is observing, taking a one-shot AI context frame, taking a screenshot, or recording video.

## Architecture split

Keep four distinct layers:

- `ObservationSession`: live Screen/Camera awareness, ephemeral frames.
- `VisualContext`: one explicit short-lived compressed frame for later inference.
- `ScreenshotCapture`: durable still image requested by the user.
- `RecordingSession`: durable encoded video/audio output with persistent status.

A shared capture source is allowed, but lifetime, storage, consent and UI state must remain separate.

## Compression targets

- Prefer hardware video encoding.
- Negotiate codec support at runtime; never assume AV1/HEVC hardware exists.
- Keep a broad H.264 compatibility profile.
- Use adaptive bitrate and resolution caps to avoid waste on small phone screens.
- Offer an “Efficient” preset intended for long recordings and sharing.
- Offer a “Text/UI” screenshot mode optimized for sharp typography at low size.
- Record actual output size/codec/resolution in local metadata so the user can audit the result.
- No cloud compression and no network dependency required.

## Storage & retention

- MediaStore-backed output in user-visible Pictures/Movies collections.
- Temporary partial recordings stay private and are deleted after failure/cancel.
- Finalize/mux before publishing the finished MediaStore item when practical.
- No broad filesystem access.
- User-controlled auto-delete/retention can be added later, disabled by default.

## Security / enterprise behavior

- Respect `FLAG_SECURE`, MediaProjection black/blocked surfaces and enterprise screen-capture policy; never attempt bypasses.
- Work-profile restrictions remain authoritative.
- Projection revoke -> immediate stop/finalize-or-discard path.
- No hidden recording, background auto-start or invisible status mode.
- Capture controls remain functional with the Assistant completely disabled.

## Implementation order

1. Finish Stage H one-shot visual context and explicit inference boundary.
2. Extract reusable MediaProjection source/session coordinator.
3. Implement screenshot output + MediaStore + compression presets.
4. Implement silent screen video (no audio) + hardware codec negotiation.
5. Add Assistant intents/actions and Privacy Live `REC` state.
6. Add optional audio feature behind a separate permission/security review.
7. Add instrumentation: consent cancel, revoke, rotation, app-window sharing, process death, low storage, long recording, thermal pressure and codec fallback.
