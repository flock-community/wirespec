#!/usr/bin/env bash

dir="$(dirname -- "$0")"

# publish and build gradle example
./gradlew src:plugin:gradle:publishToMavenLocal --no-configuration-cache &&
  ./gradlew src:plugin:maven:publishToMavenLocal --no-configuration-cache &&
  # build examples
  (cd "$dir"/../examples/wirespec-gradle-plugin-ktor-custom && ./gradlew build --no-configuration-cache) &&
  (cd "$dir"/../examples/wirespec-gradle-plugin-ktor-kotlin && ./gradlew build --no-configuration-cache) &&
  (cd "$dir"/../examples/spring-boot-maven-plugin && ./mvnw package) &&
  (cd "$dir"/../examples/spring-boot-custom-maven-plugin && ./mvnw package) &&
  (cd "$dir"/../examples/spring-boot-openapi-maven-plugin && ./mvnw package)
