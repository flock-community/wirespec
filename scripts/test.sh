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
  macWirespec=./src/plugin/$artifactName/build/bin/$macosArch/releaseExecutable/$artifactName.kexe
  "$macWirespec" compile -d "$(pwd)"/types -l Java -p "$compilePackage" -o "$(pwd)"/types/out/native/java
  "$macWirespec" compile -d "$(pwd)"/types -l Kotlin -p "$compilePackage" -o "$(pwd)"/types/out/native/kotlin
  "$macWirespec" compile -d "$(pwd)"/types -l Scala -p "$compilePackage" -o "$(pwd)"/types/out/native/scala
  "$macWirespec" compile -d "$(pwd)"/types -l TypeScript -p "$compilePackage" -o "$(pwd)"/types/out/native/typescript
  "$macWirespec" compile -d "$(pwd)"/types -l Wirespec -p "$compilePackage" -o "$(pwd)"/types/out/native/wirespec
  "$macWirespec" convert -f "$(pwd)"/types/petstore.json openapiv2 -l Java -p "$convertPackage" -o "$(pwd)"/types/out/native/java
  "$macWirespec" convert -f "$(pwd)"/types/petstore.json openapiv2 -l Kotlin -p "$convertPackage" -o "$(pwd)"/types/out/native/kotlin
  "$macWirespec" convert -f "$(pwd)"/types/petstore.json openapiv2 -l Scala -p "$convertPackage" -o "$(pwd)"/types/out/native/scala
  "$macWirespec" convert -f "$(pwd)"/types/petstore.json openapiv2 -l TypeScript -p "$convertPackage" -o "$(pwd)"/types/out/native/typescript
  "$macWirespec" convert -f "$(pwd)"/types/petstore.json openapiv2 -l Wirespec -p "$convertPackage" -o "$(pwd)"/types/out/native/wirespec
fi

if [[ $WIRESPEC_BUILD_ALL = true || $WIRESPEC_BUILD_JVM = true || $buildNothing = true ]]; then
  echo "Test JVM artifact"
  wirespecJar=src/plugin/$artifactName/build/libs/$artifactName-$version-all.jar
  java -jar "$wirespecJar" compile -d "$(pwd)"/types -l Java -p "$compilePackage" -o "$(pwd)"/types/out/jvm/java
  java -jar "$wirespecJar" compile -d "$(pwd)"/types -l Kotlin -p "$compilePackage" -o "$(pwd)"/types/out/jvm/kotlin
  java -jar "$wirespecJar" compile -d "$(pwd)"/types -l Scala -p "$compilePackage" -o "$(pwd)"/types/out/jvm/scala
  java -jar "$wirespecJar" compile -d "$(pwd)"/types -l TypeScript -p "$compilePackage" -o "$(pwd)"/types/out/jvm/typescript
  java -jar "$wirespecJar" compile -d "$(pwd)"/types -l Wirespec -p "$compilePackage" -o "$(pwd)"/types/out/jvm/wirespec
  java -jar "$wirespecJar" convert -f "$(pwd)"/types/petstore.json openapiv2 -l Java -p "$convertPackage" -o "$(pwd)"/types/out/jvm/java
  java -jar "$wirespecJar" convert -f "$(pwd)"/types/petstore.json openapiv2 -l Kotlin -p "$convertPackage" -o "$(pwd)"/types/out/jvm/kotlin
  java -jar "$wirespecJar" convert -f "$(pwd)"/types/petstore.json openapiv2 -l Scala -p "$convertPackage" -o "$(pwd)"/types/out/jvm/scala
  java -jar "$wirespecJar" convert -f "$(pwd)"/types/petstore.json openapiv2 -l TypeScript -p "$convertPackage" -o "$(pwd)"/types/out/jvm/typescript
  java -jar "$wirespecJar" convert -f "$(pwd)"/types/petstore.json openapiv2 -l Wirespec -p "$convertPackage" -o "$(pwd)"/types/out/jvm/wirespec
fi

echo "Test Node.js artifact"
wirespecJs=build/js/packages/wirespec-src-plugin-$artifactName/kotlin/wirespec-src-plugin-$artifactName.js
node "$wirespecJs" compile -d "$(pwd)"/types -l Java -p "$compilePackage" -o "$(pwd)"/types/out/java
node "$wirespecJs" compile -d "$(pwd)"/types -l Kotlin -p "$compilePackage" -o "$(pwd)"/types/out/kotlin
node "$wirespecJs" compile -d "$(pwd)"/types -l Scala -p "$compilePackage" -o "$(pwd)"/types/out/scala
node "$wirespecJs" compile -d "$(pwd)"/types -l TypeScript -p "$compilePackage" -o "$(pwd)"/types/out/typescript
node "$wirespecJs" compile -d "$(pwd)"/types -l Wirespec -p "$compilePackage" -o "$(pwd)"/types/out/wirespec
node "$wirespecJs" convert -f "$(pwd)"/types/petstore.json openapiv2 -l Java -p "$convertPackage" -o "$(pwd)"/types/out/java
node "$wirespecJs" convert -f "$(pwd)"/types/petstore.json openapiv2 -l Kotlin -p "$convertPackage" -o "$(pwd)"/types/out/kotlin
node "$wirespecJs" convert -f "$(pwd)"/types/petstore.json openapiv2 -l Scala -p "$convertPackage" -o "$(pwd)"/types/out/scala
node "$wirespecJs" convert -f "$(pwd)"/types/petstore.json openapiv2 -l TypeScript -p "$convertPackage" -o "$(pwd)"/types/out/typescript
node "$wirespecJs" convert -f "$(pwd)"/types/petstore.json openapiv2 -l Wirespec -p "$convertPackage" -o "$(pwd)"/types/out/wirespec

if [[ $WIRESPEC_BUILD_ALL = true || $WIRESPEC_BUILD_LINUX = true ]]; then
  echo "Test docker image"
  docker run $archSpecific --rm -it -v "$(pwd)"/types:/app/types wirespec
fi
