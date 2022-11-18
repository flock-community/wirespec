#!/usr/bin/env bash

buildNothing=false
archSpecific=""

if [[ $(uname -m) = arm64 ]]; then
  archSpecific="--platform=linux/amd64"
fi

if [[ $WIRESPEC_BUILD_MAC != true && $WIRESPEC_BUILD_WINDOWS != true ]]; then
  buildNothing=true
fi

if [[ $WIRESPEC_BUILD_ALL = true || $WIRESPEC_BUILD_LINUX = true || $buildNothing = true ]]; then
  docker build $archSpecific -t wirespec .
fi
