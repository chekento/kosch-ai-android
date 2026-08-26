# KoSch Assistant Asset Export v1

This document turns `KoSch_AI_Launcher_Assistant_Asset_Matrix_v1.xlsx` into a reproducible APK export gate.

## Source hierarchy

1. **Technical source of truth:** `KoSch_AI_Launcher_Assistant_Asset_Matrix_v1.xlsx`.
2. **Visual source/reference:** `Futuristische Roboter-Asset-Übersicht.png`.
3. **Runtime mirror:** `DefaultAssistantAssetManifest` in the Android project.

If the older overview sheet disagrees with the matrix, the matrix wins. In particular, the APK contract uses eight shared portal frames (`000..007`), not the older fourteen-frame reference sequence.

## Exact v1 inventory

The Default Assistant v1 pack contains **150 required WebP files**:

| Group | Count | APK location | Runtime contract |
| --- | ---: | --- | --- |
| Static body / pose | 54 | `assets/assistant/default/body/` | 384×384, alpha, ≤35 KiB each |
| Spawn | 16 | `assets/assistant/default/body/` | 384×384, alpha, ≤35 KiB each |
| Y rotation | 24 | `assets/assistant/default/body/` | 384×384, alpha, ≤35 KiB each |
| Eye overlays | 25 | `assets/assistant/default/overlay/` | 128×128, alpha, ≤12 KiB each |
| Mouth visemes | 15 | `assets/assistant/default/overlay/` | 128×128, alpha, ≤12 KiB each |
| Mouth emotions | 8 | `assets/assistant/default/overlay/` | 128×128, alpha, ≤12 KiB each |
| Shared portal | 8 | `assets/assistant/common/fx/` | 256×256, alpha, ≤20 KiB each |

Totals by runtime folder: **94 body**, **48 overlay**, **8 shared portal**.

## Naming rules

Runtime filenames must use only lowercase ASCII letters, digits and underscores plus the `.webp` extension. No spaces, hyphens, umlauts or uppercase characters.

Required animation contracts:

- spawn: `asst_default_spawn_000.webp` through `asst_default_spawn_015.webp`;
- despawn: reverse playback of spawn `015 -> 000`; no duplicate despawn images;
- Y rotation: 24 frames in 15° steps from `000` through `345`;
- `asst_default_turn_y_180.webp` must be a true back view;
- shared portal: `portal_default_000.webp` through `portal_default_007.webp`;
- visemes: `sil`, `pp`, `ff`, `th`, `dd`, `kk`, `ch`, `ss`, `nn`, `rr`, `aa`, `e`, `ih`, `oh`, `ou`.

The complete filename list lives in `DefaultAssistantAssetManifest`; CI unit tests lock its counts and uniqueness.

## Master -> APK export

Keep the high-resolution RGBA PNG masters outside the APK. Export runtime assets only after composition/cropping is approved.

Recommended matrix settings:

- body: WebP + alpha, quality approximately 78–82, then visual edge inspection;
- eye/mouth overlay: lossless or approximately Q90 because thin cyan/neon lines are sensitive to ringing;
- portal: WebP + alpha, reduce unnecessary animation frames before increasing per-frame size;
- never exceed the hard runtime byte and dimension budgets above.

Every exported file must be visually checked at actual companion size, not only zoomed in. Pay particular attention to cyan glow edges, black face-panel boundaries, one-pixel alpha halos and color fringing.

## Face-anchor calibration — do not guess

Eyes and mouth are 128×128 overlay canvases but are rendered onto the 384×384 body canvas. Their placement is intentionally **uncalibrated** in the current manifest, so the APK remains on the Canvas fallback even if a complete file set is accidentally packaged.

Calibration must use the final exported front-neutral body:

`asst_default_body_idle_neutral.webp`

For each overlay family, place the approved 128×128 reference overlay over that 384×384 body until it exactly matches the intended face geometry. Record the destination rectangle in body pixels:

- `x_px`: left edge on the 384 px body canvas;
- `y_px`: top edge;
- `width_px`: rendered overlay width;
- `height_px`: rendered overlay height.

Convert to the normalized runtime rectangle:

- `left = x_px / 384`
- `top = y_px / 384`
- `width = width_px / 384`
- `height = height_px / 384`

Record **separate eye and mouth rectangles**. Do not infer the mouth rectangle from the eye rectangle or vice versa.

Before setting `faceCalibration.isCalibrated == true`, verify at minimum:

1. center eyes + neutral/sil mouth on neutral front body;
2. blink closed and both wink states;
3. extreme eye directions (`up_left`, `down_right`);
4. widest mouth states (`aa`, `laugh`, `surprised`, `yawn`);
5. no overlay pixel leaves its intended black face panel;
6. no glow or alpha seam appears when scaled at companion size.

The source image name and body filename remain part of the calibration record. When the underlying body geometry changes, increment the asset version and remeasure anchors instead of reusing old coordinates.

## Runtime activation policy

`AssistantAssetPackInspector` inventories packaged WebPs once per process. The Default Assistant sprite renderer activates only when:

1. all 150 matrix-defined files are present;
2. no Default-Assistant/theme-scoped unexpected WebP is present;
3. face calibration contains valid normalized eye **and** mouth rectangles.

Anything else remains on the Canvas avatar. Individual corrupt, oversized or wrongly dimensioned files are additionally rejected by `AssistantAssetRuntime` during decode.

## Animation guidance

Use the matrix FPS ranges as upper guidance, not as an obligation to redraw unnecessary frames:

- body: 8–12 fps;
- spawn: 18–24 fps;
- turn: 24 fps;
- lipsync: 24–30 fps.

Prefer transforms/interpolation for micro-motion where it preserves visual quality. Keep reduced-motion support independent from asset availability.

## Stage boundary

This B-B stage establishes inventory, completeness and calibration gates. It does **not** claim that the 150 final WebPs have already been exported. The B-C runtime now contains fail-safe eye/mouth compositing, deterministic micro-motion and TTS range/PCM-driven mouth signals, but the WebP path deliberately remains inactive until real anchors and the complete export are committed. See `ASSISTANT_RUNTIME_BC.md`.
