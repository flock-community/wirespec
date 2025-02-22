dir="$(dirname -- "$0")"

./gradlew \
  publishToMavenLocal \
  src:plugin:npm:jsNodeProductionLibraryDistribution &&
(cd "$dir"/../examples && make clean build)
