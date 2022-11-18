#!/usr/bin/env bash

dir="$(dirname -- "$0")"
root="$dir/.."

./gradlew clean &&
  (cd "$root"/lsp/node/client && npm run clean) &&
  (cd "$root"/lsp/node/server && npm run clean) &&
  (cd "$root"/examples/spring-boot-gradle-plugin && ./gradlew clean) &&
  (cd "$root"/examples/spring-boot-maven-plugin && ./mvnw clean) &&
  (cd "$root"/types && ./clean.sh) &&
  docker rmi wirespec
