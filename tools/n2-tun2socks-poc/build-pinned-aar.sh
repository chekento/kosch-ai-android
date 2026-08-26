#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
POC_DIR="$ROOT_DIR/tools/n2-tun2socks-poc"
PATCH_FILE="$POC_DIR/patches/0001-kosch-safe-mobile-bridge.patch"
PINNED_COMMIT="8dda19e8e4613e014f0b12f3e624fdff5e5f23b3"
PINNED_GO="go1.26.3"
PINNED_MODULE="github.com/xjasonlyu/tun2socks/v2"
ANDROID_API="29"

SOURCE_DIR="${1:-}"
OUTPUT_DIR="${2:-$POC_DIR/out}"

fail() {
  printf 'N2 tun2socks POC: %s\n' "$*" >&2
  exit 1
}

[[ -n "$SOURCE_DIR" ]] || fail "usage: $0 /path/to/tun2socks-v2.7.0 [output-dir]"
SOURCE_DIR="$(cd "$SOURCE_DIR" && pwd)"
mkdir -p "$OUTPUT_DIR"
OUTPUT_DIR="$(cd "$OUTPUT_DIR" && pwd)"

for tool in git go gomobile sha256sum; do
  command -v "$tool" >/dev/null 2>&1 || fail "required tool not found: $tool"
done

[[ -f "$SOURCE_DIR/go.mod" ]] || fail "source checkout has no go.mod"
[[ -f "$PATCH_FILE" ]] || fail "KoSch patch is missing"

SOURCE_HEAD="$(git -C "$SOURCE_DIR" rev-parse HEAD)"
[[ "$SOURCE_HEAD" == "$PINNED_COMMIT" ]] || fail "source HEAD $SOURCE_HEAD does not match pinned $PINNED_COMMIT"
[[ -z "$(git -C "$SOURCE_DIR" status --porcelain)" ]] || fail "source checkout must be clean"

MODULE="$(awk '$1 == "module" { print $2; exit }' "$SOURCE_DIR/go.mod")"
[[ "$MODULE" == "$PINNED_MODULE" ]] || fail "unexpected Go module: $MODULE"

GO_VERSION="$(go env GOVERSION)"
[[ "$GO_VERSION" == "$PINNED_GO" ]] || fail "Go toolchain must be exactly $PINNED_GO, got $GO_VERSION"

WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/kosch-tun2socks-poc.XXXXXX")"
cleanup() {
  git -C "$SOURCE_DIR" worktree remove --force "$WORK_DIR" >/dev/null 2>&1 || true
  rm -rf "$WORK_DIR"
}
trap cleanup EXIT

git -C "$SOURCE_DIR" worktree add --detach "$WORK_DIR" "$PINNED_COMMIT" >/dev/null
git -C "$WORK_DIR" apply --check "$PATCH_FILE"
git -C "$WORK_DIR" apply "$PATCH_FILE"

# Static containment checks. This POC patch must expose safe lifecycle/protect hooks,
# and must not contain an Android VPN establishment path.
grep -q 'func StartSafe() error' "$WORK_DIR/engine/kosch_mobile.go" || fail "recoverable StartSafe missing"
grep -q 'SetMandatorySockOpt' "$WORK_DIR/dialer/dialer.go" || fail "mandatory socket hook missing"
grep -q 'protector.Protect(int64(fd))' "$WORK_DIR/engine/kosch_mobile.go" || fail "protect(fd) callback missing"
if grep -R -n --include='*.go' 'VpnService.Builder\|Builder.establish\|android.permission.INTERNET' "$WORK_DIR/engine" "$WORK_DIR/dialer"; then
  fail "active Android VPN/runtime permission code found in offline POC surface"
fi

ARTIFACT="$OUTPUT_DIR/tun2socks-kosch-v2.7.0-poc.aar"
REPORT="$OUTPUT_DIR/tun2socks-kosch-v2.7.0-poc.evidence"

# Deliberately force an offline module build. Missing cached modules are a hard failure;
# this script never downloads dependencies as part of the evidence-producing build.
export GOPROXY=off
export GOSUMDB=off

(
  cd "$WORK_DIR"
  gomobile bind \
    -target=android \
    -androidapi "$ANDROID_API" \
    -o "$ARTIFACT" \
    ./engine
)

[[ -s "$ARTIFACT" ]] || fail "gomobile did not produce an AAR"
ARTIFACT_SHA="$(sha256sum "$ARTIFACT" | awk '{print $1}')"
PATCH_SHA="$(sha256sum "$PATCH_FILE" | awk '{print $1}')"
GOMOBILE_PATH="$(command -v gomobile)"
GOMOBILE_SHA="$(sha256sum "$GOMOBILE_PATH" | awk '{print $1}')"

cat >"$REPORT" <<EOF
format=kosch-n2-offline-aar-evidence-v1
upstream=https://github.com/xjasonlyu/tun2socks
version=v2.7.0
commit=$PINNED_COMMIT
go_module=$PINNED_MODULE
go_version=$GO_VERSION
gomobile_sha256=$GOMOBILE_SHA
patch_sha256=$PATCH_SHA
android_min_api=$ANDROID_API
network_during_build=false
runtime_integrated=false
vpn_established=false
internet_permission_added=false
recoverable_start_api=true
mandatory_protect_hook=true
artifact=$(basename "$ARTIFACT")
artifact_sha256=$ARTIFACT_SHA
EOF

printf 'AAR: %s\nEvidence: %s\nSHA-256: %s\n' "$ARTIFACT" "$REPORT" "$ARTIFACT_SHA"
