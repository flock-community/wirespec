#!/usr/bin/env bash

artifactName="cli"
version="0.0.1-SNAPSHOT"

buildNothing=false

if [ "$WIRE_SPEC_BUILD_MAC" != true ] && [ "$WIRE_SPEC_BUILD_WINDOWS" != true ]; then
  buildNothing=true
fi

if [ "$WIRE_SPEC_BUILD_ALL" = true ] || [ "$WIRE_SPEC_BUILD_MAC" = true ]; then
  echo "Test macOS artifact"
  ./compiler/$artifactName/build/bin/macosX64/releaseExecutable/$artifactName.kexe "$(pwd)" Kotlin,TypeScript
fi

if [ "$WIRE_SPEC_BUILD_ALL" = true ] || [ "$WIRE_SPEC_BUILD_LINUX" = true ] || [ "$buildNothing" = true ]; then
  echo "Test docker image"
  docker run --rm -it -v "$(pwd)"/types:/app/types wire-spec
fi

echo "Test Node.js artifact"
node build/js/packages/wire-spec-$artifactName/kotlin/wire-spec-$artifactName.js "$(pwd)" Kotlin,TypeScript

echo "Test JVM artifact"
java -jar compiler/$artifactName/build/libs/$artifactName-$version-all.jar "$(pwd)" Kotlin,TypeScript
