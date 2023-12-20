#!/usr/bin/env bash

dir="$(dirname -- "$0")"

if [[ $(uname) = Darwin ]]; then
  macosArch=macosX64

  if [[ $(uname -m) = arm64 ]]; then
    macosArch="macosArm64"
  fi

  cp "$dir"/../src/compiler/cli/build/bin/"$macosArch"/releaseExecutable/cli.kexe /usr/local/bin/wirespec
fi
