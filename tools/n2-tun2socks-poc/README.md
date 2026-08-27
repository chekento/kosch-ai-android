# N2 tun2socks offline Android POC

Status: **packaging/API proof only — no VPN activation, no traffic forwarding**

This directory defines a reproducible, traffic-inert experiment for evaluating `xjasonlyu/tun2socks` as the future N2 direct packet forwarder.

## Pinned input

- upstream: `https://github.com/xjasonlyu/tun2socks`
- tag: `v2.7.0`
- commit: `8dda19e8e4613e014f0b12f3e624fdff5e5f23b3`
- upstream Go directive: `go1.26.3`
- `golang.org/x/mobile`: `v0.0.0-20260821190718-4776eadac327`
- x/mobile commit: `4776eadac327bcb80cebc7413c91f8b4abf8ffa1`
- Android NDK: `28.2.13676358`
- Android minimum API for the POC artifact: `29`

`source.lock` is authoritative. The builder refuses any different source HEAD, Go toolchain, `gomobile` module version, `gobind` module version or Android NDK revision.

The x/mobile pseudo-version contains the source revision prefix `4776eadac327`. `source.lock` additionally records the reviewed full commit mapping `4776eadac327bcb80cebc7413c91f8b4abf8ffa1`; the builder verifies that this full lock is well-formed and matches the pseudo-version prefix. This mapping is not presented as an independent reconstruction of the full Git commit from the installed binary. The actual installed `gomobile` and `gobind` executables are independently identified by their SHA-256 fingerprints in the generated evidence.

The x/mobile pin matters because the current `gomobile bind` implementation requires `golang.org/x/mobile/bind` to be resolvable from the module being bound. The POC therefore adds the exact pinned x/mobile version only inside its disposable worktree before binding; it does not change the upstream checkout or the Android app dependency graph.

The NDK pin matters because the AAR contains native code. KoSch uses NDK `28.2.13676358` for this POC so the exact native toolchain is part of the evidence rather than inherited implicitly from whichever runner happens to execute the build.

## What the KoSch patch changes

The patch is intentionally narrow:

1. adds a mandatory dialer socket option that runs **before** tun2socks' non-global-unicast short-circuit;
2. samples a gomobile-friendly `SocketProtector` during engine startup;
3. requires that protector to approve every forwarder-created socket before connect/listen continues;
4. exposes only the bounded `koschmobile.Start()` / `koschmobile.Stop()` facade, backed by recoverable `engine.StartKoSchDirect()` / `engine.StopKoSchDirect()`, so KoSch never calls the upstream `Start()` / `Stop()` wrappers that route failures through `log.Fatalf(...)`;
5. clears the mandatory hook during safe shutdown.

The intended Android implementation of `SocketProtector.Protect(fd)` is `VpnService.protect(fd)`. The app-side `StrictVpnSocketProtectorBridge` rejects negative/oversized descriptors and converts exceptions into `false`.

## Offline build contract

The build script does **not** clone or download anything. It also refuses to emit offline evidence unless its caller has placed it inside a dedicated Linux network namespace that contains only the loopback interface and no IP routes. This makes `network_during_build=false` an enforced condition rather than a label inferred from Go environment variables.

Supply a clean local checkout whose HEAD is the pinned commit and pre-install:

- Go `go1.26.3`;
- `gomobile` from x/mobile `v0.0.0-20260821190718-4776eadac327`;
- `gobind` from the same x/mobile version;
- Android NDK `28.2.13676358` exposed through `ANDROID_NDK_HOME`;
- Android SDK platform 29 required by gomobile;
- a Go module cache containing tun2socks and x/mobile dependencies.

The controlled GitHub evidence workflow supplies the required namespace automatically. A direct local invocation must provide an equivalent isolated namespace and set `KOSCH_NETWORK_NAMESPACE_ISOLATED=1`; setting that variable without the required interface/route state still fails the builder.

Before the module graph is touched inside the actual builder, the script additionally forces:

```text
GOPROXY=off
GOSUMDB=off
```

It then adds the pinned x/mobile requirement in the disposable worktree and explicitly resolves `golang.org/x/mobile/bind`. Missing cached dependencies are a hard failure rather than a reason to access the network.

The output directory contains:

- `tun2socks-kosch-v2.7.0-poc.aar`
- `tun2socks-kosch-v2.7.0-poc.evidence`

The evidence report records the upstream commit, Go version, x/mobile pseudo-version and reviewed full-commit mapping, NDK revision, detected gomobile/gobind module versions, SHA-256 of both tool binaries, KoSch patch and generated AAR, plus the containment flags used by `Tun2SocksOfflinePocContract`. Evidence format v5 also records `network_namespace_isolated=true`.

## Controlled evidence workflow

`.github/workflows/n2-forwarder-poc.yml` is intentionally **manual only** and may produce evidence only from `main`.

It separates the run into two security zones:

1. **networked preparation** — downloads a checksum-pinned official Go `1.26.3` Linux toolchain, installs the exact NDK, checks out the exact tun2socks commit, installs the exact x/mobile pseudo-version and pre-populates all required Go modules;
2. **isolated build** — creates a fresh Linux network namespace with only `lo` and no route, invokes `build-pinned-aar.sh` with `GOPROXY=off` and `GOSUMDB=off`, validates the emitted evidence and archives the AAR plus SHA-256 manifests.

Before networked preparation is allowed to become packaging evidence, the workflow also checks the current KoSch application itself: production source/Gradle surfaces must contain no tun2socks/koschmobile integration marker, `debugRuntimeClasspath` must not contain the forwarder, and Android's generated merged debug manifest must remain free of `android.permission.INTERNET`. The workflow therefore cannot truthfully emit `runtime_integrated=false` merely because one selected Kotlin source file happened not to contain a builder call.

The uploaded evidence directory contains the AAR, offline report, network-preparation report and `SHA256SUMS`. The checksum manifest deliberately contains **relative filenames only**. A reviewer can therefore download and extract the artifact anywhere and independently verify it from inside that directory:

```bash
sha256sum -c SHA256SUMS
```

The verification must succeed without the original GitHub runner path. Absolute `$RUNNER_TEMP` paths in `SHA256SUMS` are forbidden by the repository contract test.

The workflow itself does not modify the Android app, does not add the AAR to Gradle and cannot authorize N2 activation. Its output is only supply-chain/API evidence used by a later review gate.

## Hard containment boundary

This POC must not:

- modify `AndroidManifest.xml`;
- add `INTERNET` or foreground-service permissions;
- add the generated AAR to the app dependency graph;
- call `VpnService.Builder.establish()`;
- acquire a TUN descriptor;
- open network sockets from the Android app;
- mutate `KoSchConsentVpnService`;
- capture packet payloads;
- mark the forwarder activation-eligible.

A valid POC artifact is only packaging/API evidence. Active forwarding remains separately blocked by `ForwarderCandidateEvaluation`, `N2ActivationGate`, runtime permission/service gates and physical-device evidence.

## Next proof after this POC

Only after the AAR can be built reproducibly and its generated gomobile API is inspected may a later branch create an Android adapter test harness. That later harness still needs separate evidence for actual `VpnService.protect(fd)` invocation, TUN lifecycle, no-black-hole behavior, network migration, revoke/process death and API 29/33/36 plus physical OEM devices.
