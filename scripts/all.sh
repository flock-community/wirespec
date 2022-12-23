#!/usr/bin/env bash

export WIRESPEC_BUILD_MAC=true
export WIRESPEC_BUILD_LINUX=true
export WIRESPEC_BUILD_WINDOWS=false

dir="$(dirname -- "$0")"

"$dir"/build.sh &&
  "$dir"/image.sh &&
  "$dir"/test.sh &&
  "$dir"/example.sh
