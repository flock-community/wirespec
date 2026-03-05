
dir="$(dirname -- "$0")"

./gradlew \
  --no-configuration-cache \
  publishToMavenLocal \
  src:plugin:npm:jsNodeProductionLibraryDistribution &&
(cd "$dir"/../src/ide/vscode && npm i && npm run build) &&
(cd "$dir"/../examples && make build)
