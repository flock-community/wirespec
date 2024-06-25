#!/usr/bin/env bash

dir="$(dirname -- "$0")"

# publish and build gradle example
./gradlew src:plugin:gradle:publishToMavenLocal --no-configuration-cache &&
  ./gradlew src:plugin:maven:publishToMavenLocal --no-configuration-cache &&
  # build examples
  (cd "$dir"/../examples/wirespec-gradle-plugin-ktor && ./gradlew clean build) &&
  (cd "$dir"/../examples/spring-boot-maven-plugin && ./mvnw clean package) &&
  (cd "$dir"/../examples/spring-boot-custom-maven-plugin && ./mvnw clean package) &&
  (cd "$dir"/../examples/spring-boot-openapi-maven-plugin && ./mvnw clean package)
