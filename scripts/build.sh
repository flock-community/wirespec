#!/usr/bin/env bash

dir="$(dirname -- "$0")"

./gradlew build &&
  (cd "$dir"/../src/ide/vscode && npm i && npm run build) &&
  (cd "$dir"/../src/tools/docs && npm i && npm run build) &&
  (cd "$dir"/../src/tools/playground && npm i && npm run build)
