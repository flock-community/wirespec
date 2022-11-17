#!/usr/bin/env bash

export WIRESPEC_BUILD_MAC=true

dir="$(dirname -- "$0")"

"$dir"/build.sh &&
  "$dir"/image.sh &&
  "$dir"/test.sh
