#!/bin/bash
# Generate the Wirespec sources, then build and test the client.
# Installs the cargo toolchain via rustup when missing (mirrors the root
# build's installCargo task, which only wires itself to Cargo.toml-rooted
# example modules).
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CARGO_BIN="$HOME/.cargo/bin"

if ! command -v cargo > /dev/null 2>&1 && [ ! -x "$CARGO_BIN/cargo" ]; then
  echo "Installing the cargo toolchain via rustup..."
  curl -fsSL https://sh.rustup.rs | sh -s -- -y --profile minimal
fi
export PATH="$CARGO_BIN:$PATH"

"$SCRIPT_DIR/gen.sh"
cd "$SCRIPT_DIR"
cargo build --release
cargo test --release
