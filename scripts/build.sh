#!/usr/bin/env bash

dir="$(dirname -- "$0")"

./gradlew build --no-configuration-cache &&
  cd "$dir"/../src/lsp/node/server && npm i && npm run build &&
  cd "$dir"/../src/lsp/node/client && npm i && npm run build
