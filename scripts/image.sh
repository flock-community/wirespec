#!/usr/bin/env bash

archSpecific=""

if [[ $(uname -m) = arm64 ]]; then
  archSpecific="--platform=linux/amd64"
fi

docker build $archSpecific -t wirespec .
