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
    find "$dir" -name '*.kt' -print0 | xargs -0 kotlinc -d "$dir/wirespec.jar"
  done
done

printf "\nCompiling Java Classes:\n"
for platform in "${platforms[@]}"; do
  for inputDir in "${inputDirs[@]}"; do
    dir="/app/types/out/$platform/$inputDir/java"
    find "$dir" -name '*.java' -print0 | xargs -0 javac -d "$dir/target"
    eval "jar cvf $dir/wirespec.jar $dir/target/*"
  done
done

printf "\nType checking Python files:\n"
for platform in "${platforms[@]}"; do
  for inputDir in "${inputDirs[@]}"; do
    dir="/app/types/out/$platform/$inputDir/python"
    find "$dir" -name '*.py' -print0 | xargs -0 python3 -m mypy
  done
done

printf "\nType checking TypeScript files:\n"
for platform in "${platforms[@]}"; do
  for inputDir in "${inputDirs[@]}"; do
    dir="/app/types/out/$platform/$inputDir/typescript"
    find "$dir" -name '*.ts' -print0 | xargs -0 tsc --noEmit
  done
done
