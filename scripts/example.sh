#!/usr/bin/env bash

dir="$(dirname -- "$0")"

./gradlew jvmTest &&
  ./gradlew src:plugin:gradle:publishToMavenLocal &&
  ./gradlew src:plugin:maven:publishToMavenLocal &&
  (cd "$dir"/../src/ide/vscode && npm i && npm run build) &&
  (cd "$dir"/../examples && make clean && make build)
