# M2.6 Measured Professional Beta — Test Foundation

M2.6 shifts the launcher quality gate from source-level confidence toward repeatable device evidence. This document tracks the first measurable layer without overstating what has already been executed.

## What is automated in this foundation

The Android module now builds a dedicated instrumentation APK in addition to the application APK.

Instrumentation coverage currently includes:

1. **Cold-launch lifecycle smoke test**
   - launches `MainActivity` through `createAndroidComposeRule`;
   - requires the Activity to reach `RESUMED`;
   - waits for Compose to become idle;
   - verifies that the `KoSch AI` launcher root is present.
2. **Activity recreation / saved-instance smoke test**
   - launches the real launcher Activity;
   - calls the underlying `ActivityScenario.recreate()`;
   - requires the recreated Activity to return to `RESUMED`;
   - verifies that the launcher root is rendered again.
3. **Workspace persistence instrumentation test**
   - writes scene and home-page state through the real Android `SharedPreferences`-backed `WorkspaceStore`;
   - creates a fresh store instance and verifies the values round-trip;
   - restores the previous test values after the assertion.
4. **Onboarding durability test**
   - completes onboarding through the production `WorkspaceStore` API;
   - verifies the completion flag from a fresh store instance.

System animations are disabled for instrumentation runs through the Android Gradle test configuration to reduce UI-test flakiness.

## CI evidence in this stage

The regular CI verification job now compiles:

```bash
./gradlew --no-daemon \
  testDebugUnitTest \
  lintDebug \
  assembleDebug \
  assembleRelease \
  assembleDebugAndroidTest \
  --stacktrace
```

CI uploads a separate `kosch-ai-launcher-instrumentation` artifact containing:

- `KoSch-AI-Launcher-debug.apk`
- `KoSch-AI-Launcher-debug-androidTest.apk`
- `SHA256SUMS`

Building the instrumentation APK proves that the tests and their Android/Compose dependencies compile. It does **not** yet prove that the tests have executed on every target API or OEM.

## Next M2.6 execution matrix

The next gate is actual execution rather than compilation only.

| Gate | API 29 | API 33 | API 36 | API 37 | Real device required |
| --- | --- | --- | --- | --- | --- |
| Cold launch + HOME shell | planned | planned | planned | planned | yes |
| Activity recreation | planned | planned | planned | planned | yes |
| Process death / saved state | planned | planned | planned | planned | yes |
| Widget pick / cancel / configure | planned | planned | planned | planned | yes |
| SAF file + tree picker | planned | planned | planned | planned | yes |
| Backup / audit / SVG export resume | planned | planned | planned | planned | yes |
| Work profile quiet mode | n/a where unsupported | planned | planned | planned | yes |
| TalkBack + 200% font | planned | planned | planned | planned | yes |
| Switch Access | planned | planned | planned | planned | yes |
| Hardware keyboard | planned | planned | planned | planned | yes |
| Stylus pressure / hover / eraser | limited | limited | planned | planned | yes |
| Foldable / multi-window | limited | limited | planned | planned | yes |

## Evidence rule

A roadmap item is not considered measured merely because code or a test exists. For M2.6, a measured claim requires all of the following where applicable:

- test code committed;
- test APK built by CI;
- test executed on the stated API/device class;
- result retained as CI output, test report, benchmark output, or documented physical-device run;
- failures linked to a reproducible issue or fixed before the gate is marked complete.

This keeps the 9.5 target evidence-based rather than score-driven.
