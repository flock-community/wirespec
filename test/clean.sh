#!/usr/bin/env bash

dir="$(dirname -- "$0")"

rm -r "$dir"/tmp && mkdir -p "$dir"/tmp && (cd "$dir"/tmp && printf "*\n*/\n!.gitignore\n" > .gitignore)
