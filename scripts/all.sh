#!/usr/bin/env bash

export WIRESPEC_BUILD_MAC=true
export WIRESPEC_BUILD_LINUX=true

dir="$(dirname -- "$0")"

"$dir"/build.sh &&
  "$dir"/image.sh &&
  "$dir"/test.sh &&
  "$dir"/example.sh
