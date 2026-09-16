#!/bin/bash
# Generate the Wirespec sources, build and test the wirespec-aeron TypeScript
# integration and this client, then assemble target/docker: the self-contained
# Docker build context for the integration test (node binary, libaeron.so, and
# the app with its production node_modules).
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
INTEGRATION_DIR="$SCRIPT_DIR/../../../src/integration/aeron/typescript"

if ! command -v node > /dev/null 2>&1 || ! command -v npm > /dev/null 2>&1; then
  echo "node and npm are required to build the TypeScript client" >&2
  exit 1
fi

# The integration package: frame protocol golden tests need no media driver.
(cd "$INTEGRATION_DIR" && npm install && npm test)

"$SCRIPT_DIR/gen.sh"

cd "$SCRIPT_DIR"
npm install
npm test

"$SCRIPT_DIR/build-libaeron.sh"

# Docker build context: everything the image needs, built on this host, so the
# image build itself needs no network (mirroring the Rust client's image).
DOCKER_DIR="$SCRIPT_DIR/target/docker"
rm -rf "$DOCKER_DIR"
mkdir -p "$DOCKER_DIR/app"
cp -L "$(command -v node)" "$DOCKER_DIR/node"
chmod +x "$DOCKER_DIR/node"
cp "$SCRIPT_DIR/target/libaeron/libaeron.so" "$DOCKER_DIR/libaeron.so"
cp -r "$SCRIPT_DIR/dist" "$DOCKER_DIR/app/dist"

# Production node_modules for the image: the integration package packed as a
# tarball (a portable stand-in for the file: link), koffi from the registry.
(cd "$INTEGRATION_DIR" && npm pack --pack-destination "$DOCKER_DIR/app" > /dev/null)
mv "$DOCKER_DIR/app"/flock-wirespec-aeron-*.tgz "$DOCKER_DIR/app/wirespec-aeron.tgz"
cat > "$DOCKER_DIR/app/package.json" << 'EOF'
{
  "name": "wirespec-aeron-client-ts",
  "version": "0.0.0",
  "private": true,
  "dependencies": {
    "@flock/wirespec-aeron": "file:./wirespec-aeron.tgz"
  }
}
EOF
(cd "$DOCKER_DIR/app" && npm install --omit=dev > /dev/null)
echo "Docker build context assembled at $DOCKER_DIR"
