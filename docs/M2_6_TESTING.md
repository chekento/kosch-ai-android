# M2.6 Measured Professional Beta — Test Foundation

M2.6 shifts the launcher quality gate from source-level confidence toward repeatable device evidence. This document records only gates that were actually executed and keeps emulator evidence separate from physical-device/OEM evidence.

## Current measured baseline

The Android module builds a dedicated instrumentation APK in addition to the application APK. The stable reference lane is an AOSP Pixel 2 managed device on API 36.

The final Stage D drag/drop merge snapshot was tested at branch head `b2aa65c19a51155b2cd5f8743a97fdad43960239` in GitHub Actions run `32936425070` before squash merge to `main` as `a77e16292dca3e1852811dc7d87b02083f2ed802`.

That run completed both CI jobs successfully:

- `Test, lint and build` — unit tests, Android Lint, Debug build, minified Release build, instrumentation APK build, source permission budget and packaged-APK permission budget;
- `Instrumented tests · API 36 AOSP` — managed-device execution of the current instrumentation suite.

Retained evidence from that run:

- `kosch-ai-api36-managed-device-results` — artifact `9595200709`, SHA-256 digest `30a36c46a6d9397d7eb51fe7c6df774adea8c3b4ed9395d9db6340ff0fc036de`;
- `kosch-ai-launcher-instrumentation` — artifact `9595100974`, SHA-256 digest `8c545f822f0f214301b04bab54936f17079391c245875132dde328c77499350a`;
- `kosch-ai-launcher-m2.5-debug` — artifact `9595096422`, SHA-256 digest `ecdc9c4df1dac481c8919fb39254153b0863eca74feb7ef025c485dbf82f1650`.

These are emulator/AOSP results. They are not a substitute for Samsung/other-OEM, accessibility, physical stylus, battery, or performance evidence.

## Instrumentation coverage now executed on API 36

1. **Cold-launch lifecycle smoke**
   - launches `MainActivity` through `createAndroidComposeRule`;
   - requires the Activity to reach `RESUMED`;
   - waits for Compose to become idle;
   - verifies that the Compose launcher shell exists.
2. **Activity recreation**
   - calls the underlying `ActivityScenario.recreate()`;
   - requires the recreated Activity to return to `RESUMED`;
   - verifies that the launcher shell renders again.
3. **Workspace persistence**
   - writes scene/home state through the real Android `SharedPreferences`-backed `WorkspaceStore`;
   - creates a fresh store and verifies round-trip persistence.
4. **Onboarding durability**
   - completes onboarding through the production store API;
   - verifies the flag from a fresh store instance.
5. **Workspace v7 / page-editing regression coverage**
   - exercises the persisted v7 workspace paths used by the unified Home engine.
6. **Direct workspace drag/drop**
   - exercises real Compose pointer input on API 36;
   - verifies a stable workspace item can be dragged across the right page edge to the adjacent user Home page;
   - verifies the target-page move is persisted.

The cross-page drag test caught a real timing weakness before merge: the first implementations derived the final drop from Compose preview state that could lag the final pointer event. Stage D now resolves the final destination directly from the accumulated pointer delta at `onDragEnd` through the pure `WorkspaceDragResolver`. The final API 36 run is green with that production fix.

System animations are disabled for managed instrumentation runs to reduce UI-test flakiness; this does not constitute animation-performance evidence.

## CI contract

The verification job runs the equivalent of:

```bash
./gradlew --no-daemon \
  testDebugUnitTest \
  lintDebug \
  assembleDebug \
  assembleRelease \
  assembleDebugAndroidTest \
  --stacktrace
```

CI additionally enforces the source permission allowlist and verifies the permissions of the generated APK. Unexpected broad permissions remain a build failure.

## Remaining execution matrix

| Gate | API 29 | API 33 | API 36 | API 37 preview | Physical/OEM still required |
| --- | --- | --- | --- | --- | --- |
| Cold launch + Compose shell | planned | planned | **AOSP green** | planned | yes |
| Activity recreation | planned | planned | **AOSP green** | planned | yes |
| Workspace persistence | planned | planned | **AOSP green** | planned | yes |
| Direct same/cross-page drag | planned | planned | **AOSP green** | planned | touch/stylus yes |
| Process death / saved state | planned | planned | partial only | planned | yes |
| Widget pick / cancel / configure | planned | planned | planned | planned | yes |
| SAF file + tree picker | planned | planned | planned | planned | yes |
| Backup / audit / SVG export resume | planned | planned | planned | planned | yes |
| Work profile quiet mode | n/a where unsupported | planned | planned | planned | yes |
| TalkBack + 200% font | planned | planned | planned | planned | yes |
| Switch Access | planned | planned | planned | planned | yes |
| Hardware keyboard | planned | planned | planned | planned | yes |
| Stylus pressure / hover / eraser | limited | limited | planned | planned | yes |
| Foldable / multi-window | limited | limited | planned | planned | yes |
| Start/frame/RSS/battery budgets | planned | planned | planned | preview only | **yes** |

API 37 remains a preview compatibility lane until Android 17 is stable; it must not be scored as equivalent to stable API 36 production evidence.

## Evidence rule

A roadmap item is not considered measured merely because code or a test exists. A measured claim requires all applicable evidence below:

- test code committed;
- test APK built by CI;
- test executed on the stated API/device class;
- result retained as CI output, report, benchmark artifact, or documented physical-device run;
- failures linked to a reproducible cause and fixed before the gate is marked complete;
- emulator evidence labeled as emulator evidence rather than generalized to OEM hardware.

The >9.5 product target therefore remains gated by the wider manual, performance, accessibility, OEM, security and release evidence in `QUALITY_GATES.md`.
