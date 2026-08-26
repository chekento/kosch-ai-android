#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
POC_DIR="$ROOT_DIR/tools/n2-tun2socks-poc"
BASE_BUILDER="$POC_DIR/build-pinned-aar.sh"

PINNED_GO="go1.26.3"
PINNED_JAVA_MAJOR="17"
PINNED_NDK="28.2.13676358"
PINNED_ANDROID_API="29"
PINNED_BUILD_TOOLS="36.0.0"
EXPECTED_ABIS=("arm64-v8a" "armeabi-v7a" "x86" "x86_64")
MIN_LOAD_ALIGNMENT=$((16 * 1024))

SOURCE_DIR="${1:-}"
OUTPUT_DIR="${2:-$POC_DIR/out-controlled}"

fail() {
  printf 'N2 controlled evidence: %s\n' "$*" >&2
  exit 1
}

real_tool() {
  readlink -f "$(command -v "$1")"
}

sha_of_tool() {
  sha256sum "$(real_tool "$1")" | awk '{print $1}'
}

[[ -n "$SOURCE_DIR" ]] || fail "usage: $0 /path/to/tun2socks-v2.7.0 [output-dir]"
[[ -x "$BASE_BUILDER" ]] || fail "base offline builder is missing or not executable"

for tool in go java javac git gomobile gobind sha256sum unzip jar awk sed grep find sort readlink; do
  command -v "$tool" >/dev/null 2>&1 || fail "required tool not found: $tool"
done

[[ "$(go env GOVERSION)" == "$PINNED_GO" ]] || fail "Go must be exactly $PINNED_GO"
JAVAC_VERSION="$(javac -version 2>&1 | awk '{print $2}')"
[[ "$JAVAC_VERSION" == "$PINNED_JAVA_MAJOR".* ]] || fail "javac must be Java $PINNED_JAVA_MAJOR, got $JAVAC_VERSION"

ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[[ -n "$ANDROID_HOME" ]] || fail "ANDROID_HOME or ANDROID_SDK_ROOT is required"
ANDROID_HOME="$(cd "$ANDROID_HOME" && pwd)"
ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-}"
[[ -n "$ANDROID_NDK_HOME" ]] || fail "ANDROID_NDK_HOME must select the controlled NDK"
ANDROID_NDK_HOME="$(cd "$ANDROID_NDK_HOME" && pwd)"

NDK_PROPERTIES="$ANDROID_NDK_HOME/source.properties"
[[ -f "$NDK_PROPERTIES" ]] || fail "NDK source.properties missing"
NDK_VERSION="$(awk -F= '$1 ~ /Pkg.Revision/ {gsub(/[[:space:]]/, "", $2); print $2; exit}' "$NDK_PROPERTIES")"
[[ "$NDK_VERSION" == "$PINNED_NDK" ]] || fail "NDK must be exactly $PINNED_NDK, got ${NDK_VERSION:-unknown}"

ANDROID_JAR="$ANDROID_HOME/platforms/android-$PINNED_ANDROID_API/android.jar"
AAPT2="$ANDROID_HOME/build-tools/$PINNED_BUILD_TOOLS/aapt2"
READELF="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf"
CLANG="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/clang"
[[ -s "$ANDROID_JAR" ]] || fail "Android API $PINNED_ANDROID_API android.jar missing"
[[ -x "$AAPT2" ]] || fail "Android build-tools $PINNED_BUILD_TOOLS aapt2 missing"
[[ -x "$READELF" ]] || fail "NDK llvm-readelf missing"
[[ -x "$CLANG" ]] || fail "NDK clang missing"

mkdir -p "$OUTPUT_DIR"
OUTPUT_DIR="$(cd "$OUTPUT_DIR" && pwd)"

"$BASE_BUILDER" "$SOURCE_DIR" "$OUTPUT_DIR"

AAR="$OUTPUT_DIR/tun2socks-kosch-v2.7.0-poc.aar"
BASE_EVIDENCE="$OUTPUT_DIR/tun2socks-kosch-v2.7.0-poc.evidence"
CONTROLLED_EVIDENCE="$OUTPUT_DIR/tun2socks-kosch-v2.7.0-controlled.evidence"
[[ -s "$AAR" ]] || fail "base builder did not produce the AAR"
[[ -s "$BASE_EVIDENCE" ]] || fail "base builder did not produce evidence"

ARTIFACT_SHA="$(sha256sum "$AAR" | awk '{print $1}')"
BASE_ARTIFACT_SHA="$(awk -F= '$1 == "artifact_sha256" {print $2; exit}' "$BASE_EVIDENCE")"
[[ "$BASE_ARTIFACT_SHA" == "$ARTIFACT_SHA" ]] || fail "base evidence artifact digest does not match generated AAR"

EXTRACT_DIR="$(mktemp -d "${TMPDIR:-/tmp}/kosch-n2-controlled.XXXXXX")"
cleanup() {
  rm -rf "$EXTRACT_DIR"
}
trap cleanup EXIT
unzip -q "$AAR" 'jni/*/libgojni.so' -d "$EXTRACT_DIR"

mapfile -t LIBS < <(find "$EXTRACT_DIR/jni" -type f -name 'libgojni.so' | sort)
[[ "${#LIBS[@]}" -eq "${#EXPECTED_ABIS[@]}" ]] || fail "expected ${#EXPECTED_ABIS[@]} native ABIs, found ${#LIBS[@]}"

mapfile -t ACTUAL_ABIS < <(printf '%s\n' "${LIBS[@]}" | sed -E 's#^.*/jni/([^/]+)/libgojni\.so$#\1#' | sort)
mapfile -t SORTED_EXPECTED_ABIS < <(printf '%s\n' "${EXPECTED_ABIS[@]}" | sort)
[[ "$(printf '%s\n' "${ACTUAL_ABIS[@]}")" == "$(printf '%s\n' "${SORTED_EXPECTED_ABIS[@]}")" ]] || \
  fail "unexpected ABI set: ${ACTUAL_ABIS[*]}"

for lib in "${LIBS[@]}"; do
  LOAD_COUNT=0
  while read -r alignment; do
    [[ -n "$alignment" ]] || continue
    LOAD_COUNT=$((LOAD_COUNT + 1))
    ALIGNMENT_VALUE=$((alignment))
    (( ALIGNMENT_VALUE >= MIN_LOAD_ALIGNMENT )) || \
      fail "$(basename "$(dirname "$lib")") LOAD segment alignment $alignment is below 16 KiB"
  done < <("$READELF" -lW "$lib" | awk '$1 == "LOAD" {print $NF}')
  (( LOAD_COUNT > 0 )) || fail "no ELF LOAD segments found in $lib"
done

BASE_EVIDENCE_SHA="$(sha256sum "$BASE_EVIDENCE" | awk '{print $1}')"
GO_SHA="$(sha_of_tool go)"
JAVA_SHA="$(sha_of_tool javac)"
READELF_SHA="$(sha256sum "$READELF" | awk '{print $1}')"
CLANG_SHA="$(sha256sum "$CLANG" | awk '{print $1}')"
ANDROID_JAR_SHA="$(sha256sum "$ANDROID_JAR" | awk '{print $1}')"
AAPT2_SHA="$(sha256sum "$AAPT2" | awk '{print $1}')"

cat >"$CONTROLLED_EVIDENCE" <<EOF
format=kosch-n2-controlled-build-evidence-v1
artifact=$(basename "$AAR")
artifact_sha256=$ARTIFACT_SHA
base_evidence=$(basename "$BASE_EVIDENCE")
base_evidence_sha256=$BASE_EVIDENCE_SHA
go_version=$(go env GOVERSION)
go_binary_sha256=$GO_SHA
javac_version=$JAVAC_VERSION
javac_binary_sha256=$JAVA_SHA
android_api=$PINNED_ANDROID_API
android_jar_sha256=$ANDROID_JAR_SHA
android_build_tools=$PINNED_BUILD_TOOLS
aapt2_sha256=$AAPT2_SHA
ndk_version=$NDK_VERSION
clang_sha256=$CLANG_SHA
llvm_readelf_sha256=$READELF_SHA
native_abis=$(IFS=,; echo "${SORTED_EXPECTED_ABIS[*]}")
native_load_alignment_min_bytes=$MIN_LOAD_ALIGNMENT
native_16k_alignment_verified=true
runtime_integrated=false
vpn_established=false
internet_permission_added=false
traffic_forwarding_executed=false
EOF

printf 'Controlled AAR: %s\nBase evidence: %s\nControlled evidence: %s\nSHA-256: %s\n' \
  "$AAR" "$BASE_EVIDENCE" "$CONTROLLED_EVIDENCE" "$ARTIFACT_SHA"
