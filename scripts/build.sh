#!/usr/bin/env bash

build_linux=false

if [ "$WIRE_SPEC_BUILD_ALL" = true ] || [ "$WIRE_SPEC_BUILD_LINUX" = true ]; then
  build_linux=true
fi

if [ $build_linux = true ]; then
  docker build -t wire-spec .
  else
      echo "WIRE_SPEC_BUILD_LINUX or WIRE_SPEC_BUILD_ALL not set to 'true'"
fi
