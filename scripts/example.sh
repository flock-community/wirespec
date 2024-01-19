#!/usr/bin/env bash

dir="$(dirname -- "$0")"

./gradlew src:converter:avro:publishToMavenLocal \
  src:compiler:core:publishToMavenLocal \
  src:integration:avro:publishToMavenLocal \
  src:integration:wirespec:publishToMavenLocal \
  src:integration:jackson:publishToMavenLocal \
  src:integration:spring:publishToMavenLocal \
  src:plugin:gradle:publishToMavenLocal \
  src:plugin:maven:publishToMavenLocal &&
(cd "$dir"/../src/ide/vscode && npm i && npm run build) &&
(cd "$dir"/../examples && make clean && make build)
