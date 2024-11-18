#!/usr/bin/env bash

dir="$(dirname -- "$0")"

./gradlew \
  src:converter:openapi:jvmJar \
  src:converter:avro:jvmJar \
  src:plugin:arguments:jvmJar \
  src:plugin:npm:jsNodeProductionLibraryDistribution \
  src:plugin:gradle:publishToMavenLocal \
  src:plugin:maven:publishToMavenLocal &&
(cd "$dir"/../src/ide/vscode && npm i && npm run build) &&
(cd "$dir"/../examples && make clean && make build)
