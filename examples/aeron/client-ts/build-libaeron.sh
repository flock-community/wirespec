#!/bin/bash
# Build the Aeron C client library (libaeron.so) at the same version as the
# example's media drivers, for the TypeScript client's FFI binding to load.
# Skipped when target/libaeron/libaeron.so is already present.
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
AERON_VERSION=1.53.0
TARGET="$SCRIPT_DIR/target"
LIB="$TARGET/libaeron/libaeron.so"

if [ -f "$LIB" ]; then
  echo "libaeron.so already built at $LIB"
  exit 0
fi

if ! command -v cmake > /dev/null 2>&1; then
  echo "cmake is required to build the Aeron C client (>= 3.30; e.g. 'pip install cmake')" >&2
  exit 1
fi

mkdir -p "$TARGET"
if [ ! -d "$TARGET/aeron" ]; then
  git clone --depth 1 --branch "$AERON_VERSION" https://github.com/aeron-io/aeron.git "$TARGET/aeron"
fi

cmake -S "$TARGET/aeron" -B "$TARGET/aeron-build" \
  -DCMAKE_BUILD_TYPE=Release \
  -DBUILD_AERON_DRIVER=OFF \
  -DBUILD_AERON_ARCHIVE_API=OFF \
  -DAERON_TESTS=OFF \
  -DAERON_BUILD_SAMPLES=OFF \
  -DAERON_BUILD_DOCUMENTATION=OFF
cmake --build "$TARGET/aeron-build" --target aeron -j "$(nproc 2>/dev/null || sysctl -n hw.ncpu)"

mkdir -p "$TARGET/libaeron"
cp "$TARGET/aeron-build/lib/libaeron.so" "$LIB"
echo "Built $LIB"
