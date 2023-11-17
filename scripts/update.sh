#!/usr/bin/env bash

dir="$(dirname -- "$0")"
root="$dir/.."

cd "$root" && npm install -g @vscode/vsce
