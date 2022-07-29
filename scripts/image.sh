#!/usr/bin/env bash

buildNothing=false
archSpecific=""

if [[ $(uname -m) = arm64 ]]; then
  archSpecific="--platform=linux/amd64"
fi

if [[ $WIRE_SPEC_BUILD_MAC != true && $WIRE_SPEC_BUILD_WINDOWS != true ]]; then
  buildNothing=true
fi

if [[ $WIRE_SPEC_BUILD_ALL = true || $WIRE_SPEC_BUILD_LINUX = true || $buildNothing = true ]]; then
  docker build $archSpecific -t wire-spec .
fi
