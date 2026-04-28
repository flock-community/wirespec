#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

UNAME="$(uname -s)_$(uname -m)"
case "$UNAME" in
  Linux_x86_64)  PLATFORM="linuxX64" ;;
  Darwin_arm64)  PLATFORM="macosArm64" ;;
  Darwin_x86_64) PLATFORM="macosX64" ;;
  *)             echo "Unsupported platform: $UNAME" >&2; exit 1 ;;
esac

CLI="$ROOT_DIR/src/plugin/cli/build/bin/$PLATFORM/releaseExecutable/cli.kexe"

if [ ! -f "$CLI" ]; then
  echo "Building Wirespec CLI for $PLATFORM..."
  (cd "$ROOT_DIR" && ./gradlew ":src:plugin:cli:${PLATFORM}Binaries")
fi

OUT_DIR="$SCRIPT_DIR/target/generated-sources"
mkdir -p "$OUT_DIR"

echo "Generating Scala code from wirespec defs..."
"$CLI" compile \
  -i "$SCRIPT_DIR/wirespec" \
  -o "$OUT_DIR" \
  -l Scala \
  -p community.flock.wirespec.generated \
  --shared
