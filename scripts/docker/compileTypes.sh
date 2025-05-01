#!/usr/bin/env bash

# Bash script meant for Docker Image built by Dockerfile

source /root/.bashrc

combinedOutputDir="/app/types/out/combined/"

inputDirs=("openapi/petstore" "wirespec")

printf "\nCompiling Kotlin Classes:\n"
for inputDir in "${inputDirs[@]}"; do
  dir="$combinedOutputDir$inputDir/kotlin"
  find "$dir" -name '*.kt' -print0 | xargs -0 kotlinc -d "$dir/wirespec.jar"
done

printf "\nCompiling Java Classes:\n"
for inputDir in "${inputDirs[@]}"; do
  dir="$combinedOutputDir$inputDir/java"
  find "$dir" -name '*.java' -print0 | xargs -0 javac -d "$dir/target"
  eval "jar cvf $dir/wirespec.jar $dir/target/*"
done

printf "\nType checking Python files:\n"
for inputDir in "${inputDirs[@]}"; do
  dir="$combinedOutputDir$inputDir/python"
  find "$dir" -name '*.py' -print0 | xargs -0 python3 -m mypy
done

printf "\nType checking TypeScript files:\n"
for inputDir in "${inputDirs[@]}"; do
  dir="$combinedOutputDir$inputDir/typescript"
  find "$dir" -name '*.ts' -print0 | xargs -0 tsc --noEmit
done
