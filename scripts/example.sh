
./gradlew \
  --no-configuration-cache \
  publishToMavenLocal \
  src:plugin:npm:jsNodeProductionLibraryDistribution &&
./gradlew buildExamples
