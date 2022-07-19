#!/usr/bin/env bash

if [ "$WIRE_SPEC_BUILD_ALL" = true ] || [ "$WIRE_SPEC_BUILD_LINUX" = true ]; then
  docker build -t wire-spec .
  else
      echo "WIRE_SPEC_BUILD_LINUX or WIRE_SPEC_BUILD_ALL not set to 'true'"
fi
