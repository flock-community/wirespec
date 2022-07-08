#!/usr/bin/env bash

export WIRE_SPEC_BUILD_MAC=true
export WIRE_SPEC_BUILD_LINUX=true
export WIRE_SPEC_BUILD_WINDOWS=true

dir="$( dirname -- "$0"; )"

"$dir"/compile.sh && \
"$dir"/test.sh && \
"$dir"/build.sh && \
"$dir"/run.sh
