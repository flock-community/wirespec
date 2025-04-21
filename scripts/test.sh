#!/usr/bin/env bash

artifactName="cli"

macosArch=macosX64
archSpecific=""
if [[ $(uname -m) = arm64 ]]; then
  macosArch="macosArm64"
  archSpecific="--platform=linux/amd64"
fi

package="community.flock.generated"

languages=("Java" "Kotlin" "Scala" "TypeScript" "Wirespec")

localWorkDir=$(pwd)

function run() {
    local wirespec=$1
    local workDir=$2
    local platform=$3
    local language=$4
    local lang=$(echo "$language" | tr '[:upper:]' '[:lower:]')
    local compile="$wirespec compile -i $workDir/types/wirespec -l $language -p $package -o $workDir/types/out/$platform/wirespec/$lang --shared"
    local convert="$wirespec convert -i $workDir/types/openapi/petstore.json openapiv2 -l $language -p $package -o $workDir/types/out/$platform/openapi/petstore/$lang --shared"
    echo "$compile && $convert"
}

printf "\nTest macOS artifact:\n"
macWirespec=./src/plugin/$artifactName/build/bin/$macosArch/releaseExecutable/$artifactName.kexe
macCommand=""
for lang in "${languages[@]}"; do
   macCommand="$macCommand $(run $macWirespec "$localWorkDir" "native" "$lang") && "
done
eval "${macCommand%????}"

printf "\nTest JVM artifact:\n"
jvmWirespec=./src/plugin/$artifactName/build/libs/$artifactName-jvm-0.0.0-SNAPSHOT.jar
jvmCommand=""
for lang in "${languages[@]}"; do
  jvmCommand="$jvmCommand $(run "java -jar $jvmWirespec" "$localWorkDir" "jvm" "$lang") && "
done
eval "${jvmCommand%????}"

printf "\nTest Node.js artifact:\n"
nodeWirespec=build/js/packages/wirespec-src-plugin-$artifactName/kotlin/wirespec-src-plugin-$artifactName.js
nodeCommand=""
for lang in "${languages[@]}"; do
  nodeCommand="$nodeCommand $(run "node $nodeWirespec" "$localWorkDir" "node" "$lang") && "
done
eval "${nodeCommand%????}"

printf "\nTest docker image:\n"
dockerWirespec=/app/wirespec
dockerCommand=""
for lang in "${languages[@]}"; do
  dockerCommand="$dockerCommand $(run "$dockerWirespec" '/app' 'docker' "$lang") && "
done
docker run $archSpecific --rm -it -v "$localWorkDir"/types:/app/types wirespec "${dockerCommand%????}"
