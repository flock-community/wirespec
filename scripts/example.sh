#!/usr/bin/env bash

dir="$(dirname -- "$0")"

# publish and build gradle example
./gradlew src:plugin:gradle:publishToMavenLocal &&
  ./gradlew src:plugin:maven:publishToMavenLocal &&
  # build examples
  (cd "$dir"/../examples && make clean && make build)
