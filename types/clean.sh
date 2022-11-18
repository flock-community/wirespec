#!/usr/bin/env bash

dir="$(dirname -- "$0")"

rm -r "$dir"/out && mkdir -p "$dir"/out && (cd "$dir"/out && printf "*\n*/\n!.gitignore\n" > .gitignore)
