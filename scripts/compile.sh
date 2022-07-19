#!/usr/bin/env bash

dir="$( dirname -- "$0"; )"

./gradlew build &&
  cd "$dir"/../lsp/node/client && npm i && npm run compile &&
  cd "$dir"/../lsp/node/server && npm i
