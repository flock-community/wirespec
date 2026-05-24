#!/usr/bin/env bash
#
# Dev loop: build the npm library, the LSP server, and the VS Code extension,
# then launch a VS Code instance with the extension loaded from source.
#
# Usage:
#   src/ide/vscode/run.sh           # build everything and launch VS Code
#   src/ide/vscode/run.sh --no-build  # skip builds, just relaunch VS Code

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
VSCODE_DIR="${REPO_ROOT}/src/ide/vscode"

if [[ "${1:-}" != "--no-build" ]]; then
  echo "==> Building @flock/wirespec (Kotlin/JS library, ships the LSP bin)"
  (cd "${REPO_ROOT}" && ./gradlew :src:plugin:npm:jsNodeProductionLibraryDistribution)

  echo "==> Building VS Code extension"
  (cd "${VSCODE_DIR}" && npm install && npm run esbuild)
fi

if command -v code >/dev/null 2>&1; then
  CODE_BIN="code"
elif [[ -x "/Applications/Visual Studio Code.app/Contents/Resources/app/bin/code" ]]; then
  CODE_BIN="/Applications/Visual Studio Code.app/Contents/Resources/app/bin/code"
else
  echo "ERROR: 'code' CLI not found." >&2
  echo "Install it from VS Code: Command Palette → 'Shell Command: Install \"code\" command in PATH'." >&2
  exit 1
fi

echo "==> Launching VS Code with extension"
exec "${CODE_BIN}" \
  --extensionDevelopmentPath="${VSCODE_DIR}" \
  "${VSCODE_DIR}/example.ws"
