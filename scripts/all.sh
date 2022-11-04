#!/usr/bin/env bash

export WIRE_SPEC_BUILD_MAC=true
export WIRE_SPEC_BUILD_LINUX=true
export WIRE_SPEC_BUILD_WINDOWS=true

dir="$(dirname -- "$0")"

"$dir"/build.sh &&
  "$dir"/image.sh &&
  "$dir"/test.sh &&
  "$dir"/example.sh
