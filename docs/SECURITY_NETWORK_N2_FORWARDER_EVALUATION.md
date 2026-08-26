# Security & Network N2 — Forwarder evaluation

Review date: **2026-08-26**  
Status: **source-review / preflight only — no forwarder dependency integrated**

This document records why KoSch may investigate one forwarder in an isolated proof of concept without treating it as safe for active VPN traffic.

## Decision

For **N2 direct forwarding**, the current source-review candidate is:

- `xjasonlyu/tun2socks` pinned to **v2.7.0** — **POC candidate only**.

For the later **N4 proxy/upstream routing** stage, retain as a separate candidate:

- `heiher/hev-socks5-tunnel` pinned to **2.17.1** — **N4 candidate only**.

Neither candidate is currently activation-eligible in KoSch.

## Why tun2socks v2.7.0 is worth an N2 POC

Pinned upstream evidence reviewed at tag `v2.7.0`:

- the tag is MIT licensed;
- the project uses a userspace networking stack based on gVisor;
- the source tree contains an FD-based device implementation suitable for consuming a supplied TUN file descriptor;
- the source tree contains a `direct` proxy implementation;
- that direct implementation opens TCP connections directly and exposes a UDP packet connection path;
- the project documents IPv6 support.

These properties make the source a better semantic fit for **N2 direct forwarding** than a SOCKS-only engine.

### What is still unverified for KoSch

The following remain hard blockers for activation:

- reproducible Android embedding/build path inside this app;
- ABI/package-size impact;
- explicit `VpnService.protect(...)` strategy for every upstream TCP and UDP socket created by the forwarder;
- deterministic stop behavior after partial startup;
- return-path behavior after network migration;
- no-black-hole behavior if native start or forwarding fails;
- API 29 / 33 / 36 integration evidence;
- physical Samsung/Pixel/Fold-class device evidence;
- foreground-service/notification lifecycle for the eventual active service;
- crash/revoke/process-death cleanup.

Until those items are proven, `ForwarderCandidateEvaluation.activationEligible` must remain `false`.

## Why HEV 2.17.1 is not the N2 direct candidate

Pinned upstream evidence reviewed at tag `2.17.1`:

- MIT license;
- official Android support and Android ABI release artifacts;
- documented TUN-FD C/JNI API;
- IPv4/IPv6 support;
- TCP and UDP support;
- explicit quit API.

However, HEV is explicitly a **tunnel over a SOCKS5 proxy**. Requiring a SOCKS5 upstream would pull proxy-routing semantics into N2, while the roadmap intentionally reserves proxy/upstream selection for **N4**.

HEV therefore stays useful as an N4 candidate instead of being forced into the N2 direct path.

### Why the SocksTun reference is not sufficient KoSch evidence

The upstream SocksTun Android reference demonstrates a working `VpnService` + HEV integration, but its global mode excludes its own package from the VPN route rather than proving KoSch's stricter requirement that upstream sockets are explicitly escaped through `VpnService.protect(...)`.

That reference is useful architecture material, not activation evidence for this project.

## Two separate gates

### Prototype eligibility

A candidate can enter an **isolated engineering POC** only when its pinned source already proves the foundational fit:

- intended roadmap use case;
- project-approved permissive license;
- TUN-FD intake;
- direct egress for N2;
- IPv4 and IPv6;
- TCP and UDP.

Prototype eligibility **does not authorize VPN activation**.

### Activation eligibility

A candidate can become activation-eligible only after all source-fit requirements plus these integration/evidence requirements pass:

- Android embedding verified;
- upstream socket escape uses `VpnService.protect(...)`;
- deterministic stop API/lifecycle verified;
- no-black-hole behavior verified;
- physical-device evidence verified.

The existing `N2ActivationGate` remains an additional runtime boundary after candidate evaluation.

## POC containment rule

The first native/embedding POC may package and locally exercise a pinned forwarder artifact, but it must not:

- change Android manifest permissions;
- add `INTERNET`;
- call `VpnService.Builder.establish()`;
- open network sockets;
- mutate `KoSchConsentVpnService`;
- capture packet payloads.

`N2PrototypeScope` encodes these restrictions in code and unit tests.

## Upstream evidence references

- tun2socks release/tag: `https://github.com/xjasonlyu/tun2socks/releases/tag/v2.7.0`
- tun2socks MIT license at tag: `https://github.com/xjasonlyu/tun2socks/blob/v2.7.0/LICENSE`
- tun2socks FD device source: `https://github.com/xjasonlyu/tun2socks/tree/v2.7.0/core/device/fdbased`
- tun2socks direct source: `https://github.com/xjasonlyu/tun2socks/blob/v2.7.0/proxy/direct/direct.go`
- HEV release/tag: `https://github.com/heiher/hev-socks5-tunnel/releases/tag/2.17.1`
- HEV README/API: `https://github.com/heiher/hev-socks5-tunnel/blob/2.17.1/README.md`
- HEV MIT license: `https://github.com/heiher/hev-socks5-tunnel/blob/2.17.1/LICENSE`
- Android VpnService API: `https://developer.android.com/reference/android/net/VpnService`

## Score boundary

This source evaluation and POC policy do **not** increase the launcher benchmark score. No user traffic is forwarded yet.
