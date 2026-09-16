#!/usr/bin/env bash

dir="$(dirname -- "$0")"
root="$dir/.."

docker rmi wirespec

# Deletes build output only: installed dependencies (node_modules, the yarn lock in kotlin-js-store)
# stay, just like ~/.gradle and ~/.m2 do.
./gradlew clean cleanExamples &&
  (cd "$root"/src/ide/vscode && npm run clean:build) &&
  (cd "$root"/src/site && make clean) &&
  (cd "$root"/types && ./clean.sh) &&
  (cd "$root"/src/test && ./clean.sh)
