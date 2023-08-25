#!/usr/bin/env bash

artifactName="cli"
version="0.0.0-SNAPSHOT"

buildNothing=false

macosArch=macosX64
archSpecific=""
if [[ $(uname -m) = arm64 ]]; then
  macosArch="macosArm64"
  archSpecific="--platform=linux/amd64"
fi

if [[ $WIRESPEC_BUILD_MAC != true ]]; then
  buildNothing=true
fi

if [[ $WIRESPEC_BUILD_ALL = true || $WIRESPEC_BUILD_MAC = true ]]; then
  echo "Test macOS artifact"
  ./src/compiler/$artifactName/build/bin/$macosArch/releaseExecutable/$artifactName.kexe -l Java -l Kotlin -l Scala -l TypeScript -l Wirespec -p "community.flock.wirespec.generated" "$(pwd)"/types
  ./src/compiler/$artifactName/build/bin/$macosArch/releaseExecutable/$artifactName.kexe -l Java -l Kotlin -l Scala -l TypeScript -l Wirespec -p "community.flock.openapi.generated" -a v2 "$(pwd)"/types/petstore.json
fi

if [[ $WIRESPEC_BUILD_ALL = true || $WIRESPEC_BUILD_LINUX = true || $buildNothing = true ]]; then
  echo "Test docker image"
  docker run $archSpecific --rm -it -v "$(pwd)"/types:/app/types wirespec
fi

echo "Test Node.js artifact"
node build/js/packages/wirespec-$artifactName/kotlin/wirespec-$artifactName.js -l Java -l Kotlin -l Scala -l TypeScript -l Wirespec -p "community.flock.wirespec.generated" "$(pwd)"/types
node build/js/packages/wirespec-$artifactName/kotlin/wirespec-$artifactName.js -l Java -l Kotlin -l Scala -l TypeScript -l Wirespec -p "community.flock.openapi.generated" -a v2 "$(pwd)"/types/petstore.json

echo "Test JVM artifact"
java -jar src/compiler/$artifactName/build/libs/$artifactName-$version-all.jar -l Java -l Kotlin -l Scala -l TypeScript -l Wirespec -p "community.flock.wirespec.generated" "$(pwd)"/types
java -jar src/compiler/$artifactName/build/libs/$artifactName-$version-all.jar -l Java -l Kotlin -l Scala -l TypeScript -l Wirespec -p "community.flock.openapi.generated" -a v2 "$(pwd)"/types/petstore.json
