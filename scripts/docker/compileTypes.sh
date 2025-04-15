#!/usr/bin/env bash

# Bash script meant for Docker Image built by Dockerfile

source /root/.bashrc

compiledPackageDir="community/flock/generated"
sharedPackageDir="community/flock/wirespec"

platforms=("docker" "jvm" "native" "node")
inputDirs=("openapi/petstore" "wirespec")

printf "\nCompiling Kotlin Classes:\n"
for platform in "${platforms[@]}"; do
  for inputDir in "${inputDirs[@]}"; do
    dir="/app/types/out/$platform/$inputDir/kotlin"
    eval "kotlinc $dir/$compiledPackageDir/*.kt \
     $dir/$sharedPackageDir/kotlin/*.kt \
     -d $dir/wirespec.jar"
  done
done

printf "\nCompiling Java Classes:\n"
for platform in "${platforms[@]}"; do
  for inputDir in "${inputDirs[@]}"; do
    dir="/app/types/out/$platform/$inputDir/java"
    eval "javac $dir/$compiledPackageDir/*.java \
     $dir/$sharedPackageDir/java/*.java \
     -d $dir/wirespec.jar"
  done
done

printf "\nCompiling Scala Classes:\n"
for platform in "${platforms[@]}"; do
  for inputDir in "${inputDirs[@]}"; do
    dir="/app/types/out/$platform/$inputDir/scala"
    eval "scalac $dir/$compiledPackageDir/*.scala \
     $dir/$sharedPackageDir/scala/*.scala \
     -d $dir/wirespec.jar"
  done
done

printf "\nCompiling TypeScript Classes:\n"
for platform in "${platforms[@]}"; do
  for inputDir in "${inputDirs[@]}"; do
    dir="/app/types/out/$platform/$inputDir/typescript"
    eval "tsc --noEmit $dir/$compiledPackageDir/*.ts \
     $dir/$sharedPackageDir/typescript/*.ts"
  done
done
