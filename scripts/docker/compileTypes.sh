#!/usr/bin/env bash

# Bash script meant for Docker Image built by Dockerfile

source /root/.bashrc

combinedOutputDir="/app/types/out/combined/"

inputDirs=("openapi/petstore" "wirespec")

done="Done!"

echo -n "Compiling Java Classes: "
for inputDir in "${inputDirs[@]}"; do
  dir="$combinedOutputDir$inputDir/java"
  find "$dir" -name '*.java' -print0 | xargs -0 javac -d "$dir/target"
done
echo "$done"

echo -n "Compiling Kotlin Classes: "
for inputDir in "${inputDirs[@]}"; do
  dir="$combinedOutputDir$inputDir/kotlin"
  find "$dir" -name '*.kt' -print0 | xargs -0 kotlinc -d "$dir/target"
done
echo "$done"

echo -n "Type checking Python files: "
for inputDir in "${inputDirs[@]}"; do
  dir="$combinedOutputDir$inputDir/python"
  find "$dir" -name '*.py' -print0 | xargs -0 python3 -m mypy
done
echo "$done"

echo -n "Type checking TypeScript files: "
for inputDir in "${inputDirs[@]}"; do
  dir="$combinedOutputDir$inputDir/typescript"
  find "$dir" -name '*.ts' -print0 | xargs -0 tsc --noEmit
done
echo "$done"
