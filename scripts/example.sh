
dir="$(dirname -- "$0")"

UNAME="$(uname -s)_$(uname -m)"
case "$UNAME" in
  Linux_x86_64)  PLATFORM="linuxX64" ;;
  Darwin_arm64)  PLATFORM="macosArm64" ;;
  Darwin_x86_64) PLATFORM="macosX64" ;;
  *)             echo "Unsupported platform: $UNAME" >&2; exit 1 ;;
esac

./gradlew \
  --no-configuration-cache \
  "publishToMavenLocal" \
  ":src:plugin:cli:${PLATFORM}Binaries" \
  "src:plugin:npm:jsNodeProductionLibraryDistribution" \
  &&
(cd "$dir"/../src/ide/vscode && npm i && npm run build) &&
(cd "$dir"/../examples && make build)
