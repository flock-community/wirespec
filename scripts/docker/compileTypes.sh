#!/usr/bin/env bash

# Bash script meant for Docker Image built by Dockerfile

source /root/.bashrc

compiledPackageDir="community/flock/generated"
sharedPackageDir="community/flock/wirespec"

platforms=("docker" "jvm" "native" "node")
inputDirs=("openapi/petstore" "wirespec")

#printf "\nCompiling Kotlin Classes:\n"
#for platform in "${platforms[@]}"; do
#  for inputDir in "${inputDirs[@]}"; do
#    dir="/app/types/out/$platform/$inputDir/kotlin"
#    eval "kotlinc $dir/$compiledPackageDir/model/*.kt \
#     $dir/$compiledPackageDir/endpoint/*.kt \
#     $dir/$sharedPackageDir/*.kt \
#     -d $dir/wirespec.jar"
#  done
#done

printf "\nCompiling Java Classes:\n"
for platform in "${platforms[@]}"; do
  for inputDir in "${inputDirs[@]}"; do
    dir="/app/types/out/$platform/$inputDir/java"
    eval "javac $dir/$compiledPackageDir/model/*.java \
     $dir/$compiledPackageDir/endpoint/*.java \
     $dir/$sharedPackageDir/*.java \
     -d $dir/wirespec.jar"
  done
done

#printf "\nCompiling Scala Classes:\n"
#for platform in "${platforms[@]}"; do
#  for inputDir in "${inputDirs[@]}"; do
#    dir="/app/types/out/$platform/$inputDir/scala"
#    eval "scalac $dir/$compiledPackageDir/model/*.scala \
#     $dir/$compiledPackageDir/endpoint/*.scala \
#     $dir/$sharedPackageDir/*.scala \
#     -d $dir/wirespec.jar"
#  done
#done
#
#printf "\nCompiling TypeScript Classes:\n"
#for platform in "${platforms[@]}"; do
#  for inputDir in "${inputDirs[@]}"; do
#    dir="/app/types/out/$platform/$inputDir/typescript"
#    eval "tsc --noEmit $dir/$compiledPackageDir/*.ts \
#     $dir/$sharedPackageDir/typescript/*.ts"
#  done
#done
