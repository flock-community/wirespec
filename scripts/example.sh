dir="$(dirname -- "$0")"

./gradlew \
  src:converter:openapi:jvmJar \
  src:plugin:arguments:jvmJar \
  src:integration:jackson:publishToMavenLocal \
  src:integration:spring:publishToMavenLocal \
  src:plugin:gradle:publishToMavenLocal \
  src:plugin:maven:publishToMavenLocal \
  src:plugin:npm:jsNodeProductionLibraryDistribution &&
(cd "$dir"/../src/ide/vscode && npm i && npm run build) &&
(cd "$dir"/../examples && make clean && make build)