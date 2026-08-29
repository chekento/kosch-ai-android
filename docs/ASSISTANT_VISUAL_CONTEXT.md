# KoSch Assistant — Stage H Visual Context

Status: implemented one-shot capture boundary; generative vision inference remains deliberately separate.

## Goal

Stage G proved that Screen Awareness and Camera Awareness can run with visible Android consent and without persisting or continuously uploading frames. Stage H adds the next explicit boundary: the Assistant may request **one current frame** for a user question such as “Was siehst du?” without turning the observation session into continuous AI analysis.

## Five distinct states

1. **Awareness off** — no Screen/Camera capture.
2. **Awareness live** — MediaProjection or visible CameraX session is flowing, but ordinary frames are immediately closed.
3. **Visual context requested** — exactly one matching live frame may be claimed and compressed.
4. **Visual context ready** — one bounded JPEG is held briefly in process memory, but has not been transferred to a model.
5. **Context transferred for inference** — future state; not implemented by the current Stage H runtime.

The fifth state must remain separately visible and separately authorized.

## Explicit request semantics

The deterministic local parser recognizes narrow visual requests, for example:

- “Was siehst du?”
- “Analysiere meinen Bildschirm.”
- “Schau dir das Kamerabild an.”
- “What do you see?”

Generic launcher commands such as “Kamera” or “Öffne Kamera” remain ordinary launcher actions and are not silently reinterpreted as vision requests.

If the requested source is already active, the Assistant creates a one-shot request immediately. If no suitable observation source is active, it asks for the corresponding visible Screen/Camera session instead of pretending it can see.

## One-shot broker

`AssistantVisualContextRuntime` is process-only and owns the short-lived binary payload.

Properties:

- a request is tied to `SCREEN` or `CAMERA`;
- a matching capture callback can claim it exactly once;
- other live frames continue to be discarded normally;
- the payload is never placed in Compose state;
- encoded payload limit: **512 KiB**;
- request timeout: **10 seconds**;
- ready payload lifetime: **30 seconds**;
- a request-id-safe delayed cleanup actively discards a ready payload even if no later access occurs;
- disabling/clearing/closing the Assistant discards pending/ready context;
- ending the matching Screen/Camera session cancels a pending request.

## Compression

Visual context is optimized for analysis rather than archival quality.

- Maximum long edge: 1280 px after the one-shot encoder receives a frame.
- Adaptive JPEG quality ladder rather than a fixed quality.
- A second resolution pass is used only if needed to remain below 512 KiB.
- Camera YUV_420_888 planes are read with their actual row/pixel strides.
- Rotation is applied before the final analysis JPEG.
- Screen RGBA row padding is cropped before final encoding.

The future Screenshot Recorder is intentionally a different product path and will use user-facing WebP/JPEG/PNG choices rather than this transient AI-context profile.

## User-visible feedback

After a request, the Assistant reports that exactly one context frame is being requested and explicitly says it has **not** been sent to an AI model.

When ready, chat reports:

- source (Screen or Camera),
- output dimensions,
- approximate KiB,
- transient-memory status,
- no-model-transfer status.

Capture failure or source shutdown is also reported.

## Security and networking boundary

Stage H itself does **not introduce**:

- a new network permission,
- `RECORD_AUDIO`,
- persistent screenshot storage,
- video recording,
- continuous frame streaming to an LLM,
- hidden camera capture,
- bypass of MediaProjection consent,
- Accessibility-based screenshot scraping.

The **full current application package does contain `INTERNET` and `ACCESS_NETWORK_STATE`** because KAL now includes the separate optional Provider Connections layer. Their presence does not authorize Visual Context transfer. Cloud Access remains OFF by default, provider connection is explicit, and Stage H currently has no code path that attaches its JPEG to a provider request.

Screen/Camera consent, Visual Context readiness and provider/network authorization are therefore independent gates.

The reviewed production permission budget remains authoritative and CI inspects both source and packaged artifacts.

## Next step — explicit inference bridge

Build an inference bridge that consumes a snapshot only after a visible local-model/provider decision.

Required rules:

1. Inference owns a consumed snapshot and must discard it after the request finishes.
2. Local multimodal inference should remain possible in a future explicitly offline variant without network permission.
3. External vision providers require a separately visible transfer path; a text-only provider handoff must never imply the image was attached.
4. The UI must distinguish `CONTEXT READY` from `SENT TO MODEL` / `LOCAL INFERENCE`.
5. Redaction/crop controls should be available before any external transfer.
6. Secure/blocked Android surfaces remain blocked; no bypass attempts.
7. A connected provider must never convert an active Awareness session into continuous streaming.

## Recorder relationship

See `ASSISTANT_RECORDER_TODO.md`. Recorder output is durable user media and therefore stays architecturally separate from both live Awareness and short-lived Visual Context.