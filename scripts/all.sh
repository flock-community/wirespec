#!/usr/bin/env bash

dir="$( dirname -- "$0"; )"

"$dir"/compile.sh && \
"$dir"/test.sh && \
"$dir"/build.sh && \
"$dir"/run.sh
