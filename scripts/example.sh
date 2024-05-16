#!/usr/bin/env bash

dir="$(dirname -- "$0")"

# publish and build gradle example
./gradlew src:plugin:gradle:publishToMavenLocal &&
  ./gradlew src:plugin:maven:publishToMavenLocal &&
  # build examples
  (cd "$dir"/../examples/wirespec-gradle-plugin-ktor-kotlin && ./gradlew build) &&
  (cd "$dir"/../examples/spring-boot-maven-plugin && ./mvnw package) &&
  (cd "$dir"/../examples/spring-boot-custom-maven-plugin && ./mvnw package) &&
  (cd "$dir"/../examples/spring-boot-openapi-maven-plugin && ./mvnw package)
