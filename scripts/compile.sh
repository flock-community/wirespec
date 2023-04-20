#!/usr/bin/env bash

machineHardware="$(uname -m)"

if [[ $WIRESPEC_BUILD_MAC = true && $machineHardware = arm64 ]]; then
  echo "Setting Arm64 build for MacOs"
  export WIRESPEC_BUILD_MAC_ARM=true
fi

if [[ $WIRESPEC_BUILD_MAC = true && $machineHardware = x86_64 ]]; then
  echo "Setting X86 build for MacOs"
  export WIRESPEC_BUILD_MAC_X86=true
fi

./gradlew assemble
