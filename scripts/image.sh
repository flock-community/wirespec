#!/usr/bin/env bash

archSpecific=""

if [[ $(uname -m) = arm64 ]]; then
  archSpecific="--platform=linux/amd64"
fi

if [[ $WIRESPEC_BUILD_ALL = true || $WIRESPEC_BUILD_LINUX = true ]]; then
  docker build $archSpecific -t wirespec .
fi
