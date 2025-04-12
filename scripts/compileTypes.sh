#!/usr/bin/env bash

archSpecific=""
if [[ $(uname -m) = arm64 ]]; then
  archSpecific="--platform=linux/amd64"
fi

docker run $archSpecific --rm -it -v ./types/:/app/types wirespec /app/compileTypes.sh
