#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

OS="$(uname -s)"
ARCH="$(uname -m)"

case "${OS}_${ARCH}" in
  Linux_x86_64)  PLATFORM="linuxX64" ;;
  Darwin_arm64)  PLATFORM="macosArm64" ;;
  Darwin_x86_64) PLATFORM="macosX64" ;;
  *) echo "Unsupported platform: ${OS}_${ARCH}" >&2; exit 1 ;;
esac

ROOT_DIR="$SCRIPT_DIR/../.."
CLI_KEXE="$ROOT_DIR/src/plugin/cli/build/bin/${PLATFORM}/releaseExecutable/cli.kexe"

if [ ! -f "$CLI_KEXE" ]; then
  echo "Building wirespec CLI..."
  "$ROOT_DIR/gradlew" -p "$ROOT_DIR" -Pwirespec.enableNative=true ":src:plugin:cli:${PLATFORM}Binaries"
fi

"$CLI_KEXE" convert OpenAPIV2 \
  -i "$SCRIPT_DIR/petstore.json" \
  -o "$SCRIPT_DIR/src/gen" \
  -l Rust \
  -p '' \
  --shared
