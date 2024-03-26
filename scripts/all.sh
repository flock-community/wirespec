#!/usr/bin/env bash

export WIRESPEC_BUILD_MAC=false
export WIRESPEC_BUILD_LINUX=false

dir="$(dirname -- "$0")"

"$dir"/build.sh &&
  "$dir"/image.sh &&
  "$dir"/test.sh &&
  "$dir"/example.sh
