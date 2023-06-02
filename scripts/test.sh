#!/usr/bin/env bash

artifactName="cli"
version="0.0.0-SNAPSHOT"
languages="Java,Kotlin,Scala,TypeScript"

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
  ./compiler/$artifactName/build/bin/$macosArch/releaseExecutable/$artifactName.kexe "$(pwd)"/types $languages
fi

if [[ $WIRESPEC_BUILD_ALL = true || $WIRESPEC_BUILD_LINUX = true || $buildNothing = true ]]; then
  echo "Test docker image"
  docker run $archSpecific --rm -it -v "$(pwd)"/types:/app/types wirespec
fi

echo "Test Node.js artifact"
node build/js/packages/wirespec-$artifactName/kotlin/wirespec-$artifactName.js "$(pwd)"/types $languages

echo "Test JVM artifact"
java -jar compiler/$artifactName/build/libs/$artifactName-$version-all.jar "$(pwd)"/types $languages
