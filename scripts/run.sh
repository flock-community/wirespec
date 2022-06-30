#!/usr/bin/env bash

docker run --rm -it -v "$(pwd)"/types:/app/types wire-spec
