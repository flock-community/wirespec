#!/usr/bin/env bash

dir="$(dirname -- "$0")"

./gradlew src:compiler:core:publishToMavenLocal &&
./gradlew src:integration:wirespec:publishToMavenLocal &&
./gradlew src:integration:jackson:publishToMavenLocal &&
./gradlew src:integration:spring:publishToMavenLocal &&
./gradlew jvmTest &&
./gradlew src:plugin:gradle:publishToMavenLocal &&
./gradlew src:plugin:maven:publishToMavenLocal &&
(cd "$dir"/../src/ide/vscode && npm i && npm run build) &&
(cd "$dir"/../examples && make clean && make build)
