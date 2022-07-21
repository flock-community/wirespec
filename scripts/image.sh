#!/usr/bin/env bash

buildNothing=false

if [ "$WIRE_SPEC_BUILD_MAC" != true ] && [ "$WIRE_SPEC_BUILD_WINDOWS" != true ]; then
  buildNothing=true
fi

if [ "$WIRE_SPEC_BUILD_ALL" = true ] || [ "$WIRE_SPEC_BUILD_LINUX" = true ] || [ "$buildNothing" = true ]; then
  docker build -t wire-spec .
fi
