#!/usr/bin/env bash

build_mac="$WIRE_SPEC_BUILD_MAC"
build_node="$WIRE_SPEC_BUILD_NODE"
build_jvm="$WIRE_SPEC_BUILD_JVM"

if [ "$WIRE_SPEC_BUILD_ALL" = true ]; then
  build_mac=true
  build_node=true
  build_jvm=true
fi

if [ $build_mac = true ]; then
  echo "Test macOS artifact"
  ./compiler/build/bin/macosX64/releaseExecutable/compiler.kexe "$(pwd)" Kotlin,TypeScript
fi

if [ $build_node = true ]; then
  echo "Test Node.js artifact"
  node build/js/packages/wire-spec-compiler/kotlin/wire-spec-compiler.js "$(pwd)" Kotlin,TypeScript
fi

if [ $build_jvm = true ]; then
  echo "Test JVM artifact"
  java -jar compiler/build/libs/compiler-0.0.1-SNAPSHOT-all.jar "$(pwd)" Kotlin,TypeScript
fi
