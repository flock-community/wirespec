#!/usr/bin/env bash

artifactName="cli"
version="0.0.1-SNAPSHOT"

build_mac=false

if [ "$WIRE_SPEC_BUILD_ALL" = true ] || [ "$WIRE_SPEC_BUILD_MAC" = true ]; then
  build_mac=true
fi

if [ $build_mac = true ]; then
  echo "Test macOS artifact"
  ./compiler/$artifactName/build/bin/macosX64/releaseExecutable/$artifactName.kexe "$(pwd)" Kotlin,TypeScript
fi

echo "Test Node.js artifact"
node build/js/packages/wire-spec-$artifactName/kotlin/wire-spec-$artifactName.js "$(pwd)" Kotlin,TypeScript

echo "Test JVM artifact"
java -jar compiler/$artifactName/build/libs/$artifactName-$version-all.jar "$(pwd)" Kotlin,TypeScript
