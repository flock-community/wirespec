#!/usr/bin/env bash

dir="$(dirname -- "$0")"

./gradlew build &&
  cd "$dir"/../lsp/node/server && npm i && npm run build &&
  cd "$dir"/../lsp/node/client && npm i && npm run build
