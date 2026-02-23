#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$SCRIPT_DIR/../.."
CLI_KEXE="$ROOT_DIR/src/plugin/cli/build/bin/macosArm64/releaseExecutable/cli.kexe"

if [ ! -f "$CLI_KEXE" ]; then
  echo "Building wirespec CLI..."
  "$ROOT_DIR/gradlew" -p "$ROOT_DIR" :src:plugin:cli:macosArm64Binaries
fi

"$CLI_KEXE" convert OpenAPIV2 \
  -i "$SCRIPT_DIR/petstore.json" \
  -o "$SCRIPT_DIR/src/generated" \
  -l Rust \
  -p '' \
  --shared