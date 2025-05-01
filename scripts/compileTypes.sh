#!/usr/bin/env bash

dir="$(dirname -- "$0")"
root="$dir/.."

output="$root/types/out"

archSpecific=""
if [[ $(uname -m) = arm64 ]]; then
  archSpecific="--platform=linux/amd64"
fi

# Compare output directories for same content and copy one of them to a 'combined' dir.
# Then that combined directory serves as a single input for the type checkers.
diff -qr "$output/docker/" "$output/jvm/" --exclude='*.jar' && \
diff -qr "$output/jvm/" "$output/native/" --exclude='*.jar' && \
diff -qr "$output/native/" "$output/node/" --exclude='*.jar' && \
cp -r "$output/jvm/." "$output/combined" && \
docker run $archSpecific --rm -it -v ./types/:/app/types wirespec /app/compileTypes.sh
