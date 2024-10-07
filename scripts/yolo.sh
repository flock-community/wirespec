#!/usr/bin/env bash

dir="$(dirname -- "$0")"

./gradlew jvmTest &&
  (cd "$dir"/../src/ide/vscode && npm i && npm run build) &&
  "$dir"/example.sh