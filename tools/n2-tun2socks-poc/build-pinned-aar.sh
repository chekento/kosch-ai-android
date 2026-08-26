#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
POC_DIR="$ROOT_DIR/tools/n2-tun2socks-poc"
PATCH_FILE="$POC_DIR/patches/0001-kosch-safe-mobile-bridge.patch"
PINNED_COMMIT="8dda19e8e4613e014f0b12f3e624fdff5e5f23b3"
PINNED_GO="go1.26.3"
PINNED_MODULE="github.com/xjasonlyu/tun2socks/v2"
PINNED_X_MOBILE="v0.0.0-20260821190718-4776eadac327"
PINNED_X_MOBILE_COMMIT="4776eadac327bcb80cebc7413c91f8b4abf8ffa1"
PINNED_NDK="28.2.13676358"
BOUND_PACKAGE="$PINNED_MODULE/koschmobile"
ANDROID_API="29"

SOURCE_DIR="${1:-}"
OUTPUT_DIR="${2:-$POC_DIR/out}"

fail() {
  printf 'N2 tun2socks POC: %s\n' "$*" >&2
  exit 1
}

mobile_tool_version() {
  go version -m "$1" | awk '$1 == "mod" && $2 == "golang.org/x/mobile" { print $3; exit }'
}

[[ -n "$SOURCE_DIR" ]] || fail "usage: $0 /path/to/tun2socks-v2.7.0 [output-dir]"
SOURCE_DIR="$(cd "$SOURCE_DIR" && pwd)"
mkdir -p "$OUTPUT_DIR"
OUTPUT_DIR="$(cd "$OUTPUT_DIR" && pwd)"

for tool in git go gomobile gobind sha256sum unzip jar awk grep; do
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

GOMOBILE_PATH="$(command -v gomobile)"
GOBIND_PATH="$(command -v gobind)"
GOMOBILE_MODULE_VERSION="$(mobile_tool_version "$GOMOBILE_PATH")"
GOBIND_MODULE_VERSION="$(mobile_tool_version "$GOBIND_PATH")"
[[ "$GOMOBILE_MODULE_VERSION" == "$PINNED_X_MOBILE" ]] || fail "gomobile must come from $PINNED_X_MOBILE, got ${GOMOBILE_MODULE_VERSION:-unknown}"
[[ "$GOBIND_MODULE_VERSION" == "$PINNED_X_MOBILE" ]] || fail "gobind must come from $PINNED_X_MOBILE, got ${GOBIND_MODULE_VERSION:-unknown}"

[[ -n "${ANDROID_NDK_HOME:-}" ]] || fail "ANDROID_NDK_HOME must point to pinned NDK $PINNED_NDK"
NDK_SOURCE_PROPERTIES="$ANDROID_NDK_HOME/source.properties"
[[ -f "$NDK_SOURCE_PROPERTIES" ]] || fail "NDK source.properties missing at $NDK_SOURCE_PROPERTIES"
NDK_VERSION="$(awk -F= '$1 ~ /^[[:space:]]*Pkg.Revision[[:space:]]*$/ { gsub(/[[:space:]]/, "", $2); print $2; exit }' "$NDK_SOURCE_PROPERTIES")"
[[ "$NDK_VERSION" == "$PINNED_NDK" ]] || fail "Android NDK must be exactly $PINNED_NDK, got ${NDK_VERSION:-unknown}"

# The Go pseudo-version carries only the source revision prefix. source.lock records the reviewed full
# commit mapping, while the binary SHA-256 values below identify the actual installed tool binaries.
X_MOBILE_REV_SUFFIX="${PINNED_X_MOBILE##*-}"
[[ "$PINNED_X_MOBILE_COMMIT" =~ ^[0-9a-f]{40}$ ]] || fail "x/mobile full commit lock is malformed"
[[ "${PINNED_X_MOBILE_COMMIT:0:${#X_MOBILE_REV_SUFFIX}}" == "$X_MOBILE_REV_SUFFIX" ]] || \
  fail "x/mobile full commit lock does not match pseudo-version revision $X_MOBILE_REV_SUFFIX"

# No command after this point may resolve modules from the network.
export GOPROXY=off
export GOSUMDB=off

WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/kosch-tun2socks-poc.XXXXXX")"
CLASS_JAR=""
cleanup() {
  git -C "$SOURCE_DIR" worktree remove --force "$WORK_DIR" >/dev/null 2>&1 || true
  [[ -z "$CLASS_JAR" ]] || rm -f "$CLASS_JAR"
  rm -rf "$WORK_DIR"
}
trap cleanup EXIT

git -C "$SOURCE_DIR" worktree add --detach "$WORK_DIR" "$PINNED_COMMIT" >/dev/null
git -C "$WORK_DIR" apply --check "$PATCH_FILE"
git -C "$WORK_DIR" apply "$PATCH_FILE"

# Static containment checks: only the dedicated wrapper may cross the gomobile boundary.
[[ -f "$WORK_DIR/koschmobile/mobile.go" ]] || fail "koschmobile wrapper missing"
grep -q '^package koschmobile$' "$WORK_DIR/koschmobile/mobile.go" || fail "unexpected wrapper package"
grep -q 'func Start(tunFD int64, mtu int64, protector SocketProtector) (result string)' "$WORK_DIR/koschmobile/mobile.go" || fail "controlled string-return Start API missing"
grep -q 'func Stop() (result string)' "$WORK_DIR/koschmobile/mobile.go" || fail "controlled string-return Stop API missing"
grep -q 'recover()' "$WORK_DIR/koschmobile/mobile.go" || fail "panic containment missing"
grep -q 'unix.Dup(originalFD)' "$WORK_DIR/engine/kosch_direct.go" || fail "TUN fd duplication missing"
grep -q 'Proxy:[[:space:]]*"direct://"' "$WORK_DIR/engine/kosch_direct.go" || fail "fixed N2 direct egress missing"
grep -q 'SetMandatorySockOpt' "$WORK_DIR/dialer/dialer.go" || fail "mandatory socket hook missing"
grep -q 'protector(int64(fd))' "$WORK_DIR/engine/kosch_direct.go" || fail "protect(fd) callback missing"
grep -q 'KoSch socket protector is required' "$WORK_DIR/engine/kosch_direct.go" || fail "missing-protector hard block missing"
if grep -R -n --include='*.go' 'VpnService.Builder\|Builder.establish\|android.permission.INTERNET' "$WORK_DIR/engine" "$WORK_DIR/dialer" "$WORK_DIR/koschmobile"; then
  fail "active Android VPN/runtime permission code found in offline POC surface"
fi

ARTIFACT="$OUTPUT_DIR/tun2socks-kosch-v2.7.0-poc.aar"
REPORT="$OUTPUT_DIR/tun2socks-kosch-v2.7.0-poc.evidence"

(
  cd "$WORK_DIR"

  # Current gomobile requires golang.org/x/mobile/bind to be resolvable from the
  # module being bound. Add the exact module version that produced gomobile/gobind.
  # GOPROXY=off above guarantees this succeeds only from the pre-populated cache.
  go mod edit -require="golang.org/x/mobile@$PINNED_X_MOBILE"
  go list -mod=mod golang.org/x/mobile/bind >/dev/null

  # Compile the patched engine and wrapper before binding, without running tests.
  go test -run '^$' ./engine ./koschmobile

  # Bind ONLY the narrow wrapper. Upstream engine.Key and fatal Start/Stop wrappers
  # must never become part of the generated Java/Kotlin API surface.
  gomobile bind \
    -target=android \
    -androidapi "$ANDROID_API" \
    -o "$ARTIFACT" \
    ./koschmobile
)

[[ -s "$ARTIFACT" ]] || fail "gomobile did not produce an AAR"

# Inspect the Java API surface in the AAR. Native Go symbols may exist internally,
# but generated Java classes are allowlisted to the bound koschmobile API and
# gomobile's go/ support package. Non-class archive metadata/resources are ignored.
CLASS_JAR="$(mktemp "${TMPDIR:-/tmp}/kosch-tun2socks-classes.XXXXXX.jar")"
unzip -p "$ARTIFACT" classes.jar >"$CLASS_JAR"
[[ -s "$CLASS_JAR" ]] || fail "AAR classes.jar missing"
JAVA_CLASSES="$(jar tf "$CLASS_JAR")"
printf '%s\n' "$JAVA_CLASSES" | grep -q '^koschmobile/' || fail "koschmobile Java API missing"
UNEXPECTED_JAVA_CLASSES="$(
  printf '%s\n' "$JAVA_CLASSES" \
    | grep '\.class$' \
    | grep -Ev '^(koschmobile/|go/)' \
    || true
)"
[[ -z "$UNEXPECTED_JAVA_CLASSES" ]] || fail "unexpected generated Java API classes: $UNEXPECTED_JAVA_CLASSES"

ARTIFACT_SHA="$(sha256sum "$ARTIFACT" | awk '{print $1}')"
PATCH_SHA="$(sha256sum "$PATCH_FILE" | awk '{print $1}')"
GOMOBILE_SHA="$(sha256sum "$GOMOBILE_PATH" | awk '{print $1}')"
GOBIND_SHA="$(sha256sum "$GOBIND_PATH" | awk '{print $1}')"

cat >"$REPORT" <<EOF
format=kosch-n2-offline-aar-evidence-v4
upstream=https://github.com/xjasonlyu/tun2socks
version=v2.7.0
commit=$PINNED_COMMIT
go_module=$PINNED_MODULE
go_version=$GO_VERSION
x_mobile_version=$PINNED_X_MOBILE
x_mobile_commit=$PINNED_X_MOBILE_COMMIT
gomobile_module_version=$GOMOBILE_MODULE_VERSION
gobind_module_version=$GOBIND_MODULE_VERSION
ndk_version=$NDK_VERSION
gomobile_sha256=$GOMOBILE_SHA
gobind_sha256=$GOBIND_SHA
patch_sha256=$PATCH_SHA
android_min_api=$ANDROID_API
bound_package=$BOUND_PACKAGE
engine_java_api_exposed=false
tun_fd_duplicated=true
panic_cross_boundary=false
fixed_direct_egress=true
proxy_configuration_exposed=false
network_during_build=false
runtime_integrated=false
vpn_established=false
internet_permission_added=false
recoverable_start_api=true
mandatory_protect_hook=true
artifact=$(basename "$ARTIFACT")
artifact_sha256=$ARTIFACT_SHA
EOF

printf 'AAR: %s\nEvidence: %s\nSHA-256: %s\nNDK: %s\n' "$ARTIFACT" "$REPORT" "$ARTIFACT_SHA" "$NDK_VERSION"
