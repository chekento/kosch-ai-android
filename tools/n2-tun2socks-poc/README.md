# N2 tun2socks offline Android POC

Status: **packaging/API proof only — no VPN activation, no traffic forwarding**

This directory defines a reproducible, traffic-inert experiment for evaluating `xjasonlyu/tun2socks` as the future N2 direct packet forwarder.

## Pinned input

- upstream: `https://github.com/xjasonlyu/tun2socks`
- tag: `v2.7.0`
- commit: `8dda19e8e4613e014f0b12f3e624fdff5e5f23b3`
- upstream Go directive: `go1.26.3`
- Android minimum API for the POC artifact: `29`

`source.lock` is authoritative. The builder refuses any different source HEAD or Go toolchain.

## What the KoSch patch changes

The patch is intentionally narrow:

1. adds a mandatory dialer socket option that runs **before** tun2socks' non-global-unicast short-circuit;
2. samples a gomobile-friendly `SocketProtector` during engine startup;
3. requires that protector to approve every forwarder-created socket before connect/listen continues;
4. exposes `StartSafe()` and `StopSafe()` so KoSch never calls the upstream `Start()` / `Stop()` wrappers that route errors through `log.Fatalf(...)`;
5. clears the mandatory hook during safe shutdown.

The intended Android implementation of `SocketProtector.Protect(fd)` is `VpnService.protect(fd)`. The app-side `StrictVpnSocketProtectorBridge` rejects negative/oversized descriptors and converts exceptions into `false`.

## Offline build

The build script does **not** clone or download anything. Supply a clean local checkout whose HEAD is the pinned commit and pre-install the exact Go toolchain plus a `gomobile` binary.

```bash
tools/n2-tun2socks-poc/build-pinned-aar.sh /path/to/tun2socks-v2.7.0
```

The script forces:

```text
GOPROXY=off
GOSUMDB=off
```

Therefore every Go dependency must already exist in the local module cache. Missing cached dependencies are a hard failure rather than a reason to access the network.

The output directory contains:

- `tun2socks-kosch-v2.7.0-poc.aar`
- `tun2socks-kosch-v2.7.0-poc.evidence`

The evidence report records the upstream commit, Go version, SHA-256 of the `gomobile` executable, KoSch patch and generated AAR, plus the containment flags used by `Tun2SocksOfflinePocContract`.

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
