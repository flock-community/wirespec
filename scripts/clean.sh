#!/usr/bin/env bash

dir="$(dirname -- "$0")"

./gradlew clean &&
  cd "$dir"/../lsp/node/client && npm run clean &&
  cd "$dir"/../lsp/node/server && npm run clean &&
  docker rmi wire-spec
