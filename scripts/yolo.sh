#!/usr/bin/env bash

dir="$(dirname -- "$0")"

./gradlew src:bom:build compileKotlinJvm -x test &&
  ./gradlew publishToMavenLocal &&
  (cd "$dir"/../examples && make yolo)

