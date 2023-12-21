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

if [[ $WIRESPEC_BUILD_JVM != true ]]; then
  buildNothing=true
fi

compilePackage="community.flock.wirespec.generated"
convertPackage="community.flock.openapi.generated"

if [[ $WIRESPEC_BUILD_ALL = true || $WIRESPEC_BUILD_MAC = true ]]; then
  echo "Test macOS artifact"
  macWirespec=./src/compiler/$artifactName/build/bin/$macosArch/releaseExecutable/$artifactName.kexe
  "$macWirespec" compile "$(pwd)"/types -l Java -l Kotlin -l Scala -l TypeScript -l Wirespec -p "$compilePackage" -o "$(pwd)"/types/out/native
  "$macWirespec" convert "$(pwd)"/types/petstore.json openapiv2 -l Java -l Kotlin -l Scala -l TypeScript -l Wirespec -p "$convertPackage" -o "$(pwd)"/types/out/native
fi

if [[ $WIRESPEC_BUILD_ALL = true || $WIRESPEC_BUILD_JVM = true || $buildNothing = true ]]; then
  echo "Test JVM artifact"
  wirespecJar=src/compiler/$artifactName/build/libs/$artifactName-$version-all.jar
  java -jar "$wirespecJar" compile "$(pwd)"/types -l Java -l Kotlin -l Scala -l TypeScript -l Wirespec -p "$compilePackage" -o "$(pwd)"/types/out/jvm
  java -jar "$wirespecJar" convert "$(pwd)"/types/petstore.json openapiv2 -l Java -l Kotlin -l Scala -l TypeScript -l Wirespec -p "$convertPackage" -o "$(pwd)"/types/out/jvm
fi

echo "Test Node.js artifact"
wirespecJs=build/js/packages/wirespec-src-compiler-$artifactName/kotlin/wirespec-src-compiler-$artifactName.js
node "$wirespecJs" compile "$(pwd)"/types -l Java -l Kotlin -l Scala -l TypeScript -l Wirespec -p "$compilePackage" -o "$(pwd)"/types/out/node
node "$wirespecJs" convert "$(pwd)"/types/petstore.json openapiv2 -l Java -l Kotlin -l Scala -l TypeScript -l Wirespec -p "$convertPackage" -o "$(pwd)"/types/out/node

if [[ $WIRESPEC_BUILD_ALL = true || $WIRESPEC_BUILD_LINUX = true ]]; then
  echo "Test docker image"
  docker run $archSpecific --rm -it -v "$(pwd)"/types:/app/types wirespec
fi
