#!/usr/bin/env bash

dir="$(dirname -- "$0")"
root="$dir/.."

./gradlew clean &&
  (cd "$root" && rm -rf kotlin-js-store) &&
  (cd "$root"/src/ide/vscode && npm run clean) &&
  (cd "$root"/examples/npm-typescript && make clean) &&
  (cd "$root"/examples/spring-boot-custom-maven-plugin && ./mvnw clean) &&
  (cd "$root"/examples/spring-boot-maven-plugin && ./mvnw clean) &&
  (cd "$root"/examples/spring-boot-openapi-maven-plugin && ./mvnw clean) &&
  (cd "$root"/examples/wirespec-gradle-plugin-ktor && ./gradlew clean) &&
  (cd "$root"/types && ./clean.sh) &&
  (cd "$root"/src/test && ./clean.sh) &&
  docker rmi wirespec
