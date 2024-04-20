#!/usr/bin/env bash

dir="$(dirname -- "$0")"
root="$dir/.."

./gradlew clean &&
  (cd "$root" && rm -rf kotlin-js-store) &&
  (cd "$root"/src/lsp/node/client && npm run clean) &&
  (cd "$root"/src/lsp/node/server && npm run clean) &&
  (cd "$root"/examples/spring-boot-custom-maven-plugin && ./mvnw clean) &&
  (cd "$root"/examples/spring-boot-maven-plugin && ./mvnw clean) &&
  (cd "$root"/examples/spring-boot-openapi-maven-plugin && ./mvnw clean) &&
  (cd "$root"/examples/wirespec-gradle-plugin-ktor-kotlin && ./gradlew clean) &&
  (cd "$root"/types && ./clean.sh) &&
  (cd "$root"/src/test && ./clean.sh) &&
  docker rmi wirespec
