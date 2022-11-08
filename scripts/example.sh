#!/usr/bin/env bash

dir="$(dirname -- "$0")"

# publish and build gradle example
./gradlew plugin:gradle:publishToMavenLocal
(cd "$dir"/../examples/spring-boot-gradle-plugin; ./gradlew wirespec build)

# publish and build maven example
./gradlew plugin:maven:publishToMavenLocal
(cd "$dir"/../examples/spring-boot-maven-plugin; ./mvnw package)