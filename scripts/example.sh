#!/usr/bin/env bash

dir="$(dirname -- "$0")"

./gradlew jvmTest &&
  ./gradlew src:integration:wirespec:publishToMavenLocal &&
  ./gradlew src:integration:jackson:publishToMavenLocal &&
  ./gradlew src:integration:spring:publishToMavenLocal &&
  ./gradlew src:plugin:gradle:publishToMavenLocal &&
  ./gradlew src:plugin:maven:publishToMavenLocal &&
  (cd "$dir"/../src/ide/vscode && npm i && npm run build) &&
  (cd "$dir"/../examples && make clean && make build)
