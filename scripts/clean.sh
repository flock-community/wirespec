#!/usr/bin/env bash

dir="$(dirname -- "$0")"
root="$dir/.."

./gradlew clean &&
  (cd "$root"/src/lsp/node/client && npm run clean) &&
  (cd "$root"/src/lsp/node/server && npm run clean) &&
  (cd "$root"/examples/spring-boot-gradle-plugin && ./gradlew clean) &&
  (cd "$root"/examples/spring-boot-maven-plugin && ./mvnw clean) &&
  (cd "$root"/examples/spring-boot-openapi-maven-plugin && ./mvnw clean) &&
  (cd "$root"/types && ./clean.sh) &&
  (cd "$root"/src/test && ./clean.sh) &&
  (cd "$root"/test && ./clean.sh) &&
  docker rmi wirespec
