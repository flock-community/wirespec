#!/usr/bin/env bash

export WIRE_SPEC_BUILD_MAC=true

if [ "$WIRE_SPEC_BUILD_MAC" = true ] && [ "$(uname -m)" = arm64 ]; then
  export WIRE_SPEC_BUILD_MAC_ARM=true
else
  export WIRE_SPEC_BUILD_MAC_X86=true
fi

dir="$(dirname -- "$0")"

"$dir"/build.sh &&
  "$dir"/image.sh &&
  "$dir"/test.sh
