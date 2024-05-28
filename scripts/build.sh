#!/usr/bin/env bash

dir="$(dirname -- "$0")"

./gradlew build --no-configuration-cache &&
  cd "$dir"/../src/ide/vscode && npm i && npm run build
