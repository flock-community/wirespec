#!/usr/bin/env bash

machineHardware="$(uname -m)"

export WIRE_SPEC_BUILD_MAC=true
export WIRE_SPEC_BUILD_LINUX=true
export WIRE_SPEC_BUILD_WINDOWS=true

if [ "$WIRE_SPEC_BUILD_MAC" = true ] && [ "$machineHardware" = arm64 ]; then
  export WIRE_SPEC_BUILD_MAC_ARM=true
fi

if [ "$WIRE_SPEC_BUILD_MAC" = true ] && [ "$machineHardware" = x86_64 ]; then
  export WIRE_SPEC_BUILD_MAC_X86=true
fi

dir="$(dirname -- "$0")"

"$dir"/build.sh &&
  "$dir"/image.sh &&
  "$dir"/test.sh
