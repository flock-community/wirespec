#!/usr/bin/env bash

dir="$(dirname -- "$0")"

# publish and build gradle example
./gradlew src:plugin:gradle:publishToMavenLocal &&
  (cd "$dir"/../examples/wirespec-gradle-plugin-ktor-kotlin && ./gradlew build) &&

# publish and build maven example
./gradlew src:plugin:maven:publishToMavenLocal &&
  (cd "$dir"/../examples/spring-boot-maven-plugin && ./mvnw package) &&

# publish and build maven example
./gradlew src:plugin:maven:publishToMavenLocal &&
  (cd "$dir"/../examples/spring-boot-custom-maven-plugin && ./mvnw package) &&

# publish and build maven example
./gradlew src:plugin:maven:publishToMavenLocal &&
  (cd "$dir"/../examples/spring-boot-openapi-maven-plugin && ./mvnw package)
