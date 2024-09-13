#!/usr/bin/env bash

dir="$(dirname -- "$0")"

# publish and build gradle example
./gradlew src:plugin:gradle:publishToMavenLocal &&
  ./gradlew src:plugin:maven:publishToMavenLocal &&
  # build examples
  (cd "$dir"/../examples/wirespec-gradle-plugin-ktor && make build) &&
  (cd "$dir"/../examples/spring-boot-maven-plugin && make build) &&
  (cd "$dir"/../examples/spring-boot-custom-maven-plugin &&  make build) &&
  (cd "$dir"/../examples/spring-boot-openapi-maven-plugin && make build) &&
  (cd "$dir"/../examples/npm-expres && make build)

