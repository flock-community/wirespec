#!/usr/bin/env bash

dir="$(dirname -- "$0")"

macosArch=macosX64

if [[ $(uname -m) = arm64 ]]; then
  macosArch="macosArm64"
fi

if [[ $(uname) = Darwin ]]; then
  rm "$dir"/../wirespec
  cp "$dir"/../src/compiler/cli/build/bin/"$macosArch"/releaseExecutable/cli.kexe "$dir"/../wirespec
fi
