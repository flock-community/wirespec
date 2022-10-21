#!/usr/bin/env bash

# publish and build gradle example
./gradlew plugin:maven:publishToMavenLocal
(cd examples/spring-boot-gradle-plugin; ./gradlew wirespec build)

# publish and build maven example
./gradlew plugin:gradle:publishToMavenLocal
(cd examples/spring-boot-maven-plugin; ./mvnw package)