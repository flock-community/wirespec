#!/usr/bin/env bash

dir="$(dirname -- "$0")"

"$dir"/build.sh &&
  "$dir"/image.sh &&
  "$dir"/test.sh &&
  "$dir"/example.sh
