#!/usr/bin/env bash

dir="$(dirname -- "$0")"

./gradlew clean &&
  (cd ./examples/spring-boot-gradle-plugin; ./gradlew clean) &&
  (cd ./examples/spring-boot-maven-plugin; ./mvnw clean) &&
  (cd ./lsp/node/client; npm run clean) &&
  (cd ./lsp/node/server; npm run clean) &&
  docker rmi wire-spec
