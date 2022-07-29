#!/usr/bin/env bash

machineHardware="$(uname -m)"

if [ "$WIRE_SPEC_BUILD_MAC" = true ] && [ "$machineHardware" = arm64 ]; then
  echo "Setting Arm64 build for MacOs"
  export WIRE_SPEC_BUILD_MAC_ARM=true
fi

if [ "$WIRE_SPEC_BUILD_MAC" = true ] && [ "$machineHardware" = x86_64 ]; then
  echo "Setting X86 build for MacOs"
  export WIRE_SPEC_BUILD_MAC_X86=true
fi

dir="$(dirname -- "$0")"

./gradlew build &&
  cd "$dir"/../lsp/node/server && npm i && npm run build &&
  cd "$dir"/../lsp/node/client && npm i && npm run build
